package Renderer;

import FixedMath.*;
import Models.*;
import javax.microedition.lcdui.*;

public class CanvasScene extends Canvas implements Runnable {


    private Scene scene;
    private Model model;
    private SceneObject sceneObject;
    private int frame = 0;
    private final int sceneObjectsNum = 1000;
   

    public CanvasScene() {
        setFullScreenMode(true);
        SharedData.display_width = getWidth();
        SharedData.display_height = getHeight();
        SharedData.halfW_Q24_8 = FixedMath.FixedBaseMath.toQ24_8(SharedData.display_width / 2);
        SharedData.halfH_Q24_8 = FixedMath.FixedBaseMath.toQ24_8(SharedData.display_height / 2);

        int fovQ = FixedMath.FixedBaseMath.toQ24_8(60.0f);
        int aspectQ = FixedMath.FixedBaseMath.toQ24_8(
            (float) SharedData.display_width / SharedData.display_height);
        int nearQ = FixedMath.FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR);
        int farQ  = FixedMath.FixedBaseMath.toQ24_8(Constants.Common.Z_FAR);

        scene = new Scene(sceneObjectsNum, fovQ, aspectQ, nearQ, farQ);
        addObjects();
    }
    
    // TODO: make sequence loaded from Scenes package on demand.
    private void addObjects() {
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
        
        // Make another model.
        // Example: Manually create a cube object
        model = Cube.create(2);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toQ24_8(200.0f);
        sceneObject.ty = FixedBaseMath.toQ24_8(-50.0f);
        sceneObject.tz = FixedBaseMath.toQ24_8(-200.0f);
        sceneObject.scale = FixedBaseMath.toQ24_8(100.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this cube (covered below)
        sceneObject.material = new Material(
                0xFFFF00FF, // Color near
                0xFFFFFF00, // Color far
                3.0f, // Color exponent
                FixedBaseMath.toQ24_8(10f), // Material z-near
                FixedBaseMath.toQ24_8(400f), // Material z-far
                FixedBaseMath.toQ24_8(10f), // Alpha ramp distance
                0, // 0 - vertices, 1 - edges
                1);

        // Finally, add to scene
        scene.addObject(sceneObject);
    }

    protected void paint(Graphics g) {
        scene.renderAll(g);
    }

    public void run() {
        while (true) {
            repaint();
            frame++;
            try {
                Thread.sleep(Constants.Common.DELTA_RENDER);
            } catch (InterruptedException e) {
            }
        }
    }

    protected void keyPressed(int keyCode) {
        int gameAction;
        try {
            gameAction = getGameAction(keyCode);
        } catch (IllegalArgumentException e) {
            gameAction = 0;
        }
        scene.handleKeyPressed(keyCode, gameAction);
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }
}