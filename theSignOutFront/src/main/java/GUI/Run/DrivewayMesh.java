package GUI.Run;

import Data.Driveway.Driveway;
import Data.Driveway.DrivewaySegment;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.*;

public class DrivewayMesh {

    private static final double WAYPOINT_STEP_SIZE = 2.0;
    private static final double MIN_DIRECTION_LENGTH = 0.001;
    private static final double CURVE_RADIUS_MIN_FACTOR = 0.7;
    private static final double CURVE_RADIUS_RANGE = 0.6;
    private static final int MIN_CURVE_STEPS = 4;
    private static final int CAP_SEGMENTS = 12;

    public static List<double[]> buildWaypoints(Driveway driveway, double radius, Random random) {
        List<double[]> points = new ArrayList<>();
        double positionX = 0;
        double positionZ = 0;
        double heading = 0;
        double cumulativeDistance = 0;
        boolean turnLeft = true;

        for (int segmentIndex = 0; segmentIndex < driveway.segments.size(); segmentIndex++) {
            DrivewaySegment segment = driveway.segments.get(segmentIndex);
            double segmentLength = segment.end - segment.start;

            if (segmentIndex % 2 == 0) {
                int stepCount = Math.max(1, (int) (segmentLength / WAYPOINT_STEP_SIZE));
                double stepLength = segmentLength / stepCount;
                for (int step = 0; step < stepCount; step++) {
                    points.add(new double[]{positionX, 0, positionZ, cumulativeDistance});
                    positionX += Math.sin(heading) * stepLength;
                    positionZ += Math.cos(heading) * stepLength;
                    cumulativeDistance += stepLength;
                }
            } else {
                double curveRadius = radius * (CURVE_RADIUS_MIN_FACTOR + random.nextDouble() * CURVE_RADIUS_RANGE);
                double totalAngle = segmentLength / curveRadius;
                double turnSign = turnLeft ? -1 : 1;
                turnLeft = !turnLeft;
                int stepCount = Math.max(MIN_CURVE_STEPS, (int) (segmentLength / WAYPOINT_STEP_SIZE));
                double angleStep = totalAngle / stepCount;
                double distanceStep = segmentLength / stepCount;
                for (int step = 0; step < stepCount; step++) {
                    points.add(new double[]{positionX, 0, positionZ, cumulativeDistance});
                    heading += turnSign * angleStep;
                    positionX += Math.sin(heading) * distanceStep;
                    positionZ += Math.cos(heading) * distanceStep;
                    cumulativeDistance += distanceStep;
                }
            }
        }
        points.add(new double[]{positionX, 0, positionZ, cumulativeDistance});
        return points;
    }

    public static MeshView buildRoadMesh(List<double[]> waypoints, double roadWidth) {
        TriangleMesh mesh = new TriangleMesh();
        float halfWidth = (float) (roadWidth / 2);

        for (int waypointIndex = 0; waypointIndex < waypoints.size(); waypointIndex++) {
            double[] waypoint = waypoints.get(waypointIndex);
            double directionX;
            double directionZ;

            if (waypointIndex < waypoints.size() - 1) {
                double[] nextWaypoint = waypoints.get(waypointIndex + 1);
                directionX = nextWaypoint[0] - waypoint[0];
                directionZ = nextWaypoint[2] - waypoint[2];
            } else {
                double[] previousWaypoint = waypoints.get(waypointIndex - 1);
                directionX = waypoint[0] - previousWaypoint[0];
                directionZ = waypoint[2] - previousWaypoint[2];
            }

            double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
            if (directionLength < MIN_DIRECTION_LENGTH) {
                directionX = 0;
                directionZ = 1;
                directionLength = 1;
            }

            double normalX = -directionZ / directionLength;
            double normalZ = directionX / directionLength;

            mesh.getPoints().addAll(
                (float) (waypoint[0] + normalX * halfWidth), 0f, (float) (waypoint[2] + normalZ * halfWidth),
                (float) (waypoint[0] - normalX * halfWidth), 0f, (float) (waypoint[2] - normalZ * halfWidth));
        }

        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1, 1, 1);

