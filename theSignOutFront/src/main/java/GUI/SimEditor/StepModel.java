package GUI.SimEditor;

import java.util.*;

public class StepModel {

    private String id;
    private String type;
    private double nodeX;
    private double nodeY;
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<StepModel> children = new ArrayList<>();
    private final List<StepModel> elseChildren = new ArrayList<>();

    public StepModel(String type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getNodeX() { return nodeX; }
    public void setNodeX(double nodeX) { this.nodeX = nodeX; }

    public double getNodeY() { return nodeY; }
    public void setNodeY(double nodeY) { this.nodeY = nodeY; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getProperties() { return properties; }
    public Object getProperty(String key) { return properties.get(key); }
    public void setProperty(String key, Object value) { properties.put(key, value); }

    public List<StepModel> getChildren() { return children; }
    public List<StepModel> getElseChildren() { return elseChildren; }

    public boolean isContainer() {
        return type.equals("for") || type.equals("for-each") || type.equals("if");
    }

    @SuppressWarnings("unchecked")
    public static StepModel fromJson(Map<String, Object> json) {
        String type = (String) json.get("type");
        StepModel model = new StepModel(type);

        switch (type) {
            case "for":
                model.setProperty("var", json.getOrDefault("var", ""));
                model.setProperty("from", json.getOrDefault("from", 0));
                model.setProperty("to", json.getOrDefault("to", 0));
                model.setProperty("step", json.getOrDefault("step", 1));
                List<Map<String, Object>> forSteps = (List<Map<String, Object>>) json.get("steps");
                if (forSteps != null) {
                    for (Map<String, Object> s : forSteps) {
                        model.children.add(fromJson(s));
                    }
                }
                break;

            case "for-each":
                model.setProperty("collection", json.getOrDefault("collection", ""));
                model.setProperty("as", json.getOrDefault("as", ""));
                List<Map<String, Object>> feSteps = (List<Map<String, Object>>) json.get("steps");
                if (feSteps != null) {
                    for (Map<String, Object> s : feSteps) {
                        model.children.add(fromJson(s));
                    }
                }
                break;

            case "if":
                model.setProperty("condition", json.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> thenSteps = (List<Map<String, Object>>) json.get("then");
                if (thenSteps != null) {
                    for (Map<String, Object> s : thenSteps) {
                        model.children.add(fromJson(s));
                    }
                }
                List<Map<String, Object>> elseSteps = (List<Map<String, Object>>) json.get("else");
                if (elseSteps != null) {
                    for (Map<String, Object> s : elseSteps) {
                        model.elseChildren.add(fromJson(s));
                    }
                }
                break;

            case "set":
                model.setProperty("target", json.getOrDefault("target", ""));
                model.setProperty("value", json.getOrDefault("value", ""));
                break;

            case "print":
                model.setProperty("value", json.getOrDefault("value", ""));
                break;

            case "spawn":
                model.setProperty("count", json.getOrDefault("count", ""));
                model.setProperty("as", json.getOrDefault("as", ""));
                break;

            case "precompute-driveway":
            case "record-slide-view":
            case "slide-report":
                break;
        }

        return model;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", type);

        switch (type) {
            case "for":
                json.put("var", properties.getOrDefault("var", ""));
                json.put("from", properties.getOrDefault("from", 0));
                json.put("to", properties.getOrDefault("to", 0));
                json.put("step", properties.getOrDefault("step", 1));
                List<Map<String, Object>> forSteps = new ArrayList<>();
                for (StepModel child : children) {
                    forSteps.add(child.toJson());
                }
                json.put("steps", forSteps);
                break;

            case "for-each":
                json.put("collection", properties.getOrDefault("collection", ""));
                json.put("as", properties.getOrDefault("as", ""));
                List<Map<String, Object>> feSteps = new ArrayList<>();
                for (StepModel child : children) {
                    feSteps.add(child.toJson());
                }
                json.put("steps", feSteps);
                break;

            case "if":
                json.put("condition", properties.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> thenSteps = new ArrayList<>();
                for (StepModel child : children) {
                    thenSteps.add(child.toJson());
                }
                json.put("then", thenSteps);
                if (!elseChildren.isEmpty()) {
                    List<Map<String, Object>> elseSteps = new ArrayList<>();
                    for (StepModel child : elseChildren) {
                        elseSteps.add(child.toJson());
                    }
                    json.put("else", elseSteps);
                }
                break;

            case "set":
                json.put("target", properties.getOrDefault("target", ""));
                json.put("value", properties.getOrDefault("value", ""));
                break;

            case "print":
                json.put("value", properties.getOrDefault("value", ""));
                break;

            case "spawn":
                json.put("count", properties.getOrDefault("count", ""));
                json.put("as", properties.getOrDefault("as", ""));
                break;

            case "precompute-driveway":
            case "record-slide-view":
            case "slide-report":
                break;
        }

        return json;
    }

    @SuppressWarnings("unchecked")
    public static List<StepModel> fromJsonSteps(Map<String, Object> simJson) {
        List<StepModel> models = new ArrayList<>();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) simJson.get("steps");
        if (steps != null) {
            for (Map<String, Object> s : steps) {
                models.add(fromJson(s));
            }
        }
        return models;
    }

    public static Map<String, Object> toSimJson(String name, List<StepModel> steps) {
        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("name", name);
        List<Map<String, Object>> jsonSteps = new ArrayList<>();
        for (StepModel step : steps) {
            jsonSteps.add(step.toJson());
        }
        sim.put("steps", jsonSteps);
        return sim;
    }

    /**
     * Recursively flattens a tree of StepModels into a single list.
     * Each model keeps its children/elseChildren intact for tree-rebuild,
     * but all nodes appear at the top level of the returned list.
     */
    public static List<StepModel> flattenTree(List<StepModel> roots) {
        List<StepModel> flat = new ArrayList<>();
        for (StepModel root : roots) {
            flattenRecursive(root, flat);
        }
        return flat;
    }

    private static void flattenRecursive(StepModel model, List<StepModel> flat) {
        flat.add(model);
        for (StepModel child : model.getChildren()) {
            flattenRecursive(child, flat);
        }
        for (StepModel child : model.getElseChildren()) {
            flattenRecursive(child, flat);
        }
    }
}
