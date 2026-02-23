package GUI.Assets;

import Data.Slide.Slide;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;

public class SlideCard extends VBox {

    private static final double CARD_WIDTH = 220;
    private static final double IMAGE_HEIGHT = CARD_WIDTH * 9.0 / 16.0;
    private static final double NAME_LABEL_HEIGHT = IMAGE_HEIGHT * 0.5;
    private static final double CORNER_ARC_SIZE = 12;
    private static final double NAME_FONT_SIZE = 13;
    private static final double NAME_PADDING = 8;

    private final ImageView thumbnailView;

    public SlideCard(Slide slide, String setName, Runnable onRebuildGrid) {
        super(0);
        setStyle("-fx-background-color: -ControlFillColorDefaultBrush;");

        thumbnailView = createThumbnailView();
        StackPane imageContainer = createImageContainer(thumbnailView);
        Label nameLabel = createNameLabel(slide.getName());

        refreshThumbnail(setName, slide.getId());

        getChildren().addAll(imageContainer, nameLabel);
        setPrefWidth(CARD_WIDTH);
        setPadding(Insets.EMPTY);

        Rectangle cardClip = new Rectangle(CARD_WIDTH, IMAGE_HEIGHT + NAME_LABEL_HEIGHT);
        cardClip.setArcWidth(CORNER_ARC_SIZE);
        cardClip.setArcHeight(CORNER_ARC_SIZE);
        setClip(cardClip);

        ContextMenu contextMenu = buildContextMenu(slide, setName, onRebuildGrid);
        setOnContextMenuRequested(event ->
                contextMenu.show(this, event.getScreenX(), event.getScreenY()));
    }

    private ImageView createThumbnailView() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(CARD_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(false);
        return imageView;
    }

    private StackPane createImageContainer(ImageView imageView) {
        StackPane container = new StackPane(imageView);
        container.setPrefSize(CARD_WIDTH, IMAGE_HEIGHT);
        return container;
    }

    private Label createNameLabel(String slideName) {
        Label label = new Label(slideName);
        label.setStyle("-fx-font-size: " + NAME_FONT_SIZE + "px; -fx-text-fill: -TextFillColorPrimaryBrush; -fx-font-weight: bold;");
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(NAME_PADDING));
        label.setPrefSize(CARD_WIDTH, NAME_LABEL_HEIGHT);
        return label;
    }

    private ContextMenu buildContextMenu(Slide slide, String setName, Runnable onRebuildGrid) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem replaceImageItem = new MenuItem("Replace Image");
        replaceImageItem.setOnAction(event -> {
            File chosenFile = pickImageFile();
            if (chosenFile != null) {
                SlideSetStore.copySetImage(setName, slide.getId(), chosenFile);
                refreshThumbnail(setName, slide.getId());
            }
        });

        MenuItem deleteImageItem = new MenuItem("Delete Image");
        deleteImageItem.setOnAction(event -> {
            SlideSetStore.deleteSetImage(setName, slide.getId());
            refreshThumbnail(setName, slide.getId());
        });

        MenuItem removeSlideItem = new MenuItem("Remove Slide");
        removeSlideItem.setOnAction(event -> {
            SlideSetStore.removeSlide(setName, slide.getId());
            onRebuildGrid.run();
        });

        contextMenu.getItems().addAll(replaceImageItem, deleteImageItem, removeSlideItem);
        return contextMenu;
    }

    private void refreshThumbnail(String setName, String slideId) {
        Image slideImage = SlideSetStore.loadSetImage(setName, slideId);
        thumbnailView.setImage(slideImage);
    }

    private File pickImageFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose slide image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        return fileChooser.showOpenDialog(getScene().getWindow());
    }
}
