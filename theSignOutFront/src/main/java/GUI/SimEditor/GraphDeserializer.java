package GUI.SimEditor;

import javafx.application.Platform;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphDeserializer {

    private static final double AUTO_LAYOUT_START_X = 100;
    private static final double AUTO_LAYOUT_START_Y = 80;
    private static final double AUTO_LAYOUT_VERTICAL_STEP = 120;
    private static final double AUTO_LAYOUT_DEPTH_INDENT = 280;
    private static final double AUTO_LAYOUT_NODE_SPACING = 140;
    private static final double AUTO_LAYOUT_CHILD_OFFSET = 20;

    private GraphDeserializer() {}

    @SuppressWarnings("unchecked")
    public static void loadFromJson(Map<String, Object> simulationData, NodeCanvas canvas) {
        canvas.clear();

        List<StepModel> treeSteps = StepModel.fromJsonSteps(simulationData);
        if (treeSteps.isEmpty()) {
            return;
        }

        Map<String, Map<String, Double>> layoutMap = parseLayoutMap(simulationData);

        List<StepModel> allSteps = StepModel.flattenTree(treeSteps);

        boolean hasLayout = !layoutMap.isEmpty();
        double autoPositionX = AUTO_LAYOUT_START_X;
        double autoPositionY = AUTO_LAYOUT_START_Y;

        Map<StepModel, NodeBox> modelToBoxMapping = new LinkedHashMap<>();

        for (StepModel step : allSteps) {
            if (hasLayout && layoutMap.containsKey(step.getId())) {
                Map<String, Double> savedPosition = layoutMap.get(step.getId());
                step.setNodeX(savedPosition.get("x"));
                step.setNodeY(savedPosition.get("y"));
            } else {
                step.setNodeX(autoPositionX);
                step.setNodeY(autoPositionY);
                autoPositionY += AUTO_LAYOUT_VERTICAL_STEP;
            }

            NodeBox box = canvas.addNode(step);
            modelToBoxMapping.put(step, box);
        }

        recreateConnections(treeSteps, modelToBoxMapping, canvas);

        if (!hasLayout) {
            autoLayout(treeSteps, modelToBoxMapping, AUTO_LAYOUT_START_X, AUTO_LAYOUT_START_Y, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Double>> parseLayoutMap(Map<String, Object> simulationData) {
        Map<String, Map<String, Double>> layoutMap = new LinkedHashMap<>();
        Object layoutObject = simulationData.get("layout");
        if (layoutObject instanceof Map) {
            Map<String, Object> rawLayout = (Map<String, Object>) layoutObject;
            for (Map.Entry<String, Object> entry : rawLayout.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> rawPosition = (Map<String, Object>) entry.getValue();
                    Map<String, Double> parsedPosition = new LinkedHashMap<>();
                    parsedPosition.put("x", toDouble(rawPosition.get("x")));
                    parsedPosition.put("y", toDouble(rawPosition.get("y")));
                    layoutMap.put(entry.getKey(), parsedPosition);
                }
            }
        }
        return layoutMap;
    }

    private static void autoLayout(List<StepModel> steps, Map<StepModel, NodeBox> modelToBoxMapping,
                                   double startX, double startY, int depthLevel) {
        double currentX = startX + depthLevel * AUTO_LAYOUT_DEPTH_INDENT;
        double currentY = startY;

        for (StepModel step : steps) {
            NodeBox box = modelToBoxMapping.get(step);
            if (box != null) {
                box.setLayoutX(currentX);
                box.setLayoutY(currentY);
                step.setNodeX(currentX);
                step.setNodeY(currentY);
                currentY += AUTO_LAYOUT_NODE_SPACING;
            }

            if (!step.getChildren().isEmpty()) {
                autoLayout(step.getChildren(), modelToBoxMapping, startX,
                    currentY - AUTO_LAYOUT_NODE_SPACING + AUTO_LAYOUT_CHILD_OFFSET, depthLevel + 1);
                currentY += step.getChildren().size() * AUTO_LAYOUT_NODE_SPACING;
            }
            if (!step.getElseChildren().isEmpty()) {
                autoLayout(step.getElseChildren(), modelToBoxMapping, startX, currentY, depthLevel + 1);
                currentY += step.getElseChildren().size() * AUTO_LAYOUT_NODE_SPACING;
            }
        }
    }

    private static void recreateConnections(List<StepModel> steps, Map<StepModel, NodeBox> modelToBoxMapping,
                                            NodeCanvas canvas) {
        for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
            StepModel step = steps.get(stepIndex);
            NodeBox box = modelToBoxMapping.get(step);
            if (box == null) {
                continue;
            }

            if (stepIndex + 1 < steps.size()) {
                NodeBox nextBox = modelToBoxMapping.get(steps.get(stepIndex + 1));
                if (nextBox != null && box.getExecOut() != null) {
                    deferConnection(box.getExecOut(), nextBox.getExecIn(), canvas);
                }
            }

            if (!step.getChildren().isEmpty()) {
                NodeBox firstChildBox = modelToBoxMapping.get(step.getChildren().get(0));
                if (firstChildBox != null) {
                    if (box.getLoopBodyPort() != null) {
                        deferConnection(box.getLoopBodyPort(), firstChildBox.getExecIn(), canvas);
                    } else if (box.getThenPort() != null) {
                        deferConnection(box.getThenPort(), firstChildBox.getExecIn(), canvas);
                    }
                }
                recreateConnections(step.getChildren(), modelToBoxMapping, canvas);
            }

            if (!step.getElseChildren().isEmpty()) {
                NodeBox firstElseBox = modelToBoxMapping.get(step.getElseChildren().get(0));
                if (firstElseBox != null && box.getElsePort() != null) {
                    deferConnection(box.getElsePort(), firstElseBox.getExecIn(), canvas);
                }
                recreateConnections(step.getElseChildren(), modelToBoxMapping, canvas);
            }
        }
    }

    private static void deferConnection(NodePort sourcePort, NodePort targetPort, NodeCanvas canvas) {
        Platform.runLater(() -> canvas.createConnection(sourcePort, targetPort));
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0;
    }
}
