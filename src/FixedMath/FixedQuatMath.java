package FixedMath;

import java.util.Vector;

public final class FixedQuatMath {

    // Pool for quaternion arrays (each of length 4)
    private static final Vector quatPool = new Vector();
    private static final int MAX_IDLE_QUAT_POOL_SIZE = 1024;

    public static synchronized int[] acquireQuaternion() {
        if (!quatPool.isEmpty()) {
            int size = quatPool.size();
            int[] q = (int[]) quatPool.elementAt(size - 1);
            quatPool.removeElementAt(size - 1);
            return q;
        }
        return new int[4];
    }

    public static synchronized void releaseQuaternion(int[] q) {
        if (q == null || q.length != 4) {
            return;
        }
        quatPool.addElement(q);
        while (quatPool.size() > MAX_IDLE_QUAT_POOL_SIZE) {
            quatPool.removeElementAt(quatPool.size() - 1);
        }
    }

    public static int[] fromAxisAngle(int[] axis, int angle) {
        int halfAngle = angle >> 1;
        int sinHalf = FixedTrigMath.sin(halfAngle);
        int cosHalf = FixedTrigMath.cos(halfAngle);
        int[] normAxis = FixedVecMath.normalize(axis);
        int x = FixedBaseMath.fixedMul(normAxis[0], sinHalf);
        int y = FixedBaseMath.fixedMul(normAxis[1], sinHalf);
        int z = FixedBaseMath.fixedMul(normAxis[2], sinHalf);
        int w = cosHalf;
        int[] q = acquireQuaternion();
        q[0] = x;
        q[1] = y;
        q[2] = z;
        q[3] = w;
        FixedVecMath.releaseVector(normAxis);
        return q;
    }

    public static int[] multiply(int[] q1, int[] q2) {
        int x1 = q1[0], y1 = q1[1], z1 = q1[2], w1 = q1[3];
        int x2 = q2[0], y2 = q2[1], z2 = q2[2], w2 = q2[3];

        int x = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, x2),
                        FixedBaseMath.fixedMul(x1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(y1, z2),
                        FixedBaseMath.fixedMul(z1, y2)));

        int y = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, y2),
                        FixedBaseMath.fixedMul(y1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(z1, x2),
                        FixedBaseMath.fixedMul(x1, z2)));

        int z = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, z2),
                        FixedBaseMath.fixedMul(z1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(x1, y2),
                        FixedBaseMath.fixedMul(y1, x2)));

        int w = FixedBaseMath.fixedSub(
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(w1, w2),
                        FixedBaseMath.fixedMul(x1, x2)),
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(y1, y2),
                        FixedBaseMath.fixedMul(z1, z2)));

        int[] result = acquireQuaternion();
        result[0] = x;
        result[1] = y;
        result[2] = z;
        result[3] = w;
        return result;
    }

    public static int[] normalize(int[] q) {
        long sumSq = (long) q[0] * q[0] + (long) q[1] * q[1] +
                     (long) q[2] * q[2] + (long) q[3] * q[3];
        int mag = FixedBaseMath.sqrt((int)(sumSq >> FixedBaseMath.FIXED_SHIFT));
        if (mag == 0) {
            int[] identity = acquireQuaternion();
            identity[0] = 0;
            identity[1] = 0;
            identity[2] = 0;
            identity[3] = FixedBaseMath.toFixed(1.0f);
            return identity;
        }
        int normX = FixedBaseMath.fixedDiv(q[0], mag);
        int normY = FixedBaseMath.fixedDiv(q[1], mag);
        int normZ = FixedBaseMath.fixedDiv(q[2], mag);
        int normW = FixedBaseMath.fixedDiv(q[3], mag);
        int[] result = acquireQuaternion();
        result[0] = normX;
        result[1] = normY;
        result[2] = normZ;
        result[3] = normW;
        return result;
    }

    public static int[] conjugate(int[] q) {
        int[] result = acquireQuaternion();
        result[0] = -q[0];
        result[1] = -q[1];
        result[2] = -q[2];
        result[3] = q[3];
        return result;
    }

    public static int[] toRotationMatrix(int[] q) {
        int x = q[0], y = q[1], z = q[2], w = q[3];
        int xx = FixedBaseMath.fixedMul(x, x);
        int yy = FixedBaseMath.fixedMul(y, y);
        int zz = FixedBaseMath.fixedMul(z, z);
        int xy = FixedBaseMath.fixedMul(x, y);
        int xz = FixedBaseMath.fixedMul(x, z);
        int yz = FixedBaseMath.fixedMul(y, z);
        int wx = FixedBaseMath.fixedMul(w, x);
        int wy = FixedBaseMath.fixedMul(w, y);
        int wz = FixedBaseMath.fixedMul(w, z);
        int two = FixedBaseMath.toFixed(2.0f);
        int one = FixedBaseMath.toFixed(1.0f);

        int[] m = FixedMatMath.acquireMatrix();
        m[0]  = one - FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(yy, zz));
        m[1]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedSub(xy, wz));
        m[2]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(xz, wy));
        m[3]  = 0;
        m[4]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(xy, wz));
        m[5]  = one - FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(xx, zz));
        m[6]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedSub(yz, wx));
        m[7]  = 0;
        m[8]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedSub(xz, wy));
        m[9]  = FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(yz, wx));
        m[10] = one - FixedBaseMath.fixedMul(two, FixedBaseMath.fixedAdd(xx, yy));
        m[11] = 0;
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = one;
        return m;
    }

    private FixedQuatMath() {
    }
}
