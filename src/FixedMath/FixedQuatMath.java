package FixedMath;

import java.util.Vector;

public final class FixedQuatMath {

    // Pool for quaternion arrays (each of length 4)
    private static final Vector quatPool = new Vector();
    private static final int MAX_IDLE_QUAT_POOL_SIZE = 1024;

    /**
     * Acquires a quaternion (an int[4]) from the pool if available,
     * or creates a new one.
     */
    public static synchronized int[] acquireQuaternion() {
        if (!quatPool.isEmpty()) {
            int size = quatPool.size();
            int[] q = (int[]) quatPool.elementAt(size - 1);
            quatPool.removeElementAt(size - 1);
            return q;
        }
        return new int[4];
    }

    /**
     * Releases a quaternion (an int[4]) back to the pool.
     * Extra quaternions beyond MAX_IDLE_QUAT_POOL_SIZE are trimmed.
     */
    public static synchronized void releaseQuaternion(int[] q) {
        if (q == null || q.length != 4) {
            return;
        }
        quatPool.addElement(q);
        while (quatPool.size() > MAX_IDLE_QUAT_POOL_SIZE) {
            quatPool.removeElementAt(quatPool.size() - 1);
        }
    }

    /**
     * Creates a quaternion from an axis-angle representation.
     * The axis is a 3-element vector in Q24.8 and angleQ24_8 is the rotation angle (in Q24.8 radians).
     * The resulting quaternion is in the form [x, y, z, w] in Q24.8.
     * (The caller must later release the returned quaternion using releaseQuaternion().)
     */
    public static int[] fromAxisAngle(int[] axis, int angleQ24_8) {
        // Compute half the angle in Q24.8.
        int halfAngle = angleQ24_8 >> 1; // Divide by 2.
        int sinHalf = FixedTrigMath.sin(halfAngle);
        int cosHalf = FixedTrigMath.cos(halfAngle);
        // Normalize the axis.
        // (Assume FixedVecMath.q24_8_normalize uses its own pooling if implemented)
        int[] normAxis = FixedVecMath.q24_8_normalize(axis);
        int x = FixedBaseMath.q24_8_mul(normAxis[0], sinHalf);
        int y = FixedBaseMath.q24_8_mul(normAxis[1], sinHalf);
        int z = FixedBaseMath.q24_8_mul(normAxis[2], sinHalf);
        int w = cosHalf;
        // (If normAxis was acquired from a pool, the caller of normalize should release it.)
        // Acquire a quaternion for the result.
        int[] q = acquireQuaternion();
        q[0] = x;
        q[1] = y;
        q[2] = z;
        q[3] = w;
        FixedVecMath.releaseVector(normAxis);
        return q;
    }

    /**
     * Multiplies two quaternions.
     * Both q1 and q2 are arrays of length 4 representing quaternions in Q24.8.
     * The returned quaternion is acquired from the pool.
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

        int[] result = acquireQuaternion();
        result[0] = x;
        result[1] = y;
        result[2] = z;
        result[3] = w;
        return result;
    }

    /**
     * Normalizes the quaternion q (an int[4]) to unit length.
     * The returned quaternion is acquired from the pool.
     */
    public static int[] normalize(int[] q) {
        long sumSq = (long) q[0] * q[0] + (long) q[1] * q[1] +
                     (long) q[2] * q[2] + (long) q[3] * q[3];
        int mag = FixedBaseMath.sqrt((int)(sumSq >> FixedBaseMath.Q24_8_SHIFT));
        if (mag == 0) {
            int[] identity = acquireQuaternion();
            identity[0] = 0;
            identity[1] = 0;
            identity[2] = 0;
            identity[3] = FixedBaseMath.toQ24_8(1.0f);
            return identity;
        }
        int normX = FixedBaseMath.q24_8_div(q[0], mag);
        int normY = FixedBaseMath.q24_8_div(q[1], mag);
        int normZ = FixedBaseMath.q24_8_div(q[2], mag);
        int normW = FixedBaseMath.q24_8_div(q[3], mag);
        int[] result = acquireQuaternion();
        result[0] = normX;
        result[1] = normY;
        result[2] = normZ;
        result[3] = normW;
        return result;
    }

    /**
     * Returns the conjugate of the quaternion q.
     * For q = [x, y, z, w], the conjugate is [-x, -y, -z, w].
     * The returned quaternion is acquired from the pool.
     */
    public static int[] conjugate(int[] q) {
        int[] result = acquireQuaternion();
        result[0] = -q[0];
        result[1] = -q[1];
        result[2] = -q[2];
        result[3] = q[3];
        return result;
    }

    /**
     * Converts a normalized quaternion q (an int[4] in Q24.8) into a 4x4 rotation matrix.
     * The resulting matrix is acquired from FixedMatMath’s matrix pool.
     * (The caller is responsible for releasing the returned matrix using FixedMatMath.releaseMatrix().)
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

        int[] m = FixedMatMath.acquireMatrix();
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

    private FixedQuatMath() {
    }
}