package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;
import Constants.Common;

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
    
    // Near and far plane values in Q24.8 (from Common).
    private final int Z_NEAR_Q24_8 = FixedBaseMath.toQ24_8(Common.Z_NEAR);
    private final int Z_FAR_Q24_8 = FixedBaseMath.toQ24_8(Common.Z_FAR);

    public Scene(int capacity) {
        objects = new Vector(capacity);
        visibleObjects = new Vector(capacity);
        renderer = new Renderer();
    }
    
    public void addObject(SceneObject obj, int index) {
        objects.addElement(obj);
    }
    
    /**
     * Unified visibility test that performs early frustum culling using the object's bounding sphere.
     * This method transforms the object's center into view space, applies the sphere radius,
     * projects it into screen space, and checks if it falls outside the screen bounds.
     */
    private boolean isObjectVisible(SceneObject obj, int[] viewMatrix) {
        // Construct the object's center in homogeneous coordinates.
        int[] centerWorld = new int[]{
            obj.tx,
            obj.ty,
            obj.tz,
            FixedBaseMath.toQ24_8(1.0f)
        };
        // Transform the center into view space.
        FixedMatMath.transformPoint(viewMatrix, centerWorld, localScratch);
        // localScratch now holds [x, y, z, w] in view space.
        int radius = obj.model.boundingSphereRadius;
        
        // Frustum culling against near and far planes.
        if (localScratch[2] - radius > Z_FAR_Q24_8) return false;
        if (localScratch[2] + radius < Z_NEAR_Q24_8) return false;
        if (localScratch[3] <= 0) return false;
        
        // Compute projected screen coordinates.
        int xProj = FixedBaseMath.q24_8_div(localScratch[0], localScratch[3]);
        int yProj = FixedBaseMath.q24_8_div(localScratch[1], localScratch[3]);
        int sx = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8,
                        FixedBaseMath.q24_8_mul(xProj, precalc_halfW_Q24_8)));
        int sy = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8,
                        FixedBaseMath.q24_8_mul(yProj, precalc_halfH_Q24_8)));
                        
        // Compute the on-screen radius: scale the object's radius by the perspective factor.
        int screenRadius = FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(
                                radius,
                                FixedBaseMath.q24_8_div(precalc_halfW_Q24_8, localScratch[2])
                             ));
                             
        // Check if the projected sphere lies entirely outside the screen.
        if (sx + screenRadius < 0 || sx - screenRadius > SharedData.display_width) return false;
        if (sy + screenRadius < 0 || sy - screenRadius > SharedData.display_height) return false;
        
        return true;
    }
    
    /**
     * Renders the scene using the provided camera and perspective.
     * It rebuilds the view matrix, filters objects using the unified visibility test,
     * and passes only the visible objects to the renderer.
     */
    public void renderAll(Graphics g, Camera c, Perspective p) {
        // Rebuild the view matrix: viewMatrix = perspectiveMatrix * cameraMatrix.
        perspectiveMatrix = p.getPerspectiveMatrix();
        cameraMatrix = c.getCameraMatrix();
        viewMatrix = FixedMatMath.multiply4x4(perspectiveMatrix, cameraMatrix);
        
        // Update pre-calculated half-dimensions.
        precalc_halfW_Q24_8 = SharedData.halfW_Q24_8;
        precalc_halfH_Q24_8 = SharedData.halfH_Q24_8;
        
        // Recalculate visible objects every frame (or update only when camera/perspective changes).
        visibleObjects.removeAllElements();
        for (int i = 0; i < objects.size(); i++) {
            SceneObject obj = (SceneObject) objects.elementAt(i);
            if (isObjectVisible(obj, viewMatrix)) {
                obj.depth = calculateObjectDepth(obj, viewMatrix);
                visibleObjects.addElement(obj);
            }
        }
        
        renderer.setRenderables(visibleObjects, precalc_halfW_Q24_8, precalc_halfH_Q24_8);
        renderer.renderScene(g, viewMatrix);
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
        int[] temp = new int[4];
        FixedMatMath.transformPoint(viewMatrix, worldCenter, temp);
        return temp[2];
    }
}