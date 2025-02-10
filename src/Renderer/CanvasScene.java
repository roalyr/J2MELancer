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
    
       // Multiplier variables
    public int moveMultiplier = 1;              // starts at 1
    private final int MULTIPLIER_STEP = 1;         // how much to increase per key press
    private final int MULTIPLIER_MAX = 100;         // maximum multiplier value

    public CanvasScene() {
        setFullScreenMode(true);
        SharedData.display_width = getWidth();
        SharedData.display_height = getHeight();
        SharedData.halfW_Q24_8 = FixedMath.FixedBaseMath.toFixed(SharedData.display_width / 2);
        SharedData.halfH_Q24_8 = FixedMath.FixedBaseMath.toFixed(SharedData.display_height / 2);

        int fovQ = FixedMath.FixedBaseMath.toFixed(60.0f);
        int aspectQ = FixedMath.FixedBaseMath.toFixed(
                (float) SharedData.display_width / SharedData.display_height);
        int nearQ = FixedMath.FixedBaseMath.toFixed(Constants.Common.Z_NEAR);
        int farQ = FixedMath.FixedBaseMath.toFixed(Constants.Common.Z_FAR);

        scene = new Scene(sceneObjectsNum, fovQ, aspectQ, nearQ, farQ);
        addObjects();
    }

    // TODO: make sequence loaded from Scenes package on demand.
    private void addObjects() {
        // ADD OBJECTS 
        // Example: Planet
        model = Sphere.create(12, 12);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(20.0f);
        sceneObject.ty = FixedBaseMath.toFixed(10.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-500.0f);
        sceneObject.scale = FixedBaseMath.toFixed(200.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFF1D635B, // Color near
                0xFF093612, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(50000f), // Material z-far
                FixedBaseMath.toFixed(100f), // Alpha ramp distance near
                FixedBaseMath.toFixed(5000f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                8);  // Dither level

        scene.addObject(sceneObject);
        
        
         // ADD OBJECTS 
        // Example: Planet
        model = Sphere.create(12, 12);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(15000.0f);
        sceneObject.ty = FixedBaseMath.toFixed(-30000.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-50000.0f);
        sceneObject.scale = FixedBaseMath.toFixed(10000.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFAABB33, // Color near
                0xFF334411, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(500000f), // Material z-far
                FixedBaseMath.toFixed(1000f), // Alpha ramp distance near
                FixedBaseMath.toFixed(100000f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                8);  // Dither level

        scene.addObject(sceneObject);


        // Make another model.
        // Example: Cube
        model = Cube.create(2);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(-20.0f);
        sceneObject.ty = FixedBaseMath.toFixed(5.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-50.0f);
        sceneObject.scale = FixedBaseMath.toFixed(5.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFAAAAFF, // Color near
                0xFFFF2222, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(5000f), // Material z-far
                FixedBaseMath.toFixed(5f), // Alpha ramp distance near
                FixedBaseMath.toFixed(500f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                2);  // Dither level

        scene.addObject(sceneObject);
        
        
        // Make another model.
        // Example: Cube
        model = Cube.create(5);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(-200.0f);
        sceneObject.ty = FixedBaseMath.toFixed(500.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-5000.0f);
        sceneObject.scale = FixedBaseMath.toFixed(50.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFAAAAFF, // Color near
                0xFFFF2222, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(10000f), // Material z-far
                FixedBaseMath.toFixed(10f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1000f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                2);  // Dither level

        scene.addObject(sceneObject);

        // Make another model.
        // Example: Starsphere blue
        model = RandomCloud.create(16, Constants.Common.SEED + 1);

        sceneObject = new SceneObject(model);
        sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFF1155FF, // Color near
                0xFF1155FF, // Color far
                FixedBaseMath.toFixed(0f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_X, // Primitive shape
                2);  // Dither level

        scene.addObject(sceneObject);
        
        
        // Make another model.
        // Example: Starsphere yellow
        model = RandomCloud.create(32, Constants.Common.SEED + 10);

        sceneObject = new SceneObject(model);
        sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFFAAAA00, // Color near
                0xFFAAAA00, // Color far
                FixedBaseMath.toFixed(0f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                2);  // Dither level

        scene.addObject(sceneObject);
        
        // Make another model.
        // Example: Starsphere red
        model = RandomCloud.create(64, Constants.Common.SEED + 100);

        sceneObject = new SceneObject(model);
        sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFFAA0000, // Color near
                0xFFAA0000, // Color far
                FixedBaseMath.toFixed(0f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                2);  // Dither level

        scene.addObject(sceneObject);
    }

    protected void paint(Graphics g) {
        scene.renderAll(g);
    }

    public void run() {
        while (true) {
            frame++;
            try {
                Thread.sleep(Constants.Common.DELTA_RENDER);
            } catch (InterruptedException e) {
            }
        }
    }

    protected void keyPressed(int keyCode) {
        repaint();
        int gameAction;
        try {
            gameAction = getGameAction(keyCode);
        } catch (IllegalArgumentException e) {
            gameAction = 0;
        }
        
        // Increase the multiplier (up to the max)
        moveMultiplier = Math.min(MULTIPLIER_MAX, moveMultiplier + MULTIPLIER_STEP);
        
        // Pass the multiplier to your scene logic so that movement/rotation are scaled.
        // (You may need to update the Scene class to accept the multiplier as a parameter.)
        scene.handleKeyPressed(keyCode, gameAction, moveMultiplier);
    }

    protected void keyRepeated(int keyCode) {
        // For repeated events, we call keyPressed so that the multiplier continues increasing.
        keyPressed(keyCode);
    }
    
    protected void keyReleased(int keyCode) {
        // Reset the multiplier when the key is released.
        moveMultiplier = 1;
        
        // Optionally, if you need to notify the scene that the key was released:
        //scene.handleKeyReleased(keyCode);
    }
}
