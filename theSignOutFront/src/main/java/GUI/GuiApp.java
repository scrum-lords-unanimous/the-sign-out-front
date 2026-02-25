package GUI;

import com.sun.glass.ui.Window;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import GUI.Assets.AssetsPage;
import GUI.Files.FilesPage;
import GUI.Run.RunPage;
import GUI.SimEditor.SimulationsPage;
import FluentUIJavaFxKit.CustomTitleBar;
import FluentUIJavaFxKit.HotReloader;
import FluentUIJavaFxKit.NativeWindowHelper;
import FluentUIJavaFxKit.Sidebar;
import FluentUIJavaFxKit.SidebarItem;
import FluentUIJavaFxKit.ThemeManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class GuiApp extends Application {

    private static final double CONTENT_PADDING_VERTICAL = 36;
    private static final int DEFAULT_WINDOW_WIDTH = 800;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;
    private static final String PAGE_TITLE_STYLE = "-fx-font-size: 28px; -fx-font-weight: bold;";
    private static final int DEFAULT_PAGE_SPACING = 12;

    private StackPane contentArea;
    private BorderPane rootPane;
    private Scene mainScene;
    private ThemeManager themeManager;

    private Sidebar sidebar;
    private CustomTitleBar titleBar;
    private final Deque<String> pageHistory = new ArrayDeque<>();
    private String currentPage;
    private boolean suppressHistory;
    private final HotReloader hotReloader = new HotReloader(Map.of(
            "Run", "GUI.Run.RunPage",
            "Simulations", "GUI.SimEditor.SimulationsPage",
            "Assets", "GUI.Assets.AssetsPage",
            "View and Modify Files", "GUI.Files.FilesPage"
    ));

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("The Sign Out Front");
        loadApplicationIcon(primaryStage);

        primaryStage.initStyle(StageStyle.UNDECORATED);

        Application.setUserAgentStylesheet(ThemeManager.cssUrl("fluent-light.css"));

        rootPane = new BorderPane();

        Image logo = new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png"));
        titleBar = new CustomTitleBar(primaryStage, this::goBack, "The Sign Out Front", logo);
        rootPane.setTop(titleBar);

        sidebar = new Sidebar(List.of(
                new SidebarItem("\uE768", "Run"),
                new SidebarItem("\uE943", "Simulations"),
                new SidebarItem("\uF12B", "Assets"),
                new SidebarItem("\uE8A5", "View and Modify Files", true)
        ), this::onPageSelected);

        contentArea = new StackPane();
        contentArea.setAlignment(Pos.TOP_LEFT);
        contentArea.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            double columnPadding = newWidth.doubleValue() / 4;
            contentArea.setPadding(new Insets(CONTENT_PADDING_VERTICAL, columnPadding, CONTENT_PADDING_VERTICAL, columnPadding));
        });

        rootPane.setLeft(sidebar);
        rootPane.setCenter(contentArea);
        mainScene = new Scene(rootPane, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        mainScene.getStylesheets().add(ThemeManager.cssUrl("sidebar.css"));

        primaryStage.setScene(mainScene);
        primaryStage.show();

        mainScene.setOnKeyPressed(keyEvent -> {
            if (themeManager != null && keyEvent.getCode() == KeyCode.F5) {
                themeManager.reloadCss();
                hotReloader.reload(currentPage, contentArea);
                keyEvent.consume();
            }
        });

        sidebar.selectDefault();

        Platform.runLater(() -> {
            long nativeHandle = Window.getWindows().getFirst().getNativeHandle();
            NativeWindowHelper.apply(nativeHandle);

            themeManager = new ThemeManager(mainScene, rootPane);
            themeManager.setPill(sidebar.getPill());

            var platformPreferences = Platform.getPreferences();
            themeManager.applyTheme(platformPreferences.getColorScheme(), platformPreferences.getAccentColor());

            platformPreferences.colorSchemeProperty().addListener((observable, oldScheme, newScheme) ->
                    themeManager.applyTheme(newScheme, platformPreferences.getAccentColor()));
            platformPreferences.accentColorProperty().addListener((observable, oldColor, newColor) ->
                    themeManager.applyTheme(platformPreferences.getColorScheme(), newColor));
        });
    }

    private void loadApplicationIcon(Stage stage) {
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png")));
        } catch (Exception ignored) {
        }
    }

    private void onPageSelected(String pageName) {
        if (!suppressHistory && currentPage != null && !currentPage.equals(pageName)) {
            pageHistory.push(currentPage);
        }
        currentPage = pageName;
        contentArea.getChildren().setAll(buildPage(pageName));
        titleBar.setBackVisible(!pageHistory.isEmpty());
    }

    private void goBack() {
        if (pageHistory.isEmpty()) {
            return;
        }
        suppressHistory = true;
        sidebar.selectByName(pageHistory.pop());
        suppressHistory = false;
        titleBar.setBackVisible(!pageHistory.isEmpty());
    }

    private VBox buildPage(String pageName) {
        Label pageTitle = new Label(pageName);
        pageTitle.setStyle(PAGE_TITLE_STYLE);

        if (pageName.equals("Run")) {
            return new RunPage().build(pageTitle);
        }
        if (pageName.equals("Simulations")) {
            return new SimulationsPage().build(pageTitle);
        }
        if (pageName.equals("Assets")) {
            return new AssetsPage().build(pageTitle);
        }
        if (pageName.equals("View and Modify Files")) {
            return new FilesPage().build(pageTitle);
        }

        VBox fallbackPage = new VBox(DEFAULT_PAGE_SPACING, pageTitle);
        fallbackPage.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return fallbackPage;
    }
}
