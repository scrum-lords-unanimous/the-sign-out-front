package GUI.SimEditor;

import java.util.*;

public class GraphLayoutEngine {

    private static final double HORIZONTAL_GAP = 300;
    private static final double VERTICAL_GAP = 32;
    private static final double LAYOUT_START_X = 80;
    private static final double LAYOUT_START_Y = 60;
    private static final double ROOT_GROUP_SPACING = 40;
    private static final double DEFAULT_NODE_HEIGHT = 120;
    private static final double UNPLACED_NODE_SPACING = 32;

    private GraphLayoutEngine() {}

    public static void cleanupLayout(List<NodeBox> nodeBoxes, List<NodeConnection> connections, Runnable onChange) {
        if (nodeBoxes.isEmpty()) {
            return;
        }

        List<NodeBox> rootNodes = new ArrayList<>();
        for (NodeBox box : nodeBoxes) {
            if (!box.getExecIn().isConnected()) {
                rootNodes.add(box);
            }
        }

        rootNodes.sort(Comparator.comparingDouble(NodeBox::getLayoutY));

        double[] layoutCursor = {LAYOUT_START_X, LAYOUT_START_Y};

        Set<NodeBox> placedNodes = new HashSet<>();

        for (NodeBox rootNode : rootNodes) {
            if (!placedNodes.contains(rootNode)) {
                layoutChain(rootNode, layoutCursor[0], layoutCursor, placedNodes, 0);
                layoutCursor[1] += ROOT_GROUP_SPACING;
            }
        }

        for (NodeBox box : nodeBoxes) {
            if (!placedNodes.contains(box)) {
                box.setLayoutX(layoutCursor[0]);
                box.setLayoutY(layoutCursor[1]);
                box.getModel().setNodeX(layoutCursor[0]);
                box.getModel().setNodeY(layoutCursor[1]);
                placedNodes.add(box);
                layoutCursor[1] += estimateNodeHeight(box) + UNPLACED_NODE_SPACING;
            }
        }

        if (onChange != null) {
            onChange.run();
        }
    }

    private static void layoutChain(NodeBox currentBox, double xPosition, double[] layoutCursor,
                                    Set<NodeBox> placedNodes, int depthLevel) {
        if (currentBox == null || placedNodes.contains(currentBox)) {
            return;
        }
        placedNodes.add(currentBox);

        double nodeXPosition = xPosition;
        double nodeYPosition = layoutCursor[1];

        currentBox.setLayoutX(nodeXPosition);
        currentBox.setLayoutY(nodeYPosition);
        currentBox.getModel().setNodeX(nodeXPosition);
        currentBox.getModel().setNodeY(nodeYPosition);

        layoutCursor[1] += estimateNodeHeight(currentBox) + VERTICAL_GAP;

        if (currentBox.getLoopBodyPort() != null && currentBox.getLoopBodyPort().isConnected()) {
            NodeBox loopChildBox = findConnectedInputBox(currentBox.getLoopBodyPort());
            if (loopChildBox != null && !placedNodes.contains(loopChildBox)) {
                double branchXPosition = xPosition + HORIZONTAL_GAP;
                double[] branchCursor = {branchXPosition, nodeYPosition};
                layoutChain(loopChildBox, branchXPosition, branchCursor, placedNodes, depthLevel + 1);
                layoutCursor[1] = Math.max(layoutCursor[1], branchCursor[1]);
            }
        }
        if (currentBox.getThenPort() != null && currentBox.getThenPort().isConnected()) {
            NodeBox thenChildBox = findConnectedInputBox(currentBox.getThenPort());
            if (thenChildBox != null && !placedNodes.contains(thenChildBox)) {
                double branchXPosition = xPosition + HORIZONTAL_GAP;
                double[] branchCursor = {branchXPosition, nodeYPosition};
                layoutChain(thenChildBox, branchXPosition, branchCursor, placedNodes, depthLevel + 1);
                layoutCursor[1] = Math.max(layoutCursor[1], branchCursor[1]);
            }
        }
        if (currentBox.getElsePort() != null && currentBox.getElsePort().isConnected()) {
            NodeBox elseChildBox = findConnectedInputBox(currentBox.getElsePort());
            if (elseChildBox != null && !placedNodes.contains(elseChildBox)) {
                double branchXPosition = xPosition + HORIZONTAL_GAP;
                double[] branchCursor = {branchXPosition, layoutCursor[1]};
                layoutChain(elseChildBox, branchXPosition, branchCursor, placedNodes, depthLevel + 1);
                layoutCursor[1] = Math.max(layoutCursor[1], branchCursor[1]);
            }
        }

        if (currentBox.getExecOut() != null && currentBox.getExecOut().isConnected()) {
            NodeBox nextBox = findConnectedInputBox(currentBox.getExecOut());
            layoutChain(nextBox, xPosition, layoutCursor, placedNodes, depthLevel);
        }
    }

    private static double estimateNodeHeight(NodeBox box) {
        double preferredHeight = box.prefHeight(-1);
        return preferredHeight > 0 ? preferredHeight : DEFAULT_NODE_HEIGHT;
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
