package FluentUIJavaFxKit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class CustomTitleBar extends HBox {

    private static final double TITLE_BAR_HEIGHT = 38;
    private static final double LOGO_HEIGHT = 16;
    private static final double LOGO_LEFT_MARGIN = 12;
    private static final double TITLE_LEFT_MARGIN = 8;
    private static final double BACK_BUTTON_VERTICAL_MARGIN = 2;
    private static final double BACK_BUTTON_LEFT_MARGIN = 2;
    private static final double BACK_VISIBLE_OPACITY = 1.0;
    private static final double BACK_DISABLED_OPACITY = 0.75;
    private static final int LEFT_CLIENT_WIDTH = 48;

    private static final String ICON_BACK = "\uE72B";
    private static final String ICON_MINIMIZE = "\uE921";
    private static final String ICON_MAXIMIZE = "\uE922";
    private static final String ICON_RESTORE = "\uE923";
    private static final String ICON_CLOSE = "\uE8BB";

    private final Button backButton;

    public CustomTitleBar(Stage stage, Runnable onBackAction, String title, Image logo) {
        getStyleClass().add("custom-title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(TITLE_BAR_HEIGHT);
        setMinHeight(TITLE_BAR_HEIGHT);
        setMaxHeight(TITLE_BAR_HEIGHT);

        backButton = new Button(ICON_BACK);
        backButton.getStyleClass().addAll("caption-button", "back-button");
        backButton.setOnAction(event -> {
            if (onBackAction != null) {
                onBackAction.run();
            }
        });
        FluentAnimations.addPressAnimation(backButton);
        HBox.setMargin(backButton, new Insets(BACK_BUTTON_VERTICAL_MARGIN, 0, BACK_BUTTON_VERTICAL_MARGIN, BACK_BUTTON_LEFT_MARGIN));
        setBackVisible(false);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-bar-text");
        titleLabel.setMouseTransparent(true);
        HBox.setMargin(titleLabel, new Insets(0, 0, 0, TITLE_LEFT_MARGIN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeButton = createCaptionButton(ICON_MINIMIZE);
        minimizeButton.getStyleClass().add("window-button");
        minimizeButton.setOnAction(event -> NativeWindowHelper.minimize());

        Button maximizeButton = createCaptionButton(ICON_MAXIMIZE);
        maximizeButton.getStyleClass().add("window-button");
        maximizeButton.setOnAction(event -> {
            if (stage.isMaximized()) {
                NativeWindowHelper.restore();
            } else {
                NativeWindowHelper.maximize();
            }
        });

        Button closeButton = createCaptionButton(ICON_CLOSE);
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(event -> stage.close());

        if (logo != null) {
            ImageView logoImage = new ImageView(logo);
            logoImage.setFitHeight(LOGO_HEIGHT);
            logoImage.setPreserveRatio(true);
            logoImage.setMouseTransparent(true);
            HBox.setMargin(logoImage, new Insets(0, 0, 0, LOGO_LEFT_MARGIN));
            getChildren().addAll(backButton, logoImage, titleLabel, spacer, minimizeButton, maximizeButton, closeButton);
        } else {
            HBox.setMargin(titleLabel, new Insets(0, 0, 0, LOGO_LEFT_MARGIN));
            getChildren().addAll(backButton, titleLabel, spacer, minimizeButton, maximizeButton, closeButton);
        }

        stage.maximizedProperty().addListener((observable, previouslyMaximized, isMaximized) ->
                maximizeButton.setText(isMaximized ? ICON_RESTORE : ICON_MAXIMIZE));
    }

    public void setBackVisible(boolean visible) {
        backButton.setDisable(!visible);
        backButton.setOpacity(visible ? BACK_VISIBLE_OPACITY : BACK_DISABLED_OPACITY);
        NativeWindowHelper.leftClientWidth = LEFT_CLIENT_WIDTH;
    }

    private Button createCaptionButton(String iconText) {
        Button captionButton = new Button(iconText);
        captionButton.getStyleClass().add("caption-button");
        captionButton.setFocusTraversable(false);
        return captionButton;
    }
}
