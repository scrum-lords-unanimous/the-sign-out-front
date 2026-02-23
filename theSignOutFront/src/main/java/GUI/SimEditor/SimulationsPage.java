package GUI.SimEditor;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class SimulationsPage {

    private static final int PAGE_SPACING = 12;
    private static final int EDITOR_PANE_SPACING = 0;
    private static final int TITLE_MARGIN_ESTIMATE = 36;
    private static final double EDITOR_HEIGHT_RATIO = 0.75;

    private NodeCanvas nodeCanvas;
    private SimToolbar toolbar;
    private SimGrid simGrid;
    private String loadedSimulationName = null;

    public VBox build(Label titleLabel) {
        nodeCanvas = new NodeCanvas();
        nodeCanvas.setOnChange(this::markDirty);
        VBox.setVgrow(nodeCanvas, Priority.ALWAYS);

        toolbar = new SimToolbar(
            this::saveCurrentSimulation,
            () -> nodeCanvas.showAddStepMenu(toolbar),
            nodeCanvas::cleanupLayout
        );

        VBox editorPane = new VBox(EDITOR_PANE_SPACING, toolbar, nodeCanvas);
        editorPane.setStyle("-fx-border-color: -ControlFillColorDefaultBrush; -fx-border-width: 1; -fx-border-radius: 6;");
        VBox.setVgrow(editorPane, Priority.ALWAYS);

        simGrid = new SimGrid(this::loadSimulation, this::createNewSimulation, this::deleteSimulation);
        simGrid.refresh();
        VBox.setVgrow(simGrid, Priority.ALWAYS);

        VBox page = new VBox(PAGE_SPACING, titleLabel, editorPane, simGrid);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        editorPane.maxHeightProperty().bind(
            page.heightProperty()
                .subtract(titleLabel.heightProperty())
                .subtract(TITLE_MARGIN_ESTIMATE)
                .multiply(EDITOR_HEIGHT_RATIO)
        );

        return page;
    }

    private void loadSimulation(String simulationName) {
        Map<String, Object> simulationData = SimulationStore.load(simulationName);
        if (simulationData == null) {
            return;
        }
        loadedSimulationName = simulationName;
        toolbar.setName(simulationName);
        nodeCanvas.loadFromJson(simulationData);
    }

    private void clearEditor() {
        loadedSimulationName = null;
        toolbar.setName("");
        nodeCanvas.clear();
    }

    private void createNewSimulation() {
        TextInputDialog dialog = new TextInputDialog("My Simulation");
        dialog.setTitle("New Simulation");
        dialog.setHeaderText("Enter a name for the new simulation:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(enteredName -> {
            if (enteredName.isBlank()) {
                return;
            }
            if (SimulationStore.exists(enteredName)) {
                Alert duplicateAlert = new Alert(Alert.AlertType.WARNING,
                    "A simulation named '" + enteredName + "' already exists.");
                duplicateAlert.showAndWait();
                return;
            }

            Map<String, Object> emptySimulation = new LinkedHashMap<>();
            emptySimulation.put("name", enteredName);
            emptySimulation.put("steps", new ArrayList<>());
            SimulationStore.save(enteredName, emptySimulation);

            simGrid.refresh();
            loadSimulation(enteredName);
        });
    }

    private void deleteSimulation(String simulationName) {
        SimulationStore.delete(simulationName);
        simGrid.refresh();
        if (simulationName.equals(loadedSimulationName)) {
            clearEditor();
        }
    }

    private void saveCurrentSimulation() {
        String simulationName = toolbar.getName();
        if (simulationName.isEmpty()) {
            Alert emptyNameAlert = new Alert(Alert.AlertType.WARNING, "Please enter a simulation name.");
            emptyNameAlert.showAndWait();
            return;
        }

        if (loadedSimulationName != null && !loadedSimulationName.equals(simulationName)) {
            SimulationStore.delete(loadedSimulationName);
        }

        List<StepModel> steps = nodeCanvas.buildStepTree();
        Map<String, Object> simulationData = StepModel.toSimJson(simulationName, steps);
        simulationData.put("layout", nodeCanvas.buildLayoutMap());

        SimulationStore.save(simulationName, simulationData);
        loadedSimulationName = simulationName;

        simGrid.refresh();
    }

    private void markDirty() {
    }
}
