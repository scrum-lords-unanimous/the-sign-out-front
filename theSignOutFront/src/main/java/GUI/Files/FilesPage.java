package GUI.Files;

import GUI.SimEditor.SimulationStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class FilesPage {

    private static final double LEFT_PANEL_WIDTH = 220;
    private static final int PANEL_SPACING = 8;
    private static final int PAGE_SPACING = 12;
    private static final String EMPTY_STATE_STYLE =
            "-fx-font-size: 18px; -fx-text-fill: -TextFillColorTertiaryBrush;";
    private static final String BUTTON_STYLE =
            "-fx-background-color: -ControlFillColorDefaultBrush; -fx-background-radius: 8;";
    private static final String RIGHT_SCROLL_STYLE =
            "-fx-background-color: -ControlFillColorDefaultBrush; " +
            "-fx-background: -ControlFillColorDefaultBrush; " +
            "-fx-background-radius: 8;";
    private static final String SCROLL_TRANSPARENT_STYLE =
            "-fx-background-color: transparent;";
    private static final String SANITIZE_PATTERN = "[^a-zA-Z0-9_ -]";

    private final VBox rightPanel = new VBox();
    private FileListPanel fileListPanel;
    private FileListPanel.FileEntry selectedEntry;

    public VBox build(Label title) {
        fileListPanel = new FileListPanel(this::onFileSelected);

        ScrollPane listScrollPane = buildListScrollPane();
        HBox buttonRow = buildButtonRow();
        VBox leftSide = buildLeftPanel(listScrollPane, buttonRow);
        ScrollPane rightScrollPane = buildRightScrollPane();

        HBox splitLayout = new HBox(PANEL_SPACING, leftSide, rightScrollPane);
        VBox.setVgrow(splitLayout, Priority.ALWAYS);

        showEmptyState();

        VBox page = new VBox(PAGE_SPACING, title, splitLayout);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    private ScrollPane buildListScrollPane() {
        ScrollPane listScrollPane = new ScrollPane(fileListPanel);
        listScrollPane.setFitToWidth(true);
        listScrollPane.setStyle(SCROLL_TRANSPARENT_STYLE);
        return listScrollPane;
    }

    private HBox buildButtonRow() {
        Button newButton = new Button("New");
        newButton.setMaxWidth(Double.MAX_VALUE);
        newButton.setStyle(BUTTON_STYLE);
        HBox.setHgrow(newButton, Priority.ALWAYS);
        newButton.setOnAction(event -> promptNewFile(false));

        Button copyDefaultButton = new Button("Copy Default");
        copyDefaultButton.setMaxWidth(Double.MAX_VALUE);
        copyDefaultButton.setStyle(BUTTON_STYLE);
        HBox.setHgrow(copyDefaultButton, Priority.ALWAYS);
        copyDefaultButton.setOnAction(event -> promptNewFile(true));

        HBox buttonRow = new HBox(PANEL_SPACING, newButton, copyDefaultButton);
        buttonRow.setPadding(Insets.EMPTY);
        return buttonRow;
    }

    private VBox buildLeftPanel(ScrollPane listScrollPane, HBox buttonRow) {
        VBox leftSide = new VBox(PANEL_SPACING, listScrollPane, buttonRow);
        leftSide.setPrefWidth(LEFT_PANEL_WIDTH);
        leftSide.setMinWidth(LEFT_PANEL_WIDTH);
        leftSide.setMaxWidth(LEFT_PANEL_WIDTH);
        VBox.setVgrow(listScrollPane, Priority.NEVER);
        return leftSide;
    }

    private ScrollPane buildRightScrollPane() {
        rightPanel.setPadding(new Insets(PANEL_SPACING));
        rightPanel.setSpacing(PANEL_SPACING);
        rightPanel.setMinWidth(0);

        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true);
        rightScrollPane.setFitToHeight(true);
        rightScrollPane.setMinWidth(0);
        rightScrollPane.setStyle(RIGHT_SCROLL_STYLE);
        HBox.setHgrow(rightScrollPane, Priority.ALWAYS);
        return rightScrollPane;
    }

    private void showEmptyState() {
        rightPanel.getChildren().clear();
        Label promptLabel = new Label("Please select a file");
        promptLabel.setStyle(EMPTY_STATE_STYLE);
        StackPane centeredWrapper = new StackPane(promptLabel);
        centeredWrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(centeredWrapper, Priority.ALWAYS);
        rightPanel.getChildren().add(centeredWrapper);
    }

    private void onFileSelected(FileListPanel.FileEntry fileEntry) {
        selectedEntry = fileEntry;
        JsonViewer.showView(rightPanel, fileEntry, this::onEdit, () -> onDelete(fileEntry));
    }

    private void onEdit(FileListPanel.FileEntry fileEntry, String content) {
        JsonEditor.showEdit(rightPanel, fileEntry, content, () -> onFileSelected(fileEntry));
    }

    private void onDelete(FileListPanel.FileEntry fileEntry) {
        String baseName = fileEntry.name().replace(".json", "");

        if (baseName.endsWith("Driveway")) {
            DrivewayStore.delete(baseName.replace("Driveway", ""));
        } else {
            SimulationStore.delete(baseName);
            DrivewayStore.delete(baseName);
        }

        selectedEntry = null;
        fileListPanel.rebuild();
        showEmptyState();
    }

    private void promptNewFile(boolean copyDefault) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(copyDefault ? "Copy Default Simulation" : "New Simulation");
        dialog.setHeaderText(null);
        dialog.setContentText("Simulation name:");

        dialog.showAndWait().ifPresent(inputName -> {
            if (inputName.isBlank()) {
                return;
            }

            String sanitizedName = inputName.replaceAll(SANITIZE_PATTERN, "");

            if (sanitizedName.isBlank() || SimulationStore.exists(sanitizedName)) {
                return;
            }

            if (copyDefault) {
                var defaultData = SimulationStore.load("Default");
                if (defaultData != null) {
                    SimulationStore.save(sanitizedName, defaultData);
                }
            } else {
                SimulationStore.save(sanitizedName, new java.util.LinkedHashMap<>());
            }

            DrivewayStore.createFromDefault(sanitizedName);
            fileListPanel.rebuild();
        });
    }
}
