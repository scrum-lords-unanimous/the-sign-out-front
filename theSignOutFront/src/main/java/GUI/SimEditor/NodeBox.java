package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class NodeBox extends VBox {

    private static final int PREFERRED_WIDTH = 260;
    private static final int MINIMUM_WIDTH = 200;
    private static final int MAXIMUM_WIDTH = 320;
    private static final int HEADER_SPACING = 8;
    private static final int OUTPUT_ROW_SPACING = 8;
    private static final int PROPERTIES_SPACING = 8;
    private static final int FIELD_ROW_SPACING = 8;
    private static final int FIELD_LABEL_MIN_WIDTH = 65;

    private static final Map<String, String> TYPE_COLORS = Map.ofEntries(
        Map.entry("for", "#4a9eff"),
        Map.entry("for-each", "#4a9eff"),
        Map.entry("while", "#4a9eff"),
        Map.entry("if", "#e8a838"),
        Map.entry("set", "#50c878"),
        Map.entry("print", "#b0b0b0"),
        Map.entry("spawn", "#e06c75"),
        Map.entry("precompute-driveway", "#c678dd"),
        Map.entry("record-slide-view", "#c678dd"),
        Map.entry("slide-report", "#c678dd")
    );

    private static final String DEFAULT_TYPE_COLOR = "#888888";

    private final StepModel model;
    private final Runnable onChange;

    private final NodePort execInPort;
    private NodePort execOutPort;
    private NodePort loopBodyPort;
    private NodePort thenPort;
    private NodePort elsePort;

    private final List<NodePort> allPorts = new ArrayList<>();

    public NodeBox(StepModel model, Runnable onChange) {
        this.model = model;
        this.onChange = onChange;

        getStyleClass().add("node-box");
        setPrefWidth(PREFERRED_WIDTH);
        setMinWidth(MINIMUM_WIDTH);
        setMaxWidth(MAXIMUM_WIDTH);

        execInPort = new NodePort(NodePort.Direction.INPUT, NodePort.PortRole.EXEC_IN);
        allPorts.add(execInPort);

        buildContent();
    }

    public StepModel getModel() {
        return model;
    }

    public NodePort getExecIn() {
        return execInPort;
    }

    public NodePort getExecOut() {
        return execOutPort;
    }

    public NodePort getLoopBodyPort() {
        return loopBodyPort;
    }

    public NodePort getThenPort() {
        return thenPort;
    }

    public NodePort getElsePort() {
        return elsePort;
    }

    public List<NodePort> getAllPorts() {
        return allPorts;
    }

    private void buildContent() {
        getChildren().clear();

        HBox header = buildHeader();
        getChildren().add(header);

        VBox propertiesSection = buildProperties();
        if (!propertiesSection.getChildren().isEmpty()) {
            getChildren().add(propertiesSection);
        }

        HBox outputRow = buildOutputRow();
        getChildren().add(outputRow);
    }

    private HBox buildHeader() {
        String headerColor = TYPE_COLORS.getOrDefault(model.getType(), DEFAULT_TYPE_COLOR);

        Label inputLabel = new Label("In");
        inputLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.7);");

        Label typeNameLabel = new Label(formatTypeName(model.getType()));
        typeNameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(HEADER_SPACING, execInPort, inputLabel, spacer, typeNameLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("node-box-header");
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setStyle("-fx-background-color: " + headerColor + "; -fx-background-radius: 6 6 0 0;");

        return header;
    }

    private static String formatTypeName(String type) {
        return type.replace("-", " ").toUpperCase();
    }

    private HBox buildOutputRow() {
        HBox row = new HBox(OUTPUT_ROW_SPACING);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 10, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        switch (model.getType()) {
            case "for":
            case "for-each":
            case "while":
                loopBodyPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.LOOP_BODY);
                allPorts.add(loopBodyPort);

                Label loopBodyLabel = new Label("Loop Body");
                loopBodyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                execOutPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOutPort);

                Label loopOutLabel = new Label("Out");
                loopOutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(loopBodyLabel, loopBodyPort, spacer, loopOutLabel, execOutPort);
                break;

            case "if":
                thenPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.THEN);
                allPorts.add(thenPort);

                Label thenLabel = new Label("Then");
                thenLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                elsePort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.ELSE);
                allPorts.add(elsePort);

                Label elseLabel = new Label("Else");
                elseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                execOutPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOutPort);

                Label ifOutLabel = new Label("Out");
                ifOutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(thenLabel, thenPort, elseLabel, elsePort, spacer, ifOutLabel, execOutPort);
                break;

            default:
                execOutPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOutPort);

                Label defaultOutLabel = new Label("Out");
                defaultOutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(spacer, defaultOutLabel, execOutPort);
                break;
        }

        return row;
    }

    private VBox buildProperties() {
        VBox propertiesContainer = new VBox(PROPERTIES_SPACING);
        propertiesContainer.setPadding(new Insets(8, 12, 8, 12));

        switch (model.getType()) {
            case "for":
                addPropertyField(propertiesContainer, "var", "Variable");
                addPropertyField(propertiesContainer, "from", "From");
                addPropertyField(propertiesContainer, "to", "To");
                addPropertyField(propertiesContainer, "step", "Step");
                break;
            case "for-each":
                addPropertyField(propertiesContainer, "collection", "Collection");
                addPropertyField(propertiesContainer, "as", "As");
                break;
            case "while":
            case "if":
                ConditionEditor.addConditionEditor(propertiesContainer, model, onChange);
                break;
            case "set":
                addPropertyField(propertiesContainer, "target", "Target");
                if (OperationBlockView.isOpValue(model, "value")) {
                    OperationBlockView.addValueField(propertiesContainer, "value", "Value", model, onChange);
                } else {
                    addPropertyField(propertiesContainer, "value", "Value");
                }
                break;
            case "print":
                if (OperationBlockView.isOpValue(model, "value")) {
                    OperationBlockView.addValueField(propertiesContainer, "value", "Value", model, onChange);
                } else {
                    addPropertyField(propertiesContainer, "value", "Value");
                }
                break;
            case "spawn":
                addPropertyField(propertiesContainer, "count", "Count");
                addPropertyField(propertiesContainer, "as", "As");
                break;
        }

        return propertiesContainer;
    }

    private void addPropertyField(VBox container, String propertyKey, String labelText) {
        Object propertyValue = model.getProperty(propertyKey);
        String stringValue = NodeUtils.stringify(propertyValue);

        HBox fieldRow = new HBox(FIELD_ROW_SPACING);
        fieldRow.setAlignment(Pos.CENTER_LEFT);

        Label fieldLabel = new Label(labelText);
        fieldLabel.setMinWidth(FIELD_LABEL_MIN_WIDTH);
        fieldLabel.setStyle("-fx-font-size: 12px;");

        if (VariableChips.isVariableRef(stringValue)) {
            HBox variableChips = VariableChips.buildVariableChips(stringValue);
            HBox.setHgrow(variableChips, Priority.ALWAYS);
            fieldRow.getChildren().addAll(fieldLabel, variableChips);
        } else {
            TextField textField = new TextField(stringValue);
            textField.setPromptText(labelText);
            HBox.setHgrow(textField, Priority.ALWAYS);
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                model.setProperty(propertyKey, NodeUtils.parseValue(newValue));
                onChange.run();
            });
            textField.setOnMousePressed(mouseEvent -> mouseEvent.consume());
            textField.setOnMouseDragged(mouseEvent -> mouseEvent.consume());
            fieldRow.getChildren().addAll(fieldLabel, textField);
        }

        container.getChildren().add(fieldRow);
    }
}
