package GUI.SimEditor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.CubicCurve;

/**
 * A bezier curve connecting an output port to an input port.
 * Automatically updates its endpoints when ports move.
 */
public class NodeConnection extends CubicCurve {

    private static final double TANGENT_OFFSET = 80;

    private final NodePort source; // output
    private final NodePort target; // input
    private final ChangeListener<Bounds> boundsListener;

    public NodeConnection(NodePort source, NodePort target, Group coordinateSpace) {
        this.source = source;
        this.target = target;

        getStyleClass().add("node-connection");
        setFill(null);
        setStrokeWidth(2.5);
        setMouseTransparent(false);

        // Update curve when bounds change
        boundsListener = (obs, oldVal, newVal) -> updateCurve(coordinateSpace);

        source.boundsInParentProperty().addListener(boundsListener);
        target.boundsInParentProperty().addListener(boundsListener);

        // Also listen for node (ancestor) layout changes
        if (source.getParent() != null && source.getParent().getParent() != null) {
            source.getParent().getParent().layoutXProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
            source.getParent().getParent().layoutYProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
            // NodeBox is grandparent of port circle in the hierarchy:
            // NodeBox -> HBox (outputRow) -> NodePort
            javafx.scene.Node nodeBox = findNodeBox(source);
            if (nodeBox != null) {
                nodeBox.layoutXProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
                nodeBox.layoutYProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
                nodeBox.boundsInParentProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
            }
        }
        if (target.getParent() != null && target.getParent().getParent() != null) {
            javafx.scene.Node nodeBox = findNodeBox(target);
            if (nodeBox != null) {
                nodeBox.layoutXProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
                nodeBox.layoutYProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
                nodeBox.boundsInParentProperty().addListener((o, a, b) -> updateCurve(coordinateSpace));
            }
        }

        // Initial update deferred to after layout
        javafx.application.Platform.runLater(() -> updateCurve(coordinateSpace));
    }

    private javafx.scene.Node findNodeBox(javafx.scene.Node node) {
        javafx.scene.Node current = node;
        while (current != null) {
            if (current instanceof NodeBox) return current;
            current = current.getParent();
        }
        return null;
    }

    private void updateCurve(Group coordinateSpace) {
        try {
            Point2D srcScene = source.getCenterInScene();
            Point2D tgtScene = target.getCenterInScene();

            Point2D srcLocal = coordinateSpace.sceneToLocal(srcScene);
            Point2D tgtLocal = coordinateSpace.sceneToLocal(tgtScene);

            if (srcLocal == null || tgtLocal == null) return;

            setStartX(srcLocal.getX());
            setStartY(srcLocal.getY());
            setEndX(tgtLocal.getX());
            setEndY(tgtLocal.getY());

            // Horizontal tangent for bezier
            setControlX1(srcLocal.getX() + TANGENT_OFFSET);
            setControlY1(srcLocal.getY());
            setControlX2(tgtLocal.getX() - TANGENT_OFFSET);
            setControlY2(tgtLocal.getY());
        } catch (Exception ignored) {
            // Port may not be in scene yet
        }
    }

    public NodePort getSource() { return source; }
    public NodePort getTarget() { return target; }

    /**
     * Disconnects this connection from both ports.
     */
    public void disconnect() {
        if (source.getConnection() == this) {
            source.setConnection(null);
        }
        if (target.getConnection() == this) {
            target.setConnection(null);
        }
    }
}
