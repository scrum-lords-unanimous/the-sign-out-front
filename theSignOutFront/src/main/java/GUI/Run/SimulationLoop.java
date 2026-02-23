package GUI.Run;

import Data.Driveway.Driveway;
import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

import java.util.List;

public class SimulationLoop extends AnimationTimer {

    private static final double SECONDS_PER_DAY = 86_400.0;
    private static final double FEET_PER_MILE = 5_280.0;
    private static final double SECONDS_PER_HOUR = 3_600.0;
    private static final int RANKINGS_UPDATE_INTERVAL = 30;

    private static final int COL_ARRIVAL_TIME = 0;
    private static final int COL_IS_ACTIVE = 1;
    private static final int COL_POSITION = 2;

    private final double[][] students;
    private final Driveway driveway;
    private final double advancementRate;
    private final List<double[]> waypoints;
    private final List<MeshView> carMeshes;
    private final PhongMaterial signMaterial;
    private final List<Image> slideImages;
    private final Image logoImage;
    private final int slideCycleDurationSeconds;
    private final SlideViewTracker slideTracker;
    private final TimeDisplay timeDisplay;
    private final VBox rankingsBox;
    private final String slideSetName;

    private int totalDays;
    private int currentDay = 0;
    private double elapsedSimSeconds = 0;
    private double playbackSpeed = 1;
    private long previousFrameNanos = 0;
    private int frameCount = 0;
    private boolean dayHasEnded = false;

    public SimulationLoop(double[][] students, Driveway driveway, double advancementRate,
                          List<double[]> waypoints, List<MeshView> carMeshes,
                          PhongMaterial signMaterial, List<Image> slideImages,
                          Image logoImage, int slideCycleDurationSeconds,
                          SlideViewTracker slideTracker, TimeDisplay timeDisplay,
                          VBox rankingsBox, String slideSetName, int totalDays) {
        this.students = students;
        this.driveway = driveway;
        this.advancementRate = advancementRate;
        this.waypoints = waypoints;
        this.carMeshes = carMeshes;
        this.signMaterial = signMaterial;
        this.slideImages = slideImages;
        this.logoImage = logoImage;
        this.slideCycleDurationSeconds = slideCycleDurationSeconds;
        this.slideTracker = slideTracker;
        this.timeDisplay = timeDisplay;
        this.rankingsBox = rankingsBox;
        this.slideSetName = slideSetName;
        this.totalDays = totalDays;
    }

    public void setTotalDays(int days) {
        this.totalDays = days;
    }

    public void setPlaybackSpeed(double speed) {
        this.playbackSpeed = speed;
    }

    public double getElapsedSimSeconds() {
        return elapsedSimSeconds;
    }

    public void resetSimulation() {
        elapsedSimSeconds = 0;
        resetAllStudents();
    }

    @Override
    public void handle(long nowNanos) {
        if (previousFrameNanos == 0) {
            previousFrameNanos = nowNanos;
            return;
        }

        double deltaSeconds = (nowNanos - previousFrameNanos) / 1e9;
        previousFrameNanos = nowNanos;
        elapsedSimSeconds += playbackSpeed * deltaSeconds;

        if (elapsedSimSeconds >= SECONDS_PER_DAY && !dayHasEnded) {
            handleDayEnd();
            return;
        }

        timeDisplay.update(elapsedSimSeconds);
        updateSignBillboard();
        updateStudents(deltaSeconds);

        frameCount++;
        if (frameCount % RANKINGS_UPDATE_INTERVAL == 0) {
            RankingsOverlay.update(rankingsBox, slideTracker, slideSetName);
        }
    }

    private void handleDayEnd() {
        currentDay++;

        if (currentDay >= totalDays) {
            dayHasEnded = true;
            stop();
            DayStatsDialog.show(slideTracker, () -> {
                elapsedSimSeconds = 0;
                dayHasEnded = false;
                currentDay = 0;
                slideTracker.reset();
                resetAllStudents();
                start();
            });
        } else {
            elapsedSimSeconds = 0;
            previousFrameNanos = 0;
            resetAllStudents();
        }
    }

    private void updateStudents(double deltaSeconds) {
        for (int i = 0; i < students.length; i++) {
            double arrivalTime = students[i][COL_ARRIVAL_TIME];
            boolean isActive = students[i][COL_IS_ACTIVE] == 1;
            double positionAlongDriveway = students[i][COL_POSITION];

            if (!isActive && positionAlongDriveway == 0 && elapsedSimSeconds >= arrivalTime) {
                isActive = true;
                recordSlideViewOnArrival();
            }

            if (isActive) {
                double speedMilesPerHour = driveway.speedAt(positionAlongDriveway);
                double speedFeetPerSecond = speedMilesPerHour * FEET_PER_MILE / SECONDS_PER_HOUR;
                positionAlongDriveway += speedFeetPerSecond * advancementRate * playbackSpeed * deltaSeconds;

                if (positionAlongDriveway >= driveway.totalLength) {
                    isActive = false;
                    positionAlongDriveway = -1;
                }
            }

            students[i][COL_IS_ACTIVE] = isActive ? 1 : 0;
            students[i][COL_POSITION] = positionAlongDriveway;

            MeshView carMesh = carMeshes.get(i);
            if (isActive) {
                positionCarOnDriveway(carMesh, positionAlongDriveway);
                carMesh.setVisible(true);
            } else {
                carMesh.setVisible(false);
            }
        }
    }

    private void recordSlideViewOnArrival() {
        if (!slideImages.isEmpty() && slideTracker.size() > 0) {
            int currentSlideIndex = ((int) (elapsedSimSeconds / slideCycleDurationSeconds)) % slideTracker.size();
            slideTracker.recordView(currentSlideIndex);
        }
    }

    private void positionCarOnDriveway(MeshView carMesh, double position) {
        double[] point = DrivewayMesh.interp(waypoints, position);
        double[] direction = DrivewayMesh.dir(waypoints, position);

        carMesh.setTranslateX(point[0]);
        carMesh.setTranslateY(point[1] - 1);
        carMesh.setTranslateZ(point[2]);

        double headingDegrees = Math.toDegrees(Math.atan2(direction[0], direction[2])) + 180;
        carMesh.getTransforms().setAll(
                new Rotate(headingDegrees, Rotate.Y_AXIS),
                new Rotate(180, Rotate.X_AXIS)
        );
    }

    private void updateSignBillboard() {
        if (signMaterial == null) return;

        if (!slideImages.isEmpty()) {
            int slideIndex = ((int) (elapsedSimSeconds / slideCycleDurationSeconds)) % slideImages.size();
            signMaterial.setDiffuseMap(slideImages.get(slideIndex));
        } else if (logoImage != null) {
            signMaterial.setDiffuseMap(logoImage);
        }
    }

    private void resetAllStudents() {
        for (double[] student : students) {
            student[COL_IS_ACTIVE] = 0;
            student[COL_POSITION] = 0;
        }
    }
}
