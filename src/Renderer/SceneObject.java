package Renderer;

import javax.microedition.lcdui.Graphics;
import Interfaces.IRenderable;
import FixedMath.*;

/**
 * SceneObject: a transform + geometry in Q24.8
 */
public class SceneObject implements IRenderable {

    public ModelQ24_8 model;   // the geometry

    public int tx,  ty,  tz;     // translation

    public int rotX,  rotY,  rotZ; // rotation angles Q24.8

    public int scale;          // uniform scale in Q24.8 (or separate sx, sy, sz)


    public SceneObject(ModelQ24_8 model) {
        this.model = model;
        // TODO: default transform
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

        // 2) combine with the “viewMatrix” (which has camera & perspective)
        int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

        // 3) actually draw edges or polygons
        // TODO: implement buffering.
        // Clear the screen
        // g.setColor(0x000000);
        //g.fillRect(0, 0, SharedData.display_width, SharedData.display_height);
        drawEdges(g, finalMatrix, model);
    }

    private void drawEdges(Graphics g, int[] finalM, ModelQ24_8 model) {
        int[][] edges = model.edges;
        int[][] verts = model.vertices;

        // 4) Draw edges, edit colors, etc.
        g.setColor(0xFF00FF);

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            // transform each vertex => (x', y', z', w')
            int[] p0 = transformPointQ24_8(finalM, verts[i0]);
            int[] p1 = transformPointQ24_8(finalM, verts[i1]);

            // skip if w=0
            if (p0[3] == 0 || p1[3] == 0) {
                continue;
            }
            float w0f = (float) p0[3];
            float w1f = (float) p1[3];

            float x0f = p0[0] / w0f;
            float y0f = p0[1] / w0f;
            float z0f = -p0[2]; // -Z is forward.

            float x1f = p1[0] / w1f;
            float y1f = p1[1] / w1f;
            float z1f = -p1[2]; // -Z is forward.

            // map x,y to screen
            int sx0 = (int) (SharedData.halfW + x0f * SharedData.halfW);
            int sy0 = (int) (SharedData.halfH - y0f * SharedData.halfH);
            int sx1 = (int) (SharedData.halfW + x1f * SharedData.halfW);
            int sy1 = (int) (SharedData.halfH - y1f * SharedData.halfH);


            // TODO: move this out into SharedData and update from within Scene.
            float nearPlaneZ = -FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR);
            float farPlaneZ = -FixedBaseMath.toQ24_8(Constants.Common.Z_FAR);

            // near-plane cull
            if ((z0f > nearPlaneZ) || (z1f > nearPlaneZ)) {
                continue;
            }

            // far-plane cull
            if ((z0f < farPlaneZ) || (z1f < farPlaneZ)) {
                continue;
            }

            //System.out.println(
            //   " z` in float =" + FixedBaseMath.toFloat(p0[2]) +
            //   " NEAR: Q24.8 " + nearPlaneZ + " FAR: Q24.8 " + farPlaneZ);


            // X-planes cull (skip when both vertices are off)
            if ((sx0 < SharedData.leftPlaneX) || (sx1 > SharedData.rightPlaneX)) {
                continue;
            }

            //System.out.println(
            //   " sx0 =" + sx0 + "' " + " sx1 =" + sx1 +" left =" + leftPlaneX + "' " +" right =" + rightPlaneX 
            //   ); // sx is in range from 0 to screen width.


            // Y-planes cull (skip when both vertices are off)
            if ((sy0 < SharedData.upPlaneY) || (sy1 > SharedData.downPlaneY)) {
                continue;
            }

            //System.out.println(
            //    " sy0 =" + sy0 + "' " + " sy1 =" + sy1 +" up =" + upPlaneY + "' " +" down =" + downPlaneY 
            //    ); // sy is in range from 0 to screen height.

            g.drawLine(sx0, sy0, sx1, sy1);
        }
    }

    private int[] transformPointQ24_8(int[] m4x4, int[] xyz) {
        int[] out4 = new int[4];
        for (int row = 0; row < 4; row++) {
            long sum = 0;
            for (int col = 0; col < 3; col++) {
                sum += (long) m4x4[row * 4 + col] * (long) xyz[col];
            }
            // w=1
            sum += (long) m4x4[row * 4 + 3] * (long) Constants.Common.ONE_POS;
            out4[row] = (int) (sum >> 8);
        }
        return out4;
    }
}
