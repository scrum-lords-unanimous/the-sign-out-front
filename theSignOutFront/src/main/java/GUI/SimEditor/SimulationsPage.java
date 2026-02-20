package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class SimulationsPage {

    private NodeCanvas nodeCanvas;
    private TextField nameField;
    private String loadedName = null;
    private FlowPane simGrid;

    public VBox build(Label title) {
        // --- Top half: Node Editor (50% of page) ---
        VBox editorPane = buildEditorPane();
        VBox.setVgrow(editorPane, Priority.ALWAYS);

        // --- Bottom half: Saved Simulations Grid ---
        VBox gridPane = buildGridPane();
        VBox.setVgrow(gridPane, Priority.ALWAYS);

        VBox page = new VBox(12, title, editorPane, gridPane);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Constrain editor to 75% of available height
        editorPane.maxHeightProperty().bind(
                page.heightProperty().subtract(title.heightProperty()).subtract(36).multiply(0.75));

        return page;
    }

    private VBox buildEditorPane() {
        // Toolbar
        nameField = new TextField();
        nameField.setPromptText("Simulation name");
        nameField.setPrefWidth(200);

        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> saveCurrent());

        Label addIcon = new Label("\uE710");
        addIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 14px;");
        Button addStepBtn = new Button("Add Step");
        addStepBtn.setGraphic(addIcon);
        addStepBtn.setOnAction(e -> nodeCanvas.showAddStepMenu(addStepBtn));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label cleanupIcon = new Label("\uEA99");
        cleanupIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 14px;");
        Button cleanupBtn = new Button();
        cleanupBtn.setGraphic(cleanupIcon);
        cleanupBtn.setTooltip(new Tooltip("Clean up layout"));
        cleanupBtn.setOnAction(e -> nodeCanvas.cleanupLayout());

        HBox toolbar = new HBox(8, nameField, saveBtn, addStepBtn, spacer, cleanupBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getStyleClass().add("editor-toolbar");

        // Node canvas
        nodeCanvas = new NodeCanvas();
        nodeCanvas.setOnChange(this::markDirty);
        VBox.setVgrow(nodeCanvas, Priority.ALWAYS);

        VBox pane = new VBox(0, toolbar, nodeCanvas);
        pane.setStyle("-fx-border-color: -ControlFillColorDefaultBrush; -fx-border-width: 1; -fx-border-radius: 6;");
        return pane;
    }

    private VBox buildGridPane() {
        simGrid = new FlowPane(12, 12);
        refreshGrid();

        ScrollPane scroll = new ScrollPane(simGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox pane = new VBox(8, scroll);
        return pane;
    }

    private void refreshGrid() {
        simGrid.getChildren().clear();
        List<String> sims = SimulationStore.listSimulations();
        for (String name : sims) {
            simGrid.getChildren().add(createSimGridCard(name));
        }
        simGrid.getChildren().add(createAddCard());
    }

    private VBox createSimGridCard(String name) {
        Label icon = new Label("\uE943");
        icon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 28px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14px;");

        VBox card = new VBox(8, icon, nameLabel);
        card.setPadding(new Insets(16));
        card.setPrefSize(140, 120);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("sim-card");
        card.setOnMouseClicked(e -> loadSimulation(name));

        // Right-click to delete (except Default)
        if (!name.equals("Default")) {
            ContextMenu ctx = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> {
                SimulationStore.delete(name);
                refreshGrid();
                if (name.equals(loadedName)) {
                    clearEditor();
                }
            });
            ctx.getItems().add(deleteItem);
            card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));
        }

        return card;
    }

    private VBox createAddCard() {
        Label icon = new Label("\uE710");
        icon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 28px;");

        Label nameLabel = new Label("New");
        nameLabel.setStyle("-fx-font-size: 14px;");

        VBox card = new VBox(8, icon, nameLabel);
        card.setPadding(new Insets(16));
        card.setPrefSize(140, 120);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("sim-card", "add-card");
        card.setOnMouseClicked(e -> createNewSimulation());
        return card;
    }

    private void loadSimulation(String name) {
        Map<String, Object> data = SimulationStore.load(name);
        if (data == null) return;

        loadedName = name;
        nameField.setText(name);
        nodeCanvas.loadFromJson(data);
    }

    private void clearEditor() {
        loadedName = null;
        nameField.setText("");
        nodeCanvas.clear();
    }

    private void createNewSimulation() {
        TextInputDialog dialog = new TextInputDialog("My Simulation");
        dialog.setTitle("New Simulation");
        dialog.setHeaderText("Enter a name for the new simulation:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            if (SimulationStore.exists(name)) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "A simulation named '" + name + "' already exists.");
                alert.showAndWait();
                return;
            }

            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("name", name);
            empty.put("steps", new ArrayList<>());
            SimulationStore.save(name, empty);

            refreshGrid();
            loadSimulation(name);
        });
    }

    private void saveCurrent() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a simulation name.");
            alert.showAndWait();
            return;
        }

        // If renamed, delete old file
        if (loadedName != null && !loadedName.equals(name)) {
            SimulationStore.delete(loadedName);
        }

        // Build tree from graph
        List<StepModel> steps = nodeCanvas.buildStepTree();
        Map<String, Object> data = StepModel.toSimJson(name, steps);

        // Add layout map
        data.put("layout", nodeCanvas.buildLayoutMap());

        SimulationStore.save(name, data);
        loadedName = name;

        refreshGrid();
    }

    private void markDirty() {
        // Future: could show unsaved indicator
    }
}
