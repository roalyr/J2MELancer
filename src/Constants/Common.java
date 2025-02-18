package Constants;

public class Common {

    public static final int FPS_RENDER_MAX = 60;
    public static final int DELTA_RENDER = 1000 / FPS_RENDER_MAX; // ms between frames in Renderer 

    // Used for camera and perspective, not related to material z-near and z-far;
    public static final float Z_NEAR = 1.0f;
    public static final float Z_FAR = 1e5f; 

    public static final long SEED = 1234;

    private Common() {
    }
}
