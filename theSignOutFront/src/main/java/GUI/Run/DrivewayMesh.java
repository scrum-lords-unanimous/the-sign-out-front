//This file is a little hard to understand. It creates a mesh for the driveway based on the JSON data. It uses a segment system. 
package GUI.Run;

import Data.Driveway.Driveway;
import Data.Driveway.DrivewaySegment;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DrivewayMesh {

    private static final double WAYPOINT_SPACING = 2.0;
    private static final double MINIMUM_MEANINGFUL_LENGTH = 0.001;
    private static final double CURVE_RADIUS_MINIMUM_SCALE = 0.7;
    private static final double CURVE_RADIUS_RANDOM_RANGE = 0.6;
    private static final int MINIMUM_CURVE_STEPS = 4;
    private static final int SEMICIRCLE_SEGMENTS = 12;

    public static List<double[]> buildWaypoints(Driveway driveway, double radius, Random random) {
        List<double[]> waypoints = new ArrayList<>();
        double currentX = 0;
        double currentZ = 0;
        double headingAngle = 0;
        double totalDistanceTraveled = 0;
        boolean nextTurnIsLeft = true;

        for (int segmentIndex = 0; segmentIndex < driveway.segments.size(); segmentIndex++) {
            DrivewaySegment segment = driveway.segments.get(segmentIndex);
            double segmentLength = segment.end - segment.start;
            boolean isStraightSegment = (segmentIndex % 2 == 0);

            if (isStraightSegment) {
                int numberOfSteps = Math.max(1, (int) (segmentLength / WAYPOINT_SPACING));
                double stepLength = segmentLength / numberOfSteps;

                for (int step = 0; step < numberOfSteps; step++) {
                    waypoints.add(new double[]{currentX, 0, currentZ, totalDistanceTraveled});
                    currentX += Math.sin(headingAngle) * stepLength;
                    currentZ += Math.cos(headingAngle) * stepLength;
                    totalDistanceTraveled += stepLength;
                }
            } else {
                double curveRadius = radius * (CURVE_RADIUS_MINIMUM_SCALE + random.nextDouble() * CURVE_RADIUS_RANDOM_RANGE);
                double totalTurnAngle = segmentLength / curveRadius;
                double turnDirection = nextTurnIsLeft ? -1 : 1;
                nextTurnIsLeft = !nextTurnIsLeft;

                int numberOfSteps = Math.max(MINIMUM_CURVE_STEPS, (int) (segmentLength / WAYPOINT_SPACING));
                double anglePerStep = totalTurnAngle / numberOfSteps;
                double distancePerStep = segmentLength / numberOfSteps;

                for (int step = 0; step < numberOfSteps; step++) {
                    waypoints.add(new double[]{currentX, 0, currentZ, totalDistanceTraveled});
                    headingAngle += turnDirection * anglePerStep;
                    currentX += Math.sin(headingAngle) * distancePerStep;
                    currentZ += Math.cos(headingAngle) * distancePerStep;
                    totalDistanceTraveled += distancePerStep;
                }
            }
        }

        waypoints.add(new double[]{currentX, 0, currentZ, totalDistanceTraveled});
        return waypoints;
    }

    public static MeshView buildRoadMesh(List<double[]> waypoints, double roadWidth) {
        TriangleMesh mesh = new TriangleMesh();
        float halfRoadWidth = (float) (roadWidth / 2);

        for (int i = 0; i < waypoints.size(); i++) {
            double[] waypoint = waypoints.get(i);
            double forwardX;
            double forwardZ;

            if (i < waypoints.size() - 1) {
                double[] nextWaypoint = waypoints.get(i + 1);
                forwardX = nextWaypoint[0] - waypoint[0];
                forwardZ = nextWaypoint[2] - waypoint[2];
            } else {
                double[] previousWaypoint = waypoints.get(i - 1);
                forwardX = waypoint[0] - previousWaypoint[0];
                forwardZ = waypoint[2] - previousWaypoint[2];
            }

            double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
            if (forwardLength < MINIMUM_MEANINGFUL_LENGTH) {
                forwardX = 0;
                forwardZ = 1;
                forwardLength = 1;
            }

            double perpendicularX = -forwardZ / forwardLength;
            double perpendicularZ = forwardX / forwardLength;

            float leftEdgeX = (float) (waypoint[0] + perpendicularX * halfRoadWidth);
            float leftEdgeZ = (float) (waypoint[2] + perpendicularZ * halfRoadWidth);
            float rightEdgeX = (float) (waypoint[0] - perpendicularX * halfRoadWidth);
            float rightEdgeZ = (float) (waypoint[2] - perpendicularZ * halfRoadWidth);

            mesh.getPoints().addAll(leftEdgeX, 0f, leftEdgeZ, rightEdgeX, 0f, rightEdgeZ);
        }

        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1, 1, 1);

        for (int i = 0; i < waypoints.size() - 1; i++) {
            int currentLeft = i * 2;
            int currentRight = i * 2 + 1;
            int nextLeft = (i + 1) * 2;
            int nextRight = (i + 1) * 2 + 1;
            mesh.getFaces().addAll(currentLeft, 0, currentRight, 1, nextLeft, 2);
            mesh.getFaces().addAll(currentRight, 1, nextRight, 3, nextLeft, 2);
        }

        double[] firstWaypoint = waypoints.get(0);
        double[] secondWaypoint = waypoints.get(1);
        double[] lastWaypoint = waypoints.get(waypoints.size() - 1);
        double[] secondToLastWaypoint = waypoints.get(waypoints.size() - 2);

        addSemicircleCap(mesh, firstWaypoint, secondWaypoint, halfRoadWidth);
        addSemicircleCap(mesh, lastWaypoint, secondToLastWaypoint, halfRoadWidth);

        return new MeshView(mesh);
    }

    static void addSemicircleCap(TriangleMesh mesh, double[] capTipPoint, double[] adjacentPoint, float halfWidth) {
        double towardsTipX = capTipPoint[0] - adjacentPoint[0];
        double towardsTipZ = capTipPoint[2] - adjacentPoint[2];
        double towardsTipLength = Math.sqrt(towardsTipX * towardsTipX + towardsTipZ * towardsTipZ);

        if (towardsTipLength < MINIMUM_MEANINGFUL_LENGTH) {
            towardsTipX = 0;
            towardsTipZ = 1;
            towardsTipLength = 1;
        }

        double outwardAngle = Math.atan2(towardsTipX / towardsTipLength, towardsTipZ / towardsTipLength);
        int centerVertexIndex = mesh.getPoints().size() / 3;

        mesh.getPoints().addAll((float) capTipPoint[0], 0f, (float) capTipPoint[2]);

        for (int arcStep = 0; arcStep <= SEMICIRCLE_SEGMENTS; arcStep++) {
            double arcAngle = outwardAngle - Math.PI / 2 + Math.PI * arcStep / SEMICIRCLE_SEGMENTS;
            float arcPointX = (float) (capTipPoint[0] + Math.sin(arcAngle) * halfWidth);
            float arcPointZ = (float) (capTipPoint[2] + Math.cos(arcAngle) * halfWidth);
            mesh.getPoints().addAll(arcPointX, 0f, arcPointZ);
        }

        for (int arcStep = 0; arcStep < SEMICIRCLE_SEGMENTS; arcStep++) {
            mesh.getFaces().addAll(
                centerVertexIndex, 0,
                centerVertexIndex + 1 + arcStep, 0,
                centerVertexIndex + 2 + arcStep, 0);
        }
    }

    public static double[] interpolatePosition(List<double[]> waypoints, double targetDistance) {
        for (int i = 0; i < waypoints.size() - 1; i++) {
            double segmentStartDistance = waypoints.get(i)[3];
            double segmentEndDistance = waypoints.get(i + 1)[3];

            if (targetDistance >= segmentStartDistance && targetDistance <= segmentEndDistance) {
                double blendFactor = (segmentEndDistance == segmentStartDistance)
                    ? 0
                    : (targetDistance - segmentStartDistance) / (segmentEndDistance - segmentStartDistance);
                double[] startPoint = waypoints.get(i);
                double[] endPoint = waypoints.get(i + 1);
                return new double[]{
                    startPoint[0] + (endPoint[0] - startPoint[0]) * blendFactor,
                    startPoint[1] + (endPoint[1] - startPoint[1]) * blendFactor,
                    startPoint[2] + (endPoint[2] - startPoint[2]) * blendFactor
                };
            }
        }

        double[] lastWaypoint = waypoints.get(waypoints.size() - 1);
        return new double[]{lastWaypoint[0], lastWaypoint[1], lastWaypoint[2]};
    }

    public static double[] directionAtDistance(List<double[]> waypoints, double targetDistance) {
        for (int i = 0; i < waypoints.size() - 1; i++) {
            double segmentStartDistance = waypoints.get(i)[3];
            double segmentEndDistance = waypoints.get(i + 1)[3];

            if (targetDistance >= segmentStartDistance && targetDistance <= segmentEndDistance) {
                double deltaX = waypoints.get(i + 1)[0] - waypoints.get(i)[0];
                double deltaZ = waypoints.get(i + 1)[2] - waypoints.get(i)[2];
                double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (length < MINIMUM_MEANINGFUL_LENGTH) {
                    return new double[]{0, 0, 1};
                }
                return new double[]{deltaX / length, 0, deltaZ / length};
            }
        }

        return new double[]{0, 0, 1};
    }
}
