package GUI.Run;

import CLI.UserInput.DoMath.MathIsDone;
import Data.Driveway.Driveway;
import Data.Slide.Slide;
import GUI.Assets.SlideSetStore;
import FluentUIJavaFxKit.EmptyState;
import GUI.Files.DrivewayStore;
import GUI.SimEditor.SimulationStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.*;

public class RunPage {

    private static final double DRAG_SENSITIVITY = 0.3;
    private static final double DOT_GRID_SPACING = 25;
    private static final double DOT_RADIUS = 1;
    private static final double DOT_DIAMETER = 2;
    private static final double DOT_OPACITY = 0.25;
    private static final int DEFAULT_CYCLE_DURATION = 10;
    private static final int GRID_GAP = 12;
    private static final int SCENE_INSET = 8;
    private static final double EARLY_MORNING_PROBABILITY = 0.05;
    private static final int EARLY_MORNING_MAX_HOUR = 6;
    private static final int DAYTIME_START_HOUR = 6;
    private static final int DAYTIME_HOUR_RANGE = 17;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int LATE_NIGHT_HOUR = 23;

    private StackPane sceneStack;
    private SpeedControl speedControl;
    private TimeDisplay timeDisplay;
    private VBox rankBox;
    private SimulationLoop loop;
    private SceneBuilder.Result currentScene;

    private double rateOfAdvancement;
    private int studentCount;
    private List<Image> slideImages;
    private Image logoImage;
    private int cycleDuration;
    private SlideViewTracker tracker;
    private String activeSet;
    private String lastSimName;

    private final double[] spinAngle = {0};
    private final double[] dragStartX = {0};
    private final double[] dragStartSpin = {0};

