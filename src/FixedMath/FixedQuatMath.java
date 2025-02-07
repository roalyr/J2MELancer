package FixedMath;

public final class FixedQuatMath {

    /**
     * Creates a quaternion from an axis-angle representation.
     * The axis is a 3-element vector in Q24.8 and angleQ24_8 is the rotation angle (in Q24.8 radians).
     * The resulting quaternion is in the form [x, y, z, w] in Q24.8.
     */
    public static int[] fromAxisAngle(int[] axis, int angleQ24_8) {
        // Compute half the angle in Q24.8.
        int halfAngle = angleQ24_8 >> 1; // Divide by 2.
        int sinHalf = FixedTrigMath.sin(halfAngle);
        int cosHalf = FixedTrigMath.cos(halfAngle);
        // Normalize the axis.
        int[] normAxis = FixedVecMath.q24_8_normalize(axis);
        int x = FixedBaseMath.q24_8_mul(normAxis[0], sinHalf);
        int y = FixedBaseMath.q24_8_mul(normAxis[1], sinHalf);
        int z = FixedBaseMath.q24_8_mul(normAxis[2], sinHalf);
        int w = cosHalf;
        return new int[]{ x, y, z, w };
    }

    /**
     * Multiplies two quaternions.
     * Both q1 and q2 are arrays of length 4 representing quaternions in Q24.8: [x, y, z, w].
     * The multiplication is defined as:
     *   q = q1 * q2 = [w1*x2 + x1*w2 + y1*z2 - z1*y2,
     *                   w1*y2 - x1*z2 + y1*w2 + z1*x2,
     *                   w1*z2 + x1*y2 - y1*x2 + z1*w2,
     *                   w1*w2 - x1*x2 - y1*y2 - z1*z2]
     */
    public static int[] multiply(int[] q1, int[] q2) {
        int x1 = q1[0], y1 = q1[1], z1 = q1[2], w1 = q1[3];
        int x2 = q2[0], y2 = q2[1], z2 = q2[2], w2 = q2[3];

        int x = FixedBaseMath.q24_8_add(
                    FixedBaseMath.q24_8_add(
                        FixedBaseMath.q24_8_mul(w1, x2),
                        FixedBaseMath.q24_8_mul(x1, w2)),
                    FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(y1, z2),
                        FixedBaseMath.q24_8_mul(z1, y2)));
                        
        int y = FixedBaseMath.q24_8_add(
                    FixedBaseMath.q24_8_add(
                        FixedBaseMath.q24_8_mul(w1, y2),
                        FixedBaseMath.q24_8_mul(y1, w2)),
                    FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(z1, x2),
                        FixedBaseMath.q24_8_mul(x1, z2)));
                        
        int z = FixedBaseMath.q24_8_add(
                    FixedBaseMath.q24_8_add(
                        FixedBaseMath.q24_8_mul(w1, z2),
                        FixedBaseMath.q24_8_mul(z1, w2)),
                    FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(x1, y2),
                        FixedBaseMath.q24_8_mul(y1, x2)));
                        
        int w = FixedBaseMath.q24_8_sub(
                    FixedBaseMath.q24_8_sub(
                        FixedBaseMath.q24_8_mul(w1, w2),
                        FixedBaseMath.q24_8_mul(x1, x2)),
                    FixedBaseMath.q24_8_add(
                        FixedBaseMath.q24_8_mul(y1, y2),
                        FixedBaseMath.q24_8_mul(z1, z2)));
                        
        return new int[]{ x, y, z, w };
    }

    /**
     * Normalizes the quaternion q (array of 4 elements in Q24.8) to unit length.
     */
    public static int[] normalize(int[] q) {
        // Compute the squared magnitude: q[0]^2 + q[1]^2 + q[2]^2 + q[3]^2.
        long sumSq = (long) q[0] * q[0] + (long) q[1] * q[1] +
                     (long) q[2] * q[2] + (long) q[3] * q[3];
        // Convert from Q48.16 to Q24.8 by shifting right by Q24_8_SHIFT.
        int mag = FixedBaseMath.sqrt((int)(sumSq >> FixedBaseMath.Q24_8_SHIFT));
        if (mag == 0) {
            // Return the identity quaternion if magnitude is zero.
            return new int[]{ 0, 0, 0, FixedBaseMath.toQ24_8(1.0f) };
        }
        int normX = FixedBaseMath.q24_8_div(q[0], mag);
        int normY = FixedBaseMath.q24_8_div(q[1], mag);
        int normZ = FixedBaseMath.q24_8_div(q[2], mag);
        int normW = FixedBaseMath.q24_8_div(q[3], mag);
        return new int[]{ normX, normY, normZ, normW };
    }

    /**
     * Converts a normalized quaternion q (array of 4 elements in Q24.8) into a 4x4 rotation matrix.
     * The resulting matrix is in column-major order.
     */
    public static int[] toRotationMatrix(int[] q) {
        int x = q[0], y = q[1], z = q[2], w = q[3];
        int xx = FixedBaseMath.q24_8_mul(x, x);
        int yy = FixedBaseMath.q24_8_mul(y, y);
        int zz = FixedBaseMath.q24_8_mul(z, z);
        int xy = FixedBaseMath.q24_8_mul(x, y);
        int xz = FixedBaseMath.q24_8_mul(x, z);
        int yz = FixedBaseMath.q24_8_mul(y, z);
        int wx = FixedBaseMath.q24_8_mul(w, x);
        int wy = FixedBaseMath.q24_8_mul(w, y);
        int wz = FixedBaseMath.q24_8_mul(w, z);
        int two = FixedBaseMath.toQ24_8(2.0f);
        int one = FixedBaseMath.toQ24_8(1.0f);

        int[] m = new int[16];
        // First row
        m[0]  = one - FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(yy, zz));
        m[1]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_sub(xy, wz));
        m[2]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(xz, wy));
        m[3]  = 0;
        // Second row
        m[4]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(xy, wz));
        m[5]  = one - FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(xx, zz));
        m[6]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_sub(yz, wx));
        m[7]  = 0;
        // Third row
        m[8]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_sub(xz, wy));
        m[9]  = FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(yz, wx));
        m[10] = one - FixedBaseMath.q24_8_mul(two, FixedBaseMath.q24_8_add(xx, yy));
        m[11] = 0;
        // Fourth row
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = one;
        return m;
    }

    /**
     * Returns the conjugate of the quaternion q.
     * For q = [x, y, z, w], the conjugate is [-x, -y, -z, w].
     */
    public static int[] conjugate(int[] q) {
        return new int[]{ -q[0], -q[1], -q[2], q[3] };
    }

    private FixedQuatMath() {
    }
}
