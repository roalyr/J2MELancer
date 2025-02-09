package Renderer;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Canvas;
import FixedMath.*;
import java.util.Vector;

public class Scene {

    private Vector objects;
    private Vector visibleObjects;
    private Renderer renderer;
    private RendererUI rendererUI;
    private int fovQ,  aspectQ,  nearQ,  farQ;
    private int fovDegrees = 60;
    private static final int FOV_MIN = 10,  FOV_MAX = 120;
    private Camera camera;
    private Perspective perspective;

    // Movement & rotation increments in Q24.8
    private static final int MOVE_STEP = FixedBaseMath.toQ24_8(5.0f);
    private static final int ROT_STEP = FixedBaseMath.toQ24_8(0.05f);

    public Scene(int capacity, int fovQ, int aspectQ, int nearQ, int farQ) {
        objects = new Vector(capacity);
        visibleObjects = new Vector(capacity);
        renderer = new Renderer();
        rendererUI = new RendererUI();

        this.fovQ = fovQ;
        this.aspectQ = aspectQ;
        this.nearQ = nearQ;
        this.farQ = farQ;

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
        if (deg < FOV_MIN) {
            deg = FOV_MIN;
        }
        if (deg > FOV_MAX) {
            deg = FOV_MAX;
        }
        fovDegrees = deg;
        int fq = FixedBaseMath.toQ24_8((float) deg);
        perspective.setFov(fq);
        this.fovQ = fq;
    //System.out.print(this.fovQ+"\n");
    }

    public void handleKeyPressed(int keyCode, int gameAction) {

        // Directional keys
        if (gameAction == Canvas.UP || keyCode == Canvas.KEY_NUM2) {
            camera.addPitch(ROT_STEP);
        }
        if (gameAction == Canvas.DOWN || keyCode == Canvas.KEY_NUM8) {
            camera.addPitch(-ROT_STEP);
        }
        if (gameAction == Canvas.LEFT || keyCode == Canvas.KEY_NUM4) {
            camera.addYaw(ROT_STEP);
        }
        if (gameAction == Canvas.RIGHT || keyCode == Canvas.KEY_NUM6) {
            camera.addYaw(-ROT_STEP);
        }

        // Fire key (depends on context)
        if (gameAction == Canvas.FIRE || keyCode == Canvas.KEY_NUM5) {
            resetCamera();
        }

        // Additional keys
        if (keyCode == Canvas.KEY_NUM1) {
            camera.moveForward(MOVE_STEP);
        }
        if (keyCode == Canvas.KEY_NUM3) {
            increaseFov();
        }
        if (keyCode == Canvas.KEY_NUM7) {
            camera.moveForward(-MOVE_STEP);
        }
        if (keyCode == Canvas.KEY_NUM9) {
            decreaseFov();
        }

        // *, 0 and # keys
        if (keyCode == Canvas.KEY_STAR) {
            resetCamera();
        }
        if (keyCode == Canvas.KEY_NUM0) {
            resetCamera();
        }
        if (keyCode == Canvas.KEY_POUND) {
            resetCamera();
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
        rendererUI.updateFPS();
        rendererUI.printFPS(g);

        FixedMatMath.releaseMatrix(viewMatrix);

    }

    private boolean isObjectVisible(SceneObject obj, int[] camMat) {
        int[] centerCam = FixedMatMath.acquireMatrix();
        int[] center = FixedMatMath.acquireMatrix();

        center[0] = obj.tx;
        center[1] = obj.ty;
        center[2] = obj.tz;
        center[3] = FixedBaseMath.toQ24_8(1.0f);

        FixedMatMath.transformPoint(camMat, center, centerCam);
        int cx = centerCam[0];
        int cy = centerCam[1];
        int cz = centerCam[2];
        int cw = centerCam[3];
        if (cw <= 0) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        int invW = FixedBaseMath.q24_8_div(FixedBaseMath.toQ24_8(1.0f), cw);
        cx = FixedBaseMath.q24_8_mul(cx, invW);
        cy = FixedBaseMath.q24_8_mul(cy, invW);
        cz = FixedBaseMath.q24_8_mul(cz, invW);
        int distQ = -cz;
        int radius = -obj.boundingSphereRadiusScaled;

        // If the camera is inside the object's bounding sphere, consider the object visible.
        if (distQ >= radius) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return true;
        }
        if (FixedBaseMath.q24_8_add(distQ, radius) < nearQ) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        if (FixedBaseMath.q24_8_sub(distQ, radius) > farQ) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        int halfFovDegQ = FixedBaseMath.q24_8_div(fovQ, FixedBaseMath.toQ24_8(2.0f));
        int halfFovRadQ = FixedTrigMath.degreesToRadiansQ24_8(FixedBaseMath.toInt(halfFovDegQ));
        int tanHalfVertFovQ = FixedTrigMath.tan(halfFovRadQ);
        int tanHalfHorizFovQ = FixedBaseMath.q24_8_mul(tanHalfVertFovQ, aspectQ);
        int absCX = (cx < 0) ? -cx : cx;
        int lrLimit = FixedBaseMath.q24_8_mul(distQ, tanHalfHorizFovQ);
        if (FixedBaseMath.q24_8_add(absCX, radius) > lrLimit) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        int absCY = (cy < 0) ? -cy : cy;
        int tbLimit = FixedBaseMath.q24_8_mul(distQ, tanHalfVertFovQ);
        if (FixedBaseMath.q24_8_add(absCY, radius) > tbLimit) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        FixedMatMath.releaseMatrix(center);
        FixedMatMath.releaseMatrix(centerCam);
        return true;
    }
}