package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;

public class SimGridCard extends VBox {

    private static final int CARD_SPACING = 8;
    private static final int CARD_PADDING = 16;
    private static final int CARD_PREFERRED_WIDTH = 140;
    private static final int CARD_PREFERRED_HEIGHT = 120;
    private static final String SIMULATION_ICON = "\uE943";
    private static final String PROTECTED_SIMULATION_NAME = "Default";

    public SimGridCard(String simulationName, Runnable onClick, Runnable onDelete) {
        super(CARD_SPACING);

        Label iconLabel = new Label(SIMULATION_ICON);
        iconLabel.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 28px;");

        Label nameLabel = new Label(simulationName);
        nameLabel.setStyle("-fx-font-size: 14px;");

        getChildren().addAll(iconLabel, nameLabel);
        setPadding(new Insets(CARD_PADDING));
        setPrefSize(CARD_PREFERRED_WIDTH, CARD_PREFERRED_HEIGHT);
        setAlignment(Pos.CENTER);
        getStyleClass().add("sim-card");
        setOnMouseClicked(mouseEvent -> onClick.run());

        if (!simulationName.equals(PROTECTED_SIMULATION_NAME) && onDelete != null) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(actionEvent -> onDelete.run());
            contextMenu.getItems().add(deleteItem);
            setOnContextMenuRequested(contextEvent -> contextMenu.show(this, contextEvent.getScreenX(), contextEvent.getScreenY()));
        }
    }
}
