package FixedMath;

/**
 * FixedVecMath for Q24.8 vectors:
 *   - magnitude
 *   - normalize
 *   - dotProduct
 *   - crossProduct
 *   - angleBetweenVectors (returns Q24.8 radians)
 *
 * Key Fix: magnitude uses shift >>8, not >>16, to handle Q48.16 -> Q24.8.
 */
public final class FixedVecMath {

    public static final int Q24_8_SHIFT = 8; 
    public static final int Q24_8_SCALE = (1 << Q24_8_SHIFT);

    // -------------------------------------
    // Vector Add/Sub
    // -------------------------------------
    public static int[] q24_8_add(int[] v1, int[] v2) {
        int len = Math.min(v1.length, v2.length);
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.q24_8_add(v1[i], v2[i]);
        }
        return result;
    }

    public static int[] q24_8_sub(int[] v1, int[] v2) {
        int len = Math.min(v1.length, v2.length);
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = FixedBaseMath.q24_8_sub(v1[i], v2[i]);
        }
        return result;
    }

    // -------------------------------------
    // Scalar Multiply/Divide
    // -------------------------------------
    public static int[] q24_8_mul(int[] v, int scalar) {
        int[] result = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = FixedBaseMath.q24_8_mul(v[i], scalar);
        }
        return result;
    }

    public static int[] q24_8_div(int[] v, int scalar) {
        int[] result = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = FixedBaseMath.q24_8_div(v[i], scalar);
        }
        return result;
    }

    // -------------------------------------
    // Dot Product
    // -------------------------------------
    /**
     * Dot product in Q24.8 => sum( v1[i]*v2[i] ) >> 8
     * Returns a Q24.8 result.
     */
    public static int q24_8_dotProduct(int[] v1, int[] v2) {
        int len = Math.min(v1.length, v2.length);
        long sum = 0; // 64-bit
        for (int i = 0; i < len; i++) {
            // Q24.8 * Q24.8 => Q48.16 in raw integer
            sum += (long)v1[i] * (long)v2[i];
        }
        // shift >>8 => convert Q48.16 => Q24.8
        return (int)(sum >> Q24_8_SHIFT);
    }

    // -------------------------------------
    // Cross Product (3D)
    // -------------------------------------
    /**
     * Cross product v1 x v2 in Q24.8. We assume at least 3 components each.
     * result[i] also in Q24.8.
     */
    public static int[] q24_8_crossProduct(int[] v1, int[] v2) {
        int[] result = new int[3];
        result[0] = FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(v1[1], v2[2]),
                        FixedBaseMath.q24_8_mul(v1[2], v2[1])
                    );
        result[1] = FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(v1[2], v2[0]),
                        FixedBaseMath.q24_8_mul(v1[0], v2[2])
                    );
        result[2] = FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(v1[0], v2[1]),
                        FixedBaseMath.q24_8_mul(v1[1], v2[0])
                    );
        return result;
    }

    // -------------------------------------
    // Magnitude (Length)
    // -------------------------------------
    /**
     * sqrt of sum of squares, each v[i] is Q24.8 => squared => Q48.16 => sum => Q48.16
     * shift >>8 => Q24.8, then sqrt(...) => Q24.8
     */
    public static int q24_8_magnitude(int[] v) {
        long sqrSum = 0;
        for (int i = 0; i < v.length; i++) {
            long val = (long)v[i]; // Q24.8
            sqrSum += val * val;   // Q48.16
        }
        // SHIFT >>8 => Q24.8
        long q24_8_val = sqrSum >> 8;
        if (q24_8_val > 0x7FFFFFFF) {
            q24_8_val = 0x7FFFFFFF; // clamp if needed
        }
        return FixedBaseMath.sqrt((int)q24_8_val);
    }

    // -------------------------------------
    // Normalize
    // -------------------------------------
    /**
     * v / |v| => each component => Q24.8
     * If magnitude=0 => zero vector returned.
     */
    public static int[] q24_8_normalize(int[] v) {
        int mag = q24_8_magnitude(v); // Q24.8
        if (mag == 0) {
            int[] zeroV = new int[v.length];
            return zeroV;
        }
        return q24_8_div(v, mag);
    }

    // -------------------------------------
    // Angle Between Two Vectors
    // -------------------------------------
    /**
     * angleBetweenVectors(v1, v2) = acos( dot / (mag1*mag2) ), returns Q24.8 *radians*.
     * If you want degrees => toFloat(angle)*180/PI.
     */
    public static int angleBetweenVectors(int[] v1, int[] v2) {
        int dot  = q24_8_dotProduct(v1, v2); // Q24.8
        int mag1 = q24_8_magnitude(v1);      // Q24.8
        int mag2 = q24_8_magnitude(v2);      // Q24.8

        if (mag1 == 0 || mag2 == 0) {
            return 0; // undefined angle => return 0
        }

        // cosAngle = (dot << 8)/(mag1*mag2) => Q24.8
        long numerator = ((long) dot) << Q24_8_SHIFT;
        long denom = (long) mag1 * (long) mag2;
        if (denom == 0) {
            return 0;
        }
        long cosFix = numerator / denom;

        // optional: clamp to [-1..1] in Q24.8
        long limit = (1 << Q24_8_SHIFT);
        if (cosFix >  limit) cosFix =  limit;
        if (cosFix < -limit) cosFix = -limit;

        return FixedTrigMath.acos((int) cosFix);
    }

    // -------------------------------------
    // Optional: angleBetweenNormalized(v1, v2)
    // -------------------------------------
    /**
     * If you prefer normalizing each vector first, you can do:
     * angleBetweenNormalized(v1, v2).
     * This is rarely needed once magnitude is correct, but we show it for completeness.
     */
    public static int angleBetweenNormalized(int[] v1, int[] v2) {
        // 1) normalize each
        int[] vn1 = q24_8_normalize(v1);
        int[] vn2 = q24_8_normalize(v2);

        // 2) dot => Q24.8, clamp => acos
        int dotN = q24_8_dotProduct(vn1, vn2);
        // clamp to [-1..1]
        if (dotN >  Q24_8_SCALE) dotN =  Q24_8_SCALE;
        if (dotN < -Q24_8_SCALE) dotN = -Q24_8_SCALE;

        return FixedTrigMath.acos(dotN);
    }

    // Prevent instantiation
    private FixedVecMath() {}
}
