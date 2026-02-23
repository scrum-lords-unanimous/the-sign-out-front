package GUI.Files;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class DrivewayStore {

    private static final String DRIVEWAY_SUFFIX = "Driveway";
    private static final String JSON_EXTENSION = ".json";
    private static final String DEFAULT_DRIVEWAY_FILENAME = "DefaultDriveway.json";
    private static final String DEFAULT_RESOURCE_PATH = "/JsonRoot/Driveway/Driveway.json";

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path DRIVEWAYS_DIRECTORY = Paths.get("driveways");

    public static void ensureDefaults() {
        try {
            Files.createDirectories(DRIVEWAYS_DIRECTORY);
            Path defaultDrivewayFile = DRIVEWAYS_DIRECTORY.resolve(DEFAULT_DRIVEWAY_FILENAME);

            if (!Files.exists(defaultDrivewayFile)) {
                try (InputStream resourceStream = DrivewayStore.class
                        .getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
                    if (resourceStream != null) {
                        byte[] resourceData = resourceStream.readAllBytes();
                        Files.write(defaultDrivewayFile, resourceData);
                    }
                }
            }
        } catch (IOException exception) {
            System.err.println("Failed to bootstrap driveways: " + exception.getMessage());
        }
    }

    public static boolean exists(String simulationName) {
        ensureDefaults();
        return Files.exists(DRIVEWAYS_DIRECTORY.resolve(simulationName + JSON_EXTENSION));
    }

    public static void createFromDefault(String simulationName) {
        ensureDefaults();
        String drivewayFileName = simulationName + DRIVEWAY_SUFFIX;
        Path targetFile = DRIVEWAYS_DIRECTORY.resolve(drivewayFileName + JSON_EXTENSION);

        if (Files.exists(targetFile)) {
            return;
        }

        Path defaultDrivewayFile = DRIVEWAYS_DIRECTORY.resolve(DEFAULT_DRIVEWAY_FILENAME);
        try {
            Files.copy(defaultDrivewayFile, targetFile);
        } catch (IOException exception) {
            System.err.println("Failed to create driveway for '" + simulationName + "': " + exception.getMessage());
        }
    }

    public static Map<String, Object> load(String simulationName) {
        ensureDefaults();
        String drivewayFileName = simulationName + DRIVEWAY_SUFFIX;
        Path drivewayFile = DRIVEWAYS_DIRECTORY.resolve(drivewayFileName + JSON_EXTENSION);

        if (!Files.exists(drivewayFile)) {
            drivewayFile = DRIVEWAYS_DIRECTORY.resolve(DEFAULT_DRIVEWAY_FILENAME);
        }

        try {
            return mapper.readValue(drivewayFile.toFile(), new TypeReference<>() {});
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load driveway for '" + simulationName + "'", exception);
        }
    }

    public static void delete(String simulationName) {
        String drivewayFileName = simulationName + DRIVEWAY_SUFFIX;
        try {
            Files.deleteIfExists(DRIVEWAYS_DIRECTORY.resolve(drivewayFileName + JSON_EXTENSION));
        } catch (IOException exception) {
            System.err.println("Failed to delete driveway for '" + simulationName + "': " + exception.getMessage());
        }
    }
}
