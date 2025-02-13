package FixedMath;

import java.util.Hashtable;
import java.util.Vector;

public final class FixedMatMath {

    private static final Hashtable pool = new Hashtable();
    private static final int MAX_IDLE_POOL_SIZE = 1024;

    public static synchronized long[] acquireMatrix() {
        if (!pool.isEmpty()) {
            Vector vecPool = (Vector) pool.get(new Integer(16));
            if (vecPool != null && !vecPool.isEmpty()) {
                int index = vecPool.size() - 1;
                long[] m = (long[]) vecPool.elementAt(index);
                vecPool.removeElementAt(index);
                return m;
            }
        }
        return new long[16];
    }

    public static synchronized void releaseMatrix(long[] m) {
        if (m == null || m.length != 16) {
            return;
        }
        Integer key = new Integer(16);
        Vector vecPool = (Vector) pool.get(key);
        if (vecPool == null) {
            vecPool = new Vector();
            pool.put(key, vecPool);
        }
        vecPool.addElement(m);
        while (vecPool.size() > MAX_IDLE_POOL_SIZE) {
            vecPool.removeElementAt(vecPool.size() - 1);
        }
    }

    public static long[] createIdentity4x4() {
        long[] m = acquireMatrix();
        for (int i = 0; i < 16; i++) {
            m[i] = 0;
        }
        m[0] = FixedBaseMath.FIXED1;
        m[5] = FixedBaseMath.FIXED1;
        m[10] = FixedBaseMath.FIXED1;
        m[15] = FixedBaseMath.FIXED1;
        return m;
    }

    public static long[] createTranslation4x4(long tx, long ty, long tz) {
        long[] m = createIdentity4x4();
        m[3] = tx;
        m[7] = ty;
        m[11] = tz;
        return m;
    }

    public static long[] createScale4x4(long sx, long sy, long sz) {
        long[] m = createIdentity4x4();
        m[0] = sx;
        m[5] = sy;
        m[10] = sz;
        return m;
    }

