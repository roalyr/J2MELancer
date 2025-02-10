package Renderer;

import Constants.*;
import FixedMath.*;

/**
 * SceneObject: a transform + geometry in Q24.8
 */
public class SceneObject {

    public Model model;   // the geometry
    public int tx,  ty,  tz;     // translation
    public int rotX,  rotY,  rotZ; // rotation angles Q24.8
    public int scale;          // uniform scale in Q24.8 (or separate sx, sy, sz)
    public int depth; // Calculated depth for rendering
    // Now we keep all rendering-related “material” in one place
    public Material material;

    // If you want each object to keep track of an effective bounding radius
    // after scale, you can store it here:
    public int boundingSphereRadiusScaled;

    public SceneObject(Model model) {
        this.model = model;
        this.scale = Common.ONE_POS; // default scale = 1
        // you can default material = null or a default material
        this.material = null;
        // By default, boundingSphereRadiusScaled can be set based on
        // model.boundingSphereRadius * scale
        updateBoundingSphereRadiusScaled();
    }

    public void updateBoundingSphereRadiusScaled() {
        // boundingSphereRadiusScaled = model.boundingSphereRadius * scale
        boundingSphereRadiusScaled = 
            FixedBaseMath.fixedMul(model.boundingSphereRadius, scale);
    }
   
}
