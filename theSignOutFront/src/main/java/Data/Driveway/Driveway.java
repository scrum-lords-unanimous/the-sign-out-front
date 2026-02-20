package Data.Driveway;

import CLI.UserInput.DoMath.MathIsDone;

import java.util.ArrayList;
import java.util.List;

public class Driveway {

    public final List<DrivewaySegment> segments;
    public final double totalLength;

    private Driveway(List<DrivewaySegment> segments, double totalLength) {
	this.segments = segments;
	this.totalLength = totalLength;
    }

    public double speedAt(double position) {
	for (DrivewaySegment seg : segments) {
	    if (position >= seg.start && position < seg.end) {
		return seg.speed;
	    }
	}
	return segments.get(segments.size() - 1).speed;
    }

    public static Driveway build(double length, int curveCount, double avgRadius,
				 double straightSpeed, double carWeight, MathIsDone math) {
	List<DrivewaySegment> segments = new ArrayList<>();

	if (curveCount == 0) {
	    segments.add(new DrivewaySegment(0, length, straightSpeed));
	} else {
	    int totalSegments = 2 * curveCount + 1;
	    double segLength = length / totalSegments;
	    double curveSpeed = math.ComputeOperatives(straightSpeed, avgRadius, carWeight, "CENTRIFUGAL");

	    for (int i = 0; i < totalSegments; i++) {
		double start = i * segLength;
		double end = (i + 1) * segLength;
		double speed = (i % 2 == 0) ? straightSpeed : curveSpeed;
		segments.add(new DrivewaySegment(start, end, speed));
	    }
	}

	return new Driveway(segments, length);
    }
}
