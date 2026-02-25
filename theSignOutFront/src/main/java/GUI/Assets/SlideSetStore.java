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

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final Path SETS_DIRECTORY = Paths.get("assets", "slide-sets");
    private static final String JSON_EXTENSION = ".json";
    private static final String IMAGE_EXTENSION = ".png";
    private static final String ACTIVE_SET_FILENAME = "active.txt";
    private static final String DEFAULT_SET_NAME = "Default";
    private static final int JSON_EXTENSION_LENGTH = JSON_EXTENSION.length();

    public static void ensureDefaults() {
        try {
            Files.createDirectories(SETS_DIRECTORY);
            Path defaultJsonPath = SETS_DIRECTORY.resolve(DEFAULT_SET_NAME + JSON_EXTENSION);

            if (!Files.exists(defaultJsonPath)) {
                SlideConfig sourceConfig = SlideStore.loadConfig();
                JSON_MAPPER.writeValue(defaultJsonPath.toFile(), sourceConfig);

                Path defaultImagesDirectory = SETS_DIRECTORY.resolve(DEFAULT_SET_NAME);
                Files.createDirectories(defaultImagesDirectory);

                for (Slide slide : sourceConfig.getSlides()) {
                    Path originalImagePath = SlideStore.imagePath(slide.getId());
                    if (Files.exists(originalImagePath)) {
                        Path destinationPath = defaultImagesDirectory.resolve(slide.getId() + IMAGE_EXTENSION);
                        Files.copy(originalImagePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                Path activeSetFilePath = SETS_DIRECTORY.resolve(ACTIVE_SET_FILENAME);
                if (!Files.exists(activeSetFilePath)) {
                    Files.writeString(activeSetFilePath, DEFAULT_SET_NAME);
                }
            }
        } catch (IOException exception) {
            System.err.println("Failed to set slide sets: fuck you" + exception.getMessage());
        }
    }

    public static List<String> listSets() {
        ensureDefaults();
        List<String> setNames = new ArrayList<>();
        try (DirectoryStream<Path> jsonFiles = Files.newDirectoryStream(SETS_DIRECTORY, "*" + JSON_EXTENSION)) {
            for (Path jsonFilePath : jsonFiles) {
                String fileName = jsonFilePath.getFileName().toString();
                String setName = fileName.substring(0, fileName.length() - JSON_EXTENSION_LENGTH);
                setNames.add(setName);
            }
        } catch (IOException ignored) {
        }
        Collections.sort(setNames);
        return setNames;
    }

    public static SlideConfig loadSet(String setName) {
        try {
            Path setJsonPath = SETS_DIRECTORY.resolve(setName + JSON_EXTENSION);
            return JSON_MAPPER.readValue(setJsonPath.toFile(), SlideConfig.class);
        } catch (IOException exception) {
            return null;
        }
    }

    public static void saveSet(String setName, SlideConfig config) {
        ensureDefaults();
        try {
            Files.createDirectories(SETS_DIRECTORY.resolve(setName));
            Path setJsonPath = SETS_DIRECTORY.resolve(setName + JSON_EXTENSION);
            JSON_MAPPER.writeValue(setJsonPath.toFile(), config);
        } catch (IOException exception) {
            System.err.println("Failed to save set: fuck you: " + exception.getMessage());
        }
    }

    public static void deleteSet(String setName) {
        try {
            Files.deleteIfExists(SETS_DIRECTORY.resolve(setName + JSON_EXTENSION));
            Path setImagesDirectory = SETS_DIRECTORY.resolve(setName);
            if (Files.isDirectory(setImagesDirectory)) {
                try (DirectoryStream<Path> imageFiles = Files.newDirectoryStream(setImagesDirectory)) {
                    for (Path imageFile : imageFiles) {
                        Files.deleteIfExists(imageFile);
                    }
                }
            }
            Files.deleteIfExists(setImagesDirectory);
        } catch (IOException ignored) {
        }
    }

    public static void copySetImage(String setName, String slideId, File sourceFile) {
        try {
            Path setImagesDirectory = SETS_DIRECTORY.resolve(setName);
            Files.createDirectories(setImagesDirectory);
            Path destinationPath = setImagesDirectory.resolve(slideId + IMAGE_EXTENSION);
            Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("Failed to copy set image: fuck you: " + exception.getMessage());
        }
    }

    public static void deleteSetImage(String setName, String slideId) {
        try {
            Path imagePath = SETS_DIRECTORY.resolve(setName).resolve(slideId + IMAGE_EXTENSION);
            Files.deleteIfExists(imagePath);
        } catch (IOException ignored) {
        }
    }

    public static Image loadSetImage(String setName, String slideId) {
        Path imagePath = SETS_DIRECTORY.resolve(setName).resolve(slideId + IMAGE_EXTENSION);
        if (!Files.exists(imagePath)) {
            return null;
        }
        try {
            return new Image(imagePath.toUri().toString());
        } catch (Exception exception) {
            return null;
        }
    }

    public static List<Image> loadSetImages(String setName) {
        SlideConfig config = loadSet(setName);
        if (config == null) {
            return List.of();
        }
        List<Image> images = new ArrayList<>();
        for (Slide slide : config.getSlides()) {
            Image slideImage = loadSetImage(setName, slide.getId());
            if (slideImage != null) {
                images.add(slideImage);
            }
        }
        return images;
    }

    public static List<Slide> loadSlidesWithImages(String setName) {
        SlideConfig config = loadSet(setName);
        if (config == null) {
            return List.of();
        }
        List<Slide> slidesWithImages = new ArrayList<>();
        for (Slide slide : config.getSlides()) {
            Path imagePath = SETS_DIRECTORY.resolve(setName).resolve(slide.getId() + IMAGE_EXTENSION);
            if (Files.exists(imagePath)) {
                slidesWithImages.add(slide);
            }
        }
        return slidesWithImages;
    }

    public static String getActiveSet() {
        ensureDefaults();
        try {
            return Files.readString(SETS_DIRECTORY.resolve(ACTIVE_SET_FILENAME)).trim();
        } catch (IOException exception) {
            return DEFAULT_SET_NAME;
        }
    }

    public static void setActiveSet(String setName) {
        ensureDefaults();
        try {
            Files.writeString(SETS_DIRECTORY.resolve(ACTIVE_SET_FILENAME), setName);
        } catch (IOException ignored) {
        }
    }

    public static void addSlide(String setName, Slide slide) {
        SlideConfig config = loadSet(setName);
        if (config == null) {
            return;
        }
        config.getSlides().add(slide);
        saveSet(setName, config);
    }

    public static void removeSlide(String setName, String slideId) {
        SlideConfig config = loadSet(setName);
        if (config == null) {
            return;
        }
        config.getSlides().removeIf(slide -> slide.getId().equals(slideId));
        saveSet(setName, config);
        deleteSetImage(setName, slideId);
    }
}
