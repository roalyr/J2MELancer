package FixedMath;

public final class FixedVecMath {

    public static final int Q24_8_SHIFT = 8;
    public static final int Q24_8_SCALE = (1 << Q24_8_SHIFT);

    // -------------------------------------
    // Vector Add/Sub (Assumes same length)
    // -------------------------------------
    public static int[] q24_8_add(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] + v2[i];  // Inlined addition

        }
        return result;
    }

    public static int[] q24_8_sub(int[] v1, int[] v2) {
        int len = v1.length;
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = v1[i] - v2[i];  // Inlined subtraction

        }
        return result;
    }

    // -------------------------------------
    // Scalar Multiply/Divide
    // -------------------------------------
    public static int[] q24_8_mul(int[] v, int scalar) {
        int len = v.length;
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            // Fixed point multiplication: (v[i] * scalar) >> Q24_8_SHIFT
            long tmp = ((long) v[i] * (long) scalar) >> Q24_8_SHIFT;
            result[i] = (int) tmp;
        }
        return result;
    }

    public static int[] q24_8_div(int[] v, int scalar) {
        int len = v.length;
        int[] result = new int[len];
        // Avoid division by zero
        if (scalar == 0) {
            for (int i = 0; i < len; i++) {
                result[i] = (v[i] >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }
            return result;
        }
        for (int i = 0; i < len; i++) {
            // Fixed point division: (v[i] << Q24_8_SHIFT) / scalar
            result[i] = (int) (((long) v[i] << Q24_8_SHIFT) / scalar);
        }
        return result;
    }

    // -------------------------------------
    // Dot Product
    // -------------------------------------
    /**
     * Returns (v1[0]*v2[0] + ... + v1[n-1]*v2[n-1]) >> Q24_8_SHIFT, 
     * i.e. the dot product in Q24.8.
     */
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
    /**
     * Computes the cross product (v1 x v2) for 3D vectors.
     * Each component is computed in 64-bit and then shifted by Q24_8_SHIFT.
     */
    public static int[] q24_8_crossProduct(int[] v1, int[] v2) {
        int[] result = new int[3];
        result[0] = (int) ((((long) v1[1] * v2[2]) - ((long) v1[2] * v2[1])) >> Q24_8_SHIFT);
        result[1] = (int) ((((long) v1[2] * v2[0]) - ((long) v1[0] * v2[2])) >> Q24_8_SHIFT);
        result[2] = (int) ((((long) v1[0] * v2[1]) - ((long) v1[1] * v2[0])) >> Q24_8_SHIFT);
        return result;
    }

    // -------------------------------------
    // Magnitude (Length)
    // -------------------------------------
    /**
     * Returns the magnitude of vector v.
     * Each component is in Q24.8; the sum of squares (Q48.16) is shifted right by 8 
     * to convert to Q24.8 before taking the square root.
     */
    public static int q24_8_magnitude(int[] v) {
        int len = v.length;
        long sqrSum = 0;
        for (int i = 0; i < len; i++) {
            long val = v[i];
            sqrSum += val * val;
        }
        // Convert Q48.16 sum to Q24.8 by shifting right by 8 bits.
        long q24_8_val = sqrSum >> Q24_8_SHIFT;
        if (q24_8_val > 0x7FFFFFFF) {
            q24_8_val = 0x7FFFFFFF; // Clamp if needed.

        }
        return FixedBaseMath.sqrt((int) q24_8_val);
    }

    // -------------------------------------
    // Normalize
    // -------------------------------------
    /**
     * Returns the normalized vector v (v / |v|) in Q24.8.
     * If the magnitude is zero, returns a zero vector.
     */
    public static int[] q24_8_normalize(int[] v) {
        int mag = q24_8_magnitude(v);
        if (mag == 0) {
            return new int[v.length];  // Zero vector.

        }
        return q24_8_div(v, mag);
    }

    // -------------------------------------
    // Angle Between Two Vectors
    // -------------------------------------
    /**
     * Computes the angle between v1 and v2 in Q24.8 radians.
     * The formula used is acos( dot(v1,v2) / (|v1|*|v2|) ).
     * If either vector has zero magnitude, returns 0.
     */
    public static int angleBetweenVectors(int[] v1, int[] v2) {
        int dot = q24_8_dotProduct(v1, v2);
        int mag1 = q24_8_magnitude(v1);
        int mag2 = q24_8_magnitude(v2);
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        // Compute cosine in Q24.8: (dot << Q24_8_SHIFT) / (mag1 * mag2)
        long numerator = ((long) dot) << Q24_8_SHIFT;
        long denom = (long) mag1 * mag2;
        if (denom == 0) {
            return 0;
        }
        long cosVal = numerator / denom;
        // Clamp to [-1, 1] in Q24.8.
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
    /**
     * Computes the angle between normalized vectors v1 and v2.
     * First normalizes v1 and v2, then computes acos(dot).
     */
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
        return FixedTrigMath.acos(dot);
    }

    // Prevent instantiation.
    private FixedVecMath() {
    }
}