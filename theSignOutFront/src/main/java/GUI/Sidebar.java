package GUI;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class Sidebar extends VBox {

    private static final double PILL_HEIGHT = 16;
    private static final double PILL_WIDTH = 3;
    private static final double PILL_ARC_SIZE = 6;
    private static final double PILL_LAYOUT_X = 8;
    private static final double SIDEBAR_PREFERRED_WIDTH = 300;
    private static final int BUTTON_SPACING = 4;
    private static final double DIVIDER_MARGIN = 4;
    private static final double DIVIDER_HORIZONTAL_MARGIN = 8;
    private static final double ICON_FONT_SIZE = 16;
    private static final double ICON_MIN_WIDTH = 20;

    private static final double PRESS_SCALE = 0.96;
    private static final double RELEASE_SCALE = 1.0;
    private static final double PRESS_DURATION_MS = 150;
    private static final double RELEASE_DURATION_MS = 300;
    private static final double PILL_ANIMATION_STEP_MS = 167;

    private static final String ICON_RUN = "\uE768";
    private static final String ICON_SIMULATIONS = "\uE943";
    private static final String ICON_ASSETS = "\uF12B";
    private static final String ICON_FILES = "\uE8A5";

    private final Rectangle pill;
    private final Group pillGroup;
    private final Consumer<String> onPageSelect;

    private Button activeButton;
    private double sidebarWidth;

    private final Button runButton;
    private final Button simulationsButton;
    private final Button assetsButton;
    private final Button filesButton;

    public Sidebar(Consumer<String> onPageSelect) {
        super(BUTTON_SPACING);
        this.onPageSelect = onPageSelect;

        getStyleClass().add("sidebar");
        setPrefWidth(SIDEBAR_PREFERRED_WIDTH);

        runButton = createSidebarButton(ICON_RUN, "Run");
        simulationsButton = createSidebarButton(ICON_SIMULATIONS, "Simulations");
        assetsButton = createSidebarButton(ICON_ASSETS, "Assets");

        Region divider = new Region();
        divider.getStyleClass().add("sidebar-divider");
        VBox.setMargin(divider, new Insets(DIVIDER_MARGIN, DIVIDER_HORIZONTAL_MARGIN, DIVIDER_MARGIN, DIVIDER_HORIZONTAL_MARGIN));

        filesButton = createSidebarButton(ICON_FILES, "View and Modify Files");

        pill = new Rectangle(PILL_WIDTH, PILL_HEIGHT);
        pill.setArcWidth(PILL_ARC_SIZE);
        pill.setArcHeight(PILL_ARC_SIZE);
        pill.getStyleClass().add("nav-indicator");
        pill.setManaged(false);
        pill.setLayoutX(PILL_LAYOUT_X);
        pill.setOpacity(0);

        pillGroup = new Group(pill);
        pillGroup.setManaged(false);
        getChildren().addAll(runButton, simulationsButton, assetsButton, divider, filesButton, pillGroup);

        Platform.runLater(() -> sidebarWidth = getWidth());
    }

    public void selectByName(String pageName) {
        for (var child : getChildren()) {
            if (child instanceof Button button && pageName.equals(button.getText())) {
                selectButton(button);
                return;
            }
        }
    }

    public Rectangle getPill() {
        return pill;
    }

    public void selectDefault() {
        selectButton(runButton);
    }

    public void selectButton(Button button) {
        Button previousButton = activeButton;

        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("active");

        onPageSelect.accept(button.getText());

        if (previousButton == null) {
            Platform.runLater(() -> {
                pill.setLayoutY(buttonCenterY(button) - PILL_HEIGHT / 2);
                pill.setOpacity(1);
                clipPillTo(button);
            });
        } else if (previousButton != button) {
            clipPillTo(previousButton, button);
            animatePill(previousButton, button);
        }
    }

    private Button createSidebarButton(String iconCode, String labelText) {
        Label iconLabel = new Label(iconCode);
        iconLabel.setStyle(
            "-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: " + ICON_FONT_SIZE + "px;"
        );
        iconLabel.setMinWidth(ICON_MIN_WIDTH);
        Button button = new Button(labelText);
        button.setGraphic(iconLabel);
        button.getStyleClass().add("borderless-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> selectButton(button));
        addFluentPressAnimation(iconLabel);
        return button;
    }

    static void addFluentPressAnimation(Node targetNode) {
        Interpolator accelerateInterpolator = Interpolator.SPLINE(0.9, 0.1, 1.0, 0.2);
        Interpolator decelerateInterpolator = Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0);

        targetNode.setOnMousePressed(mouseEvent -> {
            ScaleTransition pressTransition = new ScaleTransition(Duration.millis(PRESS_DURATION_MS), targetNode);
            pressTransition.setToX(PRESS_SCALE);
            pressTransition.setToY(PRESS_SCALE);
            pressTransition.setInterpolator(accelerateInterpolator);
            pressTransition.play();
        });
        targetNode.setOnMouseReleased(mouseEvent -> {
            ScaleTransition releaseTransition = new ScaleTransition(Duration.millis(RELEASE_DURATION_MS), targetNode);
            releaseTransition.setToX(RELEASE_SCALE);
            releaseTransition.setToY(RELEASE_SCALE);
            releaseTransition.setInterpolator(decelerateInterpolator);
            releaseTransition.play();
        });
    }

    private double buttonCenterY(Button button) {
        return button.getBoundsInParent().getMinY() + button.getHeight() / 2;
    }

    private void clipPillTo(Button... buttons) {
        Shape combinedClip = null;
        for (Button button : buttons) {
            var buttonBounds = button.getBoundsInParent();
            Rectangle clipRect = new Rectangle(0, buttonBounds.getMinY(), sidebarWidth, buttonBounds.getHeight());
            combinedClip = combinedClip == null ? clipRect : Shape.union(combinedClip, clipRect);
        }
        pillGroup.setClip(combinedClip);
    }

    private void animatePill(Button fromButton, Button toButton) {
        double fromY = buttonCenterY(fromButton) - PILL_HEIGHT / 2;
        double toY = buttonCenterY(toButton) - PILL_HEIGHT / 2;
        double stretchTop = toY > fromY ? fromY : toY;
        double stretchHeight = Math.abs(toY - fromY) + PILL_HEIGHT;
        Duration stepDuration = Duration.millis(PILL_ANIMATION_STEP_MS);
        var easeBoth = Interpolator.EASE_BOTH;

        Timeline pillTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(pill.layoutYProperty(), fromY),
                new KeyValue(pill.heightProperty(), PILL_HEIGHT)),
            new KeyFrame(stepDuration,
                new KeyValue(pill.layoutYProperty(), stretchTop, easeBoth),
                new KeyValue(pill.heightProperty(), stretchHeight, easeBoth)),
            new KeyFrame(stepDuration.add(stepDuration),
                new KeyValue(pill.layoutYProperty(), toY, easeBoth),
                new KeyValue(pill.heightProperty(), PILL_HEIGHT, easeBoth)));

        pillTimeline.setOnFinished(event -> clipPillTo(toButton));
        pillTimeline.play();
    }
}
