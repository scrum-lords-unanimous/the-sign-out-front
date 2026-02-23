package GUI.Files;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.*;

public class JsonTokenizer {

    private static final String FONT_FAMILY = "Consolas";
    private static final int FONT_SIZE = 13;
    private static final Font CODE_FONT = Font.font(FONT_FAMILY, FONT_SIZE);

    private static final String NUMBER_PATTERN_REGEX = "-?\\d.*";
    private static final String JSON_KEY_STYLE = "json-key";
    private static final String JSON_STRING_STYLE = "json-string";
    private static final String JSON_NUMBER_STYLE = "json-number";
    private static final String JSON_BOOL_STYLE = "json-bool";
    private static final String JSON_STRUCT_STYLE = "json-struct";

    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
            "\"(?:[^\"\\\\]|\\\\.)*\"|" +
            "-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|" +
            "\\btrue\\b|\\bfalse\\b|\\bnull\\b|" +
            "[{}\\[\\]:,]|" +
            "\\s+"
    );

    public static TextFlow colorize(String json) {
        TextFlow textFlow = new TextFlow();
        Matcher tokenMatcher = JSON_TOKEN_PATTERN.matcher(json);
        int lastMatchEnd = 0;

        while (tokenMatcher.find()) {
            if (tokenMatcher.start() > lastMatchEnd) {
                String unmatchedText = json.substring(lastMatchEnd, tokenMatcher.start());
                textFlow.getChildren().add(buildStyledText(unmatchedText, JSON_STRUCT_STYLE));
            }

            String tokenText = tokenMatcher.group();
            String styleClass = classifyToken(tokenText, json, tokenMatcher.end());

            if (styleClass == null) {
                textFlow.getChildren().add(buildStyledText(tokenText, JSON_STRUCT_STYLE));
                lastMatchEnd = tokenMatcher.end();
                continue;
            }

            textFlow.getChildren().add(buildStyledText(tokenText, styleClass));
            lastMatchEnd = tokenMatcher.end();
        }

        if (lastMatchEnd < json.length()) {
            String remainingText = json.substring(lastMatchEnd);
            textFlow.getChildren().add(buildStyledText(remainingText, JSON_STRUCT_STYLE));
        }

        return textFlow;
    }

    private static String classifyToken(String tokenText, String fullJson, int tokenEndPosition) {
        if (tokenText.startsWith("\"")) {
            return isJsonKey(fullJson, tokenEndPosition) ? JSON_KEY_STYLE : JSON_STRING_STYLE;
        }

        if (tokenText.matches(NUMBER_PATTERN_REGEX)) {
            return JSON_NUMBER_STYLE;
        }

        if (tokenText.equals("true") || tokenText.equals("false") || tokenText.equals("null")) {
            return JSON_BOOL_STYLE;
        }

        if (tokenText.trim().isEmpty()) {
            return null;
        }

        return JSON_STRUCT_STYLE;
    }

    private static boolean isJsonKey(String json, int positionAfterToken) {
        for (int index = positionAfterToken; index < json.length(); index++) {
            char character = json.charAt(index);
            if (character == ':') {
                return true;
            }
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return false;
    }

    private static Text buildStyledText(String content, String styleClass) {
        Text styledText = new Text(content);
        styledText.setFont(CODE_FONT);
        styledText.getStyleClass().add(styleClass);
        return styledText;
    }
}
