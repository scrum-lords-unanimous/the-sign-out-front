package GUI.Assets;

import Data.Slide.Slide;
import Data.Slide.SlideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SlideSetStore {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final Path SETS_DIR = Paths.get("assets", "slide-sets");

    public static void ensureDefaults() {
        try {
            Files.createDirectories(SETS_DIR);
            Path defaultJson = SETS_DIR.resolve("Default.json");
            if (!Files.exists(defaultJson)) {
                SlideConfig src = SlideStore.loadConfig();
                mapper.writeValue(defaultJson.toFile(), src);
                Path defaultImgs = SETS_DIR.resolve("Default");
                Files.createDirectories(defaultImgs);
                for (Slide s : src.getSlides()) {
                    Path old = SlideStore.imagePath(s.getId());
                    if (Files.exists(old))
                        Files.copy(old, defaultImgs.resolve(s.getId() + ".png"), StandardCopyOption.REPLACE_EXISTING);
                }
                if (!Files.exists(SETS_DIR.resolve("active.txt")))
                    Files.writeString(SETS_DIR.resolve("active.txt"), "Default");
            }
        } catch (IOException e) {
            System.err.println("Failed to bootstrap slide sets: " + e.getMessage());
        }
    }

    public static List<String> listSets() {
        ensureDefaults();
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SETS_DIR, "*.json")) {
            for (Path p : ds) {
                String f = p.getFileName().toString();
                names.add(f.substring(0, f.length() - 5));
            }
        } catch (IOException ignored) {}
        Collections.sort(names);
        return names;
    }

    public static SlideConfig loadSet(String name) {
        try {
            return mapper.readValue(SETS_DIR.resolve(name + ".json").toFile(), SlideConfig.class);
        } catch (IOException e) { return null; }
    }

    public static void saveSet(String name, SlideConfig config) {
        ensureDefaults();
        try {
            Files.createDirectories(SETS_DIR.resolve(name));
            mapper.writeValue(SETS_DIR.resolve(name + ".json").toFile(), config);
        } catch (IOException e) {
            System.err.println("Failed to save set: " + e.getMessage());
        }
    }

    public static void deleteSet(String name) {
        try {
            Files.deleteIfExists(SETS_DIR.resolve(name + ".json"));
            Path dir = SETS_DIR.resolve(name);
            if (Files.isDirectory(dir))
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for (Path p : ds) Files.deleteIfExists(p);
                }
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {}
    }

    public static void copySetImage(String setName, String slideId, File source) {
        try {
            Path dir = SETS_DIR.resolve(setName);
            Files.createDirectories(dir);
            Files.copy(source.toPath(), dir.resolve(slideId + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to copy set image: " + e.getMessage());
        }
    }

    public static void deleteSetImage(String setName, String slideId) {
        try { Files.deleteIfExists(SETS_DIR.resolve(setName).resolve(slideId + ".png")); }
        catch (IOException ignored) {}
    }

    public static Image loadSetImage(String setName, String slideId) {
        Path p = SETS_DIR.resolve(setName).resolve(slideId + ".png");
        if (!Files.exists(p)) return null;
        try { return new Image(p.toUri().toString()); }
        catch (Exception e) { return null; }
    }

    public static List<Image> loadSetImages(String setName) {
        SlideConfig cfg = loadSet(setName);
        if (cfg == null) return List.of();
        List<Image> imgs = new ArrayList<>();
        for (Slide s : cfg.getSlides()) {
            Image img = loadSetImage(setName, s.getId());
            if (img != null) imgs.add(img);
        }
        return imgs;
    }

    public static List<Slide> loadSlidesWithImages(String setName) {
        SlideConfig cfg = loadSet(setName);
        if (cfg == null) return List.of();
        List<Slide> result = new ArrayList<>();
        for (Slide s : cfg.getSlides()) {
            if (Files.exists(SETS_DIR.resolve(setName).resolve(s.getId() + ".png")))
                result.add(s);
        }
        return result;
    }

    public static String getActiveSet() {
        ensureDefaults();
        try { return Files.readString(SETS_DIR.resolve("active.txt")).trim(); }
        catch (IOException e) { return "Default"; }
    }

    public static void setActiveSet(String name) {
        ensureDefaults();
        try { Files.writeString(SETS_DIR.resolve("active.txt"), name); }
        catch (IOException ignored) {}
    }

    public static void addSlide(String setName, Slide slide) {
        SlideConfig cfg = loadSet(setName);
        if (cfg == null) return;
        cfg.getSlides().add(slide);
        saveSet(setName, cfg);
    }

    public static void removeSlide(String setName, String slideId) {
        SlideConfig cfg = loadSet(setName);
        if (cfg == null) return;
        cfg.getSlides().removeIf(s -> s.getId().equals(slideId));
        saveSet(setName, cfg);
        deleteSetImage(setName, slideId);
    }
}
