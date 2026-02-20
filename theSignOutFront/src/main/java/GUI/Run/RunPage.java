package GUI.Run;

import CLI.UserInput.DoMath.MathIsDone;
import Data.Driveway.Driveway;
import Data.Slide.Slide;
import GUI.Assets.SlideSetStore;
import GUI.SimEditor.SimulationStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import java.io.InputStream;
import java.util.*;

public class RunPage {

    private static final double ROAD_W = 20;
    private static final String OBJ = "/Models/Car.obj";
    private static final Color SURFACE = Color.web("#323232");
    private double playbackSpeed = 1, simTick = 0, spin = 0, dragStartX, dragSpin;
    private PhongMaterial signMaterial;
    private List<Image> slideImages;
    private Image logoImage;
    private int cycleDuration = 10;
    private SlideViewTracker tracker;
    private AnimationTimer timer;
    private boolean dayEnded;
    private int lastSlideIdx = -1;

    public VBox build(Label title) {
        var def = loadDefaults();
        double len = d(def.get("long-is-driveway")), rad = d(def.get("avg-radii-of-drive-curves"));
        double rate = d(def.get("rate-of-advancement"));
        int count = i(def.get("childs-on-campus"));
        Driveway dw = Driveway.build(len, i(def.get("curves-in-driveway")), rad,
                d(def.get("avg-speed")), d(def.get("how-heavy-is-the-car")), new MathIsDone());
        Random rand = new Random();
        List<double[]> wps = DrivewayMesh.buildWaypoints(dw, rad, rand);

        double[][] students = new double[count][3];
        for (int j = 0; j < count; j++) {
            int hr; if (rand.nextDouble() < 0.05) hr = rand.nextBoolean() ? 23 : rand.nextInt(6);
            else hr = 6 + rand.nextInt(17);
            students[j][0] = hr * 3600.0 + rand.nextInt(60) * 60.0;
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] wp : wps) {
            minX = Math.min(minX, wp[0]); maxX = Math.max(maxX, wp[0]);
            minZ = Math.min(minZ, wp[2]); maxZ = Math.max(maxZ, wp[2]);
        }
        double cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
        double sX = maxX - minX + ROAD_W * 8, sZ = maxZ - minZ + ROAD_W * 8;

        MeshView road = DrivewayMesh.buildRoadMesh(wps, ROAD_W);
        road.setCullFace(CullFace.NONE);
        PhongMaterial roadMat = new PhongMaterial(Color.BLACK);
        roadMat.setSpecularColor(Color.BLACK);
        WritableImage ri = new WritableImage(1, 1);
        ri.getPixelWriter().setColor(0, 0, SURFACE);
        roadMat.setSelfIlluminationMap(ri);
        road.setMaterial(roadMat);

        Color accent = Platform.getPreferences().getAccentColor();
        PhongMaterial carMat = new PhongMaterial(accent);
        carMat.setSpecularColor(Color.WHITE);
        Platform.getPreferences().accentColorProperty().addListener((o, x, c) -> carMat.setDiffuseColor(c));

        MeshView template;
        try { template = ObjLoader.load(OBJ); } catch (Exception e) { template = new MeshView(); }
        double carScale = 5;
        if (template.getMesh() instanceof TriangleMesh tm && tm.getPoints().size() >= 3) {
            var mp = tm.getPoints(); float mx = 0;
            for (int k = 0; k < mp.size(); k++) mx = Math.max(mx, Math.abs(mp.get(k)));
            if (mx > 0) carScale = (ROAD_W * 0.4) / mx;
        }
        List<MeshView> cars = new ArrayList<>();
        for (int j = 0; j < count; j++) {
            MeshView c = new MeshView(template.getMesh());
            c.setMaterial(carMat); c.setCullFace(CullFace.NONE);
            c.setScaleX(carScale); c.setScaleY(carScale); c.setScaleZ(carScale);
            c.setVisible(false); cars.add(c);
        }