    public VBox build(Label title) {
        List<String> simNames = SimulationStore.listSimulations();
        if (simNames.isEmpty()) {
            VBox page = new VBox(GRID_GAP, title,
                new EmptyState("\uE768", "No simulations yet", "Go to Simulations", null));
            page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            return page;
        }

        Map<String, Object> defaults = loadDefaults();
        rateOfAdvancement = toDouble(defaults.get("rate-of-advancement"));
        studentCount = toInt(defaults.get("childs-on-campus"));

        activeSet = SlideSetStore.getActiveSet();
        List<Slide> slidesWithImages = SlideSetStore.loadSlidesWithImages(activeSet);
        slideImages = SlideSetStore.loadSetImages(activeSet);
        cycleDuration = DEFAULT_CYCLE_DURATION;
        try {
            var slideSetConfig = SlideSetStore.loadSet(activeSet);
            if (slideSetConfig != null) {
                cycleDuration = slideSetConfig.getCycleDuration();
            }
        } catch (Exception ignored) {
        }
        tracker = new SlideViewTracker(slidesWithImages);
        logoImage = null;
        try {
            logoImage = new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png"));
        } catch (Exception ignored) {
        }

        speedControl = new SpeedControl();
        speedControl.setChildCount(studentCount);
        speedControl.childCountProperty().addListener((observable, oldValue, newValue) -> {
            studentCount = newValue.intValue();
            rebuildScene(lastSimName != null ? lastSimName : "Default");
        });
        speedControl.dayCountProperty().addListener((observable, oldValue, newValue) -> {
            if (loop != null) {
                loop.setTotalDays(newValue.intValue());
            }
        });
        timeDisplay = new TimeDisplay();
        rankBox = RankingsOverlay.build(tracker, activeSet);

        sceneStack = new StackPane();
        sceneStack.setStyle("-fx-background-color: -fx-background;");
        sceneStack.setMinSize(0, 0);
        VBox.setVgrow(sceneStack, Priority.ALWAYS);

        Canvas dotGrid = new Canvas();
        dotGrid.setMouseTransparent(true);
        sceneStack.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            dotGrid.setWidth(newWidth.doubleValue());
            drawDotGrid(dotGrid);
        });
        sceneStack.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            dotGrid.setHeight(newHeight.doubleValue());
            drawDotGrid(dotGrid);
        });
        sceneStack.getChildren().add(dotGrid);

        sceneStack.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            if (currentScene == null) {
                return;
            }
            currentScene.subScene().setWidth(newWidth.doubleValue());
            SceneBuilder.fitView(
                currentScene.subScene(), currentScene.world(), currentScene.tilt(),
                spinAngle[0], currentScene.fsx(), currentScene.fsz(),
                currentScene.fcx(), currentScene.fcz());
        });
        sceneStack.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            if (currentScene == null) {
                return;
            }
            currentScene.subScene().setHeight(newHeight.doubleValue());
            SceneBuilder.fitView(
                currentScene.subScene(), currentScene.world(), currentScene.tilt(),
                spinAngle[0], currentScene.fsx(), currentScene.fsz(),
                currentScene.fcx(), currentScene.fcz());
        });
        sceneStack.setOnMousePressed(event -> {
            dragStartX[0] = event.getScreenX();
            dragStartSpin[0] = spinAngle[0];
        });
        sceneStack.setOnMouseDragged(event -> {
            if (currentScene == null) {
                return;
            }
            spinAngle[0] = dragStartSpin[0] + (event.getScreenX() - dragStartX[0]) * DRAG_SENSITIVITY;
            SceneBuilder.fitView(
                currentScene.subScene(), currentScene.world(), currentScene.tilt(),
                spinAngle[0], currentScene.fsx(), currentScene.fsz(),
                currentScene.fcx(), currentScene.fcz());
        });

        rebuildScene("Default");

        FlowPane grid = new FlowPane(GRID_GAP, GRID_GAP);
        for (String simName : simNames) {
            grid.getChildren().add(new SimCard(simName, this::rebuildScene));
        }

        VBox page = new VBox(GRID_GAP, title, sceneStack, rankBox, grid);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        page.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null && loop != null) {
                loop.stop();
            }
        });
        return page;
    }

    private record DrivewayResult(Driveway driveway, List<double[]> waypoints,
                                  double[][] students, SceneBuilder.Result sceneResult) {
    }

    private DrivewayResult buildDriveway(String simName) {
        Map<String, Object> params = DrivewayStore.load(simName);
        double length = toDouble(params.get("length"));
        int curveCount = toInt(params.get("curveCount"));
        double averageRadius = toDouble(params.get("avgRadius"));
        double straightSpeed = toDouble(params.get("straightSpeed"));
        double carWeight = toDouble(params.get("carWeight"));

        Driveway driveway = Driveway.build(length, curveCount, averageRadius, straightSpeed, carWeight, new MathIsDone());
        Random random = new Random();
        List<double[]> waypoints = DrivewayMesh.buildWaypoints(driveway, averageRadius, random);

        double[][] students = new double[studentCount][3];
        for (int studentIndex = 0; studentIndex < studentCount; studentIndex++) {
            int arrivalHour;
            if (random.nextDouble() < EARLY_MORNING_PROBABILITY) {
                arrivalHour = random.nextBoolean() ? LATE_NIGHT_HOUR : random.nextInt(EARLY_MORNING_MAX_HOUR);
            } else {
                arrivalHour = DAYTIME_START_HOUR + random.nextInt(DAYTIME_HOUR_RANGE);
            }
            students[studentIndex][0] = arrivalHour * SECONDS_PER_HOUR + random.nextInt(SECONDS_PER_MINUTE) * (double) SECONDS_PER_MINUTE;
        }

        SceneBuilder.Result sceneResult = SceneBuilder.build(driveway, studentCount, waypoints);
        return new DrivewayResult(driveway, waypoints, students, sceneResult);
    }

    private void rebuildScene(String simName) {
        lastSimName = simName;
        if (loop != null) {
            loop.stop();
        }

        DrivewayResult drivewayResult = buildDriveway(simName);
        currentScene = drivewayResult.sceneResult;

        tracker.reset();
        loop = new SimulationLoop(
            drivewayResult.students, drivewayResult.driveway, rateOfAdvancement,
            drivewayResult.waypoints, currentScene.cars(), currentScene.signMaterial(),
            slideImages, logoImage, cycleDuration, tracker, timeDisplay, rankBox,
            activeSet, speedControl.getDayCount());
        speedControl.playbackSpeedProperty().addListener((observable, oldValue, newValue) ->
            loop.setPlaybackSpeed(newValue.doubleValue()));
        loop.setPlaybackSpeed(speedControl.getPlaybackSpeed());

        spinAngle[0] = 0;

        sceneStack.getChildren().setAll(currentScene.subScene(), speedControl, timeDisplay);
        StackPane.setAlignment(speedControl, Pos.TOP_RIGHT);
        StackPane.setMargin(speedControl, new Insets(SCENE_INSET));
        StackPane.setAlignment(timeDisplay, Pos.TOP_LEFT);
        StackPane.setMargin(timeDisplay, new Insets(SCENE_INSET));

        double stackWidth = sceneStack.getWidth();
        double stackHeight = sceneStack.getHeight();
        if (stackWidth > 0 && stackHeight > 0) {
            currentScene.subScene().setWidth(stackWidth);
            currentScene.subScene().setHeight(stackHeight);
            SceneBuilder.fitView(
                currentScene.subScene(), currentScene.world(), currentScene.tilt(),
                spinAngle[0], currentScene.fsx(), currentScene.fsz(),
                currentScene.fcx(), currentScene.fcz());
        }

        RankingsOverlay.update(rankBox, tracker, activeSet);
        loop.start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDefaults() {
        try (InputStream inputStream = getClass().getResourceAsStream("/JsonRoot/InputQuestions/DefaultAnswers.json")) {
            return new ObjectMapper().readValue(inputStream, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void drawDotGrid(Canvas canvas) {
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.clearRect(0, 0, canvasWidth, canvasHeight);
        graphicsContext.setFill(Color.web("#808080", DOT_OPACITY));
        for (double dotX = 0; dotX < canvasWidth; dotX += DOT_GRID_SPACING) {
            for (double dotY = 0; dotY < canvasHeight; dotY += DOT_GRID_SPACING) {
                graphicsContext.fillOval(dotX - DOT_RADIUS, dotY - DOT_RADIUS, DOT_DIAMETER, DOT_DIAMETER);
            }
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
