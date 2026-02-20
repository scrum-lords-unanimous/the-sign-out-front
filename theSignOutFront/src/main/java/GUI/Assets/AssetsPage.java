package GUI.Assets;

import Data.Slide.Slide;
import Data.Slide.SlideConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class AssetsPage {

    private ComboBox<String> setSelector;
    private FlowPane grid;

    public VBox build(Label title) {
        SlideSetStore.ensureDefaults();
        setSelector = new ComboBox<>();
        refreshSetList();
        setSelector.setValue(SlideSetStore.getActiveSet());

        Button newSet = new Button("New Set");
        newSet.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("New Slide Set");
            dlg.setHeaderText(null);
            dlg.setContentText("Set name:");
            dlg.showAndWait().ifPresent(name -> {
                if (name.isBlank()) return;
                SlideConfig cfg = new SlideConfig();
                cfg.setCycleDuration(10);
                cfg.setSlides(new ArrayList<>());
                SlideSetStore.saveSet(name, cfg);
                refreshSetList();
                setSelector.setValue(name);
            });
        });

        Button deleteSet = new Button("Delete Set");
        deleteSet.setOnAction(e -> {
            String sel = setSelector.getValue();
            if (sel == null) return;
            SlideSetStore.deleteSet(sel);
            refreshSetList();
            if (!setSelector.getItems().isEmpty()) setSelector.setValue(setSelector.getItems().get(0));
        });

        Button addSlide = new Button("Add Slide");
        addSlide.setOnAction(e -> {
            String sel = setSelector.getValue();
            if (sel == null) return;
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Add Slide");
            dlg.setHeaderText(null);
            dlg.setContentText("Slide name:");
            dlg.showAndWait().ifPresent(name -> {
                if (name.isBlank()) return;
                String id = "slide-" + UUID.randomUUID().toString().substring(0, 8);
                SlideSetStore.addSlide(sel, new Slide(id, name));
                rebuildGrid();
            });
        });

        HBox toolbar = new HBox(8, setSelector, newSet, deleteSet, addSlide);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        setSelector.setOnAction(e -> {
            String sel = setSelector.getValue();
            if (sel != null) {
                SlideSetStore.setActiveSet(sel);
                rebuildGrid();
            }
        });

        grid = new FlowPane(12, 12);
        rebuildGrid();

        VBox page = new VBox(12, title, toolbar, grid);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    private void refreshSetList() {
        String prev = setSelector.getValue();
        setSelector.getItems().setAll(SlideSetStore.listSets());
        if (prev != null && setSelector.getItems().contains(prev)) setSelector.setValue(prev);
    }

    private void rebuildGrid() {
        grid.getChildren().clear();
        String setName = setSelector.getValue();
        if (setName == null) return;
        SlideConfig cfg = SlideSetStore.loadSet(setName);
        if (cfg == null || cfg.getSlides() == null) return;
        for (Slide slide : cfg.getSlides()) grid.getChildren().add(buildCard(slide, setName));
    }

    private VBox buildCard(Slide slide, String setName) {
        Label name = new Label(slide.getName());
        name.setStyle("-fx-font-size: 14px;");

        ImageView thumb = new ImageView();
        thumb.setFitWidth(150);
        thumb.setFitHeight(100);
        thumb.setPreserveRatio(true);
        refreshThumb(thumb, setName, slide.getId());

        Button uploadBtn = new Button("Upload");
        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose slide image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(grid.getScene().getWindow());
            if (file != null) {
                SlideSetStore.copySetImage(setName, slide.getId(), file);
                refreshThumb(thumb, setName, slide.getId());
            }
        });

        VBox card = new VBox(8, name, thumb, uploadBtn);
        card.setPadding(new Insets(16));
        card.setPrefSize(180, 180);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("sim-card");

        ContextMenu ctx = new ContextMenu();
        MenuItem delImg = new MenuItem("Delete Image");
        delImg.setOnAction(e -> {
            SlideSetStore.deleteSetImage(setName, slide.getId());
            refreshThumb(thumb, setName, slide.getId());
        });
        MenuItem removeSlide = new MenuItem("Remove Slide");
        removeSlide.setOnAction(e -> {
            SlideSetStore.removeSlide(setName, slide.getId());
            rebuildGrid();
        });
        ctx.getItems().addAll(delImg, removeSlide);
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private void refreshThumb(ImageView thumb, String setName, String slideId) {
        Image img = SlideSetStore.loadSetImage(setName, slideId);
        thumb.setImage(img);
    }
}
