package Renderer;

import Constants.*;

/**
 * SceneObject: a transform + geometry in Q24.8
 */
public class SceneObject {

    public ModelQ24_8 model;   // the geometry
    public int tx,  ty,  tz;     // translation
    public int rotX,  rotY,  rotZ; // rotation angles Q24.8
    public int scale;          // uniform scale in Q24.8 (or separate sx, sy, sz)


    public SceneObject(ModelQ24_8 model) {
        this.model = model;
        // TODO: default transform
        this.scale = Common.ONE_POS;
    }
   
}
