package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;

public class Scene {
    public Vector objects; // or Vector/ArrayList in J2ME

    private int[] cameraMatrix;
    private int[] perspectiveMatrix;
    private int[] viewMatrix;

    private Renderer renderer;

    // Pre-calculated elements of the view matrix
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;
    private boolean isViewMatrixDirty = true; // Flag to indicate if the view matrix needs to be recalculated

    // TODO: investigate is ir is worth it initiating everything here.
    public Scene(int capacity) {
        objects = new Vector(capacity);
        renderer = new Renderer();
    }

    public void addObject(SceneObject obj, int index) {
        objects.addElement(obj);
    }

    public void renderAll(Graphics g, Camera c, Perspective p) {
        // build or combine “camera * perspective” into a single “viewMatrix”
        perspectiveMatrix = p.getPerspectiveMatrix();
        cameraMatrix = c.getCameraMatrix();
        viewMatrix = FixedMatMath.multiply4x4(perspectiveMatrix, cameraMatrix);

        // Pre-calculate elements of the view matrix if they haven't changed
        if (isViewMatrixDirty) { // You'll need a mechanism to track if the view matrix has changed
            precalc_halfW_Q24_8 = SharedData.halfW_Q24_8; // Assuming these are dependent on the view matrix
            precalc_halfH_Q24_8 = SharedData.halfH_Q24_8;
            isViewMatrixDirty = false;
        }

        // Pass pre-calculated values to the renderer
        renderer.setRenderables(objects, precalc_halfW_Q24_8, precalc_halfH_Q24_8);
        renderer.renderScene(g, viewMatrix);
    }

    // Setters for camera and perspective to update the dirty flag
    public void setCamera(Camera c) {
        cameraMatrix = c.getCameraMatrix();
        isViewMatrixDirty = true;
    }

    public void setPerspective(Perspective p) {
        perspectiveMatrix = p.getPerspectiveMatrix();
        isViewMatrixDirty = true;
    }

    // Optimized isObjectVisible method for bounding sphere culling
    private boolean isObjectVisible(SceneObject obj, int[] viewMatrix) {
        // 1. Get the object's center in world space (you might need to adjust this based on your setup)
        int centerX = obj.tx;
        int centerY = obj.ty;
        int centerZ = obj.tz;

        // 2. Transform the center to view space
        int[] centerView = transformPointQ24_8(viewMatrix, new int[]{centerX, centerY, centerZ, FixedBaseMath.toQ24_8(1f)});

        // 3. Get the object's bounding sphere radius (you'll need to precalculate this for your model)
        int radius = obj.model.boundingSphereRadius;

        // 4. Perform a simplified view frustum check (you can refine this later)
        // Check against the near and far planes
        if (centerView[2] + radius < -FixedBaseMath.toQ24_8(Constants.Common.Z_FAR) ||
                centerView[2] - radius > -FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR)) {
            return false; // Object is outside the near/far clipping planes
        }

        // Check against the left and right planes (simplified)
        
        if (FixedBaseMath.toInt(centerView[0] + radius) < -precalc_halfW_Q24_8 || FixedBaseMath.toInt(centerView[0] - radius) > precalc_halfW_Q24_8) {
            return false; // Object is outside the left/right clipping planes
        }

        // Check against the top and bottom planes (simplified)
        
        if (FixedBaseMath.toInt(centerView[1] + radius) < -precalc_halfH_Q24_8 || FixedBaseMath.toInt(centerView[1] - radius) > precalc_halfH_Q24_8) {
            return false; // Object is outside the top/bottom clipping planes
        }

        return true; // Object is potentially visible
    }

    private int[] transformPointQ24_8(int[] m4x4, int[] xyz) {
        int[] out4 = new int[4];
        for (int row = 0; row < 4; row++) {
            long sum = 0;
            for (int col = 0; col < 3; col++) {
                sum += (long) m4x4[row * 4 + col] * (long) xyz[col];
            }
            // w=1
            sum += (long) m4x4[row * 4 + 3] * (long) xyz[3];
            out4[row] = (int) (sum >> FixedBaseMath.Q24_8_SHIFT);
        }
        return out4;
    }
}