package GUI;

import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final String SRC_RESOURCES = "src/main/resources";
    private static final int COLOR_CHANNEL_MAX = 255;
    private static final double SECONDARY_BRIGHTNESS_FACTOR = 1.15;
    private static final double TERTIARY_BRIGHTNESS_FACTOR = 1.3;

    private final Scene scene;
    private final BorderPane rootPane;
    private Rectangle pill;

    public ThemeManager(Scene scene, BorderPane rootPane) {
        this.scene = scene;
        this.rootPane = rootPane;
    }

    public void setPill(Rectangle pill) {
        this.pill = pill;
    }

    public void applyTheme(ColorScheme colorScheme, Color accentColor) {
        String darkCssUrl = cssUrl("dark-theme.css");
        boolean isDarkMode = colorScheme == ColorScheme.DARK;

        if (isDarkMode && !scene.getStylesheets().contains(darkCssUrl)) {
            scene.getStylesheets().add(darkCssUrl);
        } else if (!isDarkMode) {
            scene.getStylesheets().remove(darkCssUrl);
        }

        String accentHex = toHex(accentColor);
        String accentLightHex = toHex(accentColor.deriveColor(0, 1, SECONDARY_BRIGHTNESS_FACTOR, 1));
        String accentLighterHex = toHex(accentColor.deriveColor(0, 1, TERTIARY_BRIGHTNESS_FACTOR, 1));

        rootPane.setStyle(String.join("",
            "-AccentFillColorDefaultBrush:", accentHex, ";",
            "-AccentFillColorSecondaryBrush:", accentLightHex, ";",
            "-AccentFillColorTertiaryBrush:", accentLighterHex, ";",
            "-AccentTextFillColorTertiaryBrush:", accentHex, ";",
            "-AccentFillColorSelectedTextBackgroundBrush:", accentHex, ";",
            "-TextControlElevationBorderFocusedBrush:", accentHex, ";",
            "-fx-accent:", accentHex, ";"));

        if (pill != null) {
            pill.setFill(accentColor);
        }
    }

    public void reloadCss() {
        Application.setUserAgentStylesheet(cssUrl("fluent-light.css"));

        List<String> currentStylesheets = new ArrayList<>(scene.getStylesheets());
        scene.getStylesheets().clear();

        for (String stylesheetUrl : currentStylesheets) {
            String filename = stylesheetUrl.substring(stylesheetUrl.lastIndexOf('/') + 1);
            scene.getStylesheets().add(cssUrl(filename));
        }

        var platformPreferences = javafx.application.Platform.getPreferences();
        applyTheme(platformPreferences.getColorScheme(), platformPreferences.getAccentColor());

        System.out.println("CSS reloaded");
    }

    public static String cssUrl(String filename) {
        File sourceFile = new File(SRC_RESOURCES, filename);
        if (sourceFile.exists()) {
            return sourceFile.toURI().toString();
        }
        return ThemeManager.class.getResource("/" + filename).toExternalForm();
    }

    public static String toHex(Color color) {
        return String.format(
            "#%02x%02x%02x",
            (int) (color.getRed() * COLOR_CHANNEL_MAX),
            (int) (color.getGreen() * COLOR_CHANNEL_MAX),
            (int) (color.getBlue() * COLOR_CHANNEL_MAX)
        );
    }
}
