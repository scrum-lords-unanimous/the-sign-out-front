package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class SimToolbar extends HBox {

    private static final int TOOLBAR_SPACING = 8;
    private static final int NAME_FIELD_PREFERRED_WIDTH = 200;

    private final TextField nameField;

    public SimToolbar(Runnable onSave, Runnable onAddStep, Runnable onCleanup) {
        super(TOOLBAR_SPACING);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 12, 8, 12));
        getStyleClass().add("editor-toolbar");

        nameField = new TextField();
        nameField.setPromptText("Simulation name");
        nameField.setPrefWidth(NAME_FIELD_PREFERRED_WIDTH);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> onSave.run());

        Label addIcon = new Label("\uE710");
        addIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 14px;");
        Button addStepButton = new Button("Add Step");
        addStepButton.setGraphic(addIcon);
        addStepButton.setOnAction(event -> onAddStep.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label cleanupIcon = new Label("\uEA99");
        cleanupIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 14px;");
        Button cleanupButton = new Button();
        cleanupButton.setGraphic(cleanupIcon);
        cleanupButton.setTooltip(new Tooltip("Clean up layout"));
        cleanupButton.setOnAction(event -> onCleanup.run());

        getChildren().addAll(nameField, saveButton, addStepButton, spacer, cleanupButton);
    }

    public TextField getNameField() {
        return nameField;
    }

    public String getName() {
        return nameField.getText().trim();
    }

    public void setName(String name) {
        nameField.setText(name);
    }
}
