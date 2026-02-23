package GUI.SimEditor;

import java.util.*;

public class GraphSerializer {

    private GraphSerializer() {}

    public static List<StepModel> buildStepTree(List<NodeBox> nodeBoxes, List<NodeConnection> connections) {
        List<NodeBox> rootNodes = new ArrayList<>();
        for (NodeBox box : nodeBoxes) {
            if (!box.getExecIn().isConnected()) {
                rootNodes.add(box);
            }
        }

        rootNodes.sort(Comparator.comparingDouble(NodeBox::getLayoutY));

        List<StepModel> topLevelSteps = new ArrayList<>();
        Set<NodeBox> visitedNodes = new HashSet<>();

        for (NodeBox rootNode : rootNodes) {
            if (!visitedNodes.contains(rootNode)) {
                buildChain(rootNode, topLevelSteps, visitedNodes);
            }
        }

        return topLevelSteps;
    }

    private static void buildChain(NodeBox currentBox, List<StepModel> chain, Set<NodeBox> visitedNodes) {
        if (currentBox == null || visitedNodes.contains(currentBox)) {
            return;
        }
        visitedNodes.add(currentBox);

        StepModel model = currentBox.getModel();

        model.getChildren().clear();
        model.getElseChildren().clear();

        if (currentBox.getLoopBodyPort() != null && currentBox.getLoopBodyPort().isConnected()) {
            NodeBox loopBodyFirstBox = findConnectedInputBox(currentBox.getLoopBodyPort());
            if (loopBodyFirstBox != null) {
                buildChain(loopBodyFirstBox, model.getChildren(), visitedNodes);
            }
        }
        if (currentBox.getThenPort() != null && currentBox.getThenPort().isConnected()) {
            NodeBox thenFirstBox = findConnectedInputBox(currentBox.getThenPort());
            if (thenFirstBox != null) {
                buildChain(thenFirstBox, model.getChildren(), visitedNodes);
            }
        }
        if (currentBox.getElsePort() != null && currentBox.getElsePort().isConnected()) {
            NodeBox elseFirstBox = findConnectedInputBox(currentBox.getElsePort());
            if (elseFirstBox != null) {
                buildChain(elseFirstBox, model.getElseChildren(), visitedNodes);
            }
        }

        chain.add(model);

        if (currentBox.getExecOut() != null && currentBox.getExecOut().isConnected()) {
            NodeBox nextBox = findConnectedInputBox(currentBox.getExecOut());
            buildChain(nextBox, chain, visitedNodes);
        }
    }

    public static Map<String, Map<String, Double>> buildLayoutMap(List<NodeBox> nodeBoxes) {
        Map<String, Map<String, Double>> layoutMap = new LinkedHashMap<>();
        for (NodeBox box : nodeBoxes) {
            Map<String, Double> position = new LinkedHashMap<>();
            position.put("x", box.getLayoutX());
            position.put("y", box.getLayoutY());
            layoutMap.put(box.getModel().getId(), position);
        }
        return layoutMap;
    }

    private static NodeBox findConnectedInputBox(NodePort outputPort) {
        if (!outputPort.isConnected()) {
            return null;
        }
        NodeConnection connection = outputPort.getConnection();
        return findOwnerBox(connection.getTarget());
    }

    private static NodeBox findOwnerBox(NodePort port) {
        javafx.scene.Node currentNode = port;
        while (currentNode != null) {
            if (currentNode instanceof NodeBox) {
                return (NodeBox) currentNode;
            }
            currentNode = currentNode.getParent();
        }
        return null;
    }
}
