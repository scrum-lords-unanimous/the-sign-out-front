package GUI.Assets;

import Data.Slide.Slide;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.UUID;

public class UploadCard extends VBox {

    private static final double CARD_WIDTH = 220;
    private static final double IMAGE_HEIGHT = CARD_WIDTH * 9.0 / 16.0;
    private static final double NAME_LABEL_HEIGHT = IMAGE_HEIGHT * 0.5;
    private static final double CARD_HEIGHT = IMAGE_HEIGHT + NAME_LABEL_HEIGHT;
    private static final double CORNER_ARC_SIZE = 12;
    private static final double ICON_FONT_SIZE = 32;
    private static final double TEXT_FONT_SIZE = 13;
    private static final double CARD_SPACING = 8;
    private static final int SLIDE_ID_SUFFIX_LENGTH = 8;

    public UploadCard(String setName, Runnable onRebuildGrid) {
        setAlignment(Pos.CENTER);
        setSpacing(CARD_SPACING);
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        getStyleClass().addAll("sim-card", "add-card");

        Rectangle cardClip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        cardClip.setArcWidth(CORNER_ARC_SIZE);
        cardClip.setArcHeight(CORNER_ARC_SIZE);
        setClip(cardClip);

        Label addIcon = new Label("\uE710");
        addIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: " + ICON_FONT_SIZE + "px;");

        Label uploadLabel = new Label("Upload");
        uploadLabel.setStyle("-fx-font-size: " + TEXT_FONT_SIZE + "px;");

        getChildren().addAll(addIcon, uploadLabel);

        setOnMouseClicked(clickEvent -> handleUploadClick(setName, onRebuildGrid));
    }

    private void handleUploadClick(String setName, Runnable onRebuildGrid) {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("New Slide");
        nameDialog.setHeaderText(null);
        nameDialog.setContentText("Slide name:");
        nameDialog.showAndWait().ifPresent(slideName -> {
            if (slideName.isBlank()) {
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose slide image");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
            if (selectedFile == null) {
                return;
            }

            String slideId = "slide-" + UUID.randomUUID().toString().substring(0, SLIDE_ID_SUFFIX_LENGTH);
            SlideSetStore.addSlide(setName, new Slide(slideId, slideName));
            SlideSetStore.copySetImage(setName, slideId, selectedFile);
            onRebuildGrid.run();
        });
    }
}