        String activeSet = SlideSetStore.getActiveSet();
        List<Slide> slidesWithImages = SlideSetStore.loadSlidesWithImages(activeSet);
        slideImages = SlideSetStore.loadSetImages(activeSet);
        try {
            var cfg = SlideSetStore.loadSet(activeSet);
            if (cfg != null) cycleDuration = cfg.getCycleDuration();
        } catch (Exception ignored) {}
        tracker = new SlideViewTracker(slidesWithImages);
        try { logoImage = new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png")); }
        catch (Exception ignored) {}

        AmbientLight amb = new AmbientLight(Color.WHITE);
        PointLight pt = new PointLight(Color.WHITE);
        pt.setTranslateX(cx); pt.setTranslateY(-300); pt.setTranslateZ(cz);
        Group world = new Group(road, buildSign(wps, carScale), amb, pt);
        world.getChildren().addAll(cars);

        ParallelCamera cam = new ParallelCamera();
        SubScene sub = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        sub.setCamera(cam); sub.setFill(Color.web("#202020"));
        double tilt = 55, fsh = Math.sin(Math.toRadians(tilt));
        final double fsx = sX, fsz = sZ * fsh, fcx = cx, fcz = cz;
        Runnable fitView = () -> {
            double w = sub.getWidth(), h = sub.getHeight();
            if (w <= 0 || h <= 0) return;
            double sc = Math.min(w / fsx, h / fsz);
            world.getTransforms().setAll(new Translate(w / 2, h / 2, 0), new Scale(sc, sc, sc),
                    new Rotate(tilt, Rotate.X_AXIS), new Rotate(spin, Rotate.Y_AXIS),
                    new Translate(-fcx, 0, -fcz));
        };

        Label spdLabel = new Label("Speed: 1x"); spdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");
        Slider slider = new Slider(1, 10000, 1); HBox.setHgrow(slider, Priority.ALWAYS);
        slider.valueProperty().addListener((o, x, v) -> {
            playbackSpeed = v.doubleValue(); spdLabel.setText(String.format("Speed: %.0fx", playbackSpeed));
            styleTrack(slider, accent);
        });
        HBox sliderBox = new HBox(8, spdLabel, slider);
        sliderBox.setAlignment(Pos.CENTER_LEFT); sliderBox.setMaxWidth(240); sliderBox.setMaxHeight(Region.USE_PREF_SIZE);
        sliderBox.setPadding(new Insets(6, 12, 6, 12));
        sliderBox.setStyle("-fx-background-color: #323232; -fx-background-radius: 8;");
        spdLabel.setMinWidth(Region.USE_PREF_SIZE);
        Platform.runLater(() -> styleTrack(slider, accent));
        Platform.getPreferences().accentColorProperty().addListener((o, x, c) -> styleTrack(slider, c));

        Label timeLabel = new Label("12:00 AM");
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-background-color: #323232; -fx-background-radius: 8; -fx-padding: 6 12;");

        VBox rankBox = RankingsOverlay.build(tracker);

        StackPane sceneStack = new StackPane(sub, sliderBox, timeLabel, rankBox);
        StackPane.setAlignment(sliderBox, Pos.TOP_RIGHT); StackPane.setMargin(sliderBox, new Insets(8));
        StackPane.setAlignment(timeLabel, Pos.TOP_LEFT); StackPane.setMargin(timeLabel, new Insets(8));
        StackPane.setAlignment(rankBox, Pos.BOTTOM_LEFT); StackPane.setMargin(rankBox, new Insets(8));
        sceneStack.setStyle("-fx-background-color: #202020;"); sceneStack.setMinSize(0, 0);
        VBox.setVgrow(sceneStack, Priority.ALWAYS);
        sceneStack.widthProperty().addListener((o, x, w) -> { sub.setWidth(w.doubleValue()); fitView.run(); });
        sceneStack.heightProperty().addListener((o, x, h) -> { sub.setHeight(h.doubleValue()); fitView.run(); });
        sceneStack.setOnMousePressed(e -> { dragStartX = e.getScreenX(); dragSpin = spin; });
        sceneStack.setOnMouseDragged(e -> { spin = dragSpin + (e.getScreenX() - dragStartX) * 0.3; fitView.run(); });

        FlowPane grid = new FlowPane(12, 12);
        for (String n : SimulationStore.listSimulations()) grid.getChildren().add(simCard(n, students));

        VBox page = new VBox(8, title, sceneStack, grid);
        page.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        timer = new AnimationTimer() {
            long last = 0; int frameCount = 0;
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) / 1e9; last = now;
                simTick += playbackSpeed * dt;
                if (simTick >= 86400 && !dayEnded) {
                    dayEnded = true;
                    timer.stop();
                    DayStatsDialog.show(tracker, () -> {
                        simTick = 0; dayEnded = false;
                        tracker.reset(); lastSlideIdx = -1;
                        for (double[] s : students) { s[1] = 0; s[2] = 0; }
                        timer.start();
                    });
                    return;
                }
                int sec = (int) simTick, hr = (sec / 3600) % 24, mn = (sec % 3600) / 60;
                String ap = hr < 12 ? "AM" : "PM"; int h12 = hr % 12; if (h12 == 0) h12 = 12;
                timeLabel.setText(String.format("%d:%02d %s", h12, mn, ap));
                updateBillboard();
                for (int j = 0; j < count; j++) {
                    boolean on = students[j][1] == 1; double pos = students[j][2];
                    if (!on && pos == 0 && simTick >= students[j][0]) {
                        on = true;
                        if (!slideImages.isEmpty() && tracker.size() > 0) {
                            int si = ((int)(simTick / cycleDuration)) % tracker.size();
                            tracker.recordView(si);
                        }
                    }
                    if (on) {
                        pos += (dw.speedAt(pos) * 5280.0 / 3600.0) * rate * playbackSpeed * dt;
                        if (pos >= dw.totalLength) { on = false; pos = -1; }
                    }
                    students[j][1] = on ? 1 : 0; students[j][2] = pos;
                    MeshView c = cars.get(j);
                    if (on) {
                        double[] p = DrivewayMesh.interp(wps, pos), dr = DrivewayMesh.dir(wps, pos);
                        c.setTranslateX(p[0]); c.setTranslateY(p[1] - 1); c.setTranslateZ(p[2]);
                        c.getTransforms().setAll(new Rotate(Math.toDegrees(Math.atan2(dr[0], dr[2])) + 180,
                                Rotate.Y_AXIS), new Rotate(180, Rotate.X_AXIS));
                        c.setVisible(true);
                    } else c.setVisible(false);
                }
                frameCount++;
                if (frameCount % 30 == 0) RankingsOverlay.update(rankBox, tracker);
            }
        };
        timer.start();
        page.sceneProperty().addListener((o, x, sc) -> { if (sc == null) timer.stop(); });
        return page;
    }

    private void updateBillboard() {
        if (signMaterial == null) return;
        if (!slideImages.isEmpty()) {
            int idx = ((int)(simTick / cycleDuration)) % slideImages.size();
            signMaterial.setDiffuseMap(slideImages.get(idx));
        } else if (logoImage != null) {
            signMaterial.setDiffuseMap(logoImage);
        }
    }

    private Node buildSign(List<double[]> wps, double carScale) {
        double[] end = wps.get(wps.size()-1), prev = wps.get(wps.size()-2);
        double dx = end[0]-prev[0], dz = end[2]-prev[2];
        double l = Math.sqrt(dx*dx+dz*dz); if (l < 0.001) { dx=0; dz=1; l=1; }
        double heading = Math.toDegrees(Math.atan2(dx, dz));
        double hRad = Math.toRadians(heading);
        double signH = ROAD_W*0.8, signW = signH*9.0/16.0, signL = signW*4;
        double offset = ROAD_W + signW/2 + 2;
        double ox = end[0] + Math.cos(hRad)*offset, oz = end[2] - Math.sin(hRad)*offset;
        Box box = new Box(signW, signH, signL);
        PhongMaterial bm = new PhongMaterial(Color.BLACK);
        bm.setSpecularColor(Color.BLACK);
        WritableImage bi = new WritableImage(1,1); bi.getPixelWriter().setColor(0,0,SURFACE);
        bm.setSelfIlluminationMap(bi); box.setMaterial(bm);
        TriangleMesh fm = new TriangleMesh();
        float xo = -(float)(signW/2)-0.1f, hh = (float)(signH/2), hl = (float)(signL/2);
        fm.getPoints().addAll(xo,-hh,-hl, xo,-hh,hl, xo,hh,hl, xo,hh,-hl);
        fm.getTexCoords().addAll(0,0, 1,0, 1,1, 0,1);
        fm.getFaces().addAll(0,0,1,1,2,2, 0,0,2,2,3,3);
        MeshView face = new MeshView(fm); face.setCullFace(CullFace.NONE);
        signMaterial = new PhongMaterial();
        try { signMaterial.setDiffuseMap(new Image(getClass().getResourceAsStream("/Logo/signoutfronlogo.png"))); }
        catch (Exception e) { signMaterial.setDiffuseColor(Color.WHITE); }
        face.setMaterial(signMaterial);
        Group g = new Group(box, face);
        g.setTranslateX(ox); g.setTranslateY(-signH/2); g.setTranslateZ(oz);
        g.getTransforms().add(new Rotate(heading, Rotate.Y_AXIS));
        return g;
    }

    private VBox simCard(String name, double[][] students) {
        Label icon = new Label("\uE768");
        icon.setStyle("-fx-font-family: 'Segoe Fluent Icons'; -fx-font-size: 28px;");
        Label lbl = new Label(name); lbl.setStyle("-fx-font-size: 14px;");
        VBox card = new VBox(8, icon, lbl);
        card.setPadding(new Insets(16)); card.setPrefSize(140, 120);
        card.setAlignment(Pos.CENTER); card.getStyleClass().add("sim-card");
        card.setOnMouseClicked(e -> { simTick = 0; for (double[] s : students) { s[1] = 0; s[2] = 0; } });
        return card;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDefaults() {
        try (InputStream in = getClass().getResourceAsStream("/JsonRoot/InputQuestions/DefaultAnswers.json")) {
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void styleTrack(Slider s, Color c) {
        var track = s.lookup(".track");
        if (track == null) return;
        double pct = (s.getValue() - s.getMin()) / (s.getMax() - s.getMin()) * 100;
        String hex = String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
        track.setStyle("-fx-background-color: linear-gradient(to right, " + hex + " " + pct + "%, #555 " + pct + "%); -fx-background-radius: 4; -fx-background-insets: 0;");
        var thumb = s.lookup(".thumb");
        if (thumb != null) thumb.setStyle("-fx-background-color: " + hex + ";");
    }

    private double d(Object o) { return o instanceof Number n ? n.doubleValue() : Double.parseDouble(o.toString()); }
    private int i(Object o) { return o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString()); }
}
