package Renderer;

import javax.microedition.lcdui.*;

import FixedMath.FixedBaseMath;

/**
 * RotatingCubeCanvas that also animates FOV and Z translation.
 * We increment angleY, angleX, fovDeg, and zTrans in a loop,
 * then repaint each frame. 
 */
class RotatingCubeCanvas extends Canvas implements Runnable {

    private final SimpleRendererWithCulling renderer;

    // Rotation angles (Q24.8)
    private int angleYQ24_8 = 0;
    private int angleXQ24_8 = 0;

    // Animate FOV in range [20..120], Q24.8 degrees
    private int fovQ24_8   = FixedBaseMath.toQ24_8(60.0f);
    private int fovDir     = 1; // +1 or -1

    // Animate Z translation in range [-1..-10], Q24.8
    private int zTransQ24_8= FixedBaseMath.toQ24_8(-3.0f);
    private int zDir       = -1; // we'll move from -1 to -10, or vice versa

    public RotatingCubeCanvas() {
        renderer = new SimpleRendererWithCulling();
    }

    protected void paint(Graphics g) {
        // Pass these changing values to the renderer
        renderer.render(g, angleYQ24_8, angleXQ24_8, fovQ24_8, zTransQ24_8);
    }

    public void run() {
        // Increment angles, FOV, and Z each frame
        int deg2Q24_8 = FixedBaseMath.toQ24_8(2.0f * (float)Math.PI / 180.0f); 
        int deg1Q24_8 = FixedBaseMath.toQ24_8(1.0f * (float)Math.PI / 180.0f);

        // For FOV changes, about 1 deg per step in Q24.8
        int fovIncQ24_8 = FixedBaseMath.toQ24_8(1.0f);

        // Z changes by about 0.1 each step (Q24.8)
        int zIncQ24_8   = FixedBaseMath.toQ24_8(0.2f);

        while (true) {
            // Rotate
            angleYQ24_8 += deg2Q24_8;
            angleXQ24_8 += deg1Q24_8;

            // Animate FOV between 20..120 degrees
            //fovQ24_8 += (fovIncQ24_8 * fovDir);
            int fov20 = FixedBaseMath.toQ24_8(50.0f);
            int fov120= FixedBaseMath.toQ24_8(50.0f);
            if (fovQ24_8 > fov120) { 
                fovQ24_8 = fov120; 
                fovDir   = -1;
            } else if (fovQ24_8 < fov20) {
                fovQ24_8 = fov20;
                fovDir   = 1;
            }

            // Animate Z between -1..-10
            zTransQ24_8 += (zIncQ24_8 * zDir);
            int zNeg1  = FixedBaseMath.toQ24_8(20.0f);
            int zNeg10 = FixedBaseMath.toQ24_8(-20.0f);
            if (zTransQ24_8 < zNeg10) {
                zTransQ24_8 = zNeg10;
                zDir        = 1;
            } else if (zTransQ24_8 > zNeg1) {
                zTransQ24_8 = zNeg1;
                zDir        = -1;
            }

            repaint();
            try {
                Thread.sleep(33);
            } catch (InterruptedException e) {}
        }
    }
}
