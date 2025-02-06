package Renderer;

import javax.microedition.lcdui.*;
import FixedMath.*;
import Constants.*;
import Models.*;

class CanvasScene extends Canvas implements Runnable {

    private Scene scene;
    private Camera camera;
    private Perspective perspective;
    private Model model;
    private SceneObject sceneObject;
    private int frame = 0;
    private final int sceneObjectsNum = 1000;

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

        // Initialize scene.
        scene = new Scene(sceneObjectsNum, fovQ, aspectQ, nearQ, farQ);

        
        // ADD OBJECTS 
        // Example: Manually create a sphere object
        model = Sphere.create(12, 12);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toQ24_8(2.0f);
        sceneObject.ty = FixedBaseMath.toQ24_8(1.0f);
        sceneObject.tz = FixedBaseMath.toQ24_8(-500.0f);
        sceneObject.scale = FixedBaseMath.toQ24_8(200.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFF0000FF, // Color near
                0xFF00FF00, // Color far
                3.0f, // Color exponent
                FixedBaseMath.toQ24_8(1f), // Material z-near
                FixedBaseMath.toQ24_8(400f), // Material z-far
                FixedBaseMath.toQ24_8(60f), // Alpha ramp distance
                1, // 0 - vertices, 1 - edges
                1);

        // Finally, add to scene
        scene.addObject(sceneObject);


        // Make another model.
        // Example: Manually create a cube object
        model = Cube.create(2);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toQ24_8(-20.0f);
        sceneObject.ty = FixedBaseMath.toQ24_8(5.0f);
        sceneObject.tz = FixedBaseMath.toQ24_8(-20.0f);
        sceneObject.scale = FixedBaseMath.toQ24_8(5.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this cube (covered below)
        sceneObject.material = new Material(
                0xFFFF0000, // Color near
                0xFF00FF00, // Color far
                3.0f, // Color exponent
                FixedBaseMath.toQ24_8(1f), // Material z-near
                FixedBaseMath.toQ24_8(40f), // Material z-far
                FixedBaseMath.toQ24_8(10f), // Alpha ramp distance
                1, // 0 - vertices, 1 - edges
                1);

        // Finally, add to scene
        scene.addObject(sceneObject);
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