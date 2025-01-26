package Renderer;

import javax.microedition.lcdui.Graphics;
import Interfaces.IRenderable;
import FixedMath.*;

/**
 * SceneObject: a transform + geometry in Q24.8
 */
public class SceneObject implements IRenderable {

    public ModelQ24_8 model;   // the geometry
    // local transform components in Q24.8
    public int tx, ty, tz;     // translation
    public int rotX, rotY, rotZ; // rotation angles Q24.8
    public int scale;          // uniform scale in Q24.8 (or separate sx, sy, sz)

    public SceneObject(ModelQ24_8 model) {
        this.model = model;
        // default transform
        this.scale = FixedBaseMath.toQ24_8(1.0f);
    }

    public void render(Graphics g, int[] viewMatrix) {
        // 1) build the local transform => TRS => 4x4
        int[] local = FixedMatMath.createIdentity4x4();

        // T
        local = FixedMatMath.multiply4x4(local,
            FixedMatMath.createTranslation4x4(tx, ty, tz));
        
        // Rz, Ry, Rx (whatever order you prefer)
        local = FixedMatMath.multiply4x4(local,
            FixedMatMath.createRotationZ4x4(rotZ));
        local = FixedMatMath.multiply4x4(local,
            FixedMatMath.createRotationY4x4(rotY));
        local = FixedMatMath.multiply4x4(local,
            FixedMatMath.createRotationX4x4(rotX));

        // S
        local = FixedMatMath.multiply4x4(local,
            FixedMatMath.createScale4x4(scale, scale, scale));
        
        // 2) combine with the “viewMatrix” (which might already have camera & perspective)
        int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

        // 3) actually draw edges or polygons
        // For edges, do something like your “drawEdges” approach:
        drawEdges(g, finalMatrix, model);
    }

    private void drawEdges(Graphics g, int[] finalM, ModelQ24_8 model) {
        // (Similar to your single-cube code)
        int[][] edges = model.edges;
        int[][] verts= model.vertices;
        
        // loop edges
        for (int i=0; i<edges.length; i++) {
            int i0= edges[i][0];
            int i1= edges[i][1];
            // transform each vertex => out4
            int[] p0= transformPointQ24_8(finalM, verts[i0]);
            int[] p1= transformPointQ24_8(finalM, verts[i1]);

            // do w=0 check etc., do final screen map
            // drawLine( ... ) 
        }
    }

    private int[] transformPointQ24_8(int[] m, int[] xyz) {
        // same code you used before
        int[] out4= new int[4];
        // ...
        return out4;
    }
}
