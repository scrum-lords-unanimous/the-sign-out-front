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

    public static void show(SlideViewTracker tracker, Runnable onClose) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        Label title = new Label("Day Summary");
        title.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        int total = tracker.total();
        Label totalLabel = new Label("Total views: " + total);
        totalLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ccc;");

        VBox list = new VBox(4);
        int[] order = tracker.getRankOrder();
        for (int rank = 0; rank < order.length; rank++) {
            int idx = order[rank];
            int count = tracker.getCount(idx);
            double pct = total > 0 ? (count * 100.0 / total) : 0;
            Label num = new Label((rank + 1) + ".");
            num.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");
            num.setMinWidth(24);
            Label name = new Label(tracker.getName(idx));
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);
            Label stat = new Label(String.format("%d (%.0f%%)", count, pct));
            stat.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaa;");
            list.getChildren().add(new HBox(4, num, name, stat));
        }

        Button cont = new Button("Continue");
        cont.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 20;");
        cont.setOnAction(e -> {
            stage.close();
            onClose.run();
        });

        VBox root = new VBox(12, title, totalLabel, list, cont);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #323232; -fx-background-radius: 12;");
        root.setMinWidth(300);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }
}
