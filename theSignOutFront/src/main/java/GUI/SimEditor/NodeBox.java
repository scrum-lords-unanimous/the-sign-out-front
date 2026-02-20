package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class NodeBox extends VBox {

    private static final Map<String, String> TYPE_COLORS = Map.of(
        "for", "#4a9eff",
        "for-each", "#4a9eff",
        "if", "#e8a838",
        "set", "#50c878",
        "print", "#b0b0b0",
        "spawn", "#e06c75",
        "precompute-driveway", "#c678dd",
        "record-slide-view", "#c678dd",
        "slide-report", "#c678dd"
    );

    private static final Map<String, String> OP_SYMBOLS = Map.of(
        "=", "=",
        "neq", "\u2260",
        ">", ">",
        "ge", "\u2265",
        "<", "<",
        "le", "\u2264"
    );

    private final StepModel model;
    private final Runnable onChange;

    // Ports
    private final NodePort execIn;
    private NodePort execOut;
    private NodePort loopBodyPort;
    private NodePort thenPort;
    private NodePort elsePort;

    private final List<NodePort> allPorts = new ArrayList<>();

    public NodeBox(StepModel model, Runnable onChange) {
        this.model = model;
        this.onChange = onChange;

        getStyleClass().add("node-box");
        setPrefWidth(260);
        setMinWidth(200);
        setMaxWidth(320);

        // Exec In port (all nodes have this)
        execIn = new NodePort(NodePort.Direction.INPUT, NodePort.PortRole.EXEC_IN);
        allPorts.add(execIn);

        build();
    }

    public StepModel getModel() { return model; }
    public NodePort getExecIn() { return execIn; }
    public NodePort getExecOut() { return execOut; }
    public NodePort getLoopBodyPort() { return loopBodyPort; }
    public NodePort getThenPort() { return thenPort; }
    public NodePort getElsePort() { return elsePort; }
    public List<NodePort> getAllPorts() { return allPorts; }

    private void build() {
        getChildren().clear();

        // Full-width colored header
        HBox header = buildHeader();
        getChildren().add(header);

        // Properties
        VBox props = buildProperties();
        if (!props.getChildren().isEmpty()) {
            getChildren().add(props);
        }

        // Output ports row
        HBox outputRow = buildOutputRow();
        getChildren().add(outputRow);
    }

    // --- Header ---

    private HBox buildHeader() {
        String color = TYPE_COLORS.getOrDefault(model.getType(), "#888888");

        Label inLabel = new Label("In");
        inLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.7);");

        Label typeName = new Label(formatTypeName(model.getType()));
        typeName.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, execIn, inLabel, spacer, typeName);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("node-box-header");
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6 6 0 0;");

        return header;
    }

    private static String formatTypeName(String type) {
        return type.replace("-", " ").toUpperCase();
    }

    // --- Output Row ---

    private HBox buildOutputRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 10, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        switch (model.getType()) {
            case "for":
            case "for-each":
                loopBodyPort = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.LOOP_BODY);
                allPorts.add(loopBodyPort);

                Label loopLabel = new Label("Loop Body");
                loopLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                execOut = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOut);

                Label outLabel = new Label("Out");
                outLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(loopLabel, loopBodyPort, spacer, outLabel, execOut);
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

                execOut = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOut);

                Label outLabel2 = new Label("Out");
                outLabel2.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(thenLabel, thenPort, elseLabel, elsePort, spacer, outLabel2, execOut);
                break;

            default:
                execOut = new NodePort(NodePort.Direction.OUTPUT, NodePort.PortRole.EXEC_OUT);
                allPorts.add(execOut);

                Label defaultOutLabel = new Label("Out");
                defaultOutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorTertiaryBrush;");

                row.getChildren().addAll(spacer, defaultOutLabel, execOut);
                break;
        }

        return row;
    }

    // --- Properties ---

    private VBox buildProperties() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 12, 8, 12));

        switch (model.getType()) {
            case "for":
                addField(box, "var", "Variable");
                addField(box, "from", "From");
                addField(box, "to", "To");
                addField(box, "step", "Step");
                break;
            case "for-each":
                addField(box, "collection", "Collection");
                addField(box, "as", "As");
                break;
            case "if":
                addConditionEditor(box);
                break;
            case "set":
                addField(box, "target", "Target");
                addValueField(box, "value", "Value");
                break;
            case "print":
                addValueField(box, "value", "Value");
                break;
            case "spawn":
                addField(box, "count", "Count");
                addField(box, "as", "As");
                break;
        }

        return box;
    }

    private void addField(VBox box, String key, String label) {
        Object val = model.getProperty(key);
        String strVal = stringify(val);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setMinWidth(65);
        lbl.setStyle("-fx-font-size: 12px;");

        if (isVariableRef(strVal)) {
            HBox chips = buildVariableChips(strVal);
            HBox.setHgrow(chips, Priority.ALWAYS);
            row.getChildren().addAll(lbl, chips);
        } else {
            TextField field = new TextField(strVal);
            field.setPromptText(label);
            HBox.setHgrow(field, Priority.ALWAYS);
            field.textProperty().addListener((obs, old, v) -> {
                model.setProperty(key, parseValue(v));
                onChange.run();
            });
            field.setOnMousePressed(e -> e.consume());
            field.setOnMouseDragged(e -> e.consume());
            row.getChildren().addAll(lbl, field);
        }

        box.getChildren().add(row);
    }

    // --- Variable Chips ---

    private static boolean isVariableRef(String s) {
        return s != null && s.contains(".") && !s.contains(" ");
    }

    private HBox buildVariableChips(String varRef) {
        String[] parts = varRef.split("\\.");
        HBox chips = new HBox(4);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.setOnMousePressed(e -> e.consume());
        chips.setOnMouseDragged(e -> e.consume());

        for (String part : parts) {
            Label chip = new Label(formatSegment(part));
            chip.setStyle(
                "-fx-background-color: -ControlFillColorSecondaryBrush;"
                + " -fx-text-fill: -TextFillColorPrimaryBrush;"
                + " -fx-padding: 3 8;"
                + " -fx-background-radius: 4;"
                + " -fx-font-size: 11px;");
            chips.getChildren().add(chip);
        }

        return chips;
    }

    private static String formatSegment(String segment) {
        // Replace hyphens with spaces
        String s = segment.replace("-", " ");
        // Insert space before uppercase letters (camelCase split)
        s = s.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        // Title case each word
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) sb.append(w.substring(1));
        }
        return sb.toString();
    }

    // --- Value Fields (set/print) ---

    @SuppressWarnings("unchecked")
    private void addValueField(VBox box, String key, String label) {
        Object val = model.getProperty(key);
        if (val instanceof Map) {
            Map<String, Object> valMap = (Map<String, Object>) val;
            if (valMap.containsKey("op")) {
                Label lbl = new Label(label);
                lbl.setStyle("-fx-font-size: 12px;");
                box.getChildren().add(lbl);
                box.getChildren().add(buildOpBlock(valMap));
                return;
            }
        }
        addField(box, key, label);
    }

    @SuppressWarnings("unchecked")
    private VBox buildOpBlock(Map<String, Object> exprMap) {
        String op = String.valueOf(exprMap.get("op"));
        List<Object> args = (List<Object>) exprMap.get("args");

        VBox block = new VBox(6);
        block.setPadding(new Insets(4, 0, 4, 8));
        block.setStyle("-fx-border-color: transparent transparent transparent -AccentFillColorTertiaryBrush;"
                + " -fx-border-width: 0 0 0 2;");

        Label badge = new Label(op);
        badge.setStyle("-fx-background-color: #50c878;"
                + " -fx-text-fill: white;"
                + " -fx-padding: 2 8;"
                + " -fx-background-radius: 3;"
                + " -fx-font-size: 10px;"
                + " -fx-font-weight: bold;");
        block.getChildren().add(badge);

        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                final int argIndex = i;
                if (arg instanceof Map) {
                    Map<String, Object> argMap = (Map<String, Object>) arg;
                    if (argMap.containsKey("op")) {
                        block.getChildren().add(buildOpBlock(argMap));
                    } else {
                        Label lbl = new Label(compactExpr(arg));
                        lbl.setStyle("-fx-font-size: 10px;"
                                + " -fx-font-family: 'Consolas';"
                                + " -fx-background-color: -ControlFillColorTertiaryBrush;"
                                + " -fx-padding: 3 8;"
                                + " -fx-background-radius: 4;");
                        lbl.setTooltip(new Tooltip(stringify(arg)));
                        lbl.setOnMousePressed(e -> e.consume());
                        lbl.setOnMouseDragged(e -> e.consume());
                        block.getChildren().add(lbl);
                    }
                } else {
                    String strArg = stringify(arg);
                    if (isVariableRef(strArg)) {
                        HBox chips = buildVariableChips(strArg);
                        block.getChildren().add(chips);
                    } else {
                        TextField field = new TextField(strArg);
                        field.setStyle("-fx-font-size: 12px;");
                        HBox.setHgrow(field, Priority.ALWAYS);
                        field.textProperty().addListener((o, a, b) -> {
                            args.set(argIndex, parseValue(field.getText()));
                            onChange.run();
                        });
                        field.setOnMousePressed(e -> e.consume());
                        field.setOnMouseDragged(e -> e.consume());
                        block.getChildren().add(field);
                    }
                }
            }
        }

        return block;
    }

    // --- Condition Editor (if) ---

    @SuppressWarnings("unchecked")
    private void addConditionEditor(VBox box) {
        Object cond = model.getProperty("condition");
        if (!(cond instanceof Map)) return;
        box.getChildren().add(buildConditionBlock((Map<String, Object>) cond));
    }

    @SuppressWarnings("unchecked")
    private javafx.scene.Node buildConditionBlock(Map<String, Object> condMap) {
        if (condMap.containsKey("and") || condMap.containsKey("or")) {
            return buildCompoundBlock(condMap);
        }
        return buildSimpleConditionRow(condMap);
    }

    @SuppressWarnings("unchecked")
    private VBox buildCompoundBlock(Map<String, Object> condMap) {
        String logicOp = condMap.containsKey("and") ? "and" : "or";
        List<Object> subs = (List<Object>) condMap.get(logicOp);

        VBox block = new VBox(6);
        block.setPadding(new Insets(4, 0, 4, 8));
        block.setStyle("-fx-border-color: transparent transparent transparent -ControlStrongStrokeColorDefaultBrush;"
                + " -fx-border-width: 0 0 0 2;");

        Label badge = new Label(logicOp.toUpperCase());
        badge.setStyle("-fx-background-color: #e8a838;"
                + " -fx-text-fill: white;"
                + " -fx-padding: 2 8;"
                + " -fx-background-radius: 3;"
                + " -fx-font-size: 10px;"
                + " -fx-font-weight: bold;");
        block.getChildren().add(badge);

        if (subs != null) {
            for (Object sub : subs) {
                if (sub instanceof Map) {
                    block.getChildren().add(buildConditionBlock((Map<String, Object>) sub));
                }
            }
        }

        return block;
    }

    @SuppressWarnings("unchecked")
    private HBox buildSimpleConditionRow(Map<String, Object> condMap) {
        String operator = "";
        List<Object> operands = null;

        for (Map.Entry<String, Object> entry : condMap.entrySet()) {
            operator = entry.getKey();
            if (entry.getValue() instanceof List) {
                operands = (List<Object>) entry.getValue();
            }
            break;
        }

        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Object leftVal = operands != null && operands.size() > 0 ? operands.get(0) : "";
        Object rightVal = operands != null && operands.size() > 1 ? operands.get(1) : "";

        javafx.scene.Node leftControl = buildOperandControl(leftVal);
        javafx.scene.Node rightControl = buildOperandControl(rightVal);

        ComboBox<String> opBox = new ComboBox<>();
        opBox.getItems().addAll("=", "neq", ">", "ge", "<", "le");
        opBox.setValue(operator);
        opBox.setPrefWidth(65);

        // Cell factory to display unicode symbols
        opBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(OP_SYMBOLS.getOrDefault(item, item));
                    setStyle("-fx-font-size: 13px;");
                }
            }
        });
        opBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(OP_SYMBOLS.getOrDefault(item, item));
                    setStyle("-fx-font-size: 13px;");
                }
            }
        });

        Runnable updateCond = () -> {
            String newOp = opBox.getValue() != null ? opBox.getValue() : "=";
            List<Object> args = new ArrayList<>();
            args.add(getControlValue(leftControl, leftVal));
            args.add(getControlValue(rightControl, rightVal));
            condMap.clear();
            condMap.put(newOp, args);
            onChange.run();
        };

        if (leftControl instanceof TextField) {
            ((TextField) leftControl).textProperty().addListener((o, a, b) -> updateCond.run());
        }
        if (rightControl instanceof TextField) {
            ((TextField) rightControl).textProperty().addListener((o, a, b) -> updateCond.run());
        }
        opBox.valueProperty().addListener((o, a, b) -> updateCond.run());

        row.getChildren().addAll(leftControl, opBox, rightControl);
        return row;
    }

    private javafx.scene.Node buildOperandControl(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.containsKey("op")) {
                return buildOpBlock(castMap(value));
            }
            Label label = new Label(compactExpr(value));
            label.setStyle("-fx-font-size: 10px;"
                    + " -fx-font-family: 'Consolas';"
                    + " -fx-background-color: -ControlFillColorTertiaryBrush;"
                    + " -fx-padding: 3 8;"
                    + " -fx-background-radius: 4;");
            label.setMaxWidth(90);
            HBox.setHgrow(label, Priority.ALWAYS);
            label.setTooltip(new Tooltip(stringify(value)));
            label.setOnMousePressed(e -> e.consume());
            label.setOnMouseDragged(e -> e.consume());
            return label;
        }

        String strVal = stringify(value);
        if (isVariableRef(strVal)) {
            return buildVariableChips(strVal);
        }

        TextField field = new TextField(strVal);
        field.setPrefWidth(60);
        field.setStyle("-fx-font-size: 11px;");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setOnMousePressed(e -> e.consume());
        field.setOnMouseDragged(e -> e.consume());
        return field;
    }

    private Object getControlValue(javafx.scene.Node control, Object originalValue) {
        if (control instanceof TextField) {
            return parseValue(((TextField) control).getText());
        }
        return originalValue;
    }

    // --- Utilities ---

    private String compactExpr(Object expr) {
        if (expr instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) expr;
            if (map.containsKey("op")) {
                return map.get("op") + "(...)";
            }
        }
        return stringify(expr);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    private String stringify(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Map || obj instanceof List) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(obj);
            } catch (Exception e) {
                return obj.toString();
            }
        }
        return obj.toString();
    }

    private Object parseValue(String text) {
        if (text == null || text.isEmpty()) return "";
        try { return Integer.parseInt(text); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(text); } catch (NumberFormatException ignored) {}
        if (text.equals("true")) return true;
        if (text.equals("false")) return false;
        return text;
    }
}
