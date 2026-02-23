package GUI.Run;

import javafx.scene.control.Label;

public class TimeDisplay extends Label {

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int HOURS_PER_DAY = 24;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int NOON_HOUR = 12;

    public TimeDisplay() {
        super("12:00 AM");
        setStyle("-fx-font-size: 13px; -fx-text-fill: -TextFillColorPrimaryBrush; "
            + "-fx-background-color: -ControlFillColorDefaultBrush; "
            + "-fx-background-radius: 8; -fx-padding: 6 12;");
    }

    public void update(double simulationTick) {
        int totalSeconds = (int) simulationTick;
        int hour24 = (totalSeconds / SECONDS_PER_HOUR) % HOURS_PER_DAY;
        int minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        String amPmSuffix = hour24 < NOON_HOUR ? "AM" : "PM";
        int hour12 = hour24 % NOON_HOUR;
        if (hour12 == 0) {
            hour12 = NOON_HOUR;
        }
        setText(String.format("%d:%02d %s", hour12, minutes, amPmSuffix));
    }
}
