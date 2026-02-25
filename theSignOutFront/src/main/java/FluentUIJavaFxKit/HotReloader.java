//interacts with gradle to rebuild the project so I don't have to restart it all the time. It is a little fragile because it is executing gradle commands but it's fine. 
package FluentUIJavaFxKit;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

public class HotReloader {

    private static final String PREFIX = "[HotReload] ";
    private final Map<String, String> pageClassMap;
    private volatile boolean compiling;

    public HotReloader(Map<String, String> pageClassMap) {
        this.pageClassMap = pageClassMap;
    }

    public void reload(String currentPage, StackPane contentArea) {
        if (currentPage == null || !pageClassMap.containsKey(currentPage)) {
            System.out.println(PREFIX + "No reloadable page: " + currentPage);
            return;
        }
        if (compiling) {
            System.out.println(PREFIX + "Compile already in progress, skipping.");
            return;
        }
        String className = pageClassMap.get(currentPage);
        Thread thread = new Thread(() -> doReload(className, currentPage, contentArea), "HotReload-Compiler");
        thread.setDaemon(true);
        thread.start();
    }

    private void doReload(String className, String pageName, StackPane contentArea) {
        compiling = true;
        try {
            if (!compileProject()) {
                System.out.println(PREFIX + "Compile failed — UI unchanged.");
                return;
            }
            VBox newPage = buildPage(className, pageName);
            if (newPage != null) {
                Platform.runLater(() -> {
                    contentArea.getChildren().setAll(newPage);
                    System.out.println(PREFIX + "Done — page swapped.");
                });
            }
        } catch (Exception e) {
            System.out.println(PREFIX + "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            compiling = false;
        }
    }

    private boolean compileProject() {
        System.out.println(PREFIX + "Compiling...");
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "gradlew.bat", "compileJava", "processResources", "--quiet");
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] buf = new byte[4096];
            var is = process.getInputStream();
            while (is.read(buf) != -1) { /* drain */ }
            int exit = process.waitFor();
            if (exit != 0) System.out.println(PREFIX + "Gradle exited with code " + exit);
            else System.out.println(PREFIX + "Compile succeeded.");
            return exit == 0;
        } catch (Exception e) {
            System.out.println(PREFIX + "Compile error: " + e.getMessage());
            return false;
        }
    }

    private VBox buildPage(String className, String pageName) {
        try (var loader = new ChildFirstClassLoader(new URL[]{
                new File("build/classes/java/main").toURI().toURL(),
                new File("build/resources/main").toURI().toURL()
        }, getClass().getClassLoader())) {
            Class<?> pageClass = loader.loadClass(className);
            Object instance = pageClass.getDeclaredConstructor().newInstance();
            Label title = new Label(pageName);
            title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
            Method build = pageClass.getMethod("build", Label.class);
            return (VBox) build.invoke(instance, title);
        } catch (Exception e) {
            System.out.println(PREFIX + "Reflection error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
