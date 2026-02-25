package FluentUIJavaFxKit;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

class FluentAnimations {

    private static final double PRESS_SCALE = 0.96;
    private static final double RELEASE_SCALE = 1.0;
    private static final double PRESS_DURATION_MS = 150;
    private static final double RELEASE_DURATION_MS = 300;
    private static final double PILL_ANIMATION_STEP_MS = 167;

    static void addPressAnimation(Node targetNode) {
        Interpolator accel = Interpolator.SPLINE(0.9, 0.1, 1.0, 0.2);
        Interpolator decel = Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0);

        targetNode.setOnMousePressed(e -> {
            ScaleTransition t = new ScaleTransition(Duration.millis(PRESS_DURATION_MS), targetNode);
            t.setToX(PRESS_SCALE);
            t.setToY(PRESS_SCALE);
            t.setInterpolator(accel);
            t.play();
        });
        targetNode.setOnMouseReleased(e -> {
            ScaleTransition t = new ScaleTransition(Duration.millis(RELEASE_DURATION_MS), targetNode);
            t.setToX(RELEASE_SCALE);
            t.setToY(RELEASE_SCALE);
            t.setInterpolator(decel);
            t.play();
        });
    }

    static void animatePill(Rectangle pill, double pillHeight, Group pillGroup,
                            double sidebarWidth, Button fromButton, Button toButton) {
        double fromY = buttonCenterY(fromButton) - pillHeight / 2;
        double toY = buttonCenterY(toButton) - pillHeight / 2;
        double stretchTop = toY > fromY ? fromY : toY;
        double stretchHeight = Math.abs(toY - fromY) + pillHeight;
        Duration step = Duration.millis(PILL_ANIMATION_STEP_MS);
        var ease = Interpolator.EASE_BOTH;

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(pill.layoutYProperty(), fromY),
                        new KeyValue(pill.heightProperty(), pillHeight)),
                new KeyFrame(step,
                        new KeyValue(pill.layoutYProperty(), stretchTop, ease),
                        new KeyValue(pill.heightProperty(), stretchHeight, ease)),
                new KeyFrame(step.add(step),
                        new KeyValue(pill.layoutYProperty(), toY, ease),
                        new KeyValue(pill.heightProperty(), pillHeight, ease)));

        timeline.setOnFinished(event -> clipPillTo(pillGroup, sidebarWidth, toButton));
        timeline.play();
    }

    static void clipPillTo(Group pillGroup, double sidebarWidth, Button... buttons) {
        Shape clip = null;
        for (Button button : buttons) {
            var bounds = button.getBoundsInParent();
            Rectangle rect = new Rectangle(0, bounds.getMinY(), sidebarWidth, bounds.getHeight());
            clip = clip == null ? rect : Shape.union(clip, rect);
        }
        pillGroup.setClip(clip);
    }

    static double buttonCenterY(Button button) {
        return button.getBoundsInParent().getMinY() + button.getHeight() / 2;
    }
}
