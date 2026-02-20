package GUI.SimEditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class NodePort extends StackPane {

    public enum Direction { INPUT, OUTPUT }

    public enum PortRole {
        EXEC_IN, EXEC_OUT, LOOP_BODY, THEN, ELSE;

        public String label() {
            switch (this) {
                case EXEC_IN:    return "In";
                case EXEC_OUT:   return "Out";
                case LOOP_BODY:  return "Loop Body";
                case THEN:       return "Then";
                case ELSE:       return "Else";
                default:         return name();
            }
        }
    }

    private final Direction direction;
    private final PortRole role;
    private final Circle circle;
    private final ObjectProperty<NodeConnection> connection = new SimpleObjectProperty<>();

    public NodePort(Direction direction, PortRole role) {
        this.direction = direction;
        this.role = role;

        circle = new Circle(7);
        circle.getStyleClass().add("node-port-circle");
        getChildren().add(circle);
        getStyleClass().add("node-port");

        setPrefSize(18, 18);
        setMinSize(18, 18);
        setMaxSize(18, 18);

        // Update visual when connection state changes
        connection.addListener((obs, oldConn, newConn) -> {
            if (newConn != null) {
                if (!circle.getStyleClass().contains("connected")) {
                    circle.getStyleClass().add("connected");
                }
            } else {
                circle.getStyleClass().remove("connected");
            }
        });
    }

    public Direction getDirection() { return direction; }
    public PortRole getRole() { return role; }
    public Circle getCircle() { return circle; }

    public ObjectProperty<NodeConnection> connectionProperty() { return connection; }
    public NodeConnection getConnection() { return connection.get(); }
    public void setConnection(NodeConnection conn) { connection.set(conn); }

    public boolean isConnected() { return connection.get() != null; }

    /**
     * Returns the center of this port in the coordinate space of the given ancestor.
     */
    public Point2D getCenterInScene() {
        return localToScene(getWidth() / 2, getHeight() / 2);
    }
}
