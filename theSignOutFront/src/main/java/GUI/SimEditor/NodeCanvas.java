package GUI.SimEditor;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class NodeCanvas extends Pane {

    private static final String[] STEP_TYPES = {
        "print", "set", "spawn", "for", "for-each", "if",
        "precompute-driveway", "record-slide-view", "slide-report"
    };

    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 3.0;
    private static final double GRID_SPACING = 25;

    private final Group canvasGroup;
    private final Canvas gridCanvas;
    private final Pane connectionsLayer;
    private final Pane nodesLayer;
    private final CubicCurve tempCurve;

    private final List<NodeBox> nodeBoxes = new ArrayList<>();
    private final List<NodeConnection> connections = new ArrayList<>();

    private double zoomScale = 1.0;

    // Pan state
    private double panAnchorX, panAnchorY;
    private double panTranslateX, panTranslateY;
    private boolean isPanning = false;

    // Node drag state
    private double dragAnchorX, dragAnchorY;
    private double dragNodeStartX, dragNodeStartY;
    private NodeBox draggingNode = null;

    // Connection drag state
    private NodePort dragSourcePort = null;

    private Runnable onChange;

    public NodeCanvas() {
        getStyleClass().add("node-canvas");

        // Clip to bounds
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // Scene graph
        gridCanvas = new Canvas(4000, 4000);
        gridCanvas.setMouseTransparent(true);

        connectionsLayer = new Pane();
        connectionsLayer.setMouseTransparent(false);
        connectionsLayer.setPickOnBounds(false);

        nodesLayer = new Pane();
        nodesLayer.setPickOnBounds(false);

        tempCurve = new CubicCurve();
        tempCurve.setFill(null);
        tempCurve.setStroke(Color.web("#808080"));
        tempCurve.setStrokeWidth(2);
        tempCurve.getStrokeDashArray().addAll(8.0, 4.0);
        tempCurve.setVisible(false);
        tempCurve.setMouseTransparent(true);

        canvasGroup = new Group(gridCanvas, connectionsLayer, nodesLayer, tempCurve);
        getChildren().add(canvasGroup);

        drawGrid();

        // Event handlers
        setOnScroll(this::handleScroll);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    // --- Grid ---
    private void drawGrid() {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        double w = gridCanvas.getWidth();
        double h = gridCanvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#808080", 0.25));

        for (double x = 0; x < w; x += GRID_SPACING) {
            for (double y = 0; y < h; y += GRID_SPACING) {
                gc.fillOval(x - 1, y - 1, 2, 2);
            }
        }
    }

    // --- Zoom ---
    private void handleScroll(ScrollEvent e) {
        double factor = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
        double newScale = zoomScale * factor;
        newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));

        // Zoom toward cursor
        Point2D mouse = canvasGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
        if (mouse == null) return;

        double scaleFactor = newScale / zoomScale;
        zoomScale = newScale;

        canvasGroup.setScaleX(zoomScale);
        canvasGroup.setScaleY(zoomScale);

        // Adjust translate so the point under cursor stays fixed
        double dx = mouse.getX() * (scaleFactor - 1);
        double dy = mouse.getY() * (scaleFactor - 1);
        canvasGroup.setTranslateX(canvasGroup.getTranslateX() - dx * zoomScale);
        canvasGroup.setTranslateY(canvasGroup.getTranslateY() - dy * zoomScale);

        e.consume();
    }

    // --- Pan & Drag ---
    private void handleMousePressed(MouseEvent e) {
        // Middle-click or Ctrl+left-click: start pan
        if (e.getButton() == MouseButton.MIDDLE ||
            (e.getButton() == MouseButton.PRIMARY && e.isControlDown())) {
            isPanning = true;
            panAnchorX = e.getSceneX();
            panAnchorY = e.getSceneY();
            panTranslateX = canvasGroup.getTranslateX();
            panTranslateY = canvasGroup.getTranslateY();
            e.consume();
            return;
        }

        if (e.getButton() != MouseButton.PRIMARY) return;

        // Check if we hit a port
        NodePort hitPort = findPortAt(e);
        if (hitPort != null && hitPort.getDirection() == NodePort.Direction.OUTPUT) {
            startConnectionDrag(hitPort, e);
            e.consume();
            return;
        }

        // Check if we hit a node
        NodeBox hitNode = findNodeAt(e);
        if (hitNode != null) {
            draggingNode = hitNode;
            Point2D local = canvasGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (local != null) {
                dragAnchorX = local.getX();
                dragAnchorY = local.getY();
                dragNodeStartX = hitNode.getLayoutX();
                dragNodeStartY = hitNode.getLayoutY();
            }
            hitNode.toFront();
            e.consume();
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (isPanning) {
            double dx = e.getSceneX() - panAnchorX;
            double dy = e.getSceneY() - panAnchorY;
            canvasGroup.setTranslateX(panTranslateX + dx);
            canvasGroup.setTranslateY(panTranslateY + dy);
            e.consume();
            return;
        }

        if (dragSourcePort != null) {
            updateConnectionDrag(e);
            e.consume();
            return;
        }

        if (draggingNode != null) {
            Point2D local = canvasGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (local != null) {
                double dx = local.getX() - dragAnchorX;
                double dy = local.getY() - dragAnchorY;
                draggingNode.setLayoutX(dragNodeStartX + dx);
                draggingNode.setLayoutY(dragNodeStartY + dy);
            }
            e.consume();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (isPanning) {
            isPanning = false;
            e.consume();
            return;
        }

        if (dragSourcePort != null) {
            finishConnectionDrag(e);
            e.consume();
            return;
        }

        if (draggingNode != null) {
            // Save position back to model
            draggingNode.getModel().setNodeX(draggingNode.getLayoutX());
            draggingNode.getModel().setNodeY(draggingNode.getLayoutY());
            draggingNode = null;
            if (onChange != null) onChange.run();
            e.consume();
        }
    }

    // --- Connection Drag ---
    private void startConnectionDrag(NodePort port, MouseEvent e) {
        dragSourcePort = port;

        // Remove existing connection from this output port
        if (port.isConnected()) {
            removeConnection(port.getConnection());
        }

        Point2D srcScene = port.getCenterInScene();
        Point2D srcLocal = canvasGroup.sceneToLocal(srcScene);
        if (srcLocal == null) return;

        tempCurve.setStartX(srcLocal.getX());
        tempCurve.setStartY(srcLocal.getY());
        tempCurve.setEndX(srcLocal.getX());
        tempCurve.setEndY(srcLocal.getY());
        tempCurve.setControlX1(srcLocal.getX());
        tempCurve.setControlY1(srcLocal.getY());
        tempCurve.setControlX2(srcLocal.getX());
        tempCurve.setControlY2(srcLocal.getY());
        tempCurve.setVisible(true);
    }

    private void updateConnectionDrag(MouseEvent e) {
        Point2D local = canvasGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
        if (local == null) return;

        tempCurve.setEndX(local.getX());
        tempCurve.setEndY(local.getY());
        tempCurve.setControlX1(tempCurve.getStartX() + 80);
        tempCurve.setControlY1(tempCurve.getStartY());
        tempCurve.setControlX2(local.getX() - 80);
        tempCurve.setControlY2(local.getY());
    }

    private void finishConnectionDrag(MouseEvent e) {
        tempCurve.setVisible(false);

        NodePort targetPort = findPortAt(e);
        if (targetPort != null
                && targetPort.getDirection() == NodePort.Direction.INPUT
                && targetPort != dragSourcePort) {

            // Verify we're not connecting a node to itself
            NodeBox sourceBox = findOwnerBox(dragSourcePort);
            NodeBox targetBox = findOwnerBox(targetPort);
            if (sourceBox != targetBox) {
                // Remove existing connection on the target input
                if (targetPort.isConnected()) {
                    removeConnection(targetPort.getConnection());
                }
                createConnection(dragSourcePort, targetPort);
            }
        }

        dragSourcePort = null;
    }

    // --- Connection Management ---
    public void createConnection(NodePort source, NodePort target) {
        NodeConnection conn = new NodeConnection(source, target, canvasGroup);
        source.setConnection(conn);
        target.setConnection(conn);
        connections.add(conn);
        connectionsLayer.getChildren().add(conn);

        // Right-click to delete
        conn.setOnContextMenuRequested(ce -> {
            ContextMenu ctx = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Connection");
            deleteItem.setOnAction(ae -> removeConnection(conn));
            ctx.getItems().add(deleteItem);
            ctx.show(conn, ce.getScreenX(), ce.getScreenY());
            ce.consume();
        });

        if (onChange != null) onChange.run();
    }

    public void removeConnection(NodeConnection conn) {
        conn.disconnect();
        connections.remove(conn);
        connectionsLayer.getChildren().remove(conn);
        if (onChange != null) onChange.run();
    }

    // --- Node Management ---
    public NodeBox addNode(StepModel model) {
        NodeBox box = new NodeBox(model, onChange != null ? onChange : () -> {});
        box.setLayoutX(model.getNodeX());
        box.setLayoutY(model.getNodeY());
        nodeBoxes.add(box);
        nodesLayer.getChildren().add(box);

        // Setup port drag handlers
        for (NodePort port : box.getAllPorts()) {
            setupPortHandlers(port);
        }

        // Right-click to delete node
        box.setOnContextMenuRequested(ce -> {
            ContextMenu ctx = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Node");
            deleteItem.setOnAction(ae -> removeNode(box));
            ctx.getItems().add(deleteItem);
            ctx.show(box, ce.getScreenX(), ce.getScreenY());
            ce.consume();
        });

        return box;
    }

    private void setupPortHandlers(NodePort port) {
        port.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && port.getDirection() == NodePort.Direction.OUTPUT) {
                startConnectionDrag(port, e);
                e.consume();
            }
        });
        port.setOnMouseDragged(e -> {
            if (dragSourcePort != null) {
                updateConnectionDrag(e);
                e.consume();
            }
        });
        port.setOnMouseReleased(e -> {
            if (dragSourcePort != null) {
                finishConnectionDrag(e);
                e.consume();
            }
        });
    }

    public void removeNode(NodeBox box) {
        // Remove all connections attached to this node
        List<NodeConnection> toRemove = new ArrayList<>();
        for (NodePort port : box.getAllPorts()) {
            if (port.isConnected()) {
                toRemove.add(port.getConnection());
            }
        }
        for (NodeConnection conn : toRemove) {
            removeConnection(conn);
        }

        // Also remove connections where this node is the target
        List<NodeConnection> incoming = new ArrayList<>();
        for (NodeConnection conn : connections) {
            if (findOwnerBox(conn.getTarget()) == box || findOwnerBox(conn.getSource()) == box) {
                incoming.add(conn);
            }
        }
        for (NodeConnection conn : incoming) {
            removeConnection(conn);
        }

        nodeBoxes.remove(box);
        nodesLayer.getChildren().remove(box);
        if (onChange != null) onChange.run();
    }

    public void clear() {
        for (NodeConnection conn : new ArrayList<>(connections)) {
            conn.disconnect();
        }
        connections.clear();
        connectionsLayer.getChildren().clear();
        nodeBoxes.clear();
        nodesLayer.getChildren().clear();

        // Reset pan/zoom
        canvasGroup.setTranslateX(0);
        canvasGroup.setTranslateY(0);
        zoomScale = 1.0;
        canvasGroup.setScaleX(1.0);
        canvasGroup.setScaleY(1.0);
    }

    // --- Hit Testing ---
    private NodePort findPortAt(MouseEvent e) {
        for (NodeBox box : nodeBoxes) {
            for (NodePort port : box.getAllPorts()) {
                Point2D portScene = port.getCenterInScene();
                double dist = portScene.distance(e.getSceneX(), e.getSceneY());
                if (dist < 12) {
                    return port;
                }
            }
        }
        return null;
    }

    private NodeBox findNodeAt(MouseEvent e) {
        Point2D local = canvasGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
        if (local == null) return null;

        // Iterate in reverse order (topmost first)
        for (int i = nodeBoxes.size() - 1; i >= 0; i--) {
            NodeBox box = nodeBoxes.get(i);
            if (box.getBoundsInParent().contains(local)) {
                return box;
            }
        }
        return null;
    }

    private NodeBox findOwnerBox(NodePort port) {
        javafx.scene.Node current = port;
        while (current != null) {
            if (current instanceof NodeBox) return (NodeBox) current;
            current = current.getParent();
        }
        return null;
    }

    // --- Add Step via Menu ---
    public void showAddStepMenu(javafx.scene.Node anchor) {
        ContextMenu menu = new ContextMenu();
        for (String type : STEP_TYPES) {
            MenuItem item = new MenuItem(type);
            item.setOnAction(e -> {
                StepModel model = new StepModel(type);
                initDefaults(model);

                // Place at viewport center
                Point2D center = canvasGroup.sceneToLocal(
                        getWidth() / 2 + localToScene(0, 0).getX(),
                        getHeight() / 2 + localToScene(0, 0).getY());
                if (center != null) {
                    model.setNodeX(center.getX());
                    model.setNodeY(center.getY());
                }

                addNode(model);
                if (onChange != null) onChange.run();
            });
            menu.getItems().add(item);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private static void initDefaults(StepModel step) {
        switch (step.getType()) {
            case "for":
                step.setProperty("var", "i");
                step.setProperty("from", 0);
                step.setProperty("to", 10);
                step.setProperty("step", 1);
                break;
            case "for-each":
                step.setProperty("collection", "");
                step.setProperty("as", "item");
                break;
            case "if":
                Map<String, Object> cond = new LinkedHashMap<>();
                List<Object> args = new ArrayList<>();
                args.add("a");
                args.add("b");
                cond.put("=", args);
                step.setProperty("condition", cond);
                break;
            case "set":
                step.setProperty("target", "");
                step.setProperty("value", "");
                break;
            case "print":
                step.setProperty("value", "");
                break;
            case "spawn":
                step.setProperty("count", "1");
                step.setProperty("as", "");
                break;
        }
    }

    // --- Serialization: Graph -> Tree ---

    /**
     * Builds the step tree from the current graph for saving.
     * Returns the top-level steps list.
     */
    public List<StepModel> buildStepTree() {
        // Find root nodes: nodes whose EXEC_IN port has no incoming connection
        List<NodeBox> roots = new ArrayList<>();
        for (NodeBox box : nodeBoxes) {
            if (!box.getExecIn().isConnected()) {
                roots.add(box);
            }
        }

        // If there are multiple roots, find the one that starts a chain
        // Sort roots by Y position (top to bottom)
        roots.sort(Comparator.comparingDouble(NodeBox::getLayoutY));

        List<StepModel> topLevel = new ArrayList<>();
        Set<NodeBox> visited = new HashSet<>();

        for (NodeBox root : roots) {
            if (!visited.contains(root)) {
                buildChain(root, topLevel, visited);
            }
        }

        return topLevel;
    }

    private void buildChain(NodeBox current, List<StepModel> chain, Set<NodeBox> visited) {
        if (current == null || visited.contains(current)) return;
        visited.add(current);

        StepModel model = current.getModel();

        // Clear children so we rebuild from graph
        model.getChildren().clear();
        model.getElseChildren().clear();

        // Handle container children
        if (current.getLoopBodyPort() != null && current.getLoopBodyPort().isConnected()) {
            NodeBox bodyFirst = findConnectedInputBox(current.getLoopBodyPort());
            if (bodyFirst != null) {
                buildChain(bodyFirst, model.getChildren(), visited);
            }
        }
        if (current.getThenPort() != null && current.getThenPort().isConnected()) {
            NodeBox thenFirst = findConnectedInputBox(current.getThenPort());
            if (thenFirst != null) {
                buildChain(thenFirst, model.getChildren(), visited);
            }
        }
        if (current.getElsePort() != null && current.getElsePort().isConnected()) {
            NodeBox elseFirst = findConnectedInputBox(current.getElsePort());
            if (elseFirst != null) {
                buildChain(elseFirst, model.getElseChildren(), visited);
            }
        }

        chain.add(model);

        // Follow EXEC_OUT to next step
        if (current.getExecOut() != null && current.getExecOut().isConnected()) {
            NodeBox next = findConnectedInputBox(current.getExecOut());
            buildChain(next, chain, visited);
        }
    }

    private NodeBox findConnectedInputBox(NodePort outputPort) {
        if (!outputPort.isConnected()) return null;
        NodeConnection conn = outputPort.getConnection();
        return findOwnerBox(conn.getTarget());
    }

    /**
     * Builds a layout map: id -> {x, y}
     */
    public Map<String, Map<String, Double>> buildLayoutMap() {
        Map<String, Map<String, Double>> layout = new LinkedHashMap<>();
        for (NodeBox box : nodeBoxes) {
            Map<String, Double> pos = new LinkedHashMap<>();
            pos.put("x", box.getLayoutX());
            pos.put("y", box.getLayoutY());
            layout.put(box.getModel().getId(), pos);
        }
        return layout;
    }

    // --- Deserialization: Tree -> Graph ---

    /**
     * Loads a simulation from JSON data, creating nodes and connections.
     */
    @SuppressWarnings("unchecked")
    public void loadFromJson(Map<String, Object> simData) {
        clear();

        List<StepModel> treeSteps = StepModel.fromJsonSteps(simData);
        if (treeSteps.isEmpty()) return;

        // Read layout map
        Map<String, Map<String, Double>> layoutMap = new LinkedHashMap<>();
        Object layoutObj = simData.get("layout");
        if (layoutObj instanceof Map) {
            Map<String, Object> rawLayout = (Map<String, Object>) layoutObj;
            for (Map.Entry<String, Object> entry : rawLayout.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> posRaw = (Map<String, Object>) entry.getValue();
                    Map<String, Double> pos = new LinkedHashMap<>();
                    pos.put("x", toDouble(posRaw.get("x")));
                    pos.put("y", toDouble(posRaw.get("y")));
                    layoutMap.put(entry.getKey(), pos);
                }
            }
        }

        // Flatten tree into all individual step models
        List<StepModel> allSteps = StepModel.flattenTree(treeSteps);

        // Apply layout positions or auto-layout
        boolean hasLayout = !layoutMap.isEmpty();
        double autoX = 100;
        double autoY = 80;

        Map<StepModel, NodeBox> modelToBox = new LinkedHashMap<>();

        for (StepModel step : allSteps) {
            if (hasLayout && layoutMap.containsKey(step.getId())) {
                Map<String, Double> pos = layoutMap.get(step.getId());
                step.setNodeX(pos.get("x"));
                step.setNodeY(pos.get("y"));
            } else {
                step.setNodeX(autoX);
                step.setNodeY(autoY);
                autoY += 120;
            }

            NodeBox box = addNode(step);
            modelToBox.put(step, box);
        }

        // Recreate connections from tree structure
        recreateConnections(treeSteps, modelToBox);

        // If no layout was saved, do an auto-layout pass
        if (!hasLayout) {
            autoLayout(treeSteps, modelToBox, 100, 80, 0);
        }
    }

    private void autoLayout(List<StepModel> steps, Map<StepModel, NodeBox> modelToBox,
                            double startX, double startY, int depth) {
        double x = startX + depth * 280;
        double y = startY;

        for (StepModel step : steps) {
            NodeBox box = modelToBox.get(step);
            if (box != null) {
                box.setLayoutX(x);
                box.setLayoutY(y);
                step.setNodeX(x);
                step.setNodeY(y);
                y += 140;
            }

            // Layout children to the right
            if (!step.getChildren().isEmpty()) {
                autoLayout(step.getChildren(), modelToBox, startX, y - 140 + 20, depth + 1);
                y += step.getChildren().size() * 140;
            }
            if (!step.getElseChildren().isEmpty()) {
                autoLayout(step.getElseChildren(), modelToBox, startX, y, depth + 1);
                y += step.getElseChildren().size() * 140;
            }
        }
    }

    private void recreateConnections(List<StepModel> steps, Map<StepModel, NodeBox> modelToBox) {
        for (int i = 0; i < steps.size(); i++) {
            StepModel step = steps.get(i);
            NodeBox box = modelToBox.get(step);
            if (box == null) continue;

            // EXEC_OUT -> next sibling's EXEC_IN
            if (i + 1 < steps.size()) {
                NodeBox nextBox = modelToBox.get(steps.get(i + 1));
                if (nextBox != null && box.getExecOut() != null) {
                    deferConnection(box.getExecOut(), nextBox.getExecIn());
                }
            }

            // Container children connections
            if (!step.getChildren().isEmpty()) {
                NodeBox firstChild = modelToBox.get(step.getChildren().get(0));
                if (firstChild != null) {
                    if (box.getLoopBodyPort() != null) {
                        deferConnection(box.getLoopBodyPort(), firstChild.getExecIn());
                    } else if (box.getThenPort() != null) {
                        deferConnection(box.getThenPort(), firstChild.getExecIn());
                    }
                }
                // Connect children in sequence
                recreateConnections(step.getChildren(), modelToBox);
            }

            if (!step.getElseChildren().isEmpty()) {
                NodeBox firstElse = modelToBox.get(step.getElseChildren().get(0));
                if (firstElse != null && box.getElsePort() != null) {
                    deferConnection(box.getElsePort(), firstElse.getExecIn());
                }
                recreateConnections(step.getElseChildren(), modelToBox);
            }
        }
    }

    private void deferConnection(NodePort source, NodePort target) {
        javafx.application.Platform.runLater(() -> createConnection(source, target));
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return 0;
    }

    /**
     * Reorganizes all nodes into a clean layout based on their connections.
     * Roots are placed top-left, exec chains flow downward, children branch right.
     */
    public void cleanupLayout() {
        if (nodeBoxes.isEmpty()) return;

        // Find root nodes (EXEC_IN has no incoming connection)
        List<NodeBox> roots = new ArrayList<>();
        Set<NodeBox> allConnected = new HashSet<>();

        for (NodeBox box : nodeBoxes) {
            if (!box.getExecIn().isConnected()) {
                roots.add(box);
            }
        }

        // Sort roots by current Y so relative order is preserved
        roots.sort(Comparator.comparingDouble(NodeBox::getLayoutY));

        double startX = 80;
        double startY = 60;
        double[] cursor = {startX, startY};

        Set<NodeBox> placed = new HashSet<>();

        for (NodeBox root : roots) {
            if (!placed.contains(root)) {
                layoutChain(root, cursor[0], cursor, placed, 0);
                cursor[1] += 40; // gap between root chains
            }
        }

        // Place any unconnected/orphan nodes at the bottom
        for (NodeBox box : nodeBoxes) {
            if (!placed.contains(box)) {
                box.setLayoutX(cursor[0]);
                box.setLayoutY(cursor[1]);
                box.getModel().setNodeX(cursor[0]);
                box.getModel().setNodeY(cursor[1]);
                placed.add(box);
                cursor[1] += estimateHeight(box) + 32;
            }
        }

        if (onChange != null) onChange.run();
    }

    private static final double H_GAP = 300; // horizontal gap for branches
    private static final double V_GAP = 32;  // vertical gap between nodes

    private void layoutChain(NodeBox current, double x, double[] cursor,
                             Set<NodeBox> placed, int depth) {
        if (current == null || placed.contains(current)) return;
        placed.add(current);

        double nodeX = x;
        double nodeY = cursor[1];

        current.setLayoutX(nodeX);
        current.setLayoutY(nodeY);
        current.getModel().setNodeX(nodeX);
        current.getModel().setNodeY(nodeY);

        cursor[1] += estimateHeight(current) + V_GAP;

        // Layout branch children to the right (Loop Body, Then, Else)
        if (current.getLoopBodyPort() != null && current.getLoopBodyPort().isConnected()) {
            NodeBox child = findConnectedInputBox(current.getLoopBodyPort());
            if (child != null && !placed.contains(child)) {
                double branchX = x + H_GAP;
                double[] branchCursor = {branchX, nodeY};
                layoutChain(child, branchX, branchCursor, placed, depth + 1);
                cursor[1] = Math.max(cursor[1], branchCursor[1]);
            }
        }
        if (current.getThenPort() != null && current.getThenPort().isConnected()) {
            NodeBox child = findConnectedInputBox(current.getThenPort());
            if (child != null && !placed.contains(child)) {
                double branchX = x + H_GAP;
                double[] branchCursor = {branchX, nodeY};
                layoutChain(child, branchX, branchCursor, placed, depth + 1);
                cursor[1] = Math.max(cursor[1], branchCursor[1]);
            }
        }
        if (current.getElsePort() != null && current.getElsePort().isConnected()) {
            NodeBox child = findConnectedInputBox(current.getElsePort());
            if (child != null && !placed.contains(child)) {
                double branchX = x + H_GAP;
                double[] branchCursor = {branchX, cursor[1]};
                layoutChain(child, branchX, branchCursor, placed, depth + 1);
                cursor[1] = Math.max(cursor[1], branchCursor[1]);
            }
        }

        // Follow EXEC_OUT to next sibling
        if (current.getExecOut() != null && current.getExecOut().isConnected()) {
            NodeBox next = findConnectedInputBox(current.getExecOut());
            layoutChain(next, x, cursor, placed, depth);
        }
    }

    private double estimateHeight(NodeBox box) {
        // Use the actual rendered height if available, otherwise a default
        double h = box.prefHeight(-1);
        return h > 0 ? h : 120;
    }

    public List<NodeBox> getNodeBoxes() { return nodeBoxes; }
}
