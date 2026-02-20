package GUI.Assets;

import Data.Slide.Slide;
import Data.Slide.SlideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SlideStore {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Path ASSETS_DIR = Paths.get("assets", "slides");

    public static void ensureDir() {
        try { Files.createDirectories(ASSETS_DIR); } catch (IOException ignored) {}
    }

    public static SlideConfig loadConfig() {
        try (InputStream in = SlideStore.class.getResourceAsStream("/JsonRoot/Slides/Slides.json")) {
            return mapper.readValue(in, SlideConfig.class);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Path imagePath(String slideId) {
        return ASSETS_DIR.resolve(slideId + ".png");
    }

    public static boolean hasImage(String slideId) {
        return Files.exists(imagePath(slideId));
    }

    public static void copyImage(String slideId, File source) {
        ensureDir();
        try { Files.copy(source.toPath(), imagePath(slideId), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { System.err.println("Failed to copy image: " + e.getMessage()); }
    }

    public static void deleteImage(String slideId) {
        try { Files.deleteIfExists(imagePath(slideId)); }
        catch (IOException e) { System.err.println("Failed to delete image: " + e.getMessage()); }
    }

    public static Image loadImage(String slideId) {
        if (!hasImage(slideId)) return null;
        try { return new Image(imagePath(slideId).toUri().toString()); }
        catch (Exception e) { return null; }
    }

    public static List<Image> loadAllImages() {
        SlideConfig config = loadConfig();
        List<Image> images = new ArrayList<>();
        for (Slide s : config.getSlides()) {
            Image img = loadImage(s.getId());
            if (img != null) images.add(img);
        }
        return images;
    }
}
