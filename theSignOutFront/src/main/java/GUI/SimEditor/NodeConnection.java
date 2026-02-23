package GUI.SimEditor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.CubicCurve;

public class NodeConnection extends CubicCurve {

    private static final double TANGENT_OFFSET = 80;
    private static final double STROKE_WIDTH = 2.5;

    private final NodePort source;
    private final NodePort target;
    private final ChangeListener<Bounds> boundsListener;

    public NodeConnection(NodePort source, NodePort target, Group coordinateSpace) {
        this.source = source;
        this.target = target;

        getStyleClass().add("node-connection");
        setFill(null);
        setStrokeWidth(STROKE_WIDTH);
        setMouseTransparent(false);

        boundsListener = (observable, oldBounds, newBounds) -> updateCurve(coordinateSpace);

        source.boundsInParentProperty().addListener(boundsListener);
        target.boundsInParentProperty().addListener(boundsListener);

        if (source.getParent() != null && source.getParent().getParent() != null) {
            source.getParent().getParent().layoutXProperty().addListener(
                (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
            source.getParent().getParent().layoutYProperty().addListener(
                (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
            javafx.scene.Node sourceNodeBox = findNodeBox(source);
            if (sourceNodeBox != null) {
                sourceNodeBox.layoutXProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
                sourceNodeBox.layoutYProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
                sourceNodeBox.boundsInParentProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
            }
        }
        if (target.getParent() != null && target.getParent().getParent() != null) {
            javafx.scene.Node targetNodeBox = findNodeBox(target);
            if (targetNodeBox != null) {
                targetNodeBox.layoutXProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
                targetNodeBox.layoutYProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
                targetNodeBox.boundsInParentProperty().addListener(
                    (observable, oldValue, newValue) -> updateCurve(coordinateSpace));
            }
        }

        javafx.application.Platform.runLater(() -> updateCurve(coordinateSpace));
    }

    private javafx.scene.Node findNodeBox(javafx.scene.Node startNode) {
        javafx.scene.Node currentNode = startNode;
        while (currentNode != null) {
            if (currentNode instanceof NodeBox) {
                return currentNode;
            }
            currentNode = currentNode.getParent();
        }
        return null;
    }

    private void updateCurve(Group coordinateSpace) {
        try {
            Point2D sourceScenePosition = source.getCenterInScene();
            Point2D targetScenePosition = target.getCenterInScene();

            Point2D sourceLocalPosition = coordinateSpace.sceneToLocal(sourceScenePosition);
            Point2D targetLocalPosition = coordinateSpace.sceneToLocal(targetScenePosition);

            if (sourceLocalPosition == null || targetLocalPosition == null) {
                return;
            }

            setStartX(sourceLocalPosition.getX());
            setStartY(sourceLocalPosition.getY());
            setEndX(targetLocalPosition.getX());
            setEndY(targetLocalPosition.getY());

            setControlX1(sourceLocalPosition.getX() + TANGENT_OFFSET);
            setControlY1(sourceLocalPosition.getY());
            setControlX2(targetLocalPosition.getX() - TANGENT_OFFSET);
            setControlY2(targetLocalPosition.getY());
        } catch (Exception ignored) {
        }
    }

    public NodePort getSource() {
        return source;
    }

    public NodePort getTarget() {
        return target;
    }

    public void disconnect() {
        if (source.getConnection() == this) {
            source.setConnection(null);
        }
        if (target.getConnection() == this) {
            target.setConnection(null);
        }
    }
}
