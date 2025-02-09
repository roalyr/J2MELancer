package FixedMath;

import java.util.Hashtable;
import java.util.Vector;

public final class FixedVecMath {

    public static final int Q24_8_SHIFT = 8;
    public static final int Q24_8_SCALE = (1 << Q24_8_SHIFT);

    // ---------------------------
    // Pool for int[] arrays by length.
    // ---------------------------
    private static final Hashtable pool = new Hashtable();
    private static final int MAX_IDLE_POOL_SIZE = 16;

    /**
     * Acquires an int[] array of the specified length from the pool.
     * If none is available, a new one is created.
     */
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

    /**
     * Releases an int[] array back to the pool.
     */
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
        // Trim if too many idle arrays.
        while (vecPool.size() > MAX_IDLE_POOL_SIZE) {
            vecPool.removeElementAt(vecPool.size() - 1);
        }
    }

    // -------------------------------------
    // Vector Add/Sub (Assumes same length)
    // -------------------------------------
    public static int[] q24_8_add(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] + v2[i];
        }
        return result;
    }

    public static int[] q24_8_sub(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] - v2[i];
        }
        return result;
    }

    // -------------------------------------
    // Scalar Multiply/Divide
    // -------------------------------------
    public static int[] q24_8_mul(int[] v, int scalar) {
        int len = v.length;
        int[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            // (v[i] * scalar) >> Q24_8_SHIFT
            long tmp = ((long) v[i] * (long) scalar) >> Q24_8_SHIFT;
            result[i] = (int) tmp;
        }
        return result;
    }

    public static int[] q24_8_div(int[] v, int scalar) {
        int len = v.length;
        int[] result = acquireVector(len);
        if (scalar == 0) {
            for (int i = 0; i < len; i++) {
                result[i] = (v[i] >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            return result;
        }
        for (int i = 0; i < len; i++) {
            result[i] = (int) (((long) v[i] << Q24_8_SHIFT) / scalar);
        }
        return result;
    }

    // -------------------------------------
    // Dot Product
    // -------------------------------------
    public static int q24_8_dotProduct(int[] v1, int[] v2) {
        int len = v1.length;
        long sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (long) v1[i] * (long) v2[i];
        }
        return (int) (sum >> Q24_8_SHIFT);
    }

    // -------------------------------------
    // Cross Product (3D only)
    // -------------------------------------
    public static int[] q24_8_crossProduct(int[] v1, int[] v2) {
        int[] result = acquireVector(3);
        result[0] = (int) ((((long) v1[1] * v2[2]) - ((long) v1[2] * v2[1])) >> Q24_8_SHIFT);
        result[1] = (int) ((((long) v1[2] * v2[0]) - ((long) v1[0] * v2[2])) >> Q24_8_SHIFT);
        result[2] = (int) ((((long) v1[0] * v2[1]) - ((long) v1[1] * v2[0])) >> Q24_8_SHIFT);
        return result;
    }

    // -------------------------------------
    // Magnitude (Length)
    // -------------------------------------
    public static int q24_8_magnitude(int[] v) {
        int len = v.length;
        long sqrSum = 0;
        for (int i = 0; i < len; i++) {
            long val = v[i];
            sqrSum += val * val;
        }
        long q24_8_val = sqrSum >> Q24_8_SHIFT;
        if (q24_8_val > 0x7FFFFFFF) {
            q24_8_val = 0x7FFFFFFF;
        }
        return FixedBaseMath.sqrt((int) q24_8_val);
    }

    // -------------------------------------
    // Normalize
    // -------------------------------------
    public static int[] q24_8_normalize(int[] v) {
        int mag = q24_8_magnitude(v);
        if (mag == 0) {
            return acquireVector(v.length);  // Return a zero vector from the pool.
        }
        return q24_8_div(v, mag);
    }

    // -------------------------------------
    // Angle Between Two Vectors
    // -------------------------------------
    public static int angleBetweenVectors(int[] v1, int[] v2) {
        int dot = q24_8_dotProduct(v1, v2);
        int mag1 = q24_8_magnitude(v1);
        int mag2 = q24_8_magnitude(v2);
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        long numerator = ((long) dot) << Q24_8_SHIFT;
        long denom = (long) mag1 * mag2;
        if (denom == 0) {
            return 0;
        }
        long cosVal = numerator / denom;
        long limit = (1 << Q24_8_SHIFT);
        if (cosVal > limit) {
            cosVal = limit;
        }
        if (cosVal < -limit) {
            cosVal = -limit;
        }
        return FixedTrigMath.acos((int) cosVal);
    }

    // -------------------------------------
    // Angle Between Normalized Vectors (Optional)
    // -------------------------------------
    public static int angleBetweenNormalized(int[] v1, int[] v2) {
        int[] vn1 = q24_8_normalize(v1);
        int[] vn2 = q24_8_normalize(v2);
        int dot = q24_8_dotProduct(vn1, vn2);
        if (dot > Q24_8_SCALE) {
            dot = Q24_8_SCALE;
        }
        if (dot < -Q24_8_SCALE) {
            dot = -Q24_8_SCALE;
        }
        // Optionally, you might want to release vn1 and vn2 if they were acquired from the pool.
        return FixedTrigMath.acos(dot);
    }

    // Prevent instantiation.
    private FixedVecMath() {
    }
}