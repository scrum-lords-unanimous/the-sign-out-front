package GUI.Run;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RankingsOverlay {

    public static VBox build(SlideViewTracker tracker) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setMaxWidth(220);
        box.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: #323232; -fx-background-radius: 8;");
        Label title = new Label("Rankings");
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-font-weight: bold;");
        box.getChildren().add(title);
        update(box, tracker);
        return box;
    }

    public static void update(VBox box, SlideViewTracker tracker) {
        box.getChildren().removeIf(n -> n instanceof HBox);
        if (tracker.size() == 0) return;
        int[] order = tracker.getRankOrder();
        for (int rank = 0; rank < order.length; rank++) {
            int idx = order[rank];
            Label num = new Label((rank + 1) + ".");
            num.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
            num.setMinWidth(20);
            Label name = new Label(tracker.getName(idx));
            name.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);
            Label count = new Label(String.valueOf(tracker.getCount(idx)));
            count.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
            HBox row = new HBox(4, num, name, count);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
    }
}
