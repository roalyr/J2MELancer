package Renderer;

import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import FixedMath.FixedTrigMath;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import java.util.Vector;

public class Scene {

    private Vector objects;
    private Vector visibleObjects;
    private Renderer renderer;
    private RendererUI rendererUI;
    private long fovQ, aspectQ, nearQ, farQ;
    private int fovDegrees = 60;
    private static final int FOV_MIN = 10, FOV_MAX = 120;
    private Camera camera;
    private Perspective perspective;
    private static final long MOVE_STEP = FixedBaseMath.toFixed(0.1f);
    private static final long ROT_STEP = FixedBaseMath.toFixed(0.01f);

    public Scene(int capacity, long fovQ, long aspectQ, long nearQ, long farQ) {
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

    public void resetCamera() {
        camera.setPosition(0, 0, 0);
        camera.setOrientation(new long[]{0, 0, 0, FixedBaseMath.toFixed(1.0f)});
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
        long fq = FixedBaseMath.toFixed((float) deg);
        perspective.setFov(fq);
        this.fovQ = fq;
    }

    public void handleKeyPressed(int keyCode, int gameAction, int moveMultiplier) {
        if (gameAction == Canvas.UP || keyCode == Canvas.KEY_NUM2) {
            camera.addPitch(ROT_STEP * moveMultiplier);
        }
        if (gameAction == Canvas.DOWN || keyCode == Canvas.KEY_NUM8) {
            camera.addPitch(-ROT_STEP * moveMultiplier);
        }
        if (gameAction == Canvas.LEFT || keyCode == Canvas.KEY_NUM4) {
            camera.addYaw(ROT_STEP * moveMultiplier);
        }
        if (gameAction == Canvas.RIGHT || keyCode == Canvas.KEY_NUM6) {
            camera.addYaw(-ROT_STEP * moveMultiplier);
        }
        if (gameAction == Canvas.FIRE || keyCode == Canvas.KEY_NUM5) {
            resetCamera();
        }
        if (keyCode == Canvas.KEY_NUM1) {
            camera.moveForward(MOVE_STEP * moveMultiplier * moveMultiplier);
        }
        if (keyCode == Canvas.KEY_NUM3) {
            increaseFov();
        }
        if (keyCode == Canvas.KEY_NUM7) {
            camera.moveForward(-MOVE_STEP * moveMultiplier * moveMultiplier);
        }
        if (keyCode == Canvas.KEY_NUM9) {
            decreaseFov();
        }
        if (keyCode == Canvas.KEY_STAR || keyCode == Canvas.KEY_NUM0 || keyCode == Canvas.KEY_POUND) {
            resetCamera();
        }
    }

    public void renderAll(Graphics g) {
        long[] viewMatrix = FixedMatMath.multiply4x4(perspective.getPerspectiveMatrix(), camera.getViewMatrix());
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

    private boolean isObjectVisible(SceneObject obj, long[] camMat) {
        long[] centerCam = FixedMatMath.acquireMatrix();
        long[] center = FixedMatMath.acquireMatrix();
        center[0] = obj.tx;
        center[1] = obj.ty;
        center[2] = obj.tz;
        center[3] = FixedBaseMath.toFixed(1.0f);
        
        long cull_far = obj.material.farMarginQ24_8;
        long cull_near = obj.material.nearMarginQ24_8;

        FixedMatMath.transformPoint(camMat, center, centerCam);
        long cx = centerCam[0];
        long cy = centerCam[1];
        long cz = centerCam[2];
        long cw = centerCam[3];
        if (cw <= 0) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        long invW = FixedBaseMath.fixedDiv(FixedBaseMath.toFixed(1.0f), cw);
        cx = FixedBaseMath.fixedMul(cx, invW);
        cy = FixedBaseMath.fixedMul(cy, invW);
        cz = FixedBaseMath.fixedMul(cz, invW);
        long distQ = -cz;
        long radius = -obj.boundingSphereRadiusScaled;

        if (FixedBaseMath.fixedAdd(distQ, radius) > cull_far) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        long halfFovDegQ = FixedBaseMath.fixedDiv(fovQ, FixedBaseMath.toFixed(2.0f));
        long halfFovRadQ = FixedTrigMath.degreesToRadians(FixedBaseMath.toInt(halfFovDegQ));
        long tanHalfVertFovQ = FixedTrigMath.tan(halfFovRadQ);
        long tanHalfHorizFovQ = FixedBaseMath.fixedMul(tanHalfVertFovQ, aspectQ);
        long absCX = (cx < 0) ? -cx : cx;
        long lrLimit = FixedBaseMath.fixedMul(distQ, tanHalfHorizFovQ);
        if (FixedBaseMath.fixedAdd(absCX, radius) > lrLimit) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        long absCY = (cy < 0) ? -cy : cy;
        long tbLimit = FixedBaseMath.fixedMul(distQ, tanHalfVertFovQ);
        if (FixedBaseMath.fixedAdd(absCY, radius) > tbLimit) {
            FixedMatMath.releaseMatrix(center);
            FixedMatMath.releaseMatrix(centerCam);
            return false;
        }
        FixedMatMath.releaseMatrix(center);
        FixedMatMath.releaseMatrix(centerCam);
        return true;
    }
}
