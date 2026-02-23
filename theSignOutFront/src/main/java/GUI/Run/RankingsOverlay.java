package GUI.Run;

import GUI.Assets.SlideSetStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class RankingsOverlay {

    private static final double CARD_WIDTH = 140;
    private static final double NAME_HEIGHT = 40;
    private static final double IMAGE_HEIGHT = 80;
    private static final double CARD_HEIGHT = IMAGE_HEIGHT + NAME_HEIGHT;
    private static final int CARD_SPACING = 12;
    private static final int PILL_INNER_SPACING = 3;
    private static final int PILL_PADDING_VERTICAL = 2;
    private static final int PILL_PADDING_HORIZONTAL = 8;
    private static final int IMAGE_PANE_PADDING = 6;
    private static final int NAME_PADDING_VERTICAL = 8;
    private static final int NAME_PADDING_HORIZONTAL = 6;
    private static final int CLIP_ARC_SIZE = 12;

    public static VBox build(SlideViewTracker tracker, String setName) {
        HBox row = new HBox(CARD_SPACING);
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Rankings");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -TextFillColorPrimaryBrush; -fx-font-weight: bold;");

        VBox container = new VBox(CARD_SPACING, titleLabel, row);
        container.setMaxHeight(Region.USE_PREF_SIZE);
        update(container, tracker, setName);
        return container;
    }

    public static void update(VBox container, SlideViewTracker tracker, String setName) {
        if (container.getChildren().size() < 2) {
            return;
        }
        HBox row = (HBox) container.getChildren().get(1);
        row.getChildren().clear();
        if (tracker.size() == 0) {
            return;
        }

        int[] rankOrder = tracker.getRankOrder();
        for (int rank = 0; rank < rankOrder.length; rank++) {
            int slideIndex = rankOrder[rank];
            VBox card = buildRankCard(tracker, setName, rank, slideIndex);
            row.getChildren().add(card);
        }
    }

    private static VBox buildRankCard(SlideViewTracker tracker, String setName, int rank, int slideIndex) {
        ImageView thumbnail = buildThumbnail(setName, tracker.getId(slideIndex));

        Label rankPill = new Label("#" + (rank + 1));
        rankPill.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorPrimaryBrush; -fx-font-weight: bold; "
            + "-fx-background-color: -ControlFillColorDefaultBrush; -fx-background-radius: 8; "
            + "-fx-padding: " + PILL_PADDING_VERTICAL + " " + PILL_PADDING_HORIZONTAL + ";");

        HBox viewsPill = buildViewsPill(tracker.getCount(slideIndex));

        Region pillSpacer = new Region();
        HBox.setHgrow(pillSpacer, Priority.ALWAYS);

        HBox pillRow = new HBox(rankPill, pillSpacer, viewsPill);
        pillRow.setPadding(new Insets(IMAGE_PANE_PADDING, IMAGE_PANE_PADDING, 0, IMAGE_PANE_PADDING));
        pillRow.setAlignment(Pos.TOP_CENTER);

        StackPane imagePane = new StackPane(thumbnail, pillRow);
        imagePane.setPrefSize(CARD_WIDTH, IMAGE_HEIGHT);
        imagePane.setMaxSize(CARD_WIDTH, IMAGE_HEIGHT);
        imagePane.setClip(new Rectangle(CARD_WIDTH, IMAGE_HEIGHT));
        StackPane.setAlignment(pillRow, Pos.TOP_CENTER);

        Label nameLabel = new Label(tracker.getName(slideIndex));
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -TextFillColorPrimaryBrush; -fx-font-weight: bold;");
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setPadding(new Insets(NAME_PADDING_VERTICAL, NAME_PADDING_HORIZONTAL,
            NAME_PADDING_VERTICAL, NAME_PADDING_HORIZONTAL));
        nameLabel.setPrefWidth(CARD_WIDTH);
        nameLabel.setPrefHeight(NAME_HEIGHT);

        VBox card = new VBox(0, imagePane, nameLabel);
        card.setPrefWidth(CARD_WIDTH);
        card.setStyle("-fx-background-color: -ControlFillColorDefaultBrush;");
        Rectangle clipRect = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clipRect.setArcWidth(CLIP_ARC_SIZE);
        clipRect.setArcHeight(CLIP_ARC_SIZE);
        card.setClip(clipRect);

        return card;
    }

    private static ImageView buildThumbnail(String setName, String slideId) {
        Image image = SlideSetStore.loadSetImage(setName, slideId);
        ImageView thumbnail = new ImageView(image);
        thumbnail.setPreserveRatio(true);

        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            double imageAspectRatio = image.getWidth() / image.getHeight();
            double cardAspectRatio = CARD_WIDTH / IMAGE_HEIGHT;
            if (imageAspectRatio > cardAspectRatio) {
                thumbnail.setFitHeight(IMAGE_HEIGHT);
            } else {
                thumbnail.setFitWidth(CARD_WIDTH);
            }
        } else {
            thumbnail.setFitWidth(CARD_WIDTH);
            thumbnail.setFitHeight(IMAGE_HEIGHT);
        }

        return thumbnail;
    }

    private static HBox buildViewsPill(int viewCount) {
        Label eyeIcon = new Label("\uE7B3");
        eyeIcon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 11px; "
            + "-fx-text-fill: -TextFillColorPrimaryBrush;");

        Label viewCountLabel = new Label(String.valueOf(viewCount));
        viewCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -TextFillColorPrimaryBrush; -fx-font-weight: bold;");

        HBox viewsPill = new HBox(PILL_INNER_SPACING, eyeIcon, viewCountLabel);
        viewsPill.setAlignment(Pos.CENTER);
        viewsPill.setMaxHeight(Region.USE_PREF_SIZE);
        viewsPill.setStyle("-fx-background-color: -ControlFillColorDefaultBrush; -fx-background-radius: 8; "
            + "-fx-padding: " + PILL_PADDING_VERTICAL + " " + PILL_PADDING_HORIZONTAL + ";");

        return viewsPill;
    }
}
