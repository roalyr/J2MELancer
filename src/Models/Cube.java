package Models;

import Constants.Common;
import FixedMath.FixedBaseMath;


public class Cube {
    private static int one_p = Common.ONE_POS;
    private static int one_n = Common.ONE_NEG;
    // Bounding box.
    public static final int minX = one_n;
    public static final int minY = one_n;
    public static final int minZ = one_n;
    
    public static final int maxX = one_p;
    public static final int maxY = one_p;
    public static final int maxZ = one_p;
    
    public static final int BOUNDING_SPHERE_RADIUS = FixedBaseMath.toQ24_8(2.0f);
    
    // Model data.
    public static final int[][] VERTICES = {
        {one_n, one_n, one_n}, {one_n, one_n, one_p},
        {one_n, one_p, one_n}, {one_n, one_p, one_p},
        {one_p, one_n, one_n}, {one_p, one_n, one_p},
        {one_p, one_p, one_n}, {one_p, one_p, one_p}
    };
    public static final int[][] EDGES = {
        {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7},
        {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

}
