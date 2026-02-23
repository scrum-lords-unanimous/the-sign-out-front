package GUI.SimEditor;

import GUI.Components.EmptyState;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class SimGrid extends VBox {

    private static final int GRID_SPACING = 12;
    private static final int CONTAINER_SPACING = 8;
    private static final String EMPTY_STATE_ICON = "\uE943";
    private static final String EMPTY_STATE_MESSAGE = "Create your first simulation";
    private static final String EMPTY_STATE_BUTTON_TEXT = "New Simulation";

    private final FlowPane simulationFlowPane;
    private final StackPane container;
    private final Consumer<String> onLoadSimulation;
    private final Runnable onNewSimulation;
    private final Consumer<String> onDeleteSimulation;

    public SimGrid(Consumer<String> onLoadSimulation, Runnable onNewSimulation, Consumer<String> onDeleteSimulation) {
        super(CONTAINER_SPACING);
        this.onLoadSimulation = onLoadSimulation;
        this.onNewSimulation = onNewSimulation;
        this.onDeleteSimulation = onDeleteSimulation;

        simulationFlowPane = new FlowPane(GRID_SPACING, GRID_SPACING);
        container = new StackPane(simulationFlowPane);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);
    }

    public void refresh() {
        simulationFlowPane.getChildren().clear();
        container.getChildren().clear();

        List<String> simulationNames = SimulationStore.listSimulations();

        if (simulationNames.isEmpty()) {
            container.getChildren().add(new EmptyState(
                EMPTY_STATE_ICON, EMPTY_STATE_MESSAGE, EMPTY_STATE_BUTTON_TEXT, onNewSimulation));
        } else {
            container.getChildren().add(simulationFlowPane);
            for (String simulationName : simulationNames) {
                simulationFlowPane.getChildren().add(new SimGridCard(
                    simulationName,
                    () -> onLoadSimulation.accept(simulationName),
                    () -> onDeleteSimulation.accept(simulationName)
                ));
            }
            simulationFlowPane.getChildren().add(new AddSimCard(onNewSimulation));
        }
    }
}
