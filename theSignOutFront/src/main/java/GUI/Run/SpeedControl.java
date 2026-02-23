package GUI.Run;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.application.ColorScheme;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class SpeedControl extends HBox {

    private static final int COMPONENT_SPACING = 8;
    private static final int INNER_SPACING = 4;
    private static final int VERTICAL_PADDING = 6;
    private static final int HORIZONTAL_PADDING = 12;
    private static final int FIELD_WIDTH = 64;
    private static final String LABEL_STYLE = "-fx-font-size: 13px; -fx-text-fill: -TextFillColorPrimaryBrush;";
    private static final String FIELD_STYLE = "-fx-font-size: 13px; -fx-text-fill: -TextFillColorPrimaryBrush; "
        + "-fx-background-color: -ControlFillColorTertiaryBrush; "
        + "-fx-background-radius: 4; -fx-border-color: -ControlElevationBorderBrush; "
        + "-fx-border-radius: 4; -fx-padding: 2 4;";
    private static final String BOX_STYLE = "-fx-background-color: -ControlFillColorDefaultBrush; -fx-background-radius: 8;";
    private static final int BACKGROUND_RADIUS = 8;
    private static final double MIN_SPEED = 1;
    private static final double MAX_SPEED = 10000;
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 10000;
    private static final double SLIDER_PREFERRED_WIDTH = 200;
    private static final double SLIDER_MINIMUM_WIDTH = 100;
    private static final double THUMB_BORDER_DEFAULT = 6;
    private static final double THUMB_BORDER_HOVER = 2;
    private static final double ANIMATION_DURATION_MS = 200;
    private static final int THUMB_RADIUS = 20;
    private static final int TRACK_RADIUS = 4;

    private final DoubleProperty playbackSpeed = new SimpleDoubleProperty(1);
    private final IntegerProperty childCount = new SimpleIntegerProperty(1);
    private final IntegerProperty dayCount = new SimpleIntegerProperty(1);
    private final DoubleProperty thumbBorderWidth = new SimpleDoubleProperty(THUMB_BORDER_DEFAULT);
    private Slider speedSlider;
    private TextField speedField;
    private TextField childField;
    private TextField dayField;
    private boolean updatingFromSlider = false;
    private boolean updatingFromField = false;

    public SpeedControl() {
        super(COMPONENT_SPACING);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);

        HBox speedBox = buildSpeedBox();
        HBox dayBox = buildDayBox();
        HBox childBox = buildChildBox();
        HBox sliderBox = buildSliderBox();

        getChildren().addAll(dayBox, childBox, speedBox, sliderBox);
    }

    private HBox buildSpeedBox() {
        HBox speedBox = new HBox(INNER_SPACING);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        speedBox.setMinWidth(Region.USE_PREF_SIZE);
        speedBox.setPadding(new Insets(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));
        speedBox.setStyle(BOX_STYLE);

        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle(LABEL_STYLE);
        speedLabel.setMinWidth(Region.USE_PREF_SIZE);

        speedField = new TextField("1");
        speedField.setPrefWidth(FIELD_WIDTH);
        speedField.setMaxWidth(FIELD_WIDTH);
        speedField.setStyle(FIELD_STYLE);

        speedField.setOnAction(event -> commitSpeedFieldValue());
        speedField.focusedProperty().addListener((observable, oldFocused, isFocused) -> {
            if (!isFocused) {
                commitSpeedFieldValue();
            }
        });

        speedBox.getChildren().addAll(speedLabel, speedField);
        return speedBox;
    }

    private HBox buildDayBox() {
        HBox dayBox = new HBox(INNER_SPACING);
        dayBox.setAlignment(Pos.CENTER_LEFT);
        dayBox.setMinWidth(Region.USE_PREF_SIZE);
        dayBox.setPadding(new Insets(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));
        dayBox.setStyle(BOX_STYLE);

        Label dayLabel = new Label("Days:");
        dayLabel.setStyle(LABEL_STYLE);
        dayLabel.setMinWidth(Region.USE_PREF_SIZE);

        dayField = new TextField("1");
        dayField.setPrefWidth(FIELD_WIDTH);
        dayField.setMaxWidth(FIELD_WIDTH);
        dayField.setStyle(FIELD_STYLE);

        dayField.setOnAction(event -> commitDayValue());
        dayField.focusedProperty().addListener((observable, oldFocused, isFocused) -> {
            if (!isFocused) {
                commitDayValue();
            }
        });

        dayBox.getChildren().addAll(dayLabel, dayField);
        return dayBox;
    }

    private HBox buildChildBox() {
        HBox childBox = new HBox(INNER_SPACING);
        childBox.setAlignment(Pos.CENTER_LEFT);
        childBox.setMinWidth(Region.USE_PREF_SIZE);
        childBox.setPadding(new Insets(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));
        childBox.setStyle(BOX_STYLE);

        Label childLabel = new Label("Students:");
        childLabel.setStyle(LABEL_STYLE);
        childLabel.setMinWidth(Region.USE_PREF_SIZE);

        childField = new TextField("1");
        childField.setPrefWidth(FIELD_WIDTH);
        childField.setMaxWidth(FIELD_WIDTH);
        childField.setStyle(FIELD_STYLE);

        childField.setOnAction(event -> commitChildValue());
        childField.focusedProperty().addListener((observable, oldFocused, isFocused) -> {
            if (!isFocused) {
                commitChildValue();
            }
        });

        childBox.getChildren().addAll(childLabel, childField);
        return childBox;
    }

    private HBox buildSliderBox() {
        HBox sliderBox = new HBox();
        sliderBox.setAlignment(Pos.CENTER);
        sliderBox.setPadding(new Insets(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));
        sliderBox.setStyle(BOX_STYLE);
        sliderBox.setMaxWidth(Region.USE_PREF_SIZE);

        speedSlider = new Slider(MIN_SPEED, MAX_SPEED, MIN_SPEED);
        speedSlider.setPrefWidth(SLIDER_PREFERRED_WIDTH);
        speedSlider.setMinWidth(SLIDER_MINIMUM_WIDTH);
        sliderBox.getChildren().add(speedSlider);

        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingFromField) {
                return;
            }
            updatingFromSlider = true;
            double sliderValue = newValue.doubleValue();
            playbackSpeed.set(sliderValue);
            speedField.setText(String.format("%.0f", sliderValue));
            styleTrack(speedSlider, Platform.getPreferences().getAccentColor());
            updatingFromSlider = false;
        });

        Interpolator decelerationCurve = Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0);
        thumbBorderWidth.addListener((observable, oldBorder, newBorder) ->
            styleTrack(speedSlider, Platform.getPreferences().getAccentColor()));
        speedSlider.setOnMouseEntered(event -> {
            new Timeline(new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                new KeyValue(thumbBorderWidth, THUMB_BORDER_HOVER, decelerationCurve))).play();
        });
        speedSlider.setOnMouseExited(event -> {
            new Timeline(new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                new KeyValue(thumbBorderWidth, THUMB_BORDER_DEFAULT, decelerationCurve))).play();
        });

        speedSlider.skinProperty().addListener((observable, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> styleTrack(speedSlider, Platform.getPreferences().getAccentColor()));
            }
        });
        Platform.getPreferences().accentColorProperty().addListener(
            (observable, oldColor, newColor) -> styleTrack(speedSlider, newColor));
        Platform.getPreferences().colorSchemeProperty().addListener(
            (observable, oldScheme, newScheme) -> styleTrack(speedSlider, Platform.getPreferences().getAccentColor()));

        return sliderBox;
    }

    private void commitSpeedFieldValue() {
        if (updatingFromSlider) {
            return;
        }
        updatingFromField = true;
        try {
            double parsedSpeed = Double.parseDouble(speedField.getText().trim());
            parsedSpeed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, parsedSpeed));
            playbackSpeed.set(parsedSpeed);
            speedSlider.setValue(parsedSpeed);
            speedField.setText(String.format("%.0f", parsedSpeed));
            styleTrack(speedSlider, Platform.getPreferences().getAccentColor());
        } catch (NumberFormatException ignored) {
            speedField.setText(String.format("%.0f", playbackSpeed.get()));
        }
        updatingFromField = false;
    }

    private void commitChildValue() {
        try {
            int parsedCount = Integer.parseInt(childField.getText().trim());
            parsedCount = Math.max(MIN_COUNT, Math.min(MAX_COUNT, parsedCount));
            childCount.set(parsedCount);
            childField.setText(String.valueOf(parsedCount));
        } catch (NumberFormatException ignored) {
            childField.setText(String.valueOf(childCount.get()));
        }
    }

    private void commitDayValue() {
        try {
            int parsedDays = Integer.parseInt(dayField.getText().trim());
            parsedDays = Math.max(MIN_COUNT, Math.min(MAX_COUNT, parsedDays));
            dayCount.set(parsedDays);
            dayField.setText(String.valueOf(parsedDays));
        } catch (NumberFormatException ignored) {
            dayField.setText(String.valueOf(dayCount.get()));
        }
    }

    public void setChildCount(int count) {
        childCount.set(count);
        childField.setText(String.valueOf(count));
    }

    public IntegerProperty childCountProperty() {
        return childCount;
    }

    public int getChildCount() {
        return childCount.get();
    }

    public void setDayCount(int count) {
        dayCount.set(count);
        dayField.setText(String.valueOf(count));
    }

    public IntegerProperty dayCountProperty() {
        return dayCount;
    }

    public int getDayCount() {
        return dayCount.get();
    }

    public DoubleProperty playbackSpeedProperty() {
        return playbackSpeed;
    }

    public double getPlaybackSpeed() {
        return playbackSpeed.get();
    }

    private void styleTrack(Slider slider, Color accentColor) {
        var track = slider.lookup(".track");
        if (track == null) {
            return;
        }
        boolean isDarkMode = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        String unfilledColor = isDarkMode ? "#777" : "#c8c8c8";
        String ringColor = isDarkMode ? "#555555" : "#c8c8c8";
        double percentage = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin()) * 100;
        String hexColor = String.format("#%02x%02x%02x",
            (int) (accentColor.getRed() * 255),
            (int) (accentColor.getGreen() * 255),
            (int) (accentColor.getBlue() * 255));

        track.setStyle(
            "-fx-background-color: linear-gradient(to right, " + hexColor + " " + percentage
                + "%, " + unfilledColor + " " + percentage + "%); "
                + "-fx-background-radius: " + TRACK_RADIUS + "; -fx-background-insets: 0;");

        var thumb = slider.lookup(".thumb");
        if (thumb != null) {
            int borderWidth = (int) thumbBorderWidth.get();
            thumb.setStyle(
                "-fx-background-color: " + ringColor + ", " + hexColor + "; "
                    + "-fx-background-insets: 0, " + borderWidth + "; "
                    + "-fx-background-radius: " + THUMB_RADIUS + "; "
                    + "-fx-border-width: 0;");
        }
    }
}
