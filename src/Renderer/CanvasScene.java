package Renderer;

import javax.microedition.lcdui.*;

import FixedMath.*;
import Models.Cube;

class CanvasScene extends Canvas implements Runnable {

    private Scene scene;
    private ModelQ24_8 model;
    private int[] cam;
    private int[] persp;

    public CanvasScene() {
        
        this.setFullScreenMode(true);
        
        // Init scene and models.
        int scene_objects_num = 10000;
        model = new ModelQ24_8(Cube.VERTICES, Cube.EDGES);
        scene = new Scene(scene_objects_num);

        // Populate the scene with objects (init).
        for (int i = 0; i < scene_objects_num; i++) {

            // Test layout.
            SceneObject so = new SceneObject(model);
            so.tx = 4*FixedTrigMath.sin(30*i); // Q24.8
            so.ty = 4*FixedTrigMath.cos(30*i);
            so.tz = FixedBaseMath.toQ24_8((float) -4 * i);
            scene.addObject(so, i);
        }
    }

    protected void paint(Graphics g) {
        // Default Perspective Matrix (some defaults for testing here).
        int fovQ24_8 = FixedBaseMath.toQ24_8(60.0f);
        int aspectQ24_8 = FixedBaseMath.toQ24_8((float) SharedData.display_width / SharedData.display_height);
        int nearQ24_8 = FixedBaseMath.toQ24_8(1.0f);
        int farQ24_8 = FixedBaseMath.toQ24_8(100.0f);
        persp = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);

        // Default Camera Matrix (at (0, 0, 5))
        cam = FixedMatMath.createTranslation4x4(0, 0, FixedBaseMath.toQ24_8(-5.0f));

        // Pass these changing values to the renderer
        scene.renderAll(g, cam, persp);
        
    }

    public void run() {
        //int frame = 0;
        while (true) {
            // Scene logic here (updates).
            // models
            // transforms

            // TODO: make methods. Investigate calculating this once or update only
            // when FoV or screen size changes.
            // scene.cameraMatrix = ...
            // scene.prespectiveMatrix = ...

            // Call for re-rendering.
            
            // Skip for the sake of single-frame stress-test
            // repaint();
            
            
            
            try {
                Thread.sleep(Constants.Common.DELTA_RENDER);
            } catch (InterruptedException e) {
            }

        //System.out.print("Frame: " + frame);
        //frame ++;
        }
    }
}
