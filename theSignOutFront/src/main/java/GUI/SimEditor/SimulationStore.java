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

    private static final Path SIMS_DIR = Paths.get("simulations");

    public static void ensureDefaults() {
        try {
            if (!Files.exists(SIMS_DIR)) {
                Files.createDirectories(SIMS_DIR);
            }
            Path defaultFile = SIMS_DIR.resolve("Default.json");
            if (!Files.exists(defaultFile)) {
                try (InputStream in = SimulationStore.class
                        .getResourceAsStream("/JsonRoot/Simulation/Simulation.json")) {
                    if (in != null) {
                        Map<String, Object> sim = mapper.readValue(in,
                                new TypeReference<Map<String, Object>>() {});
                        sim.put("name", "Default");
                        mapper.writeValue(defaultFile.toFile(), sim);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to bootstrap simulations: " + e.getMessage());
        }
    }

    public static List<String> listSimulations() {
        ensureDefaults();
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(SIMS_DIR, "*.json")) {
            for (Path p : stream) {
                String filename = p.getFileName().toString();
                names.add(filename.substring(0, filename.length() - 5));
            }
        } catch (IOException e) {
            System.err.println("Failed to list simulations: " + e.getMessage());
        }
        Collections.sort(names);
        return names;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String name) {
        Path file = SIMS_DIR.resolve(name + ".json");
        try {
            return mapper.readValue(file.toFile(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load simulation '" + name + "': " + e.getMessage());
            return null;
        }
    }

    public static void save(String name, Map<String, Object> data) {
        ensureDefaults();
        data.put("name", name);
        Path file = SIMS_DIR.resolve(name + ".json");
        try {
            mapper.writeValue(file.toFile(), data);
        } catch (IOException e) {
            System.err.println("Failed to save simulation '" + name + "': " + e.getMessage());
        }
    }

    public static void delete(String name) {
        Path file = SIMS_DIR.resolve(name + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Failed to delete simulation '" + name + "': " + e.getMessage());
        }
    }

    public static boolean exists(String name) {
        return Files.exists(SIMS_DIR.resolve(name + ".json"));
    }
}
