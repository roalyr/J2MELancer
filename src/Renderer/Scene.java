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
}