package GUI.Run;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;

import java.util.List;

public class SignBuilder {

    private static final double MIN_DIRECTION_LENGTH = 0.001;
    private static final double SIGN_HEIGHT_RATIO = 0.8;
    private static final double SIGN_ASPECT_RATIO = 9.0 / 16.0;
    private static final double SIGN_LENGTH_MULTIPLIER = 4;
    private static final double SIGN_LATERAL_GAP = 2;
    private static final float FACE_OFFSET = -0.1f;
    private static final String DARK_SURFACE_COLOR = "#323232";
    private static final String LIGHT_SURFACE_COLOR = "#d0d0d0";

    private SignBuilder() {
    }

    public static Result build(List<double[]> waypoints, double roadWidth) {
        double[] endPoint = waypoints.get(waypoints.size() - 1);
        double[] previousPoint = waypoints.get(waypoints.size() - 2);

        double directionX = endPoint[0] - previousPoint[0];
        double directionZ = endPoint[2] - previousPoint[2];
        double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (directionLength < MIN_DIRECTION_LENGTH) {
            directionX = 0;
            directionZ = 1;
            directionLength = 1;
        }

        double headingDegrees = Math.toDegrees(Math.atan2(directionX, directionZ));
        double headingRadians = Math.toRadians(headingDegrees);

        double signHeight = roadWidth * SIGN_HEIGHT_RATIO;
        double signWidth = signHeight * SIGN_ASPECT_RATIO;
        double signLength = signWidth * SIGN_LENGTH_MULTIPLIER;

        double lateralOffset = roadWidth + signWidth / 2 + SIGN_LATERAL_GAP;
        double signPositionX = endPoint[0] + Math.cos(headingRadians) * lateralOffset;
        double signPositionZ = endPoint[2] - Math.sin(headingRadians) * lateralOffset;

        Box signBox = new Box(signWidth, signHeight, signLength);
        PhongMaterial boxMaterial = new PhongMaterial(Color.BLACK);
        boxMaterial.setSpecularColor(Color.BLACK);

        Runnable updateBoxColor = () -> {
            boolean isDarkMode = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
            WritableImage colorImage = new WritableImage(1, 1);
            colorImage.getPixelWriter().setColor(0, 0,
                isDarkMode ? Color.web(DARK_SURFACE_COLOR) : Color.web(LIGHT_SURFACE_COLOR));
            boxMaterial.setSelfIlluminationMap(colorImage);
        };
        updateBoxColor.run();
        Platform.getPreferences().colorSchemeProperty().addListener(
            (observable, oldScheme, newScheme) -> updateBoxColor.run());
        signBox.setMaterial(boxMaterial);

        TriangleMesh faceMesh = new TriangleMesh();
        float faceX = -(float) (signWidth / 2) + FACE_OFFSET;
        float halfHeight = (float) (signHeight / 2);
        float halfLength = (float) (signLength / 2);
        faceMesh.getPoints().addAll(
            faceX, -halfHeight, -halfLength,
            faceX, -halfHeight, halfLength,
            faceX, halfHeight, halfLength,
            faceX, halfHeight, -halfLength);
        faceMesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        faceMesh.getFaces().addAll(
            0, 0, 1, 1, 2, 2,
            0, 0, 2, 2, 3, 3);

        MeshView faceView = new MeshView(faceMesh);
        faceView.setCullFace(CullFace.NONE);

        PhongMaterial signMaterial = new PhongMaterial();
        try {
            signMaterial.setDiffuseMap(new Image(SignBuilder.class.getResourceAsStream("/Logo/signoutfronlogo.png")));
        } catch (Exception exception) {
            signMaterial.setDiffuseColor(Color.WHITE);
        }
        faceView.setMaterial(signMaterial);

        Group signGroup = new Group(signBox, faceView);
        signGroup.setTranslateX(signPositionX);
        signGroup.setTranslateY(-signHeight / 2);
        signGroup.setTranslateZ(signPositionZ);
        signGroup.getTransforms().add(new Rotate(headingDegrees, Rotate.Y_AXIS));

        return new Result(signGroup, signMaterial);
    }

    public record Result(Node node, PhongMaterial signMaterial) {
    }
}
