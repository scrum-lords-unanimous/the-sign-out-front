package GUI.Run;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DayStatsDialog {

    private static final int DIALOG_SPACING = 12;
    private static final int LIST_ITEM_SPACING = 4;
    private static final int DIALOG_PADDING = 24;
    private static final int DIALOG_MIN_WIDTH = 300;
    private static final int DIALOG_BORDER_RADIUS = 12;
    private static final int BUTTON_BORDER_RADIUS = 6;
    private static final int BUTTON_VERTICAL_PADDING = 6;
    private static final int BUTTON_HORIZONTAL_PADDING = 20;
    private static final double RANK_NUMBER_MIN_WIDTH = 24;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    public static void show(SlideViewTracker tracker, Runnable onClose) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        Label titleLabel = new Label("Day Summary");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        int totalViews = tracker.total();
        Label totalViewsLabel = new Label("Total views: " + totalViews);
        totalViewsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ccc;");

        VBox rankingList = buildRankingList(tracker, totalViews);

        Button continueButton = new Button("Continue");
        continueButton.setStyle("-fx-background-color: #555; -fx-text-fill: white; "
            + "-fx-background-radius: " + BUTTON_BORDER_RADIUS + "; "
            + "-fx-padding: " + BUTTON_VERTICAL_PADDING + " " + BUTTON_HORIZONTAL_PADDING + ";");
        continueButton.setOnAction(event -> {
            stage.close();
            onClose.run();
        });

        VBox rootContainer = new VBox(DIALOG_SPACING, titleLabel, totalViewsLabel, rankingList, continueButton);
        rootContainer.setPadding(new Insets(DIALOG_PADDING));
        rootContainer.setAlignment(Pos.CENTER);
        rootContainer.setStyle("-fx-background-color: #323232; -fx-background-radius: " + DIALOG_BORDER_RADIUS + ";");
        rootContainer.setMinWidth(DIALOG_MIN_WIDTH);

        Scene dialogScene = new Scene(rootContainer);
        dialogScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(dialogScene);
        stage.showAndWait();
    }

    private static VBox buildRankingList(SlideViewTracker tracker, int totalViews) {
        VBox list = new VBox(LIST_ITEM_SPACING);
        int[] rankOrder = tracker.getRankOrder();

        for (int rank = 0; rank < rankOrder.length; rank++) {
            int slideIndex = rankOrder[rank];
            int slideViewCount = tracker.getCount(slideIndex);
            double percentage = totalViews > 0 ? (slideViewCount * PERCENTAGE_MULTIPLIER / totalViews) : 0;

            Label rankNumber = new Label((rank + 1) + ".");
            rankNumber.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");
            rankNumber.setMinWidth(RANK_NUMBER_MIN_WIDTH);

            Label slideName = new Label(tracker.getName(slideIndex));
            slideName.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");
            HBox.setHgrow(slideName, Priority.ALWAYS);
            slideName.setMaxWidth(Double.MAX_VALUE);

            Label statsLabel = new Label(String.format("%d (%.0f%%)", slideViewCount, percentage));
            statsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");

            list.getChildren().add(new HBox(LIST_ITEM_SPACING, rankNumber, slideName, statsLabel));
        }

        return list;
    }
}
