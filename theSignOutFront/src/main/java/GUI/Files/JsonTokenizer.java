package GUI.Files;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.*;

public class JsonTokenizer {

    private static final Font FONT = Font.font("Consolas", 13);
    private static final String KEY_COLOR = "#4a9eff";
    private static final String STRING_COLOR = "#50c878";
    private static final String NUMBER_COLOR = "#e8a838";
    private static final String BOOL_COLOR = "#c678dd";
    private static final String STRUCT_COLOR = "#9b9b9b";

    private static final Pattern TOKEN = Pattern.compile(
            "\"(?:[^\"\\\\]|\\\\.)*\"|" +
            "-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|" +
            "\\btrue\\b|\\bfalse\\b|\\bnull\\b|" +
            "[{}\\[\\]:,]|" +
            "\\s+"
    );

    public static TextFlow colorize(String json) {
        TextFlow flow = new TextFlow();
        Matcher m = TOKEN.matcher(json);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) flow.getChildren().add(styled(json.substring(last, m.start()), STRUCT_COLOR));
            String tok = m.group();
            String color;
            if (tok.startsWith("\"")) {
                color = isKey(json, m.end()) ? KEY_COLOR : STRING_COLOR;
            } else if (tok.matches("-?\\d.*")) {
                color = NUMBER_COLOR;
            } else if (tok.equals("true") || tok.equals("false") || tok.equals("null")) {
                color = BOOL_COLOR;
            } else if (tok.trim().isEmpty()) {
                flow.getChildren().add(styled(tok, STRUCT_COLOR));
                last = m.end();
                continue;
            } else {
                color = STRUCT_COLOR;
            }
            flow.getChildren().add(styled(tok, color));
            last = m.end();
        }
        if (last < json.length()) flow.getChildren().add(styled(json.substring(last), STRUCT_COLOR));
        return flow;
    }

    private static boolean isKey(String json, int pos) {
        for (int i = pos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ':') return true;
            if (!Character.isWhitespace(c)) return false;
        }
        return false;
    }

    private static Text styled(String content, String color) {
        Text t = new Text(content);
        t.setFont(FONT);
        t.setStyle("-fx-fill: " + color + ";");
        return t;
    }
}
