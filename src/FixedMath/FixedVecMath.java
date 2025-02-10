package FixedMath;

import java.util.Hashtable;
import java.util.Vector;

public final class FixedVecMath {

    public static final int FIXED_SHIFT = FixedBaseMath.FIXED_SHIFT;
    public static final int FIXED_SCALE = FixedBaseMath.FIXED_SCALE;

    // Pool for int[] arrays by length.
    private static final Hashtable pool = new Hashtable();
    private static final int MAX_IDLE_POOL_SIZE = 16;

    public static synchronized int[] acquireVector(int length) {
        Integer key = new Integer(length);
        Vector vecPool = (Vector) pool.get(key);
        if (vecPool == null) {
            vecPool = new Vector();
            pool.put(key, vecPool);
        }
        if (!vecPool.isEmpty()) {
            int size = vecPool.size();
            int[] arr = (int[]) vecPool.elementAt(size - 1);
            vecPool.removeElementAt(size - 1);
            return arr;
        }
        return new int[length];
    }

    public static synchronized void releaseVector(int[] v) {
        if (v == null) {
            return;
        }
        Integer key = new Integer(v.length);
        Vector vecPool = (Vector) pool.get(key);
        if (vecPool == null) {
            vecPool = new Vector();
            pool.put(key, vecPool);
        }
        vecPool.addElement(v);
        while (vecPool.size() > MAX_IDLE_POOL_SIZE) {
            vecPool.removeElementAt(vecPool.size() - 1);
        }
    }

    // -------------------------------------
    // Vector Addition
    // -------------------------------------
    public static int[] fixedAdd(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] + v2[i];
        }
        return result;
    }

    // -------------------------------------
    // Vector Subtraction
    // -------------------------------------
    public static int[] fixedSub(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] - v2[i];
        }
        return result;
    }

    // -------------------------------------
    // Scalar Multiplication
    // -------------------------------------
    public static int[] fixedMul(int[] v, int scalar) {
        int len = v.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.fixedMul(v[i], scalar);
        }
        return result;
    }

    // -------------------------------------
    // Scalar Division
    // -------------------------------------
    public static int[] fixedDiv(int[] v, int scalar) {
        int len = v.length;
        int[] result = acquireVector(len);
        if (scalar == 0) {
            for (int i = 0; i < len; i++) {
                result[i] = (v[i] >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            return result;
        }
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.fixedDiv(v[i], scalar);
        }
        return result;
    }

    // -------------------------------------
    // Dot Product
    // -------------------------------------
    public static int fixedDotProduct(int[] v1, int[] v2) {
        int len = v1.length;
        long sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (long) v1[i] * v2[i];
        }
        return (int) (sum >> FixedBaseMath.FIXED_SHIFT);
    }

    // -------------------------------------
    // Cross Product (3D only)
    // -------------------------------------
    public static int[] fixedCrossProduct(int[] v1, int[] v2) {
        int[] result = acquireVector(3);
        result[0] = (int) (((long) v1[1] * v2[2] - (long) v1[2] * v2[1]) >> FixedBaseMath.FIXED_SHIFT);
        result[1] = (int) (((long) v1[2] * v2[0] - (long) v1[0] * v2[2]) >> FixedBaseMath.FIXED_SHIFT);
        result[2] = (int) (((long) v1[0] * v2[1] - (long) v1[1] * v2[0]) >> FixedBaseMath.FIXED_SHIFT);
        return result;
    }

    // -------------------------------------
    // Magnitude (Length)
    // -------------------------------------
    public static int fixedMagnitude(int[] v) {
        int len = v.length;
        long sqrSum = 0;
        for (int i = 0; i < len; i++) {
            long val = v[i];
            sqrSum += val * val;
        }
        return FixedBaseMath.sqrt((int) (sqrSum >> FixedBaseMath.FIXED_SHIFT));
    }

    // -------------------------------------
    // Normalize
    // -------------------------------------
    public static int[] normalize(int[] v) {
        int mag = fixedMagnitude(v);
        if (mag == 0) {
            return acquireVector(v.length);
        }
        return fixedDiv(v, mag);
    }

    // -------------------------------------
    // Angle Between Two Vectors (in radians, fixed-point)
    // -------------------------------------
    public static int angleBetweenVectors(int[] v1, int[] v2) {
        int dot = fixedDotProduct(v1, v2);
        int mag1 = fixedMagnitude(v1);
        int mag2 = fixedMagnitude(v2);
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        long numerator = ((long) dot) << FixedBaseMath.FIXED_SHIFT;
        long denom = (long) mag1 * mag2;
        if (denom == 0) {
            return 0;
        }
        long cosVal = numerator / denom;
        long limit = (1 << FixedBaseMath.FIXED_SHIFT);
        if (cosVal > limit) {
            cosVal = limit;
        }
        if (cosVal < -limit) {
            cosVal = -limit;
        }
        return FixedTrigMath.acos((int) cosVal);
    }

    // -------------------------------------
    // Angle Between Normalized Vectors
    // -------------------------------------
    public static int angleBetweenNormalized(int[] v1, int[] v2) {
        int[] vn1 = normalize(v1);
        int[] vn2 = normalize(v2);
        int dot = fixedDotProduct(vn1, vn2);
        if (dot > FIXED_SCALE) {
            dot = FIXED_SCALE;
        }
        if (dot < -FIXED_SCALE) {
            dot = -FIXED_SCALE;
        }
        return FixedTrigMath.acos(dot);
    }

    private FixedVecMath() {
    }
}
