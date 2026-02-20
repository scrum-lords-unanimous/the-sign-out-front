package GUI.Run;

import Data.Driveway.Driveway;
import Data.Driveway.DrivewaySegment;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.*;

public class DrivewayMesh {

    private static final double WP_STEP = 2.0;

    public static List<double[]> buildWaypoints(Driveway dw, double radius, Random rand) {
        List<double[]> pts = new ArrayList<>();
        double x = 0, z = 0, h = 0, cum = 0; boolean left = true;
        for (int idx = 0; idx < dw.segments.size(); idx++) {
            DrivewaySegment seg = dw.segments.get(idx);
            double segLen = seg.end - seg.start;
            if (idx % 2 == 0) {
                int steps = Math.max(1, (int) (segLen / WP_STEP)); double sl = segLen / steps;
                for (int s = 0; s < steps; s++) {
                    pts.add(new double[]{x, 0, z, cum});
                    x += Math.sin(h) * sl; z += Math.cos(h) * sl; cum += sl;
                }
            } else {
                double r = radius * (0.7 + rand.nextDouble() * 0.6);
                double angle = segLen / r, sign = left ? -1 : 1; left = !left;
                int steps = Math.max(4, (int) (segLen / WP_STEP));
                double as = angle / steps, ds = segLen / steps;
                for (int s = 0; s < steps; s++) {
                    pts.add(new double[]{x, 0, z, cum});
                    h += sign * as; x += Math.sin(h) * ds; z += Math.cos(h) * ds; cum += ds;
                }
            }
        }
        pts.add(new double[]{x, 0, z, cum});
        return pts;
    }

    public static MeshView buildRoadMesh(List<double[]> wps, double roadW) {
        TriangleMesh mesh = new TriangleMesh(); float hw = (float) (roadW / 2);
        for (int idx = 0; idx < wps.size(); idx++) {
            double[] wp = wps.get(idx); double dx, dz;
            if (idx < wps.size() - 1) { double[] n = wps.get(idx + 1); dx = n[0] - wp[0]; dz = n[2] - wp[2]; }
            else { double[] p = wps.get(idx - 1); dx = wp[0] - p[0]; dz = wp[2] - p[2]; }
            double l = Math.sqrt(dx * dx + dz * dz);
            if (l < 0.001) { dx = 0; dz = 1; l = 1; }
            double nx = -dz / l, nz = dx / l;
            mesh.getPoints().addAll((float)(wp[0]+nx*hw), 0f, (float)(wp[2]+nz*hw),
                    (float)(wp[0]-nx*hw), 0f, (float)(wp[2]-nz*hw));
        }
        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1, 1, 1);
        for (int idx = 0; idx < wps.size() - 1; idx++) {
            int a = idx*2, b = idx*2+1, c = (idx+1)*2, d = (idx+1)*2+1;
            mesh.getFaces().addAll(a,0,b,1,c,2); mesh.getFaces().addAll(b,1,d,3,c,2);
        }
        addCap(mesh, wps.get(0), wps.get(1), hw);
        addCap(mesh, wps.get(wps.size()-1), wps.get(wps.size()-2), hw);
        return new MeshView(mesh);
    }

    static void addCap(TriangleMesh mesh, double[] tip, double[] next, float hw) {
        double dx = tip[0]-next[0], dz = tip[2]-next[2];
        double l = Math.sqrt(dx*dx+dz*dz); if (l < 0.001) { dx=0; dz=1; l=1; }
        double fwd = Math.atan2(dx/l, dz/l); int ctr = mesh.getPoints().size()/3;
        mesh.getPoints().addAll((float)tip[0], 0f, (float)tip[2]);
        for (int s = 0; s <= 12; s++) {
            double a = fwd - Math.PI/2 + Math.PI*s/12;
            mesh.getPoints().addAll((float)(tip[0]+Math.sin(a)*hw), 0f, (float)(tip[2]+Math.cos(a)*hw));
        }
        for (int s = 0; s < 12; s++) mesh.getFaces().addAll(ctr,0,ctr+1+s,0,ctr+2+s,0);
    }

    public static double[] interp(List<double[]> wps, double dist) {
        for (int idx = 0; idx < wps.size()-1; idx++) {
            double d0 = wps.get(idx)[3], d1 = wps.get(idx+1)[3];
            if (dist >= d0 && dist <= d1) {
                double t = d1==d0 ? 0 : (dist-d0)/(d1-d0);
                return new double[]{wps.get(idx)[0]+(wps.get(idx+1)[0]-wps.get(idx)[0])*t,
                        wps.get(idx)[1]+(wps.get(idx+1)[1]-wps.get(idx)[1])*t,
                        wps.get(idx)[2]+(wps.get(idx+1)[2]-wps.get(idx)[2])*t};
            }
        }
        double[] e = wps.get(wps.size()-1); return new double[]{e[0], e[1], e[2]};
    }

    public static double[] dir(List<double[]> wps, double dist) {
        for (int idx = 0; idx < wps.size()-1; idx++) {
            if (dist >= wps.get(idx)[3] && dist <= wps.get(idx+1)[3]) {
                double dx = wps.get(idx+1)[0]-wps.get(idx)[0], dz = wps.get(idx+1)[2]-wps.get(idx)[2];
                double l = Math.sqrt(dx*dx+dz*dz);
                return l < 0.001 ? new double[]{0,0,1} : new double[]{dx/l, 0, dz/l};
            }
        }
        return new double[]{0, 0, 1};
    }
}