        for (int waypointIndex = 0; waypointIndex < waypoints.size() - 1; waypointIndex++) {
            int topLeft = waypointIndex * 2;
            int topRight = waypointIndex * 2 + 1;
            int bottomLeft = (waypointIndex + 1) * 2;
            int bottomRight = (waypointIndex + 1) * 2 + 1;
            mesh.getFaces().addAll(topLeft, 0, topRight, 1, bottomLeft, 2);
            mesh.getFaces().addAll(topRight, 1, bottomRight, 3, bottomLeft, 2);
        }

        addCap(mesh, waypoints.get(0), waypoints.get(1), halfWidth);
        addCap(mesh, waypoints.get(waypoints.size() - 1), waypoints.get(waypoints.size() - 2), halfWidth);
        return new MeshView(mesh);
    }

    static void addCap(TriangleMesh mesh, double[] tipPoint, double[] adjacentPoint, float halfWidth) {
        double directionX = tipPoint[0] - adjacentPoint[0];
        double directionZ = tipPoint[2] - adjacentPoint[2];
        double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (directionLength < MIN_DIRECTION_LENGTH) {
            directionX = 0;
            directionZ = 1;
            directionLength = 1;
        }

        double forwardAngle = Math.atan2(directionX / directionLength, directionZ / directionLength);
        int centerVertexIndex = mesh.getPoints().size() / 3;

        mesh.getPoints().addAll((float) tipPoint[0], 0f, (float) tipPoint[2]);
        for (int segmentStep = 0; segmentStep <= CAP_SEGMENTS; segmentStep++) {
            double arcAngle = forwardAngle - Math.PI / 2 + Math.PI * segmentStep / CAP_SEGMENTS;
            mesh.getPoints().addAll(
                (float) (tipPoint[0] + Math.sin(arcAngle) * halfWidth), 0f,
                (float) (tipPoint[2] + Math.cos(arcAngle) * halfWidth));
        }

        for (int segmentStep = 0; segmentStep < CAP_SEGMENTS; segmentStep++) {
            mesh.getFaces().addAll(
                centerVertexIndex, 0,
                centerVertexIndex + 1 + segmentStep, 0,
                centerVertexIndex + 2 + segmentStep, 0);
        }
    }

    public static double[] interp(List<double[]> waypoints, double distance) {
        for (int waypointIndex = 0; waypointIndex < waypoints.size() - 1; waypointIndex++) {
            double startDistance = waypoints.get(waypointIndex)[3];
            double endDistance = waypoints.get(waypointIndex + 1)[3];
            if (distance >= startDistance && distance <= endDistance) {
                double interpolationFactor = (endDistance == startDistance)
                    ? 0
                    : (distance - startDistance) / (endDistance - startDistance);
                double[] startPoint = waypoints.get(waypointIndex);
                double[] endPoint = waypoints.get(waypointIndex + 1);
                return new double[]{
                    startPoint[0] + (endPoint[0] - startPoint[0]) * interpolationFactor,
                    startPoint[1] + (endPoint[1] - startPoint[1]) * interpolationFactor,
                    startPoint[2] + (endPoint[2] - startPoint[2]) * interpolationFactor
                };
            }
        }
        double[] lastPoint = waypoints.get(waypoints.size() - 1);
        return new double[]{lastPoint[0], lastPoint[1], lastPoint[2]};
    }

    public static double[] dir(List<double[]> waypoints, double distance) {
        for (int waypointIndex = 0; waypointIndex < waypoints.size() - 1; waypointIndex++) {
            if (distance >= waypoints.get(waypointIndex)[3]
                    && distance <= waypoints.get(waypointIndex + 1)[3]) {
                double directionX = waypoints.get(waypointIndex + 1)[0] - waypoints.get(waypointIndex)[0];
                double directionZ = waypoints.get(waypointIndex + 1)[2] - waypoints.get(waypointIndex)[2];
                double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
                if (directionLength < MIN_DIRECTION_LENGTH) {
                    return new double[]{0, 0, 1};
                }
                return new double[]{directionX / directionLength, 0, directionZ / directionLength};
            }
        }
        return new double[]{0, 0, 1};
    }
}
