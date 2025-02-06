package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;

public class Scene {
    // All objects in the scene.

    private Vector objects;
    // Cached vector of objects that pass frustum culling.
    private Vector visibleObjects;
    // Matrices for combining camera and perspective.
    private int[] cameraMatrix;
    private int[] perspectiveMatrix;
    private int[] viewMatrix;

    // Pre-calculated screen half dimensions (in Q24.8).
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;
    // Reusable scratch array for 4D point transformation.
    private int[] localScratch = new int[4];
    // The renderer instance.
    private Renderer renderer;
    // Additional fields to store your frustum info
    private int fovQ;
    private int aspectQ;
    private int nearQ;
    private int farQ;

    public Scene(int capacity, int fovQ, int aspectQ, int nearQ, int farQ) {
        objects = new Vector(capacity);
        visibleObjects = new Vector(capacity);
        renderer = new Renderer();

        this.fovQ = fovQ;
        this.aspectQ = aspectQ;
        this.nearQ = nearQ;
        this.farQ = farQ;
    }

    public void addObject(SceneObject obj) {
        // TODO: keep index for later.
        objects.addElement(obj);
    }

    private boolean isObjectVisible(SceneObject obj, int[] cameraMatrix) {
        // 1) Transform object's center from world -> camera space
        int[] centerWorld = {
            obj.tx,
            obj.ty,
            obj.tz,
            FixedBaseMath.toQ24_8(1.0f)
        };
        int[] centerCam = new int[4];
        FixedMatMath.transformPoint(cameraMatrix, centerWorld, centerCam);

        // 2) Convert to actual camera space (divide by w if needed).
        int cx = centerCam[0];
        int cy = centerCam[1];
        int cz = centerCam[2];
        int cw = centerCam[3];

        if (cw <= 0) {
            // behind or invalid
            return false;
        }
        // If cw != 1, scale x,y,z by 1/w
        int invW = FixedBaseMath.q24_8_div(FixedBaseMath.toQ24_8(1.0f), cw);
        cx = FixedBaseMath.q24_8_mul(cx, invW);
        cy = FixedBaseMath.q24_8_mul(cy, invW);
        cz = FixedBaseMath.q24_8_mul(cz, invW);

        // 3) Since the camera looks down -Z, define distance = -cz for near/far checks
        // Minus for in front of camera.
        int distanceQ = -cz;

        // The bounding sphere radius (already scaled in Q24.8)
        // Minus for "fully outside".
        int radius = -obj.boundingSphereRadiusScaled;

        // 4) Near/far planes:
        // If (distance + radius < nearQ) => cull (sphere is "behind" near plane)
        // If (distance - radius > farQ)  => cull (sphere is "beyond" far plane)
        if (FixedBaseMath.q24_8_add(distanceQ, radius) < nearQ) {
            return false;
        }
        if (FixedBaseMath.q24_8_sub(distanceQ, radius) > farQ) {
            return false;
        }

        // 5) Lateral planes using FOV
        //    We'll interpret fovQ as the VERTICAL field-of-view in degrees.
        //    halfVertFOV = fovQ/2, then convert to radians, compute tan
        int halfFovDegQ = FixedBaseMath.q24_8_div(fovQ, FixedBaseMath.toQ24_8(2.0f));
        int halfFovRadQ = FixedTrigMath.degreesToRadiansQ24_8(FixedBaseMath.toInt(halfFovDegQ));
        int tanHalfVertFovQ = FixedTrigMath.tan(halfFovRadQ);

        // Horizontal fov ~ vertical fov * aspect => 
        // tanHalfHorizFov = tan(halfVertFov)* aspect
        int tanHalfHorizFovQ = FixedBaseMath.q24_8_mul(tanHalfVertFovQ, aspectQ);

        // For left/right cull: (|cx| + radius) > distance * tanHalfHorizFov => cull
        int absCX = (cx < 0) ? -cx : cx;
        int leftRightLimit = FixedBaseMath.q24_8_mul(distanceQ, tanHalfHorizFovQ);
        if (FixedBaseMath.q24_8_add(absCX, radius) > leftRightLimit) {
            return false;
        }

        // For top/bottom cull: (|cy| + radius) > distance * tanHalfVertFov => cull
        int absCY = (cy < 0) ? -cy : cy;
        int topBottomLimit = FixedBaseMath.q24_8_mul(distanceQ, tanHalfVertFovQ);
        if (FixedBaseMath.q24_8_add(absCY, radius) > topBottomLimit) {
            return false;
        }

        // If all tests pass => object is in frustum
        return true;
    }

    public void renderAll(Graphics g, Camera camera, Perspective p) {
        // We still compute viewMatrix for actual rendering
        perspectiveMatrix = p.getPerspectiveMatrix();
        cameraMatrix = camera.getCameraMatrix();
        viewMatrix = FixedMatMath.multiply4x4(perspectiveMatrix, cameraMatrix);

        // Clear & update visible objects
        renderer.clearBuffers(g);
        visibleObjects.removeAllElements();

        for (int i = 0; i < objects.size(); i++) {
            SceneObject obj = (SceneObject) objects.elementAt(i);
            if (isObjectVisible(obj, cameraMatrix)) {
                // we do the local culling with cameraMatrix only
                // for final sorting we might get the clip-space depth:
                obj.depth = calculateObjectDepth(obj, viewMatrix);
                visibleObjects.addElement(obj);
            }
        }

        renderer.setRenderables(visibleObjects, SharedData.halfW_Q24_8, SharedData.halfH_Q24_8);
        renderer.renderScene(g, viewMatrix);

        renderer.updateFPS();
        renderer.printFPS(g);
    }

    /**
     * Computes the object's depth by transforming its center into view space.
     */
    private int calculateObjectDepth(SceneObject obj, int[] viewMatrix) {
        int[] worldCenter = new int[]{
            obj.tx,
            obj.ty,
            obj.tz,
            FixedBaseMath.toQ24_8(1.0f)
        };
        localScratch = new int[4];
        FixedMatMath.transformPoint(viewMatrix, worldCenter, localScratch);
        return localScratch[2];
    }
}