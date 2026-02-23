package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionEditor {

    private static final int COMPOUND_BLOCK_SPACING = 6;
    private static final int SIMPLE_ROW_SPACING = 4;
    private static final int OPERATOR_COMBO_WIDTH = 65;
    private static final int OPERAND_FIELD_WIDTH = 60;
    private static final int OPERAND_LABEL_MAX_WIDTH = 90;

    private static final Map<String, String> OPERATOR_DISPLAY_SYMBOLS = Map.of(
        "=", "=",
        "neq", "\u2260",
        ">", ">",
        "ge", "\u2265",
        "<", "<",
        "le", "\u2264"
    );

    private ConditionEditor() {}

    @SuppressWarnings("unchecked")
    public static void addConditionEditor(VBox container, StepModel model, Runnable onChange) {
        Object conditionValue = model.getProperty("condition");
        if (!(conditionValue instanceof Map)) {
            return;
        }
        container.getChildren().add(buildConditionBlock((Map<String, Object>) conditionValue, onChange));
    }

    @SuppressWarnings("unchecked")
    private static javafx.scene.Node buildConditionBlock(Map<String, Object> conditionMap, Runnable onChange) {
        if (conditionMap.containsKey("and") || conditionMap.containsKey("or")) {
            return buildCompoundBlock(conditionMap, onChange);
        }
        return buildSimpleConditionRow(conditionMap, onChange);
    }

    @SuppressWarnings("unchecked")
    private static VBox buildCompoundBlock(Map<String, Object> conditionMap, Runnable onChange) {
        String logicOperator = conditionMap.containsKey("and") ? "and" : "or";
        List<Object> subConditions = (List<Object>) conditionMap.get(logicOperator);

        VBox block = new VBox(COMPOUND_BLOCK_SPACING);
        block.setPadding(new Insets(4, 0, 4, 8));
        block.setStyle("-fx-border-color: transparent transparent transparent -ControlStrongStrokeColorDefaultBrush;"
                + " -fx-border-width: 0 0 0 2;");

        Label operatorBadge = new Label(logicOperator.toUpperCase());
        operatorBadge.setStyle("-fx-background-color: #e8a838;"
                + " -fx-text-fill: white;"
                + " -fx-padding: 2 8;"
                + " -fx-background-radius: 3;"
                + " -fx-font-size: 10px;"
                + " -fx-font-weight: bold;");
        block.getChildren().add(operatorBadge);

        if (subConditions != null) {
            for (Object subCondition : subConditions) {
                if (subCondition instanceof Map) {
                    block.getChildren().add(buildConditionBlock((Map<String, Object>) subCondition, onChange));
                }
            }
        }

        return block;
    }

    @SuppressWarnings("unchecked")
    private static HBox buildSimpleConditionRow(Map<String, Object> conditionMap, Runnable onChange) {
        String operator = "";
        List<Object> operands = null;

        for (Map.Entry<String, Object> entry : conditionMap.entrySet()) {
            operator = entry.getKey();
            if (entry.getValue() instanceof List) {
                operands = (List<Object>) entry.getValue();
            }
            break;
        }

        HBox row = new HBox(SIMPLE_ROW_SPACING);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Object leftOperandValue = operands != null && operands.size() > 0 ? operands.get(0) : "";
        Object rightOperandValue = operands != null && operands.size() > 1 ? operands.get(1) : "";

        javafx.scene.Node leftOperandControl = buildOperandControl(leftOperandValue, onChange);
        javafx.scene.Node rightOperandControl = buildOperandControl(rightOperandValue, onChange);

        ComboBox<String> operatorComboBox = new ComboBox<>();
        operatorComboBox.getItems().addAll("=", "neq", ">", "ge", "<", "le");
        operatorComboBox.setValue(operator);
        operatorComboBox.setPrefWidth(OPERATOR_COMBO_WIDTH);

        operatorComboBox.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(OPERATOR_DISPLAY_SYMBOLS.getOrDefault(item, item));
                    setStyle("-fx-font-size: 13px;");
                }
            }
        });
        operatorComboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(OPERATOR_DISPLAY_SYMBOLS.getOrDefault(item, item));
                    setStyle("-fx-font-size: 13px;");
                }
            }
        });

        Runnable updateCondition = () -> {
            String selectedOperator = operatorComboBox.getValue() != null ? operatorComboBox.getValue() : "=";
            List<Object> updatedOperands = new ArrayList<>();
            updatedOperands.add(getControlValue(leftOperandControl, leftOperandValue));
            updatedOperands.add(getControlValue(rightOperandControl, rightOperandValue));
            conditionMap.clear();
            conditionMap.put(selectedOperator, updatedOperands);
            onChange.run();
        };

        if (leftOperandControl instanceof TextField) {
            ((TextField) leftOperandControl).textProperty().addListener(
                (observable, oldValue, newValue) -> updateCondition.run());
        }
        if (rightOperandControl instanceof TextField) {
            ((TextField) rightOperandControl).textProperty().addListener(
                (observable, oldValue, newValue) -> updateCondition.run());
        }
        operatorComboBox.valueProperty().addListener(
            (observable, oldValue, newValue) -> updateCondition.run());

        row.getChildren().addAll(leftOperandControl, operatorComboBox, rightOperandControl);
        return row;
    }

    private static javafx.scene.Node buildOperandControl(Object value, Runnable onChange) {
        if (value instanceof Map) {
            Map<?, ?> valueMap = (Map<?, ?>) value;
            if (valueMap.containsKey("op")) {
                return OperationBlockView.buildOpBlock(NodeUtils.castMap(value), onChange);
            }
            Label expressionLabel = new Label(NodeUtils.compactExpr(value));
            expressionLabel.setStyle("-fx-font-size: 10px;"
                    + " -fx-font-family: 'Consolas';"
                    + " -fx-background-color: -ControlFillColorTertiaryBrush;"
                    + " -fx-padding: 3 8;"
                    + " -fx-background-radius: 4;");
            expressionLabel.setMaxWidth(OPERAND_LABEL_MAX_WIDTH);
            HBox.setHgrow(expressionLabel, Priority.ALWAYS);
            expressionLabel.setTooltip(new Tooltip(NodeUtils.stringify(value)));
            expressionLabel.setOnMousePressed(mouseEvent -> mouseEvent.consume());
            expressionLabel.setOnMouseDragged(mouseEvent -> mouseEvent.consume());
            return expressionLabel;
        }

        String stringValue = NodeUtils.stringify(value);
        if (VariableChips.isVariableRef(stringValue)) {
            return VariableChips.buildVariableChips(stringValue);
        }

        TextField textField = new TextField(stringValue);
        textField.setPrefWidth(OPERAND_FIELD_WIDTH);
        textField.setStyle("-fx-font-size: 11px;");
        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.setOnMousePressed(mouseEvent -> mouseEvent.consume());
        textField.setOnMouseDragged(mouseEvent -> mouseEvent.consume());
        return textField;
    }

    private static Object getControlValue(javafx.scene.Node control, Object originalValue) {
        if (control instanceof TextField) {
            return NodeUtils.parseValue(((TextField) control).getText());
        }
        return originalValue;
    }
}
