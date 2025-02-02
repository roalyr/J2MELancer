package Renderer;

import javax.microedition.lcdui.*;
import FixedMath.*;
import Constants.*;

class CanvasScene extends Canvas implements Runnable {

    private Scene scene;
    private ModelQ24_8 model;
    private Camera camera;
    private Perspective perspective;
    private int frame = 0;
    private final int sceneObjectsNum = 1000;
    
    // Pre-calculate 30° in Q24.8 format once.
    private final int ANGLE_30_Q = FixedTrigMath.degreesToRadiansQ24_8(30);

    public CanvasScene() {
        setFullScreenMode(true);
        SharedData.display_width = getWidth();
        SharedData.display_height = getHeight();
        SharedData.halfW_Q24_8 = FixedBaseMath.toQ24_8(SharedData.display_width / 2);
        SharedData.halfH_Q24_8 = FixedBaseMath.toQ24_8(SharedData.display_height / 2);
        
        // Initialize camera.
        camera = new Camera();
        camera.setPosition(0, 0, 0);
        camera.setRotation(0, 0, 0);
        
        // Initialize perspective using constants from Common.
        int fovQ = FixedBaseMath.toQ24_8(80.0f);
        int aspectQ = FixedBaseMath.toQ24_8((float) SharedData.display_width / SharedData.display_height);
        int nearQ = FixedBaseMath.toQ24_8(Common.Z_NEAR);
        int farQ = FixedBaseMath.toQ24_8(Common.Z_FAR);
        perspective = new Perspective(fovQ, aspectQ, nearQ, farQ);
        
        // Initialize model and scene.
        model = new ModelQ24_8(Models.Sphere.VERTICES, Models.Sphere.EDGES, Models.Sphere.BOUNDING_SPHERE_RADIUS);
        scene = new Scene(sceneObjectsNum);
        
        // Populate the scene with objects.
        for (int i = 0; i < sceneObjectsNum; i++) {
            SceneObject so = new SceneObject(model);
            int angle = ANGLE_30_Q * i; // Angle in Q24.8
            so.tx = 4 * FixedTrigMath.sin(angle);
            so.ty = 4 * FixedTrigMath.cos(angle);
            so.tz = FixedBaseMath.toQ24_8(-5 * i + 1000);
            scene.addObject(so, i);
        }
    }

    protected void paint(Graphics g) {
        // Use constants from Common.
        int onePos = Common.ONE_POS;
        int oneDegRad = Common.ONE_DEGREE_IN_RADIANS;
        int currentFrame = frame;
        
        // Update camera position and rotation.
        int tr = currentFrame * onePos / 100;
        camera.setPosition(tr, 0, tr);
        int ro = currentFrame * oneDegRad / 5;
        camera.setRotation(0, -ro, 0);
        
        // Render the scene.
        scene.renderAll(g, camera, perspective);
    }

    public void run() {
        while (true) {
            repaint();
            frame++;
            try {
                Thread.sleep(Common.DELTA_RENDER);
            } catch (InterruptedException e) {
                // Handle interruption if needed.
            }
        }
    }
}