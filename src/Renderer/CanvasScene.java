package Renderer;

import javax.microedition.lcdui.*;

import FixedMath.*;
import Models.Cube;

class CanvasScene extends Canvas implements Runnable {

    private Scene scene;
    private ModelQ24_8 model;
    private Camera camera;
    private Perspective perspective;
    int frame = 0;

    public CanvasScene() {

        this.setFullScreenMode(true);
        
        // Init camera and perspective.
        camera = new Camera();
        camera.setPosition(
                0 * Constants.Common.ONE_POS,
                0 * Constants.Common.ONE_POS,
                0 * Constants.Common.ONE_POS);
        
        camera.setRotation(
                0 * Constants.Common.ONE_DEGREE_IN_RADIANS,
                0 * Constants.Common.ONE_DEGREE_IN_RADIANS,
                0 * Constants.Common.ONE_DEGREE_IN_RADIANS);
        
        // Default Perspective Matrix (some defaults for testing here).
        int fovQ24_8 = FixedBaseMath.toQ24_8(60.0f);
        int aspectQ24_8 = FixedBaseMath.toQ24_8((float) SharedData.display_width / SharedData.display_height);
        int nearQ24_8 = FixedBaseMath.toQ24_8(1.0f);
        int farQ24_8 = FixedBaseMath.toQ24_8(100.0f);
        
        perspective = new Perspective(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);

        // Init scene and models.
        int scene_objects_num = 1000;
        model = new ModelQ24_8(Cube.VERTICES, Cube.EDGES);
        scene = new Scene(scene_objects_num);

        // Populate the scene with objects (init).
        for (int i = 0; i < scene_objects_num; i++) {

            // Test layout.
            SceneObject so = new SceneObject(model);
            so.tx = FixedBaseMath.toQ24_8((float) 0 * i); //4*FixedTrigMath.sin(30*i);

            so.ty = FixedBaseMath.toQ24_8((float) 0 * i); // 4*FixedTrigMath.cos(30*i);

            so.tz = FixedBaseMath.toQ24_8((float) -5 * i);
            scene.addObject(so, i);
        }
    }

    protected void paint(Graphics g) {

        long startTime = System.currentTimeMillis();


        // Scene logic here (updates).
        // models
        // transforms

        // TODO: make methods. Investigate calculating this once or update only
        // when FoV or screen size changes.
        // scene.cameraMatrix = ...
        // scene.prespectiveMatrix = ...


        // Adjust camera and perspective here if needed.
        int tr = 1 * frame * Constants.Common.ONE_POS / 1000;
        
        camera.setPosition(tr , tr , tr * 10 );
        
        int ro = 1 *frame * Constants.Common.ONE_DEGREE_IN_RADIANS / 1000;
          
        camera.setRotation(ro, ro, ro);

        // Pass these changing values to the renderer
        scene.renderAll(g, camera, perspective);


        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        System.out.println("Elapsed time: " + elapsedTime + " ms; FPS: " + 1000/elapsedTime);

        System.out.print("\n==== DONE RENDERING ====\n");

    }

    public void run() {
        while (true) {
            // Call for re-rendering.
            repaint();

            frame++;

            try {
                Thread.sleep(Constants.Common.DELTA_RENDER);
            } catch (InterruptedException e) {
            }

        }
    }
}
