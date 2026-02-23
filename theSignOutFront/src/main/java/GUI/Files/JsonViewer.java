package GUI.Files;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class JsonViewer {

    private static final int CONTENT_PADDING = 12;
    private static final int TOOLBAR_SPACING = 8;
    private static final String FILE_NAME_STYLE =
            "-fx-font-size: 14px; -fx-font-weight: bold;";
    private static final String CONTENT_BACKGROUND_STYLE =
            "-fx-background-color: -ControlFillColorDefaultBrush;";
    private static final String SCROLL_PANE_STYLE =
            "-fx-background-radius: 8;";
    private static final String DELETE_BUTTON_STYLE =
            "-fx-text-fill: #ff4444;";
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found";
    private static final String ERROR_READING_PREFIX = "Error reading file: ";

    private static final ObjectMapper mapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonViewer() {}

    public static void showView(VBox panel, FileListPanel.FileEntry fileEntry,
                                BiConsumer<FileListPanel.FileEntry, String> onEdit,
                                Runnable onDelete) {
        panel.getChildren().clear();

        String rawContent = readContent(fileEntry);
        String prettyContent = prettify(rawContent);

        TextFlow colorizedContent = JsonTokenizer.colorize(prettyContent);
        colorizedContent.setPadding(new Insets(CONTENT_PADDING));
        colorizedContent.setStyle(CONTENT_BACKGROUND_STYLE);

        ScrollPane contentScrollPane = buildContentScrollPane(colorizedContent);
        HBox toolbar = buildToolbar(fileEntry, rawContent, onEdit, onDelete);

        panel.getChildren().addAll(toolbar, contentScrollPane);
    }

    private static ScrollPane buildContentScrollPane(TextFlow colorizedContent) {
        ScrollPane contentScrollPane = new ScrollPane(colorizedContent);
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScrollPane.getStyleClass().add("json-viewer");
        contentScrollPane.setStyle(SCROLL_PANE_STYLE);
        VBox.setVgrow(contentScrollPane, Priority.ALWAYS);
        return contentScrollPane;
    }

    private static HBox buildToolbar(FileListPanel.FileEntry fileEntry, String rawContent,
                                     BiConsumer<FileListPanel.FileEntry, String> onEdit,
                                     Runnable onDelete) {
        Label fileNameLabel = new Label(fileEntry.name());
        fileNameLabel.setStyle(FILE_NAME_STYLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(TOOLBAR_SPACING, fileNameLabel, spacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        if (fileEntry.editable()) {
            Button editButton = new Button("Edit");
            editButton.setOnAction(event -> onEdit.accept(fileEntry, rawContent));
            toolbar.getChildren().add(editButton);
        }

        if (fileEntry.deletable()) {
            Button deleteButton = new Button("Delete");
            deleteButton.setStyle(DELETE_BUTTON_STYLE);
            deleteButton.setOnAction(event -> onDelete.run());
            toolbar.getChildren().add(deleteButton);
        }

        return toolbar;
    }

    public static String readContent(FileListPanel.FileEntry fileEntry) {
        try {
            if (fileEntry.resource()) {
                try (InputStream inputStream = JsonViewer.class.getResourceAsStream(fileEntry.path())) {
                    if (inputStream == null) {
                        return FILE_NOT_FOUND_MESSAGE;
                    }
                    return new String(inputStream.readAllBytes());
                }
            }
            return Files.readString(Path.of(fileEntry.path()));
        } catch (IOException exception) {
            return ERROR_READING_PREFIX + exception.getMessage();
        }
    }

    public static String prettify(String json) {
        try {
            Object parsedObject = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedObject);
        } catch (Exception exception) {
            return json;
        }
    }
}
