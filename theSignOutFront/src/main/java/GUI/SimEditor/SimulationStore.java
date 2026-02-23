package GUI.SimEditor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SimulationStore {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Path SIMULATIONS_DIRECTORY = Paths.get("simulations");
    private static final String JSON_EXTENSION = ".json";
    private static final String DEFAULT_SIMULATION_RESOURCE_PATH = "/JsonRoot/Simulation/Simulation.json";
    private static final String DEFAULT_SIMULATION_NAME = "Default";

    public static void ensureDefaults() {
        try {
            if (!Files.exists(SIMULATIONS_DIRECTORY)) {
                Files.createDirectories(SIMULATIONS_DIRECTORY);
            }
            Path defaultSimulationFile = SIMULATIONS_DIRECTORY.resolve(DEFAULT_SIMULATION_NAME + JSON_EXTENSION);
            if (!Files.exists(defaultSimulationFile)) {
                try (InputStream resourceStream = SimulationStore.class
                        .getResourceAsStream(DEFAULT_SIMULATION_RESOURCE_PATH)) {
                    if (resourceStream != null) {
                        Map<String, Object> simulationData = mapper.readValue(resourceStream,
                                new TypeReference<Map<String, Object>>() {});
                        simulationData.put("name", DEFAULT_SIMULATION_NAME);
                        mapper.writeValue(defaultSimulationFile.toFile(), simulationData);
                    }
                }
            }
        } catch (IOException ioException) {
            System.err.println("Failed to bootstrap simulations: " + ioException.getMessage());
        }
    }

    public static List<String> listSimulations() {
        ensureDefaults();
        List<String> simulationNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(SIMULATIONS_DIRECTORY, "*" + JSON_EXTENSION)) {
            for (Path filePath : directoryStream) {
                String fileName = filePath.getFileName().toString();
                simulationNames.add(fileName.substring(0, fileName.length() - JSON_EXTENSION.length()));
            }
        } catch (IOException ioException) {
            System.err.println("Failed to list simulations: " + ioException.getMessage());
        }
        Collections.sort(simulationNames);
        return simulationNames;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String simulationName) {
        Path simulationFile = SIMULATIONS_DIRECTORY.resolve(simulationName + JSON_EXTENSION);
        try {
            return mapper.readValue(simulationFile.toFile(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException ioException) {
            System.err.println("Failed to load simulation '" + simulationName + "': " + ioException.getMessage());
            return null;
        }
    }

    public static void save(String simulationName, Map<String, Object> simulationData) {
        ensureDefaults();
        simulationData.put("name", simulationName);
        Path simulationFile = SIMULATIONS_DIRECTORY.resolve(simulationName + JSON_EXTENSION);
        try {
            mapper.writeValue(simulationFile.toFile(), simulationData);
        } catch (IOException ioException) {
            System.err.println("Failed to save simulation '" + simulationName + "': " + ioException.getMessage());
        }
    }

    public static void delete(String simulationName) {
        Path simulationFile = SIMULATIONS_DIRECTORY.resolve(simulationName + JSON_EXTENSION);
        try {
            Files.deleteIfExists(simulationFile);
        } catch (IOException ioException) {
            System.err.println("Failed to delete simulation '" + simulationName + "': " + ioException.getMessage());
        }
    }

    public static boolean exists(String simulationName) {
        return Files.exists(SIMULATIONS_DIRECTORY.resolve(simulationName + JSON_EXTENSION));
    }
}
