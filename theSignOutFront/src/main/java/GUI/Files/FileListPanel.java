package GUI.Files;

import GUI.SimEditor.SimulationStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class FileListPanel extends VBox {

    private static final int NO_SPACING = 0;
    private static final int ICON_LABEL_GAP = 6;
    private static final double HEADER_PADDING = 8;
    private static final int DIVIDER_HEIGHT = 1;

    private static final String PANEL_BACKGROUND_STYLE =
            "-fx-background-color: -ControlFillColorDefaultBrush; -fx-background-radius: 8;";
    private static final String ICON_FONT_STYLE =
            "-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 13px;";
    private static final String HEADER_LABEL_STYLE =
            "-fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String DIVIDER_BORDER_STYLE =
            "-fx-border-color: -divider-color; -fx-border-width: 0 0 1 0;";

    private static final String FOLDER_ICON = "\uE8B7";
    private static final String FILE_ICON = "\uE8A5";
    private static final String DEFAULT_SIMULATION_NAME = "Default";

    public record FileEntry(String name, String path, boolean resource, boolean editable, boolean deletable) {}

    private final Consumer<FileEntry> onSelectCallback;

    public FileListPanel(Consumer<FileEntry> onSelectCallback) {
        super(NO_SPACING);
        this.onSelectCallback = onSelectCallback;
        setPadding(Insets.EMPTY);
        setStyle(PANEL_BACKGROUND_STYLE);
        rebuild();
    }

    public void rebuild() {
        getChildren().clear();

        addResourcesSection();
        addSimulationsSection();
        addDrivewaysSection();
    }

    private void addResourcesSection() {
        getChildren().add(buildHeaderRow(FOLDER_ICON, "Resources"));

        List<FileEntry> resourceEntries = List.of(
                new FileEntry("Slides.json", "/JsonRoot/Slides/Slides.json", true, false, false),
                new FileEntry("Simulation.json", "/JsonRoot/Simulation/Simulation.json", true, false, false),
                new FileEntry("Driveway.json", "/JsonRoot/Driveway/Driveway.json", true, false, false),
                new FileEntry("Questions.json", "/JsonRoot/InputQuestions/Questions.json", true, false, false),
                new FileEntry("DefaultAnswers.json", "/JsonRoot/InputQuestions/DefaultAnswers.json", true, false, false),
                new FileEntry("Operations.json", "/JsonRoot/Operations/Operations.json", true, false, false)
        );

        for (FileEntry resourceEntry : resourceEntries) {
            getChildren().add(buildFileItemButton(resourceEntry));
        }
    }

    private void addSimulationsSection() {
        getChildren().add(buildHeaderRow(FOLDER_ICON, "Simulations"));

        for (String simulationName : SimulationStore.listSimulations()) {
            boolean isDefaultSimulation = simulationName.equals(DEFAULT_SIMULATION_NAME);
            FileEntry simulationEntry = new FileEntry(
                    simulationName + ".json",
                    "simulations/" + simulationName + ".json",
                    false,
                    !isDefaultSimulation,
                    !isDefaultSimulation
            );
            getChildren().add(buildFileItemButton(simulationEntry));
        }
    }

    private void addDrivewaysSection() {
        getChildren().add(buildHeaderRow(FOLDER_ICON, "Driveways"));

        DrivewayStore.ensureDefaults();
        FileEntry defaultDrivewayEntry = new FileEntry(
                "DefaultDriveway.json",
                "driveways/DefaultDriveway.json",
                false,
                false,
                false
        );
        getChildren().add(buildFileItemButton(defaultDrivewayEntry));

        for (String simulationName : SimulationStore.listSimulations()) {
            if (simulationName.equals(DEFAULT_SIMULATION_NAME)) {
                continue;
            }

            DrivewayStore.createFromDefault(simulationName);
            FileEntry drivewayEntry = new FileEntry(
                    simulationName + "Driveway.json",
                    "driveways/" + simulationName + "Driveway.json",
                    false,
                    true,
                    true
            );
            getChildren().add(buildFileItemButton(drivewayEntry));
        }
    }

    private HBox buildHeaderRow(String iconCode, String headerText) {
        Label iconLabel = new Label(iconCode);
        iconLabel.setStyle(ICON_FONT_STYLE);

        Label textLabel = new Label(headerText);
        textLabel.setStyle(HEADER_LABEL_STYLE);

        iconLabel.textFillProperty().bind(textLabel.textFillProperty());

        HBox headerRow = new HBox(ICON_LABEL_GAP, iconLabel, textLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(HEADER_PADDING, HEADER_PADDING, HEADER_PADDING, HEADER_PADDING));
        headerRow.setStyle(DIVIDER_BORDER_STYLE);
        return headerRow;
    }

    private Region buildDivider() {
        Region dividerLine = new Region();
        dividerLine.setMinHeight(DIVIDER_HEIGHT);
        dividerLine.setMaxHeight(DIVIDER_HEIGHT);
        dividerLine.setStyle(DIVIDER_BORDER_STYLE);
        return dividerLine;
    }

    private Button buildFileItemButton(FileEntry fileEntry) {
        Label fileIcon = new Label(FILE_ICON);
        fileIcon.setStyle(ICON_FONT_STYLE);

        Button fileButton = new Button(fileEntry.name());
        fileButton.setGraphic(fileIcon);
        fileIcon.textFillProperty().bind(fileButton.textFillProperty());
        fileButton.setMaxWidth(Double.MAX_VALUE);
        fileButton.setAlignment(Pos.CENTER_LEFT);
        fileButton.getStyleClass().addAll("borderless-button", "file-list-item");
        fileButton.setStyle(DIVIDER_BORDER_STYLE);
        fileButton.setOnAction(event -> onSelectCallback.accept(fileEntry));
        return fileButton;
    }
}
