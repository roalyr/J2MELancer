package Renderer;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Canvas;
import FixedMath.*;
import java.util.Vector;

public class Scene {

    private Vector objects;
    private Vector visibleObjects;
    private Renderer renderer;

    private int fovQ, aspectQ, nearQ, farQ;
    private int fovDegrees = 60;
    private static final int FOV_MIN = 10, FOV_MAX = 120;

    private Camera camera;
    private Perspective perspective;

    // Movement & rotation increments in Q24.8
    private static final int MOVE_STEP = FixedBaseMath.toQ24_8(5.0f);
    private static final int ROT_STEP  = FixedBaseMath.toQ24_8(0.05f);

    public Scene(int capacity, int fovQ, int aspectQ, int nearQ, int farQ) {
        objects = new Vector(capacity);
        visibleObjects = new Vector(capacity);
        renderer = new Renderer();

        this.fovQ    = fovQ;
        this.aspectQ = aspectQ;
        this.nearQ   = nearQ;
        this.farQ    = farQ;

        camera = new Camera();
        perspective = new Perspective(fovQ, aspectQ, nearQ, farQ);
    }

    public Camera getCamera() {
        return camera;
    }

    public void addObject(SceneObject obj) {
        objects.addElement(obj);
    }

    // Reset the spaceship's (camera's) position and orientation.
    public void resetCamera() {
        camera.setPosition(0, 0, 0);
        // Reset orientation to identity quaternion: [0, 0, 0, 1.0] in Q24.8
        camera.setOrientation(new int[]{0, 0, 0, FixedBaseMath.toQ24_8(1.0f)});
    }

    public void increaseFov() {
        setFovDegrees(fovDegrees + 5);
    }

    public void decreaseFov() {
        setFovDegrees(fovDegrees - 5);
    }

    private void setFovDegrees(int deg) {
        if (deg < FOV_MIN) deg = FOV_MIN;
        if (deg > FOV_MAX) deg = FOV_MAX;
        fovDegrees = deg;
        int fq = FixedBaseMath.toQ24_8((float) deg);
        perspective.setFov(fq);
        this.fovQ = fq;
        //System.out.print(this.fovQ+"\n");
    }

    /**
     * Key mapping for spaceship controls:
     *   - Arrow keys: adjust pitch & yaw (rotate the ship).
     *   - NUM1: thrust forward; NUM7: thrust backward.
     *   - NUM4: strafe left; NUM6: strafe right.
     *   - NUM2: move up; NUM8: move down.
     *   - NUM0: reset spaceship (camera).
     *   - STAR/POUND: adjust field-of-view.
     */
    public void handleKeyPressed(int keyCode, int gameAction) {
        // Rotation adjustments via arrow keys.
        switch (gameAction) {
            case Canvas.UP:
                camera.addPitch(ROT_STEP);
                break;
            case Canvas.DOWN:
                camera.addPitch(-ROT_STEP);
                break;
            case Canvas.LEFT:
                camera.addYaw(ROT_STEP);
                break;
            case Canvas.RIGHT:
                camera.addYaw(-ROT_STEP);
                break;
        }

        // Translation controls.
        switch (keyCode) {
            case Canvas.KEY_NUM1:
                camera.moveForward(MOVE_STEP);
                break;
            case Canvas.KEY_NUM7:
                camera.moveForward(-MOVE_STEP);
                break;
            case Canvas.KEY_NUM4:
                camera.moveRight(-MOVE_STEP);
                break;
            case Canvas.KEY_NUM6:
                camera.moveRight(MOVE_STEP);
                break;
            case Canvas.KEY_NUM2:
                camera.moveUp(MOVE_STEP);
                break;
            case Canvas.KEY_NUM8:
                camera.moveUp(-MOVE_STEP);
                break;
            case Canvas.KEY_NUM0:
                resetCamera();
                break;
            case Canvas.KEY_STAR:
                increaseFov();
                break;
            case Canvas.KEY_POUND:
                decreaseFov();
                break;
        }
    }

    public void renderAll(Graphics g) {
        int[] viewMatrix = FixedMatMath.multiply4x4(perspective.getPerspectiveMatrix(), camera.getViewMatrix());
        renderer.clearBuffers(g);
        visibleObjects.removeAllElements();
        for (int i = 0; i < objects.size(); i++) {
            SceneObject obj = (SceneObject) objects.elementAt(i);
            if (isObjectVisible(obj, camera.getViewMatrix())) {
                visibleObjects.addElement(obj);
            }
        }
        renderer.setRenderables(visibleObjects, SharedData.halfW_Q24_8, SharedData.halfH_Q24_8);
        renderer.renderScene(g, viewMatrix);
        renderer.updateFPS();
        renderer.printFPS(g);
    }

    private boolean isObjectVisible(SceneObject obj, int[] camMat) {
    int[] center = {
        obj.tx,
        obj.ty,
        obj.tz,
        FixedBaseMath.toQ24_8(1.0f)
    };
    
    int[] centerCam = new int[4];
    FixedMatMath.transformPoint(camMat, center, centerCam);
    int cx = centerCam[0];
    int cy = centerCam[1];
    int cz = centerCam[2];
    int cw = centerCam[3];
    if (cw <= 0) return false;
    int invW = FixedBaseMath.q24_8_div(FixedBaseMath.toQ24_8(1.0f), cw);
    cx = FixedBaseMath.q24_8_mul(cx, invW);
    cy = FixedBaseMath.q24_8_mul(cy, invW);
    cz = FixedBaseMath.q24_8_mul(cz, invW);

    int distQ = -cz;
    int radius = -obj.boundingSphereRadiusScaled;

    
    // If the camera is inside the object's bounding sphere, consider the object visible.
    if (distQ >= radius) return true;
    
    //System.out.print("dist " + distQ + " radius " + radius + "\n");

    if (FixedBaseMath.q24_8_add(distQ, radius) < nearQ) return false;
    if (FixedBaseMath.q24_8_sub(distQ, radius) > farQ)  return false;

    int halfFovDegQ = FixedBaseMath.q24_8_div(fovQ, FixedBaseMath.toQ24_8(2.0f));
    int halfFovRadQ = FixedTrigMath.degreesToRadiansQ24_8(FixedBaseMath.toInt(halfFovDegQ));
    int tanHalfVertFovQ = FixedTrigMath.tan(halfFovRadQ);
    int tanHalfHorizFovQ = FixedBaseMath.q24_8_mul(tanHalfVertFovQ, aspectQ);

    int absCX = (cx < 0) ? -cx : cx;
    int lrLimit = FixedBaseMath.q24_8_mul(distQ, tanHalfHorizFovQ);
    if (FixedBaseMath.q24_8_add(absCX, radius) > lrLimit) return false;

    int absCY = (cy < 0) ? -cy : cy;
    int tbLimit = FixedBaseMath.q24_8_mul(distQ, tanHalfVertFovQ);
    if (FixedBaseMath.q24_8_add(absCY, radius) > tbLimit) return false;

    return true;
}
}