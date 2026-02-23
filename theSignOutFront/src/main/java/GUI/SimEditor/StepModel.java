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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getNodeX() {
        return nodeX;
    }

    public void setNodeX(double nodeX) {
        this.nodeX = nodeX;
    }

    public double getNodeY() {
        return nodeY;
    }

    public void setNodeY(double nodeY) {
        this.nodeY = nodeY;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public List<StepModel> getChildren() {
        return children;
    }

    public List<StepModel> getElseChildren() {
        return elseChildren;
    }

    public boolean isContainer() {
        return type.equals("for")
            || type.equals("for-each")
            || type.equals("if")
            || type.equals("while");
    }

    @SuppressWarnings("unchecked")
    public static StepModel fromJson(Map<String, Object> jsonMap) {
        String stepType = (String) jsonMap.get("type");
        StepModel model = new StepModel(stepType);

        switch (stepType) {
            case "for":
                model.setProperty("var", jsonMap.getOrDefault("var", ""));
                model.setProperty("from", jsonMap.getOrDefault("from", 0));
                model.setProperty("to", jsonMap.getOrDefault("to", 0));
                model.setProperty("step", jsonMap.getOrDefault("step", 1));
                List<Map<String, Object>> forBodySteps = (List<Map<String, Object>>) jsonMap.get("steps");
                if (forBodySteps != null) {
                    for (Map<String, Object> childStep : forBodySteps) {
                        model.children.add(fromJson(childStep));
                    }
                }
                break;

            case "for-each":
                model.setProperty("collection", jsonMap.getOrDefault("collection", ""));
                model.setProperty("as", jsonMap.getOrDefault("as", ""));
                List<Map<String, Object>> forEachBodySteps = (List<Map<String, Object>>) jsonMap.get("steps");
                if (forEachBodySteps != null) {
                    for (Map<String, Object> childStep : forEachBodySteps) {
                        model.children.add(fromJson(childStep));
                    }
                }
                break;

            case "while":
                model.setProperty("condition", jsonMap.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> whileBodySteps = (List<Map<String, Object>>) jsonMap.get("steps");
                if (whileBodySteps != null) {
                    for (Map<String, Object> childStep : whileBodySteps) {
                        model.children.add(fromJson(childStep));
                    }
                }
                break;

            case "if":
                model.setProperty("condition", jsonMap.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> thenBranchSteps = (List<Map<String, Object>>) jsonMap.get("then");
                if (thenBranchSteps != null) {
                    for (Map<String, Object> childStep : thenBranchSteps) {
                        model.children.add(fromJson(childStep));
                    }
                }
                List<Map<String, Object>> elseBranchSteps = (List<Map<String, Object>>) jsonMap.get("else");
                if (elseBranchSteps != null) {
                    for (Map<String, Object> childStep : elseBranchSteps) {
                        model.elseChildren.add(fromJson(childStep));
                    }
                }
                break;

            case "set":
                model.setProperty("target", jsonMap.getOrDefault("target", ""));
                model.setProperty("value", jsonMap.getOrDefault("value", ""));
                break;

            case "print":
                model.setProperty("value", jsonMap.getOrDefault("value", ""));
                break;

            case "spawn":
                model.setProperty("count", jsonMap.getOrDefault("count", ""));
                model.setProperty("as", jsonMap.getOrDefault("as", ""));
                break;

            case "precompute-driveway":
            case "record-slide-view":
            case "slide-report":
                break;
        }

        return model;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("type", type);

        switch (type) {
            case "for":
                jsonMap.put("var", properties.getOrDefault("var", ""));
                jsonMap.put("from", properties.getOrDefault("from", 0));
                jsonMap.put("to", properties.getOrDefault("to", 0));
                jsonMap.put("step", properties.getOrDefault("step", 1));
                List<Map<String, Object>> forBodySteps = new ArrayList<>();
                for (StepModel child : children) {
                    forBodySteps.add(child.toJson());
                }
                jsonMap.put("steps", forBodySteps);
                break;

            case "for-each":
                jsonMap.put("collection", properties.getOrDefault("collection", ""));
                jsonMap.put("as", properties.getOrDefault("as", ""));
                List<Map<String, Object>> forEachBodySteps = new ArrayList<>();
                for (StepModel child : children) {
                    forEachBodySteps.add(child.toJson());
                }
                jsonMap.put("steps", forEachBodySteps);
                break;

            case "while":
                jsonMap.put("condition", properties.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> whileBodySteps = new ArrayList<>();
                for (StepModel child : children) {
                    whileBodySteps.add(child.toJson());
                }
                jsonMap.put("steps", whileBodySteps);
                break;

            case "if":
                jsonMap.put("condition", properties.getOrDefault("condition", new LinkedHashMap<>()));
                List<Map<String, Object>> thenBranchSteps = new ArrayList<>();
                for (StepModel child : children) {
                    thenBranchSteps.add(child.toJson());
                }
                jsonMap.put("then", thenBranchSteps);
                if (!elseChildren.isEmpty()) {
                    List<Map<String, Object>> elseBranchSteps = new ArrayList<>();
                    for (StepModel child : elseChildren) {
                        elseBranchSteps.add(child.toJson());
                    }
                    jsonMap.put("else", elseBranchSteps);
                }
                break;

            case "set":
                jsonMap.put("target", properties.getOrDefault("target", ""));
                jsonMap.put("value", properties.getOrDefault("value", ""));
                break;

            case "print":
                jsonMap.put("value", properties.getOrDefault("value", ""));
                break;

            case "spawn":
                jsonMap.put("count", properties.getOrDefault("count", ""));
                jsonMap.put("as", properties.getOrDefault("as", ""));
                break;

            case "precompute-driveway":
            case "record-slide-view":
            case "slide-report":
                break;
        }

        return jsonMap;
    }

    @SuppressWarnings("unchecked")
    public static List<StepModel> fromJsonSteps(Map<String, Object> simulationJson) {
        List<StepModel> models = new ArrayList<>();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) simulationJson.get("steps");
        if (steps != null) {
            for (Map<String, Object> stepJson : steps) {
                models.add(fromJson(stepJson));
            }
        }
        return models;
    }

    public static Map<String, Object> toSimJson(String simulationName, List<StepModel> steps) {
        Map<String, Object> simulationMap = new LinkedHashMap<>();
        simulationMap.put("name", simulationName);
        List<Map<String, Object>> serializedSteps = new ArrayList<>();
        for (StepModel step : steps) {
            serializedSteps.add(step.toJson());
        }
        simulationMap.put("steps", serializedSteps);
        return simulationMap;
    }

    public static List<StepModel> flattenTree(List<StepModel> roots) {
        List<StepModel> flatList = new ArrayList<>();
        for (StepModel root : roots) {
            flattenRecursive(root, flatList);
        }
        return flatList;
    }

    private static void flattenRecursive(StepModel model, List<StepModel> flatList) {
        flatList.add(model);
        for (StepModel child : model.getChildren()) {
            flattenRecursive(child, flatList);
        }
        for (StepModel elseChild : model.getElseChildren()) {
            flattenRecursive(elseChild, flatList);
        }
    }
}
