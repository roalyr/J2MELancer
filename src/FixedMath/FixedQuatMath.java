package FixedMath;

import java.util.Vector;

public final class FixedQuatMath {

    private static final Vector quatPool = new Vector();
    private static final int MAX_IDLE_QUAT_POOL_SIZE = 1024;

    public static synchronized long[] acquireQuaternion() {
        if (!quatPool.isEmpty()) {
            int size = quatPool.size();
            long[] q = (long[]) quatPool.elementAt(size - 1);
            quatPool.removeElementAt(size - 1);
            return q;
        }
        return new long[4];
    }

    public static synchronized void releaseQuaternion(long[] q) {
        if (q == null || q.length != 4) {
            return;
        }
        quatPool.addElement(q);
        while (quatPool.size() > MAX_IDLE_QUAT_POOL_SIZE) {
            quatPool.removeElementAt(quatPool.size() - 1);
        }
    }

    public static long[] fromAxisAngle(long[] axis, long angle) {
        long halfAngle = angle >> 1;
        long sinHalf = FixedTrigMath.sin(halfAngle);
        long cosHalf = FixedTrigMath.cos(halfAngle);
        long[] normAxis = FixedVecMath.normalize(axis);
        long x = FixedBaseMath.fixedMul(normAxis[0], sinHalf);
        long y = FixedBaseMath.fixedMul(normAxis[1], sinHalf);
        long z = FixedBaseMath.fixedMul(normAxis[2], sinHalf);
        long w = cosHalf;
        long[] q = acquireQuaternion();
        q[0] = x;
        q[1] = y;
        q[2] = z;
        q[3] = w;
        FixedVecMath.releaseVector(normAxis);
        return q;
    }

    public static long[] multiply(long[] q1, long[] q2) {
        long x1 = q1[0], y1 = q1[1], z1 = q1[2], w1 = q1[3];
        long x2 = q2[0], y2 = q2[1], z2 = q2[2], w2 = q2[3];

        long x = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, x2),
                        FixedBaseMath.fixedMul(x1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(y1, z2),
                        FixedBaseMath.fixedMul(z1, y2)));

        long y = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, y2),
                        FixedBaseMath.fixedMul(y1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(z1, x2),
                        FixedBaseMath.fixedMul(x1, z2)));

        long z = FixedBaseMath.fixedAdd(
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(w1, z2),
                        FixedBaseMath.fixedMul(z1, w2)),
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(x1, y2),
                        FixedBaseMath.fixedMul(y1, x2)));

        long w = FixedBaseMath.fixedSub(
                    FixedBaseMath.fixedSub(
                        FixedBaseMath.fixedMul(w1, w2),
                        FixedBaseMath.fixedMul(x1, x2)),
                    FixedBaseMath.fixedAdd(
                        FixedBaseMath.fixedMul(y1, y2),
                        FixedBaseMath.fixedMul(z1, z2)));

        long[] result = acquireQuaternion();
        result[0] = x;
        result[1] = y;
        result[2] = z;
        result[3] = w;
        return result;
    }

    public static long[] normalize(long[] q) {
        long sumSq = q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3];
        long mag = FixedBaseMath.sqrt(sumSq >> FixedBaseMath.FIXED_SHIFT);
        if (mag == 0) {
            long[] identity = acquireQuaternion();
            identity[0] = 0;
            identity[1] = 0;
            identity[2] = 0;
            identity[3] = FixedBaseMath.toFixed(1.0f);
            return identity;
        }
        long normX = FixedBaseMath.fixedDiv(q[0], mag);
        long normY = FixedBaseMath.fixedDiv(q[1], mag);
        long normZ = FixedBaseMath.fixedDiv(q[2], mag);
        long normW = FixedBaseMath.fixedDiv(q[3], mag);
        long[] result = acquireQuaternion();
        result[0] = normX;
        result[1] = normY;
        result[2] = normZ;
        result[3] = normW;
        return result;
    }

    public static long[] conjugate(long[] q) {
        long[] result = acquireQuaternion();
        result[0] = -q[0];
        result[1] = -q[1];
        result[2] = -q[2];
        result[3] = q[3];
        return result;
    }

    public static long[] toRotationMatrix(long[] q) {
        long x = q[0], y = q[1], z = q[2], w = q[3];
        long xx = FixedBaseMath.fixedMul(x, x);
        long yy = FixedBaseMath.fixedMul(y, y);
        long zz = FixedBaseMath.fixedMul(z, z);
        long xy = FixedBaseMath.fixedMul(x, y);
        long xz = FixedBaseMath.fixedMul(x, z);
        long yz = FixedBaseMath.fixedMul(y, z);
        long wx = FixedBaseMath.fixedMul(w, x);
        long wy = FixedBaseMath.fixedMul(w, y);
        long wz = FixedBaseMath.fixedMul(w, z);
        long two = FixedBaseMath.toFixed(2.0f);
        long one = FixedBaseMath.toFixed(1.0f);

        long[] m = FixedMatMath.acquireMatrix();
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
