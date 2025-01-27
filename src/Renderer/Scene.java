package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;

public class Scene {
    public SceneObject[] objects; // or Vector/ArrayList in J2ME

    public int[] cameraMatrix;     // or separate camera logic
    public int[] perspectiveMatrix; // or combined view-proj

    // TODO: investigate is ir is  worth it initiating everything here.
    public Scene(int capacity) {
        objects = new SceneObject[capacity];
    }

    public void addObject(SceneObject obj, int index) {
        objects[index]= obj;
    }
    
    public void renderAll(Graphics g, int[] cam, int[] persp) {
        // build or combine “camera * perspective” into a single “viewMatrix”
        perspectiveMatrix = persp;
        cameraMatrix = cam;
        int[] viewMatrix= FixedMatMath.multiply4x4(perspectiveMatrix, cameraMatrix);

        // then for each object:
        for (int i=0; i<objects.length; i++) {
            if (objects[i]!=null) {
                objects[i].render(g, viewMatrix);
            }
        }
    }
}
