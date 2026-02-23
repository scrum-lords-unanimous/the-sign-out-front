package GUI.Files;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonEditor {

    private static final int TOOLBAR_SPACING = 8;
    private static final String EDITOR_STYLE =
            "-fx-background-color: #202020; -fx-font-family: Consolas; -fx-font-size: 13px;";
    private static final String NUMBER_PATTERN_REGEX = "-?\\d.*";

    private static final ObjectMapper mapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
            "\"(?:[^\"\\\\]|\\\\.)*\"|" +
            "-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|" +
            "\\btrue\\b|\\bfalse\\b|\\bnull\\b|" +
            "[{}\\[\\]:,]"
    );

    private JsonEditor() {}

    public static void showEdit(VBox panel, FileListPanel.FileEntry fileEntry, String content,
                                Runnable onCancel) {
        panel.getChildren().clear();
        String prettyContent = JsonViewer.prettify(content);

        CodeArea codeEditor = buildCodeEditor(prettyContent);
        VBox.setVgrow(codeEditor, Priority.ALWAYS);

        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> onCancel.run());

        saveButton.setOnAction(event -> {
            saveEditedContent(codeEditor, fileEntry, onCancel);
        });

        HBox toolbar = new HBox(TOOLBAR_SPACING, saveButton, cancelButton);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(toolbar, codeEditor);
    }

    private static CodeArea buildCodeEditor(String initialContent) {
        CodeArea codeEditor = new CodeArea(initialContent);
        codeEditor.setStyle(EDITOR_STYLE);
        codeEditor.getStylesheets().add(
                JsonEditor.class.getResource("/json-editor.css").toExternalForm()
        );

        codeEditor.richChanges()
                .filter(change -> !change.getInserted().equals(change.getRemoved()))
                .subscribe(change -> codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText())));

        codeEditor.setStyleSpans(0, computeHighlighting(initialContent));
        return codeEditor;
    }

    private static void saveEditedContent(CodeArea codeEditor, FileListPanel.FileEntry fileEntry,
                                          Runnable onCancel) {
        String editorText = codeEditor.getText();
        try {
            mapper.readTree(editorText);
            String formattedJson = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(mapper.readTree(editorText));
            Files.writeString(Path.of(fileEntry.path()), formattedJson);
            onCancel.run();
        } catch (Exception exception) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Invalid JSON: " + exception.getMessage());
            errorAlert.showAndWait();
        }
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher tokenMatcher = JSON_TOKEN_PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastMatchEnd = 0;

        while (tokenMatcher.find()) {
            if (tokenMatcher.start() > lastMatchEnd) {
                spansBuilder.add(Collections.emptyList(), tokenMatcher.start() - lastMatchEnd);
            }

            String tokenText = tokenMatcher.group();
            String styleClass = classifyToken(tokenText, text, tokenMatcher.end());
            spansBuilder.add(Collections.singleton(styleClass), tokenText.length());
            lastMatchEnd = tokenMatcher.end();
        }

        if (lastMatchEnd < text.length()) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastMatchEnd);
        }

        return spansBuilder.create();
    }

    private static String classifyToken(String tokenText, String fullText, int tokenEndPosition) {
        if (tokenText.startsWith("\"")) {
            return isJsonKey(fullText, tokenEndPosition) ? "json-key" : "json-string";
        }

        if (tokenText.matches(NUMBER_PATTERN_REGEX)) {
            return "json-number";
        }

        if (tokenText.equals("true") || tokenText.equals("false") || tokenText.equals("null")) {
            return "json-bool";
        }

        return "json-struct";
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
}
