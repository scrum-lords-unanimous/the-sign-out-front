package GUI.SimEditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class NodePort extends StackPane {

    private static final double PORT_CIRCLE_RADIUS = 7;
    private static final double PORT_SIZE = 18;
    private static final String CONNECTED_STYLE_CLASS = "connected";

    public enum Direction { INPUT, OUTPUT }

    public enum PortRole {
        EXEC_IN, EXEC_OUT, LOOP_BODY, THEN, ELSE;

        public String label() {
            switch (this) {
                case EXEC_IN:
                    return "In";
                case EXEC_OUT:
                    return "Out";
                case LOOP_BODY:
                    return "Loop Body";
                case THEN:
                    return "Then";
                case ELSE:
                    return "Else";
                default:
                    return name();
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

        circle = new Circle(PORT_CIRCLE_RADIUS);
        circle.getStyleClass().add("node-port-circle");
        getChildren().add(circle);
        getStyleClass().add("node-port");

        setPrefSize(PORT_SIZE, PORT_SIZE);
        setMinSize(PORT_SIZE, PORT_SIZE);
        setMaxSize(PORT_SIZE, PORT_SIZE);

        connection.addListener((observable, oldConnection, newConnection) -> {
            if (newConnection != null) {
                if (!circle.getStyleClass().contains(CONNECTED_STYLE_CLASS)) {
                    circle.getStyleClass().add(CONNECTED_STYLE_CLASS);
                }
            } else {
                circle.getStyleClass().remove(CONNECTED_STYLE_CLASS);
            }
        });
    }

    public Direction getDirection() {
        return direction;
    }

    public PortRole getRole() {
        return role;
    }

    public Circle getCircle() {
        return circle;
    }

    public ObjectProperty<NodeConnection> connectionProperty() {
        return connection;
    }

    public NodeConnection getConnection() {
        return connection.get();
    }

    public void setConnection(NodeConnection connectionValue) {
        connection.set(connectionValue);
    }

    public boolean isConnected() {
        return connection.get() != null;
    }

    public Point2D getCenterInScene() {
        return localToScene(getWidth() / 2, getHeight() / 2);
    }
}
