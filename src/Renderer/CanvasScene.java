package Renderer;

import FixedMath.FixedBaseMath;
import FixedMath.FixedTrigMath;

import java.io.IOException;
import javax.microedition.lcdui.*;
import Models.*;

public class CanvasScene extends Canvas implements Runnable {

    private Scene scene;
    private Model model;
    private SceneObject sceneObject;
    private int frame = 0;
    private final int sceneObjectsNum = 1000;
    
    public int moveMultiplier = 1;
    private final int MULTIPLIER_STEP = 1;
    private final int MULTIPLIER_MAX = 100;
    
    private static int DITHER_LEVEL_GLOBAL = 0;

    public CanvasScene() {
        setFullScreenMode(true);
        SharedData.display_width = getWidth();
        SharedData.display_height = getHeight();
        SharedData.halfW_Q24_8 = FixedBaseMath.toFixed(SharedData.display_width / 2);
        SharedData.halfH_Q24_8 = FixedBaseMath.toFixed(SharedData.display_height / 2);

        long fovQ = FixedBaseMath.toFixed(60.0f);
        long aspectQ = FixedBaseMath.toFixed((float) SharedData.display_width / SharedData.display_height);
        long nearQ = FixedBaseMath.toFixed(Constants.Common.Z_NEAR);
        long farQ = FixedBaseMath.toFixed(Constants.Common.Z_FAR);

        scene = new Scene(sceneObjectsNum, fovQ, aspectQ, nearQ, farQ);
        addObjects();
    }


    private void addObjects() {
        try {
            // Example: Ship
            model = ObjParser.create("/Models/ObjModels/TestShip.obj");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(20.0f);
        sceneObject.ty = FixedBaseMath.toFixed(10.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-500.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(1f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.4f);
        sceneObject.scale = FixedBaseMath.toFixed(200.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFBBBBAA, // Color near
                0xFFBBBBAA, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e3f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e2f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        try {
            // Example: Ship
            model = ObjParser.create("/Models/ObjModels/TestShip.obj");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(200.0f);
        sceneObject.ty = FixedBaseMath.toFixed(100.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-500.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(1f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.4f);
        sceneObject.scale = FixedBaseMath.toFixed(200.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFFF0022, // Color near
                0xFFBBBBAA, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e4f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e3f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        try {
            // Example: Ship
            model = ObjParser.create("/Models/ObjModels/TestShip.obj");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(400.0f);
        sceneObject.ty = FixedBaseMath.toFixed(200.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-500.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(1f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.4f);
        sceneObject.scale = FixedBaseMath.toFixed(200.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFF77AAFF, // Color near
                0xFFBBBBAA, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e5f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e4f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
         // ADD OBJECTS 
        // Example: Planet
        try {
            model = ObjParser.create("/Models/ObjModels/TestPlanet.obj");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(15000.0f);
        sceneObject.ty = FixedBaseMath.toFixed(-30000.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-50000.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(FixedTrigMath.degreesToRadians(0)+0.2f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.8f);
        sceneObject.rotZ = FixedBaseMath.toFixed(-0.8f);
        sceneObject.scale = FixedBaseMath.toFixed(10000.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFF772233, // Color near
                0xFF111111, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
     
        
        
        // Ring 1
        model = RingHorizontal.create(36);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(15000.0f);
        sceneObject.ty = FixedBaseMath.toFixed(-30000.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-50000.0f);
        sceneObject.scale = FixedBaseMath.toFixed(15000.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(0.2f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.8f);
        sceneObject.rotZ = FixedBaseMath.toFixed(-0.8f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFAA5533, // Color near
                0xAA334411, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        // Ring 2
        model = RingHorizontal.create(36);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(15000.0f);
        sceneObject.ty = FixedBaseMath.toFixed(-30000.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-50000.0f);
        sceneObject.scale = FixedBaseMath.toFixed(18000.0f);
        sceneObject.rotX = FixedBaseMath.toFixed(0.2f);
        sceneObject.rotY = FixedBaseMath.toFixed(0.8f);
        sceneObject.rotZ = FixedBaseMath.toFixed(-0.8f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0x88AA9966, // Color near
                0x88334411, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

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
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        
        // Make another model.
        // Example: Cube
        model = Cube.create(5);

        sceneObject = new SceneObject(model);
        sceneObject.tx = FixedBaseMath.toFixed(2000.0f);
        sceneObject.ty = FixedBaseMath.toFixed(-500.0f);
        sceneObject.tz = FixedBaseMath.toFixed(-10000.0f);
        sceneObject.scale = FixedBaseMath.toFixed(50.0f);
        sceneObject.updateBoundingSphereRadiusScaled();

        sceneObject.material = new Material(
                0xFFAAAAFF, // Color near
                0xFFFF2222, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e5f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e4f), // Alpha ramp distance far
                RenderEffects.TYPE_EDGES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);

        // Make another model.
        // Example: Starsphere blue
        model = RandomCloud.create(16, Constants.Common.SEED + 1);

        sceneObject = new SceneObject(model);
        sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFF88AAFF, // Color near
                0xFF1155FF, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_S, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        
        // Make another model.
        // Example: Starsphere yellow
        model = RandomCloud.create(64, Constants.Common.SEED + 10);

        sceneObject = new SceneObject(model);
        sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFFAAAA00, // Color near
                0xFFAAAA00, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_P, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
        
        // Make another model.
        // Example: Starsphere red
        model = RandomCloud.create(128, Constants.Common.SEED + 100);

        sceneObject = new SceneObject(model);
                sceneObject.scale = FixedBaseMath.toFixed(1e6f);
        sceneObject.updateBoundingSphereRadiusScaled();

        // Example: set a material on this sphere (covered below)
        sceneObject.material = new Material(
                0xFFAA3300, // Color near
                0xFF110000, // Color far
                FixedBaseMath.toFixed(1f), // Material z-near
                FixedBaseMath.toFixed(1e6f), // Material z-far
                FixedBaseMath.toFixed(1f), // Alpha ramp distance near
                FixedBaseMath.toFixed(1e5f), // Alpha ramp distance far
                RenderEffects.TYPE_VERTICES, // 0 - vertices, 1 - edges
                1, // Primitive width (TODO)
                RenderEffects.SHAPE_X, // Primitive shape
                DITHER_LEVEL_GLOBAL);  // Dither level

        scene.addObject(sceneObject);
    }

    protected void paint(Graphics g) {
        scene.renderAll(g);
    }

    public void run() {
        //while (true) {
        //    frame++;
        //    try {
        //        Thread.sleep(Constants.Common.DELTA_RENDER);
        //    } catch (InterruptedException e) {
        //    }
        //}
    }

    protected void keyPressed(int keyCode) {
        repaint();
        int gameAction;
        try {
            gameAction = getGameAction(keyCode);
        } catch (IllegalArgumentException e) {
            gameAction = 0;
        }
        
        moveMultiplier = Math.min(MULTIPLIER_MAX, moveMultiplier + MULTIPLIER_STEP);
        scene.handleKeyPressed(keyCode, gameAction, moveMultiplier);
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }
    
    protected void keyReleased(int keyCode) {
        moveMultiplier = 1;
    }
}
