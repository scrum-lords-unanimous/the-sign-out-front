package GUI.Assets;

import Data.Slide.Slide;
import Data.Slide.SlideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SlideStore {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Path ASSETS_DIRECTORY = Paths.get("assets", "slides");
    private static final String IMAGE_EXTENSION = ".png";
    private static final String CONFIG_RESOURCE_PATH = "/JsonRoot/Slides/Slides.json";

    public static void ensureDir() {
        try {
            Files.createDirectories(ASSETS_DIRECTORY);
        } catch (IOException ignored) {
        }
    }

    public static SlideConfig loadConfig() {
        try (InputStream configStream = SlideStore.class.getResourceAsStream(CONFIG_RESOURCE_PATH)) {
            return JSON_MAPPER.readValue(configStream, SlideConfig.class);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Path imagePath(String slideId) {
        return ASSETS_DIRECTORY.resolve(slideId + IMAGE_EXTENSION);
    }

    public static boolean hasImage(String slideId) {
        return Files.exists(imagePath(slideId));
    }

    public static void copyImage(String slideId, File sourceFile) {
        ensureDir();
        try {
            Files.copy(sourceFile.toPath(), imagePath(slideId), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("Failed to copy image: " + exception.getMessage());
        }
    }

    public static void deleteImage(String slideId) {
        try {
            Files.deleteIfExists(imagePath(slideId));
        } catch (IOException exception) {
            System.err.println("Failed to delete image: " + exception.getMessage());
        }
    }

    public static Image loadImage(String slideId) {
        if (!hasImage(slideId)) {
            return null;
        }
        try {
            return new Image(imagePath(slideId).toUri().toString());
        } catch (Exception exception) {
            return null;
        }
    }

    public static List<Image> loadAllImages() {
        SlideConfig config = loadConfig();
        List<Image> images = new ArrayList<>();
        for (Slide slide : config.getSlides()) {
            Image slideImage = loadImage(slide.getId());
            if (slideImage != null) {
                images.add(slideImage);
            }
        }
        return images;
    }
}
