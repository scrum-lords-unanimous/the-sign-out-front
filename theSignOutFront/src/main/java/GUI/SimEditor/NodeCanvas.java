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
        "print", "set", "spawn", "for", "for-each", "while", "if",
        "precompute-driveway", "record-slide-view", "slide-report"
    };

    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 3.0;
    private static final double GRID_SPACING = 25;
    private static final double GRID_CANVAS_SIZE = 4000;
    private static final double GRID_DOT_RADIUS = 1;
    private static final double GRID_DOT_DIAMETER = 2;
    private static final double GRID_DOT_OPACITY = 0.25;
    private static final double ZOOM_STEP_FACTOR = 1.1;
    private static final double PORT_HIT_RADIUS = 12;
    private static final double TEMP_CURVE_STROKE_WIDTH = 2;
    private static final double TEMP_CURVE_DASH_LENGTH = 8.0;
    private static final double TEMP_CURVE_GAP_LENGTH = 4.0;
    private static final double BEZIER_TANGENT_OFFSET = 80;

    private final Group canvasGroup;
    private final Canvas gridCanvas;
    private final Pane connectionsLayer;
    private final Pane nodesLayer;
    private final CubicCurve temporaryCurve;

    private final List<NodeBox> nodeBoxes = new ArrayList<>();
    private final List<NodeConnection> connections = new ArrayList<>();

    private double zoomScale = 1.0;

    private double panAnchorX;
    private double panAnchorY;
    private double panTranslateStartX;
    private double panTranslateStartY;
    private boolean isPanning = false;

    private double dragAnchorX;
    private double dragAnchorY;
    private double dragNodeStartX;
    private double dragNodeStartY;
    private NodeBox draggingNode = null;

    private NodePort dragSourcePort = null;

    private Runnable onChange;

    public NodeCanvas() {
        getStyleClass().add("node-canvas");

        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(widthProperty());
        clipRect.heightProperty().bind(heightProperty());
        setClip(clipRect);

        gridCanvas = new Canvas(GRID_CANVAS_SIZE, GRID_CANVAS_SIZE);
        gridCanvas.setMouseTransparent(true);

        connectionsLayer = new Pane();
        connectionsLayer.setMouseTransparent(false);
        connectionsLayer.setPickOnBounds(false);

        nodesLayer = new Pane();
        nodesLayer.setPickOnBounds(false);

        temporaryCurve = new CubicCurve();
        temporaryCurve.setFill(null);
        temporaryCurve.setStroke(Color.web("#808080"));
        temporaryCurve.setStrokeWidth(TEMP_CURVE_STROKE_WIDTH);
        temporaryCurve.getStrokeDashArray().addAll(TEMP_CURVE_DASH_LENGTH, TEMP_CURVE_GAP_LENGTH);
        temporaryCurve.setVisible(false);
        temporaryCurve.setMouseTransparent(true);

        canvasGroup = new Group(gridCanvas, connectionsLayer, nodesLayer, temporaryCurve);
        getChildren().add(canvasGroup);

        drawGrid();

        setOnScroll(this::handleScroll);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void drawGrid() {
        GraphicsContext graphicsContext = gridCanvas.getGraphicsContext2D();
        double canvasWidth = gridCanvas.getWidth();
        double canvasHeight = gridCanvas.getHeight();

        graphicsContext.clearRect(0, 0, canvasWidth, canvasHeight);
        graphicsContext.setFill(Color.web("#808080", GRID_DOT_OPACITY));

        for (double dotX = 0; dotX < canvasWidth; dotX += GRID_SPACING) {
            for (double dotY = 0; dotY < canvasHeight; dotY += GRID_SPACING) {
                graphicsContext.fillOval(
                    dotX - GRID_DOT_RADIUS,
                    dotY - GRID_DOT_RADIUS,
                    GRID_DOT_DIAMETER,
                    GRID_DOT_DIAMETER
                );
            }
        }
    }

    private void handleScroll(ScrollEvent scrollEvent) {
        double zoomDirection = scrollEvent.getDeltaY() > 0 ? ZOOM_STEP_FACTOR : 1 / ZOOM_STEP_FACTOR;
        double newScale = zoomScale * zoomDirection;
        newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));

        Point2D mouseInCanvas = canvasGroup.sceneToLocal(scrollEvent.getSceneX(), scrollEvent.getSceneY());
        if (mouseInCanvas == null) {
            return;
        }

        double scaleFactor = newScale / zoomScale;
        zoomScale = newScale;

        canvasGroup.setScaleX(zoomScale);
        canvasGroup.setScaleY(zoomScale);

        double translateDeltaX = mouseInCanvas.getX() * (scaleFactor - 1);
        double translateDeltaY = mouseInCanvas.getY() * (scaleFactor - 1);
        canvasGroup.setTranslateX(canvasGroup.getTranslateX() - translateDeltaX * zoomScale);
        canvasGroup.setTranslateY(canvasGroup.getTranslateY() - translateDeltaY * zoomScale);

        scrollEvent.consume();
    }

    private void handleMousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.MIDDLE ||
            (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.isControlDown())) {
            isPanning = true;
            panAnchorX = mouseEvent.getSceneX();
            panAnchorY = mouseEvent.getSceneY();
            panTranslateStartX = canvasGroup.getTranslateX();
            panTranslateStartY = canvasGroup.getTranslateY();
            mouseEvent.consume();
            return;
        }

        if (mouseEvent.getButton() != MouseButton.PRIMARY) {
            return;
        }

        NodePort hitPort = findPortAt(mouseEvent);
        if (hitPort != null && hitPort.getDirection() == NodePort.Direction.OUTPUT) {
            startConnectionDrag(hitPort, mouseEvent);
            mouseEvent.consume();
            return;
        }

        NodeBox hitNode = findNodeAt(mouseEvent);
        if (hitNode != null) {
            draggingNode = hitNode;
            Point2D localPosition = canvasGroup.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            if (localPosition != null) {
                dragAnchorX = localPosition.getX();
                dragAnchorY = localPosition.getY();
                dragNodeStartX = hitNode.getLayoutX();
                dragNodeStartY = hitNode.getLayoutY();
            }
            hitNode.toFront();
            mouseEvent.consume();
        }
    }

    private void handleMouseDragged(MouseEvent mouseEvent) {
        if (isPanning) {
            double deltaX = mouseEvent.getSceneX() - panAnchorX;
            double deltaY = mouseEvent.getSceneY() - panAnchorY;
            canvasGroup.setTranslateX(panTranslateStartX + deltaX);
            canvasGroup.setTranslateY(panTranslateStartY + deltaY);
            mouseEvent.consume();
            return;
        }

        if (dragSourcePort != null) {
            updateConnectionDrag(mouseEvent);
            mouseEvent.consume();
            return;
        }

        if (draggingNode != null) {
            Point2D localPosition = canvasGroup.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            if (localPosition != null) {
                double deltaX = localPosition.getX() - dragAnchorX;
                double deltaY = localPosition.getY() - dragAnchorY;
                draggingNode.setLayoutX(dragNodeStartX + deltaX);
                draggingNode.setLayoutY(dragNodeStartY + deltaY);
            }
            mouseEvent.consume();
        }
    }

    private void handleMouseReleased(MouseEvent mouseEvent) {
        if (isPanning) {
            isPanning = false;
            mouseEvent.consume();
            return;
        }

        if (dragSourcePort != null) {
            finishConnectionDrag(mouseEvent);
            mouseEvent.consume();
            return;
        }

        if (draggingNode != null) {
            draggingNode.getModel().setNodeX(draggingNode.getLayoutX());
            draggingNode.getModel().setNodeY(draggingNode.getLayoutY());
            draggingNode = null;
            if (onChange != null) {
                onChange.run();
            }
            mouseEvent.consume();
        }
    }

    private void startConnectionDrag(NodePort port, MouseEvent mouseEvent) {
        dragSourcePort = port;

        if (port.isConnected()) {
            removeConnection(port.getConnection());
        }

        Point2D sourceScenePosition = port.getCenterInScene();
        Point2D sourceLocalPosition = canvasGroup.sceneToLocal(sourceScenePosition);
        if (sourceLocalPosition == null) {
            return;
        }

        temporaryCurve.setStartX(sourceLocalPosition.getX());
        temporaryCurve.setStartY(sourceLocalPosition.getY());
        temporaryCurve.setEndX(sourceLocalPosition.getX());
        temporaryCurve.setEndY(sourceLocalPosition.getY());
        temporaryCurve.setControlX1(sourceLocalPosition.getX());
        temporaryCurve.setControlY1(sourceLocalPosition.getY());
        temporaryCurve.setControlX2(sourceLocalPosition.getX());
        temporaryCurve.setControlY2(sourceLocalPosition.getY());
        temporaryCurve.setVisible(true);
    }

    private void updateConnectionDrag(MouseEvent mouseEvent) {
        Point2D mouseLocalPosition = canvasGroup.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        if (mouseLocalPosition == null) {
            return;
        }

        temporaryCurve.setEndX(mouseLocalPosition.getX());
        temporaryCurve.setEndY(mouseLocalPosition.getY());
        temporaryCurve.setControlX1(temporaryCurve.getStartX() + BEZIER_TANGENT_OFFSET);
        temporaryCurve.setControlY1(temporaryCurve.getStartY());
        temporaryCurve.setControlX2(mouseLocalPosition.getX() - BEZIER_TANGENT_OFFSET);
        temporaryCurve.setControlY2(mouseLocalPosition.getY());
    }

    private void finishConnectionDrag(MouseEvent mouseEvent) {
        temporaryCurve.setVisible(false);

        NodePort targetPort = findPortAt(mouseEvent);
        if (targetPort != null
                && targetPort.getDirection() == NodePort.Direction.INPUT
                && targetPort != dragSourcePort) {

            NodeBox sourceBox = findOwnerBox(dragSourcePort);
            NodeBox targetBox = findOwnerBox(targetPort);
            if (sourceBox != targetBox) {
                if (targetPort.isConnected()) {
                    removeConnection(targetPort.getConnection());
                }
                createConnection(dragSourcePort, targetPort);
            }
        }

        dragSourcePort = null;
    }

    public void createConnection(NodePort sourcePort, NodePort targetPort) {
        NodeConnection connection = new NodeConnection(sourcePort, targetPort, canvasGroup);
        sourcePort.setConnection(connection);
        targetPort.setConnection(connection);
        connections.add(connection);
        connectionsLayer.getChildren().add(connection);

        connection.setOnContextMenuRequested(contextEvent -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Connection");
            deleteItem.setOnAction(actionEvent -> removeConnection(connection));
            contextMenu.getItems().add(deleteItem);
            contextMenu.show(connection, contextEvent.getScreenX(), contextEvent.getScreenY());
            contextEvent.consume();
        });

        if (onChange != null) {
            onChange.run();
        }
    }

    public void removeConnection(NodeConnection connection) {
        connection.disconnect();
        connections.remove(connection);
        connectionsLayer.getChildren().remove(connection);
        if (onChange != null) {
            onChange.run();
        }
    }

    public NodeBox addNode(StepModel model) {
        NodeBox box = new NodeBox(model, onChange != null ? onChange : () -> {});
        box.setLayoutX(model.getNodeX());
        box.setLayoutY(model.getNodeY());
        nodeBoxes.add(box);
        nodesLayer.getChildren().add(box);

        for (NodePort port : box.getAllPorts()) {
            setupPortHandlers(port);
        }

        box.setOnContextMenuRequested(contextEvent -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Node");
            deleteItem.setOnAction(actionEvent -> removeNode(box));
            contextMenu.getItems().add(deleteItem);
            contextMenu.show(box, contextEvent.getScreenX(), contextEvent.getScreenY());
            contextEvent.consume();
        });

        return box;
    }

    private void setupPortHandlers(NodePort port) {
        port.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY && port.getDirection() == NodePort.Direction.OUTPUT) {
                startConnectionDrag(port, mouseEvent);
                mouseEvent.consume();
            }
        });
        port.setOnMouseDragged(mouseEvent -> {
            if (dragSourcePort != null) {
                updateConnectionDrag(mouseEvent);
                mouseEvent.consume();
            }
        });
        port.setOnMouseReleased(mouseEvent -> {
            if (dragSourcePort != null) {
                finishConnectionDrag(mouseEvent);
                mouseEvent.consume();
            }
        });
    }

    public void removeNode(NodeBox box) {
        List<NodeConnection> connectionsFromPorts = new ArrayList<>();
        for (NodePort port : box.getAllPorts()) {
            if (port.isConnected()) {
                connectionsFromPorts.add(port.getConnection());
            }
        }
        for (NodeConnection connection : connectionsFromPorts) {
            removeConnection(connection);
        }

        List<NodeConnection> connectionsToBox = new ArrayList<>();
        for (NodeConnection connection : connections) {
            if (findOwnerBox(connection.getTarget()) == box || findOwnerBox(connection.getSource()) == box) {
                connectionsToBox.add(connection);
            }
        }
        for (NodeConnection connection : connectionsToBox) {
            removeConnection(connection);
        }

        nodeBoxes.remove(box);
        nodesLayer.getChildren().remove(box);
        if (onChange != null) {
            onChange.run();
        }
    }

    public void clear() {
        for (NodeConnection connection : new ArrayList<>(connections)) {
            connection.disconnect();
        }
        connections.clear();
        connectionsLayer.getChildren().clear();
        nodeBoxes.clear();
        nodesLayer.getChildren().clear();

        canvasGroup.setTranslateX(0);
        canvasGroup.setTranslateY(0);
        zoomScale = 1.0;
        canvasGroup.setScaleX(1.0);
        canvasGroup.setScaleY(1.0);
    }

    private NodePort findPortAt(MouseEvent mouseEvent) {
        for (NodeBox box : nodeBoxes) {
            for (NodePort port : box.getAllPorts()) {
                Point2D portScenePosition = port.getCenterInScene();
                double distanceToMouse = portScenePosition.distance(mouseEvent.getSceneX(), mouseEvent.getSceneY());
                if (distanceToMouse < PORT_HIT_RADIUS) {
                    return port;
                }
            }
        }
        return null;
    }

    private NodeBox findNodeAt(MouseEvent mouseEvent) {
        Point2D localPosition = canvasGroup.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        if (localPosition == null) {
            return null;
        }

        for (int index = nodeBoxes.size() - 1; index >= 0; index--) {
            NodeBox box = nodeBoxes.get(index);
            if (box.getBoundsInParent().contains(localPosition)) {
                return box;
            }
        }
        return null;
    }

    private NodeBox findOwnerBox(NodePort port) {
        javafx.scene.Node currentNode = port;
        while (currentNode != null) {
            if (currentNode instanceof NodeBox) {
                return (NodeBox) currentNode;
            }
            currentNode = currentNode.getParent();
        }
        return null;
    }

    public void showAddStepMenu(javafx.scene.Node anchor) {
        ContextMenu menu = new ContextMenu();
        for (String stepType : STEP_TYPES) {
            MenuItem menuItem = new MenuItem(stepType);
            menuItem.setOnAction(actionEvent -> {
                StepModel model = new StepModel(stepType);
                StepDefaults.initDefaults(model);

                Point2D canvasCenter = canvasGroup.sceneToLocal(
                    getWidth() / 2 + localToScene(0, 0).getX(),
                    getHeight() / 2 + localToScene(0, 0).getY()
                );
                if (canvasCenter != null) {
                    model.setNodeX(canvasCenter.getX());
                    model.setNodeY(canvasCenter.getY());
                }

                addNode(model);
                if (onChange != null) {
                    onChange.run();
                }
            });
            menu.getItems().add(menuItem);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    public List<StepModel> buildStepTree() {
        return GraphSerializer.buildStepTree(nodeBoxes, connections);
    }

    public Map<String, Map<String, Double>> buildLayoutMap() {
        return GraphSerializer.buildLayoutMap(nodeBoxes);
    }

    public void loadFromJson(Map<String, Object> simulationData) {
        GraphDeserializer.loadFromJson(simulationData, this);
    }

    public void cleanupLayout() {
        GraphLayoutEngine.cleanupLayout(nodeBoxes, connections, onChange);
    }

    public List<NodeBox> getNodeBoxes() {
        return nodeBoxes;
    }
}
