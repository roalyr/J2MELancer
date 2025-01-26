package Renderer;

import javax.microedition.lcdui.*;
import FixedMath.*;

public class SimpleRendererWithCulling {

    // Let's define some near/far in *float* space, which we convert to Q24.8 later.
    // Typically, near=1.0, far=100.0, but let's pick 1..50 for example
    private static final float NEAR_F = 1.0f;
    private static final float FAR_F = 8.3e6f;  // ~8.3e6 is near the upper limit. 

    // We'll do a quick near-plane check in final clip space: z'/w' > -NEAR_F => visible
    // (if negative z is in front). If we want far-plane culling, do z'/w' < -FAR_F => skip
    private static final int ONE = FixedBaseMath.toQ24_8(1.0f);

    // A unit cube from -1..+1 in Q24.8
    private static final int NEGONE = FixedBaseMath.toQ24_8(-1.0f);
    private static final int POSONE = FixedBaseMath.toQ24_8(1.0f);
    private static final int[][] VERTICES = {
        {NEGONE, NEGONE, NEGONE}, {NEGONE, NEGONE, POSONE},
        {NEGONE, POSONE, NEGONE}, {NEGONE, POSONE, POSONE},
        {POSONE, NEGONE, NEGONE}, {POSONE, NEGONE, POSONE},
        {POSONE, POSONE, NEGONE}, {POSONE, POSONE, POSONE}
    };
    private static final int[][] EDGES = {
        {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7},
        {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

    public SimpleRendererWithCulling() {
    }

    public void render(
            Graphics g,
            int angleYQ24_8,
            int angleXQ24_8,
            int fovDegQ24_8,
            int zTransQ24_8) {
        // Clear the screen
        g.setColor(0x000000);
        g.fillRect(0, 0, SharedData.display_width, SharedData.display_height);

        // 1) Build perspective matrix
        int nearQ24_8 = FixedBaseMath.toQ24_8(NEAR_F);
        int farQ24_8 = FixedBaseMath.toQ24_8(FAR_F);
        float aspectF = (float) SharedData.display_width / (float) SharedData.display_height;
        int aspectQ24_8 = FixedBaseMath.toQ24_8(aspectF);

        int[] perspM = FixedMatMath.createPerspective4x4(
                fovDegQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);

        // 2) Model-View transform
        int[] mvp = FixedMatMath.createIdentity4x4();

        // Translate in z => e.g. -3
        mvp = FixedMatMath.multiply4x4(
                mvp,
                FixedMatMath.createTranslation4x4(
                zTransQ24_8, // +X is right
                zTransQ24_8, // +Y is up
                FixedBaseMath.toQ24_8(-10f))); // -Z is forward

        // Rotate Y, X
        int[] rotY = FixedMatMath.createRotationY4x4(angleYQ24_8);
        int[] rotX = FixedMatMath.createRotationX4x4(angleXQ24_8);
        mvp = FixedMatMath.multiply4x4(mvp, rotY);
        mvp = FixedMatMath.multiply4x4(mvp, rotX);

        // ADD SCALE step here
        // e.g. if you want the cube to be 2x bigger:
        int scaleQ = FixedBaseMath.toQ24_8(1.5f);
        int[] scaleM = FixedMatMath.createScale4x4(scaleQ, scaleQ, scaleQ);
        mvp = FixedMatMath.multiply4x4(mvp, scaleM);

        // 3) Multiply by perspective
        mvp = FixedMatMath.multiply4x4(perspM, mvp);

        // 4) Draw edges, do near/far culling
        g.setColor(0xFFFFFF);

        float nearPlaneZ = -FixedBaseMath.toQ24_8(NEAR_F);
        float farPlaneZ = -FixedBaseMath.toQ24_8(FAR_F);

        // map x,y to screen
        float halfW = SharedData.display_width * 0.5f;
        float halfH = SharedData.display_height * 0.5f;

        int leftPlaneX = 0 - SharedData.display_border_offset;
        int rightPlaneX = SharedData.display_width + SharedData.display_border_offset;

        int upPlaneY = 0 - SharedData.display_border_offset;
        int downPlaneY = SharedData.display_height + SharedData.display_border_offset;
        
        for (int i = 0; i < EDGES.length; i++) {
            int i0 = EDGES[i][0];
            int i1 = EDGES[i][1];

            // transform each vertex => (x', y', z', w')
            int[] p0 = transformPointQ24_8(mvp, VERTICES[i0]);
            int[] p1 = transformPointQ24_8(mvp, VERTICES[i1]);

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
            int sx0 = (int) (halfW + x0f * halfW);
            int sy0 = (int) (halfH - y0f * halfH);
            int sx1 = (int) (halfW + x1f * halfW);
            int sy1 = (int) (halfH - y1f * halfH);

            // DOONE.
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
            if ((sx0 < leftPlaneX) || (sx1 > rightPlaneX)) {
                continue;
            }
            
            //System.out.println(
            //   " sx0 =" + sx0 + "' " + " sx1 =" + sx1 +" left =" + leftPlaneX + "' " +" right =" + rightPlaneX 
            //   ); // sx is in range from 0 to screen width.


            // Y-planes cull (skip when both vertices are off)
            if ((sy0 < upPlaneY) || (sy1 > downPlaneY)) {
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
            sum += (long) m4x4[row * 4 + 3] * (long) ONE;
            out4[row] = (int) (sum >> 8);
        }
        return out4;
    }
}
