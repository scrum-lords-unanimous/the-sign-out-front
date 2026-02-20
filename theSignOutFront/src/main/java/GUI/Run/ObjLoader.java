package GUI.Run;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {

    public static MeshView load(String resourcePath) {
        TriangleMesh mesh = new TriangleMesh();
        List<float[]> verts = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        try (InputStream in = ObjLoader.class.getResourceAsStream(resourcePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    verts.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    texCoords.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("f ")) {
                    String[] p = line.split("\\s+");
                    int[][] faceVerts = new int[p.length - 1][2];
                    for (int i = 1; i < p.length; i++) {
                        String[] parts = p[i].split("/");
                        faceVerts[i - 1][0] = Integer.parseInt(parts[0]) - 1;
                        faceVerts[i - 1][1] = parts.length > 1 && !parts[1].isEmpty()
                                ? Integer.parseInt(parts[1]) - 1 : 0;
                    }
                    for (int i = 1; i < faceVerts.length - 1; i++) {
                        faces.add(new int[]{
                                faceVerts[0][0], faceVerts[0][1],
                                faceVerts[i][0], faceVerts[i][1],
                                faceVerts[i + 1][0], faceVerts[i + 1][1]
                        });
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load OBJ: " + resourcePath, e);
        }

        for (float[] v : verts) mesh.getPoints().addAll(v[0], v[1], v[2]);
        if (texCoords.isEmpty()) texCoords.add(new float[]{0, 0});
        for (float[] t : texCoords) mesh.getTexCoords().addAll(t[0], t[1]);
        for (int[] f : faces) mesh.getFaces().addAll(f);

        MeshView view = new MeshView(mesh);
        PhongMaterial mat = new PhongMaterial(Color.RED);
        mat.setSpecularColor(Color.WHITE);
        view.setMaterial(mat);
        return view;
    }
}
