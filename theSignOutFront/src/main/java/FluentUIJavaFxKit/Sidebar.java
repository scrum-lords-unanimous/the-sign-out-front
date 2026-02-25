//self-explanatory it is a sidebar. 
package FluentUIJavaFxKit;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

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

    private final Rectangle pill;
    private final Group pillGroup;
    private final Consumer<String> onPageSelect;
    private Button activeButton;
    private Button firstButton;
    private double sidebarWidth;

    public Sidebar(List<SidebarItem> items, Consumer<String> onPageSelect) {
        super(BUTTON_SPACING);
        this.onPageSelect = onPageSelect;
        getStyleClass().add("sidebar");
        setPrefWidth(SIDEBAR_PREFERRED_WIDTH);

        for (SidebarItem item : items) {
            if (item.dividerBefore()) {
                Region divider = new Region();
                divider.getStyleClass().add("sidebar-divider");
                VBox.setMargin(divider, new Insets(DIVIDER_MARGIN, DIVIDER_HORIZONTAL_MARGIN, DIVIDER_MARGIN, DIVIDER_HORIZONTAL_MARGIN));
                getChildren().add(divider);
            }
            Button button = createSidebarButton(item.icon(), item.label());
            if (firstButton == null) firstButton = button;
            getChildren().add(button);
        }

        pill = new Rectangle(PILL_WIDTH, PILL_HEIGHT);
        pill.setArcWidth(PILL_ARC_SIZE);
        pill.setArcHeight(PILL_ARC_SIZE);
        pill.getStyleClass().add("nav-indicator");
        pill.setManaged(false);
        pill.setLayoutX(PILL_LAYOUT_X);
        pill.setOpacity(0);
        pillGroup = new Group(pill);
        pillGroup.setManaged(false);
        getChildren().add(pillGroup);
        Platform.runLater(() -> sidebarWidth = getWidth());
    }

    public void selectByName(String pageName) {
        for (var child : getChildren())
            if (child instanceof Button b && pageName.equals(b.getText())) { selectButton(b); return; }
    }

    public Rectangle getPill() { return pill; }

    public void selectDefault() { if (firstButton != null) selectButton(firstButton); }

    public void selectButton(Button button) {
        Button prev = activeButton;
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = button;
        activeButton.getStyleClass().add("active");
        onPageSelect.accept(button.getText());

        if (prev == null) {
            Platform.runLater(() -> {
                pill.setLayoutY(FluentAnimations.buttonCenterY(button) - PILL_HEIGHT / 2);
                pill.setOpacity(1);
                FluentAnimations.clipPillTo(pillGroup, sidebarWidth, button);
            });
        } else if (prev != button) {
            FluentAnimations.clipPillTo(pillGroup, sidebarWidth, prev, button);
            FluentAnimations.animatePill(pill, PILL_HEIGHT, pillGroup, sidebarWidth, prev, button);
        }
    }

    private Button createSidebarButton(String iconCode, String labelText) {
        Label iconLabel = new Label(iconCode);
        iconLabel.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: " + ICON_FONT_SIZE + "px;");
        iconLabel.setMinWidth(ICON_MIN_WIDTH);
        Button button = new Button(labelText);
        button.setGraphic(iconLabel);
        button.getStyleClass().add("borderless-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> selectButton(button));
        FluentAnimations.addPressAnimation(iconLabel);
        return button;
    }
}
