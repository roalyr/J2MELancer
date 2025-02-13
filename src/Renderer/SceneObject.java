package Renderer;

import FixedMath.FixedBaseMath;

public class SceneObject {

    public Model model;   // the geometry
    public long tx, ty, tz;     // translation in Q
    public long rotX, rotY, rotZ; // rotation angles in Q
    public long scale;          // uniform scale in Q
    public int depth; // Calculated depth for rendering
    public Material material;
    public long boundingSphereRadiusScaled;

    public SceneObject(Model model) {
        this.model = model;
        this.scale = FixedBaseMath.FIXED1;
        this.material = null;
        updateBoundingSphereRadiusScaled();
    }

    public void updateBoundingSphereRadiusScaled() {
        boundingSphereRadiusScaled = FixedBaseMath.fixedMul(model.boundingSphereRadius, scale);
    }
}
