package GUI;

import com.sun.glass.ui.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.Duration;
import GUI.Assets.AssetsPage;
import GUI.Files.FilesPage;
import GUI.Run.RunPage;
import GUI.SimEditor.SimulationsPage;

public class GuiApp extends Application {

    private StackPane contentArea;
    private Button activeButton;
    private BorderPane root;
    private Scene scene;
    private long nativeWindowHandle;
    private Rectangle pill;
    private Group pillGroup;
    private double sidebarWidth;
    private static final double PILL_HEIGHT = 16;
    private static final String SRC_RESOURCES = "src/main/resources";

    @Override
    public void start(Stage stage) {
        stage.setTitle("The Sign Out Front");
        try { stage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png"))); }
        catch (Exception ignored) {}

        Application.setUserAgentStylesheet(cssUrl("fluent-light.css"));

        root = new BorderPane();

        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(300);

        Button runBtn = createSidebarButton("\uE768", "Run");
        Button simBtn = createSidebarButton("\uE943", "Simulations");
        Button assetsBtn = createSidebarButton("\uF12B", "Assets");

        Region divider = new Region();
        divider.getStyleClass().add("sidebar-divider");
        VBox.setMargin(divider, new Insets(4, 8, 4, 8));

        Button filesBtn = createSidebarButton("\uE8A5", "View and Modify Files");

        pill = new Rectangle(3, PILL_HEIGHT);
        pill.setArcWidth(6);
        pill.setArcHeight(6);
        pill.getStyleClass().add("nav-indicator");
        pill.setManaged(false);
        pill.setLayoutX(8);
        pill.setOpacity(0);

        List<Button> navButtons = List.of(runBtn, simBtn, assetsBtn, filesBtn);

        pillGroup = new Group(pill);
        pillGroup.setManaged(false);
        sidebar.getChildren().addAll(runBtn, simBtn, assetsBtn, divider, filesBtn, pillGroup);

        Platform.runLater(() -> sidebarWidth = sidebar.getWidth());

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_LEFT);
        contentArea.widthProperty().addListener((obs, old, w) -> {
            double col = w.doubleValue() / 4;
            contentArea.setPadding(new Insets(36, col, 36, col));
        });

        root.setLeft(sidebar);
        root.setCenter(contentArea);
        scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(cssUrl("sidebar.css"));

        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.F5) { reloadCss(); e.consume(); } });
        stage.setScene(scene);
        stage.show();

        nativeWindowHandle = Window.getWindows().getFirst().getNativeHandle();

        var prefs = Platform.getPreferences();
        applyTheme(prefs.getColorScheme(), prefs.getAccentColor());

        prefs.colorSchemeProperty().addListener((obs, old, s) -> applyTheme(s, prefs.getAccentColor()));
        prefs.accentColorProperty().addListener((obs, old, a) -> applyTheme(prefs.getColorScheme(), a));

        selectButton(runBtn);
    }

    private String cssUrl(String filename) {
        File srcFile = new File(SRC_RESOURCES, filename);
        if (srcFile.exists()) {
            return srcFile.toURI().toString();
        }
        return getClass().getResource("/" + filename).toExternalForm();
    }

    private void reloadCss() {
        Application.setUserAgentStylesheet(cssUrl("fluent-light.css"));

        List<String> current = new ArrayList<>(scene.getStylesheets());
        scene.getStylesheets().clear();

        for (String url : current) {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            scene.getStylesheets().add(cssUrl(filename));
        }

        var prefs = Platform.getPreferences();
        applyTheme(prefs.getColorScheme(), prefs.getAccentColor());

        System.out.println("CSS reloaded");
    }

    private void applyTheme(ColorScheme scheme, Color accent) {
        String darkCss = cssUrl("dark-theme.css");
        boolean isDark = scheme == ColorScheme.DARK;

        if (isDark && !scene.getStylesheets().contains(darkCss)) {
            scene.getStylesheets().add(darkCss);
        } else if (!isDark) {
            scene.getStylesheets().remove(darkCss);
        }

        String accentHex = toHex(accent);
        String accentLight = toHex(accent.deriveColor(0, 1, 1.15, 1));
        String accentLighter = toHex(accent.deriveColor(0, 1, 1.3, 1));

        root.setStyle(String.join("",
            "-AccentFillColorDefaultBrush:", accentHex, ";",
            "-AccentFillColorSecondaryBrush:", accentLight, ";",
            "-AccentFillColorTertiaryBrush:", accentLighter, ";",
            "-AccentTextFillColorTertiaryBrush:", accentHex, ";",
            "-AccentFillColorSelectedTextBackgroundBrush:", accentHex, ";",
            "-TextControlElevationBorderFocusedBrush:", accentHex, ";",
            "-fx-accent:", accentHex, ";"));

        pill.setFill(accent);

        if (isDark) {
            TitleBar.apply(nativeWindowHandle, true, 0x20, 0x20, 0x20);
        } else {
            TitleBar.apply(nativeWindowHandle, false, 0xf3, 0xf3, 0xf3);
        }
    }

    private String toHex(Color c) {
        return String.format(
            "#%02x%02x%02x",
            (int) (c.getRed() * 255),
            (int) (c.getGreen() * 255),
            (int) (c.getBlue() * 255)
        );
    }

    private Button createSidebarButton(String icon, String text) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
            "-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 16px;"
        );
        iconLabel.setMinWidth(20);
        Button button = new Button(text);
        button.setGraphic(iconLabel);
        button.getStyleClass().add("borderless-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> selectButton(button));
        return button;
    }

    private double buttonCenterY(Button b) {
        return b.getBoundsInParent().getMinY() + b.getHeight() / 2;
    }

    private VBox buildPage(String name) {
        Label title = new Label(name);
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        if (name.equals("Run")) return buildRunPage(title);
        if (name.equals("Simulations")) return new SimulationsPage().build(title);
        if (name.equals("Assets")) return new AssetsPage().build(title);
        if (name.equals("View and Modify Files")) return new FilesPage().build(title);

        VBox page = new VBox(12, title);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return page;
    }

    private VBox buildRunPage(Label title) {
        return new RunPage().build(title);
    }

    private void selectButton(Button button) {
        Button prev = activeButton;

        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = button;
        activeButton.getStyleClass().add("active");

        contentArea.getChildren().setAll(buildPage(button.getText()));

        if (prev == null) {
            Platform.runLater(() -> {
                pill.setLayoutY(buttonCenterY(button) - PILL_HEIGHT / 2);
                pill.setOpacity(1);
                clipPillTo(button);
            });
        } else if (prev != button) {
            clipPillTo(prev, button);
            animatePill(prev, button);
        }
    }

    private void clipPillTo(Button... buttons) {
        Shape clip = null;
        for (Button b : buttons) {
            var bounds = b.getBoundsInParent();
            Rectangle r = new Rectangle(0, bounds.getMinY(), sidebarWidth, bounds.getHeight());
            clip = clip == null ? r : Shape.union(clip, r);
        }
        pillGroup.setClip(clip);
    }

    private void animatePill(Button from, Button to) {
        double fromY = buttonCenterY(from) - PILL_HEIGHT / 2;
        double toY = buttonCenterY(to) - PILL_HEIGHT / 2;
        double stretchTop = toY > fromY ? fromY : toY;
        double stretchH = Math.abs(toY - fromY) + PILL_HEIGHT;
        Duration d = Duration.millis(167);
        var ease = Interpolator.EASE_BOTH;
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(pill.layoutYProperty(), fromY),
                new KeyValue(pill.heightProperty(), PILL_HEIGHT)),
            new KeyFrame(d,
                new KeyValue(pill.layoutYProperty(), stretchTop, ease),
                new KeyValue(pill.heightProperty(), stretchH, ease)),
            new KeyFrame(d.add(d),
                new KeyValue(pill.layoutYProperty(), toY, ease),
                new KeyValue(pill.heightProperty(), PILL_HEIGHT, ease)));
        tl.setOnFinished(e -> clipPillTo(to));
        tl.play();
    }
}
