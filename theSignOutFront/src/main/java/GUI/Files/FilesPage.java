package GUI.Files;

import GUI.SimEditor.SimulationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FilesPage {

    private record FileEntry(String name, String path, boolean editable) {}

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final VBox rightPanel = new VBox();
    private FileEntry selected;

    public VBox build(Label title) {
        VBox fileList = buildFileList();
        fileList.setPrefWidth(200);
        fileList.setMinWidth(200);

        ScrollPane listScroll = new ScrollPane(fileList);
        listScroll.setFitToWidth(true);
        listScroll.setFitToHeight(true);
        listScroll.setStyle("-fx-background-color: transparent;");

        rightPanel.setPadding(new Insets(8));
        rightPanel.setSpacing(8);
        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(true);
        HBox.setHgrow(rightScroll, Priority.ALWAYS);

        HBox split = new HBox(8, listScroll, rightScroll);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox page = new VBox(12, title, split);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    private VBox buildFileList() {
        VBox list = new VBox(2);
        list.setPadding(new Insets(4));

        Label resHeader = new Label("Resources");
        resHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 4 8;");
        list.getChildren().add(resHeader);

        List<FileEntry> resources = List.of(
                new FileEntry("Slides.json", "/JsonRoot/Slides/Slides.json", false),
                new FileEntry("Simulation.json", "/JsonRoot/Simulation/Simulation.json", false),
                new FileEntry("Driveway.json", "/JsonRoot/Driveway/Driveway.json", false),
                new FileEntry("Questions.json", "/JsonRoot/InputQuestions/Questions.json", false),
                new FileEntry("DefaultAnswers.json", "/JsonRoot/InputQuestions/DefaultAnswers.json", false),
                new FileEntry("Operations.json", "/JsonRoot/Operations/Operations.json", false)
        );
        for (FileEntry fe : resources) list.getChildren().add(fileItem(fe));

        Label simHeader = new Label("Simulations");
        simHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 8 4 8;");
        list.getChildren().add(simHeader);

        for (String name : SimulationStore.listSimulations()) {
            FileEntry fe = new FileEntry(name + ".json", "simulations/" + name + ".json", true);
            list.getChildren().add(fileItem(fe));
        }
        return list;
    }

    private Button fileItem(FileEntry fe) {
        Button btn = new Button(fe.name);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().addAll("borderless-button", "file-list-item");
        btn.setOnAction(e -> { selected = fe; showView(fe); });
        return btn;
    }

    private String readContent(FileEntry fe) {
        try {
            if (!fe.editable) {
                try (InputStream in = getClass().getResourceAsStream(fe.path)) {
                    if (in == null) return "File not found";
                    return new String(in.readAllBytes());
                }
            }
            return Files.readString(Path.of(fe.path));
        } catch (IOException e) { return "Error reading file: " + e.getMessage(); }
    }

    private void showView(FileEntry fe) {
        rightPanel.getChildren().clear();
        String content = readContent(fe);
        String pretty = prettify(content);
        TextFlow flow = JsonTokenizer.colorize(pretty);
        flow.setPadding(new Insets(12));
        flow.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 6;");

        ScrollPane sp = new ScrollPane(flow);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("json-viewer");
        VBox.setVgrow(sp, Priority.ALWAYS);

        if (fe.editable) {
            Button editBtn = new Button("Edit");
            editBtn.setOnAction(e -> showEdit(fe, content));
            HBox toolbar = new HBox(editBtn);
            toolbar.setAlignment(Pos.CENTER_RIGHT);
            rightPanel.getChildren().addAll(toolbar, sp);
        } else {
            rightPanel.getChildren().add(sp);
        }
    }

    private void showEdit(FileEntry fe, String content) {
        rightPanel.getChildren().clear();
        String pretty = prettify(content);

        TextArea editor = new TextArea(pretty);
        editor.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #d4d4d4; " +
                "-fx-font-family: Consolas; -fx-font-size: 13px;");
        VBox.setVgrow(editor, Priority.ALWAYS);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> showView(fe));

        saveBtn.setOnAction(e -> {
            String text = editor.getText();
            try {
                mapper.readTree(text);
                String formatted = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(mapper.readTree(text));
                Files.writeString(Path.of(fe.path), formatted);
                showView(fe);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid JSON: " + ex.getMessage());
                alert.showAndWait();
            }
        });

        HBox toolbar = new HBox(8, saveBtn, cancelBtn);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        rightPanel.getChildren().addAll(toolbar, editor);
    }

    private String prettify(String json) {
        try {
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) { return json; }
    }
}
