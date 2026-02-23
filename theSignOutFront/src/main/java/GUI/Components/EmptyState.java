package GUI.Components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EmptyState extends VBox {

    private static final int ITEM_SPACING = 12;
    private static final int CONTAINER_PADDING = 48;
    private static final String ICON_STYLE = "-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 40px;"
            + " -fx-text-fill: -TextFillColorTertiaryBrush;";
    private static final String MESSAGE_STYLE = "-fx-font-size: 16px; -fx-text-fill: -TextFillColorSecondaryBrush;";

    public EmptyState(String iconText, String messageText) {
        this(iconText, messageText, null, null);
    }

    public EmptyState(String iconText, String messageText, String actionButtonText, Runnable actionHandler) {
        setAlignment(Pos.CENTER);
        setSpacing(ITEM_SPACING);
        setPadding(new Insets(CONTAINER_PADDING));

        Label iconLabel = new Label(iconText);
        iconLabel.setStyle(ICON_STYLE);

        Label messageLabel = new Label(messageText);
        messageLabel.setStyle(MESSAGE_STYLE);

        getChildren().addAll(iconLabel, messageLabel);

        if (actionButtonText != null && actionHandler != null) {
            Button actionButton = new Button(actionButtonText);
            actionButton.getStyleClass().add("accent");
            actionButton.setOnAction(event -> actionHandler.run());
            getChildren().add(actionButton);
        }
    }
}
