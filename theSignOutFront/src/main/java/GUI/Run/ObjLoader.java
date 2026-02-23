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

    private static final int VERTEX_INDEX = 0;
    private static final int TEXTURE_INDEX = 1;

    public static MeshView load(String resourcePath) {
        TriangleMesh mesh = new TriangleMesh();
        List<float[]> vertices = new ArrayList<>();
        List<float[]> textureCoordinates = new ArrayList<>();
        List<int[]> faceIndices = new ArrayList<>();

        try (InputStream inputStream = ObjLoader.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    vertices.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])
                    });
                } else if (line.startsWith("vt ")) {
                    String[] parts = line.split("\\s+");
                    textureCoordinates.add(new float[]{
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2])
                    });
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    int[][] faceVertices = new int[parts.length - 1][2];
                    for (int partIndex = 1; partIndex < parts.length; partIndex++) {
                        String[] indexComponents = parts[partIndex].split("/");
                        faceVertices[partIndex - 1][VERTEX_INDEX] = Integer.parseInt(indexComponents[0]) - 1;
                        faceVertices[partIndex - 1][TEXTURE_INDEX] =
                            indexComponents.length > 1 && !indexComponents[1].isEmpty()
                                ? Integer.parseInt(indexComponents[1]) - 1
                                : 0;
                    }
                    for (int triangleIndex = 1; triangleIndex < faceVertices.length - 1; triangleIndex++) {
                        faceIndices.add(new int[]{
                            faceVertices[0][VERTEX_INDEX], faceVertices[0][TEXTURE_INDEX],
                            faceVertices[triangleIndex][VERTEX_INDEX], faceVertices[triangleIndex][TEXTURE_INDEX],
                            faceVertices[triangleIndex + 1][VERTEX_INDEX], faceVertices[triangleIndex + 1][TEXTURE_INDEX]
                        });
                    }
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load OBJ: " + resourcePath, exception);
        }

        for (float[] vertex : vertices) {
            mesh.getPoints().addAll(vertex[0], vertex[1], vertex[2]);
        }
        if (textureCoordinates.isEmpty()) {
            textureCoordinates.add(new float[]{0, 0});
        }
        for (float[] texCoord : textureCoordinates) {
            mesh.getTexCoords().addAll(texCoord[0], texCoord[1]);
        }
        for (int[] face : faceIndices) {
            mesh.getFaces().addAll(face);
        }

        MeshView meshView = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial(Color.RED);
        material.setSpecularColor(Color.WHITE);
        meshView.setMaterial(material);
        return meshView;
    }
}
