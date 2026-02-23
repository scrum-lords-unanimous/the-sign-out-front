package GUI.SimEditor;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class VariableChips {

    private static final int CHIP_SPACING = 4;

    private VariableChips() {}

    public static boolean isVariableRef(String text) {
        return text != null && text.contains(".") && !text.contains(" ");
    }

    public static HBox buildVariableChips(String variableReference) {
        String[] segments = variableReference.split("\\.");
        HBox chipsContainer = new HBox(CHIP_SPACING);
        chipsContainer.setAlignment(Pos.CENTER_LEFT);
        chipsContainer.setOnMousePressed(mouseEvent -> mouseEvent.consume());
        chipsContainer.setOnMouseDragged(mouseEvent -> mouseEvent.consume());

        for (String segment : segments) {
            Label chipLabel = new Label(formatSegment(segment));
            chipLabel.setStyle(
                "-fx-background-color: -ControlFillColorSecondaryBrush;"
                + " -fx-text-fill: -TextFillColorPrimaryBrush;"
                + " -fx-padding: 3 8;"
                + " -fx-background-radius: 4;"
                + " -fx-font-size: 11px;");
            chipsContainer.getChildren().add(chipLabel);
        }

        return chipsContainer;
    }

    static String formatSegment(String segment) {
        String withSpacesForHyphens = segment.replace("-", " ");
        String withSpacesForCamelCase = withSpacesForHyphens.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        String[] words = withSpacesForCamelCase.trim().split("\\s+");
        StringBuilder formattedSegment = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (formattedSegment.length() > 0) {
                formattedSegment.append(" ");
            }
            formattedSegment.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                formattedSegment.append(word.substring(1));
            }
        }
        return formattedSegment.toString();
    }
}
