package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AddSimCard extends VBox {

    private static final int CARD_SPACING = 8;
    private static final int CARD_PADDING = 16;
    private static final int CARD_PREFERRED_WIDTH = 140;
    private static final int CARD_PREFERRED_HEIGHT = 120;
    private static final String ADD_ICON = "\uE710";

    public AddSimCard(Runnable onClick) {
        super(CARD_SPACING);

        Label iconLabel = new Label(ADD_ICON);
        iconLabel.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 28px;");

        Label nameLabel = new Label("New");
        nameLabel.setStyle("-fx-font-size: 14px;");

        getChildren().addAll(iconLabel, nameLabel);
        setPadding(new Insets(CARD_PADDING));
        setPrefSize(CARD_PREFERRED_WIDTH, CARD_PREFERRED_HEIGHT);
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("sim-card", "add-card");
        setOnMouseClicked(mouseEvent -> onClick.run());
    }
}