    public static long[] createRotationX4x4(long angle) {
        long sin = FixedTrigMath.sin(angle);
        long cos = FixedTrigMath.cos(angle);
        long[] m = createIdentity4x4();
        m[5] = cos;
        m[6] = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, sin);
        m[9] = sin;
        m[10] = cos;
        return m;
    }

    public static long[] createRotationY4x4(long angle) {
        long sin = FixedTrigMath.sin(angle);
        long cos = FixedTrigMath.cos(angle);
        long[] m = createIdentity4x4();
        m[0] = cos;
        m[2] = sin;
        m[8] = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, sin);
        m[10] = cos;
        return m;
    }

    public static long[] createRotationZ4x4(long angle) {
        long sin = FixedTrigMath.sin(angle);
        long cos = FixedTrigMath.cos(angle);
        long[] m = createIdentity4x4();
        m[0] = cos;
        m[1] = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, sin);
        m[4] = sin;
        m[5] = cos;
        return m;
    }

    public static long[] createRotationAroundAxis4x4(long[] axis, long angle) {
        long[] nAxis = FixedVecMath.normalize(axis);
        long x = nAxis[0];
        long y = nAxis[1];
        long z = nAxis[2];

        long cos = FixedTrigMath.cos(angle);
        long sin = FixedTrigMath.sin(angle);
        long t = FixedBaseMath.FIXED1 - cos;

        long[] m = acquireMatrix();
        m[0] = cos + FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(x, x));
        m[1] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(x, y)) - FixedBaseMath.fixedMul(sin, z);
        m[2] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(x, z)) + FixedBaseMath.fixedMul(sin, y);
        m[3] = 0;
        m[4] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(x, y)) + FixedBaseMath.fixedMul(sin, z);
        m[5] = cos + FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(y, y));
        m[6] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(y, z)) - FixedBaseMath.fixedMul(sin, x);
        m[7] = 0;
        m[8] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(x, z)) - FixedBaseMath.fixedMul(sin, y);
        m[9] = FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(y, z)) + FixedBaseMath.fixedMul(sin, x);
        m[10] = cos + FixedBaseMath.fixedMul(t, FixedBaseMath.fixedMul(z, z));
        m[11] = 0;
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = FixedBaseMath.FIXED1;
        return m;
    }

    public static long[] multiply4x4(long[] m1, long[] m2) {
        long[] r = acquireMatrix();
        for (int row = 0; row < 4; row++) {
            int rBase = row * 4;
            long m1_0 = m1[rBase + 0];
            long m1_1 = m1[rBase + 1];
            long m1_2 = m1[rBase + 2];
            long m1_3 = m1[rBase + 3];
            long sum0 = m1_0 * m2[0] + m1_1 * m2[4] + m1_2 * m2[8] + m1_3 * m2[12];
            long sum1 = m1_0 * m2[1] + m1_1 * m2[5] + m1_2 * m2[9] + m1_3 * m2[13];
            long sum2 = m1_0 * m2[2] + m1_1 * m2[6] + m1_2 * m2[10] + m1_3 * m2[14];
            long sum3 = m1_0 * m2[3] + m1_1 * m2[7] + m1_2 * m2[11] + m1_3 * m2[15];
            r[rBase + 0] = sum0 >> FixedBaseMath.FIXED_SHIFT;
            r[rBase + 1] = sum1 >> FixedBaseMath.FIXED_SHIFT;
            r[rBase + 2] = sum2 >> FixedBaseMath.FIXED_SHIFT;
            r[rBase + 3] = sum3 >> FixedBaseMath.FIXED_SHIFT;
        }
        return r;
    }

    public static void transformPoint(long[] m4x4, long[] xyz, long[] out) {
        long w;
        if (xyz.length == 3) {
            w = FixedBaseMath.FIXED1;
        } else {
            w = xyz[3];
        }
        for (int row = 0; row < 4; row++) {
            int base = row * 4;
            long sum = m4x4[base + 0] * xyz[0] +
                       m4x4[base + 1] * xyz[1] +
                       m4x4[base + 2] * xyz[2] +
                       m4x4[base + 3] * w;
            out[row] = sum >> FixedBaseMath.FIXED_SHIFT;
        }
    }

    public static long[] transformVector4x4(long[] m, long[] xyz) {
        long[] out3 = new long[3];
        for (int row = 0; row < 3; row++) {
            int base = row * 4;
            long sum = m[base + 0] * xyz[0] +
                       m[base + 1] * xyz[1] +
                       m[base + 2] * xyz[2];
            out3[row] = sum >> FixedBaseMath.FIXED_SHIFT;
        }
        return out3;
    }

    public static long[] createLookAt4x4(long[] eye, long[] target, long[] up) {
        long[] eyeL = new long[eye.length];
        long[] targetL = new long[target.length];
        long[] upL = new long[up.length];
        for (int i = 0; i < eye.length; i++) {
            eyeL[i] = eye[i];
        }
        for (int i = 0; i < target.length; i++) {
            targetL[i] = target[i];
        }
        for (int i = 0; i < up.length; i++) {
            upL[i] = up[i];
        }
        long[] zAxis = FixedVecMath.fixedSub(eyeL, targetL);
        zAxis = FixedVecMath.normalize(zAxis);
        long[] xAxis = FixedVecMath.fixedCrossProduct(upL, zAxis);
        xAxis = FixedVecMath.normalize(xAxis);
        long[] yAxis = FixedVecMath.fixedCrossProduct(zAxis, xAxis);
        long[] m = acquireMatrix();
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[3] = 0;
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[7] = 0;
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[11] = 0;
        long negDotX = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, FixedVecMath.fixedDotProduct(xAxis, eyeL));
        long negDotY = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, FixedVecMath.fixedDotProduct(yAxis, eyeL));
        long negDotZ = FixedBaseMath.fixedMul(FixedBaseMath.FIXEDNEG1, FixedVecMath.fixedDotProduct(zAxis, eyeL));
        m[12] = negDotX;
        m[13] = negDotY;
        m[14] = negDotZ;
        m[15] = FixedBaseMath.FIXED1;
        return m;
    }

    public static long[] createPerspective4x4(long fovY, long aspect, long near, long far) {

        long factor = FixedTrigMath.RADFACTOR;
        long halfFov_deg = fovY >> 1;
        long halfFov_rad = FixedBaseMath.fixedMul(halfFov_deg, factor);
        long sinHalf = FixedTrigMath.sin(halfFov_rad);
        long cosHalf = FixedTrigMath.cos(halfFov_rad);
        long tanHalf = FixedBaseMath.fixedDiv(sinHalf, cosHalf);
        long top = FixedBaseMath.fixedMul(near, tanHalf);
        //long bottom = -top;
        long right = FixedBaseMath.fixedMul(top, aspect);
        //long left = -right;
        long twoN = FixedBaseMath.fixedMul(near, FixedBaseMath.FIXED2);
        long rl = right << 1;
        long tb = top << 1;
        long fn = far - near;
        long A = FixedBaseMath.fixedDiv(twoN, rl);
        long B = 0;
        long C = FixedBaseMath.fixedDiv(twoN, tb);
        long D = 0;
        long sumFarNear = far + near;
        long E = -FixedBaseMath.fixedDiv(sumFarNear, fn);
        long productFarNear = FixedBaseMath.fixedMul(far, near);
        long twoProduct = FixedBaseMath.fixedMul(productFarNear, FixedBaseMath.FIXED2);
        long F_val = -FixedBaseMath.fixedDiv(twoProduct, fn);
        long[] m = acquireMatrix();
        m[0]  = A;
        m[1]  = 0;
        m[2]  = B;
        m[3]  = 0;
        m[4]  = 0;
        m[5]  = C;
        m[6]  = D;
        m[7]  = 0;
        m[8]  = 0;
        m[9]  = 0;
        m[10] = E;
        m[11] = F_val;
        m[12] = 0;
        m[13] = 0;
        m[14] = -FixedBaseMath.FIXED1;
        m[15] = 0;
        return m;
    }

    public static void printMatrix(long[] m) {
        for (int i = 0; i < 4; i++) {
            System.out.print("[");
            for (int j = 0; j < 4; j++) {
                System.out.print(FixedBaseMath.toFloat(m[i * 4 + j]));
                if (j < 3) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }

    public static long[] transpose(long[] m) {
        long[] t = acquireMatrix();
        t[0] = m[0];
        t[1] = m[4];
        t[2] = m[8];
        t[3] = m[12];
        t[4] = m[1];
        t[5] = m[5];
        t[6] = m[9];
        t[7] = m[13];
        t[8] = m[2];
        t[9] = m[6];
        t[10] = m[10];
        t[11] = m[14];
        t[12] = m[3];
        t[13] = m[7];
        t[14] = m[11];
        t[15] = m[15];
        return t;
    }

    private FixedMatMath() {
    }
}
