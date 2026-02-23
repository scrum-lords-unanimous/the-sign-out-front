package GUI.SimEditor;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class OperationBlockView {

    private static final int BLOCK_SPACING = 6;

    private OperationBlockView() {}

    @SuppressWarnings("unchecked")
    public static VBox buildOpBlock(Map<String, Object> expressionMap, Runnable onChange) {
        String operationName = String.valueOf(expressionMap.get("op"));
        List<Object> arguments = (List<Object>) expressionMap.get("args");

        VBox block = new VBox(BLOCK_SPACING);
        block.setPadding(new Insets(4, 0, 4, 8));
        block.setStyle("-fx-border-color: transparent transparent transparent -AccentFillColorTertiaryBrush;"
                + " -fx-border-width: 0 0 0 2;");

        Label operationBadge = new Label(operationName);
        operationBadge.setStyle("-fx-background-color: #50c878;"
                + " -fx-text-fill: white;"
                + " -fx-padding: 2 8;"
                + " -fx-background-radius: 3;"
                + " -fx-font-size: 10px;"
                + " -fx-font-weight: bold;");
        block.getChildren().add(operationBadge);

        if (arguments != null) {
            for (int argumentIndex = 0; argumentIndex < arguments.size(); argumentIndex++) {
                Object argument = arguments.get(argumentIndex);
                final int capturedIndex = argumentIndex;
                if (argument instanceof Map) {
                    Map<String, Object> argumentMap = (Map<String, Object>) argument;
                    if (argumentMap.containsKey("op")) {
                        block.getChildren().add(buildOpBlock(argumentMap, onChange));
                    } else {
                        Label expressionLabel = new Label(NodeUtils.compactExpr(argument));
                        expressionLabel.setStyle("-fx-font-size: 10px;"
                                + " -fx-font-family: 'Consolas';"
                                + " -fx-background-color: -ControlFillColorTertiaryBrush;"
                                + " -fx-padding: 3 8;"
                                + " -fx-background-radius: 4;");
                        expressionLabel.setTooltip(new Tooltip(NodeUtils.stringify(argument)));
                        expressionLabel.setOnMousePressed(mouseEvent -> mouseEvent.consume());
                        expressionLabel.setOnMouseDragged(mouseEvent -> mouseEvent.consume());
                        block.getChildren().add(expressionLabel);
                    }
                } else {
                    String stringArgument = NodeUtils.stringify(argument);
                    if (VariableChips.isVariableRef(stringArgument)) {
                        HBox variableChips = VariableChips.buildVariableChips(stringArgument);
                        block.getChildren().add(variableChips);
                    } else {
                        TextField textField = new TextField(stringArgument);
                        textField.setStyle("-fx-font-size: 12px;");
                        HBox.setHgrow(textField, Priority.ALWAYS);
                        textField.textProperty().addListener((observable, oldValue, newValue) -> {
                            arguments.set(capturedIndex, NodeUtils.parseValue(textField.getText()));
                            onChange.run();
                        });
                        textField.setOnMousePressed(mouseEvent -> mouseEvent.consume());
                        textField.setOnMouseDragged(mouseEvent -> mouseEvent.consume());
                        block.getChildren().add(textField);
                    }
                }
            }
        }

        return block;
    }

    @SuppressWarnings("unchecked")
    public static void addValueField(VBox container, String propertyKey, String labelText, StepModel model, Runnable onChange) {
        Object propertyValue = model.getProperty(propertyKey);
        if (propertyValue instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) propertyValue;
            if (valueMap.containsKey("op")) {
                Label fieldLabel = new Label(labelText);
                fieldLabel.setStyle("-fx-font-size: 12px;");
                container.getChildren().add(fieldLabel);
                container.getChildren().add(buildOpBlock(valueMap, onChange));
                return;
            }
        }
    }

    public static boolean isOpValue(StepModel model, String propertyKey) {
        Object propertyValue = model.getProperty(propertyKey);
        return propertyValue instanceof Map && ((Map<?, ?>) propertyValue).containsKey("op");
    }
}
