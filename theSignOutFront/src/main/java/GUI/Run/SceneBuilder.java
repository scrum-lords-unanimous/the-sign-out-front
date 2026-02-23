package GUI.Run;

import Data.Driveway.Driveway;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SceneBuilder {

    private static final double ROAD_WIDTH = 20;
    private static final String CAR_MODEL_PATH = "/Models/Car.obj";
    private static final double ROAD_PADDING_MULTIPLIER = 8;
    private static final double CAR_SCALE_ROAD_FRACTION = 0.4;
    private static final double DEFAULT_CAR_SCALE = 5;
    private static final double LIGHT_Y_OFFSET = -300;
    private static final double TILT_ANGLE_DEGREES = 55;
    private static final String DARK_SURFACE_COLOR = "#323232";
    private static final String LIGHT_SURFACE_COLOR = "#d0d0d0";
    private static final String DARK_BACKGROUND_COLOR = "#202020";
    private static final String LIGHT_BACKGROUND_COLOR = "#f3f3f3";

    private SceneBuilder() {
    }

    public static Result build(Driveway driveway, int carCount, List<double[]> waypoints) {
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (double[] waypoint : waypoints) {
            minX = Math.min(minX, waypoint[0]);
            maxX = Math.max(maxX, waypoint[0]);
            minZ = Math.min(minZ, waypoint[2]);
            maxZ = Math.max(maxZ, waypoint[2]);
        }
        double centerX = (minX + maxX) / 2;
        double centerZ = (minZ + maxZ) / 2;
        double spanX = maxX - minX + ROAD_WIDTH * ROAD_PADDING_MULTIPLIER;
        double spanZ = maxZ - minZ + ROAD_WIDTH * ROAD_PADDING_MULTIPLIER;

        MeshView roadMesh = DrivewayMesh.buildRoadMesh(waypoints, ROAD_WIDTH);
        roadMesh.setCullFace(CullFace.NONE);
        PhongMaterial roadMaterial = new PhongMaterial(Color.BLACK);
        roadMaterial.setSpecularColor(Color.BLACK);
        Runnable updateRoadColor = () -> {
            boolean isDarkMode = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
            Color surfaceColor = isDarkMode ? Color.web(DARK_SURFACE_COLOR) : Color.web(LIGHT_SURFACE_COLOR);
            WritableImage roadColorImage = new WritableImage(1, 1);
            roadColorImage.getPixelWriter().setColor(0, 0, surfaceColor);
            roadMaterial.setSelfIlluminationMap(roadColorImage);
        };
        updateRoadColor.run();
        Platform.getPreferences().colorSchemeProperty().addListener(
            (observable, oldScheme, newScheme) -> updateRoadColor.run());
        roadMesh.setMaterial(roadMaterial);

        Color accentColor = Platform.getPreferences().getAccentColor();
        PhongMaterial carMaterial = new PhongMaterial(accentColor);
        carMaterial.setSpecularColor(Color.WHITE);
        Platform.getPreferences().accentColorProperty().addListener(
            (observable, oldColor, newColor) -> carMaterial.setDiffuseColor(newColor));

        MeshView carTemplate;
        try {
            carTemplate = ObjLoader.load(CAR_MODEL_PATH);
        } catch (Exception exception) {
            carTemplate = new MeshView();
        }

        double carScale = DEFAULT_CAR_SCALE;
        if (carTemplate.getMesh() instanceof TriangleMesh triangleMesh
                && triangleMesh.getPoints().size() >= 3) {
            var meshPoints = triangleMesh.getPoints();
            float maxExtent = 0;
            for (int pointIndex = 0; pointIndex < meshPoints.size(); pointIndex++) {
                maxExtent = Math.max(maxExtent, Math.abs(meshPoints.get(pointIndex)));
            }
            if (maxExtent > 0) {
                carScale = (ROAD_WIDTH * CAR_SCALE_ROAD_FRACTION) / maxExtent;
            }
        }

        List<MeshView> cars = new ArrayList<>();
        for (int carIndex = 0; carIndex < carCount; carIndex++) {
            MeshView car = new MeshView(carTemplate.getMesh());
            car.setMaterial(carMaterial);
            car.setCullFace(CullFace.NONE);
            car.setScaleX(carScale);
            car.setScaleY(carScale);
            car.setScaleZ(carScale);
            car.setVisible(false);
            cars.add(car);
        }

        SignBuilder.Result sign = SignBuilder.build(waypoints, ROAD_WIDTH);

        AmbientLight ambientLight = new AmbientLight(Color.WHITE);
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(centerX);
        pointLight.setTranslateY(LIGHT_Y_OFFSET);
        pointLight.setTranslateZ(centerZ);
        Group world = new Group(roadMesh, sign.node(), ambientLight, pointLight);
        world.getChildren().addAll(cars);

        ParallelCamera camera = new ParallelCamera();
        SubScene subScene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        Runnable updateBackgroundFill = () -> {
            boolean isDarkMode = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
            subScene.setFill(isDarkMode ? Color.web(DARK_BACKGROUND_COLOR) : Color.web(LIGHT_BACKGROUND_COLOR));
        };
        updateBackgroundFill.run();
        Platform.getPreferences().colorSchemeProperty().addListener(
            (observable, oldScheme, newScheme) -> updateBackgroundFill.run());

        double tiltAngle = TILT_ANGLE_DEGREES;
        double foreshortening = Math.sin(Math.toRadians(tiltAngle));
        final double fitSpanX = spanX;
        final double fitSpanZ = spanZ * foreshortening;
        final double fitCenterX = centerX;
        final double fitCenterZ = centerZ;

        return new Result(subScene, world, cars, sign.signMaterial(), carScale,
            tiltAngle, fitSpanX, fitSpanZ, fitCenterX, fitCenterZ);
    }

    public static void fitView(SubScene subScene, Group world, double tiltAngle, double spinAngle,
                                double fitSpanX, double fitSpanZ, double fitCenterX, double fitCenterZ) {
        double viewWidth = subScene.getWidth();
        double viewHeight = subScene.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        double scaleFactor = Math.min(viewWidth / fitSpanX, viewHeight / fitSpanZ);
        world.getTransforms().setAll(
            new Translate(viewWidth / 2, viewHeight / 2, 0),
            new Scale(scaleFactor, scaleFactor, scaleFactor),
            new Rotate(tiltAngle, Rotate.X_AXIS),
            new Rotate(spinAngle, Rotate.Y_AXIS),
            new Translate(-fitCenterX, 0, -fitCenterZ));
    }

    public record Result(SubScene subScene, Group world, List<MeshView> cars,
                         PhongMaterial signMaterial, double carScale,
                         double tilt, double fsx, double fsz, double fcx, double fcz) {
    }
}
