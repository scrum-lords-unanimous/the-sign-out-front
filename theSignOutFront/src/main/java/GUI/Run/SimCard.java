package GUI.Run;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SimCard extends VBox {

    private static final int CARD_SPACING = 8;
    private static final int CARD_PADDING = 16;
    private static final double CARD_WIDTH = 140;
    private static final double CARD_HEIGHT = 120;
    private static final int ICON_FONT_SIZE = 28;
    private static final int NAME_FONT_SIZE = 14;

    public SimCard(String simulationName, Consumer<String> onClickHandler) {
        super(CARD_SPACING);

        Label icon = new Label("\uE768");
        icon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: " + ICON_FONT_SIZE + "px;");

        Label nameLabel = new Label(simulationName);
        nameLabel.setStyle("-fx-font-size: " + NAME_FONT_SIZE + "px;");

        getChildren().addAll(icon, nameLabel);
        setPadding(new Insets(CARD_PADDING));
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setAlignment(Pos.CENTER);
        getStyleClass().add("sim-card");
        setOnMouseClicked(event -> onClickHandler.accept(simulationName));
    }
}
