package Constants;
import FixedMath.*;


public class Common {
    public static final int ZERO = FixedBaseMath.toQ24_8(0.0f);
    public static final int ONE_NEG = FixedBaseMath.toQ24_8(-1.0f);
    public static final int ONE_POS = FixedBaseMath.toQ24_8(1.0f);
    public static final int ONE_POS_HALF = FixedBaseMath.toQ24_8(0.5f);
    
    public static final int ONE_DEGREE_IN_RADIANS = FixedBaseMath.toQ24_8(0.0174533f);
    
    public static final int DELTA_RENDER = 33; // ms between frames in Renderer 
    
    public static final float Z_NEAR = 1.0f;
    public static final float Z_FAR = 1e2f;  // ~8.3e6 is near the upper limit. 
}
