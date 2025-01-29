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

    // TODO: investigate is ir is  worth it initiating everything here.
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

        renderer.setRenderables(objects);
        renderer.renderScene(g, viewMatrix);
    }
}
