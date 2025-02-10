package FixedMath;

import java.util.Hashtable;
import java.util.Vector;

public final class FixedVecMath {

    public static final int FIXED_SHIFT = FixedBaseMath.FIXED_SHIFT;
    public static final long FIXED_SCALE = FixedBaseMath.FIXED_SCALE;

    private static final Hashtable pool = new Hashtable();
    private static final int MAX_IDLE_POOL_SIZE = 16;

    public static synchronized long[] acquireVector(int length) {
        Integer key = new Integer(length);
        Vector vecPool = (Vector) pool.get(key);
        if (vecPool == null) {
            vecPool = new Vector();
            pool.put(key, vecPool);
        }
        if (!vecPool.isEmpty()) {
            int size = vecPool.size();
            long[] arr = (long[]) vecPool.elementAt(size - 1);
            vecPool.removeElementAt(size - 1);
            return arr;
        }
        return new long[length];
    }

    public static synchronized void releaseVector(long[] v) {
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

    public static long[] fixedAdd(long[] v1, long[] v2) {
        int len = v1.length;
        long[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] + v2[i];
        }
        return result;
    }

    public static long[] fixedSub(long[] v1, long[] v2) {
        int len = v1.length;
        long[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] - v2[i];
        }
        return result;
    }

    public static long[] fixedMul(long[] v, long scalar) {
        int len = v.length;
        long[] result = acquireVector(len);
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.fixedMul(v[i], scalar);
        }
        return result;
    }

    public static long[] fixedDiv(long[] v, long scalar) {
        int len = v.length;
        long[] result = acquireVector(len);
        if (scalar == 0) {
            for (int i = 0; i < len; i++) {
                result[i] = (v[i] >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            return result;
        }
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.fixedDiv(v[i], scalar);
        }
        return result;
    }

    public static long fixedDotProduct(long[] v1, long[] v2) {
        int len = v1.length;
        long sum = 0;
        for (int i = 0; i < len; i++) {
            sum += v1[i] * v2[i];
        }
        return sum >> FixedBaseMath.FIXED_SHIFT;
    }

    public static long[] fixedCrossProduct(long[] v1, long[] v2) {
        long[] result = acquireVector(3);
        result[0] = ((v1[1] * v2[2] - v1[2] * v2[1]) >> FixedBaseMath.FIXED_SHIFT);
        result[1] = ((v1[2] * v2[0] - v1[0] * v2[2]) >> FixedBaseMath.FIXED_SHIFT);
        result[2] = ((v1[0] * v2[1] - v1[1] * v2[0]) >> FixedBaseMath.FIXED_SHIFT);
        return result;
    }

    public static long fixedMagnitude(long[] v) {
        int len = v.length;
        long sqrSum = 0;
        for (int i = 0; i < len; i++) {
            sqrSum += v[i] * v[i];
        }
        return FixedBaseMath.sqrt(sqrSum >> FixedBaseMath.FIXED_SHIFT);
    }

    public static long[] normalize(long[] v) {
        long mag = fixedMagnitude(v);
        if (mag == 0) {
            return acquireVector(v.length);
        }
        return fixedDiv(v, mag);
    }

    public static long angleBetweenVectors(long[] v1, long[] v2) {
        long dot = fixedDotProduct(v1, v2);
        long mag1 = fixedMagnitude(v1);
        long mag2 = fixedMagnitude(v2);
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        long numerator = dot << FixedBaseMath.FIXED_SHIFT;
        long denom = mag1 * mag2;
        if (denom == 0) {
            return 0;
        }
        long cosVal = numerator / denom;
        long limit = (1L << FixedBaseMath.FIXED_SHIFT);
        if (cosVal > limit) {
            cosVal = limit;
        }
        if (cosVal < -limit) {
            cosVal = -limit;
        }
        return FixedTrigMath.acos(cosVal);
    }

    public static long angleBetweenNormalized(long[] v1, long[] v2) {
        long[] vn1 = normalize(v1);
        long[] vn2 = normalize(v2);
        long dot = fixedDotProduct(vn1, vn2);
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
