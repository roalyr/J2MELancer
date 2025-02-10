package FixedMath;

import java.util.Vector;

public final class FixedMatMath {

    public static final int FIXED_SHIFT = FixedBaseMath.FIXED_SHIFT;
    public static final int FIXED_SCALE = FixedBaseMath.FIXED_SCALE;

    private static final int ONE = FixedBaseMath.toFixed(1.0f);
    private static final int NEG_ONE = FixedBaseMath.toFixed(-1.0f);

    // Dynamic Object Pool for 4x4 Matrices (using Vector for Java 1.3)
    private static final Vector matrixPool = new Vector();
    private static final int MAX_IDLE_POOL_SIZE = 1024;

    public static synchronized int[] acquireMatrix() {
        if (!matrixPool.isEmpty()) {
            int size = matrixPool.size();
            int[] m = (int[]) matrixPool.elementAt(size - 1);
            matrixPool.removeElementAt(size - 1);
            return m;
        }
        return new int[16];
    }

    public static synchronized void releaseMatrix(int[] m) {
        if (m == null || m.length != 16) {
            return;
        }
        matrixPool.addElement(m);
        while (matrixPool.size() > MAX_IDLE_POOL_SIZE) {
            matrixPool.removeElementAt(matrixPool.size() - 1);
        }
    }

    public static int[] createIdentity4x4() {
        int[] m = acquireMatrix();
        for (int i = 0; i < 16; i++) {
            m[i] = 0;
        }
        m[0] = ONE;
        m[5] = ONE;
        m[10] = ONE;
        m[15] = ONE;
        return m;
    }

    public static int[] createTranslation4x4(int tx, int ty, int tz) {
        int[] m = createIdentity4x4();
        m[3] = tx;
        m[7] = ty;
        m[11] = tz;
        return m;
    }

    public static int[] createScale4x4(int sx, int sy, int sz) {
        int[] m = createIdentity4x4();
        m[0] = sx;
        m[5] = sy;
        m[10] = sz;
        return m;
    }

    public static int[] createRotationX4x4(int angle) {
        int sin = FixedTrigMath.sin(angle);
        int cos = FixedTrigMath.cos(angle);
        int[] m = createIdentity4x4();
        m[5] = cos;
        m[6] = FixedBaseMath.fixedMul(NEG_ONE, sin);
        m[9] = sin;
        m[10] = cos;
        return m;
    }

    public static int[] createRotationY4x4(int angle) {
        int sin = FixedTrigMath.sin(angle);
        int cos = FixedTrigMath.cos(angle);
        int[] m = createIdentity4x4();
        m[0] = cos;
        m[2] = sin;
        m[8] = FixedBaseMath.fixedMul(NEG_ONE, sin);
        m[10] = cos;
        return m;
    }

    public static int[] createRotationZ4x4(int angle) {
        int sin = FixedTrigMath.sin(angle);
        int cos = FixedTrigMath.cos(angle);
        int[] m = createIdentity4x4();
        m[0] = cos;
        m[1] = FixedBaseMath.fixedMul(NEG_ONE, sin);
        m[4] = sin;
        m[5] = cos;
        return m;
    }

    public static int[] createRotationAroundAxis4x4(int[] axis, int angle) {
        int[] nAxis = FixedVecMath.normalize(axis);
        int x = nAxis[0];
        int y = nAxis[1];
        int z = nAxis[2];

        int cos = FixedTrigMath.cos(angle);
        int sin = FixedTrigMath.sin(angle);
        int t = ONE - cos;

        int[] m = acquireMatrix();
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
        m[15] = ONE;
        return m;
    }

    public static int[] multiply4x4(int[] m1, int[] m2) {
        int[] r = acquireMatrix();
        for (int row = 0; row < 4; row++) {
            int rBase = row * 4;
            int m1_0 = m1[rBase + 0];
            int m1_1 = m1[rBase + 1];
            int m1_2 = m1[rBase + 2];
            int m1_3 = m1[rBase + 3];
            long sum0 = (long) m1_0 * m2[0]  + (long) m1_1 * m2[4]  + (long) m1_2 * m2[8]  + (long) m1_3 * m2[12];
            long sum1 = (long) m1_0 * m2[1]  + (long) m1_1 * m2[5]  + (long) m1_2 * m2[9]  + (long) m1_3 * m2[13];
            long sum2 = (long) m1_0 * m2[2]  + (long) m1_1 * m2[6]  + (long) m1_2 * m2[10] + (long) m1_3 * m2[14];
            long sum3 = (long) m1_0 * m2[3]  + (long) m1_1 * m2[7]  + (long) m1_2 * m2[11] + (long) m1_3 * m2[15];
            r[rBase + 0] = (int) (sum0 >> FIXED_SHIFT);
            r[rBase + 1] = (int) (sum1 >> FIXED_SHIFT);
            r[rBase + 2] = (int) (sum2 >> FIXED_SHIFT);
            r[rBase + 3] = (int) (sum3 >> FIXED_SHIFT);
        }
        return r;
    }

    public static void transformPoint(int[] m4x4, int[] xyz, int[] out) {
        int w;
        if (xyz.length == 3) {
            w = FixedBaseMath.toFixed(1.0f);
        } else {
            w = xyz[3];
        }
        for (int row = 0; row < 4; row++) {
            int base = row * 4;
            long sum = (long) m4x4[base + 0] * xyz[0] +
                       (long) m4x4[base + 1] * xyz[1] +
                       (long) m4x4[base + 2] * xyz[2] +
                       (long) m4x4[base + 3] * w;
            out[row] = (int) (sum >> FIXED_SHIFT);
        }
    }

    public static int[] transformVector4x4(int[] m, int[] xyz) {
        int[] out3 = new int[3];
        for (int row = 0; row < 3; row++) {
            int base = row * 4;
            long sum = (long) m[base + 0] * xyz[0] +
                       (long) m[base + 1] * xyz[1] +
                       (long) m[base + 2] * xyz[2];
            out3[row] = (int) (sum >> FIXED_SHIFT);
        }
        return out3;
    }

    public static int[] createLookAt4x4(int[] eye, int[] target, int[] up) {
        int[] zAxis = FixedVecMath.fixedSub(eye, target);
        zAxis = FixedVecMath.normalize(zAxis);
        int[] xAxis = FixedVecMath.fixedCrossProduct(up, zAxis);
        xAxis = FixedVecMath.normalize(xAxis);
        int[] yAxis = FixedVecMath.fixedCrossProduct(zAxis, xAxis);
        int[] m = acquireMatrix();
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
        int negDotX = FixedBaseMath.fixedMul(NEG_ONE, FixedVecMath.fixedDotProduct(xAxis, eye));
        int negDotY = FixedBaseMath.fixedMul(NEG_ONE, FixedVecMath.fixedDotProduct(yAxis, eye));
        int negDotZ = FixedBaseMath.fixedMul(NEG_ONE, FixedVecMath.fixedDotProduct(zAxis, eye));
        m[12] = negDotX;
        m[13] = negDotY;
        m[14] = negDotZ;
        m[15] = ONE;
        return m;
    }

    public static int[] createPerspective4x4(int fovY, int aspect, int near, int far) {
        final int ONE_Q = 1 << FIXED_SHIFT;
        final int TWO_Q = 2 << FIXED_SHIFT;
        final int PI_Q = FixedBaseMath.toFixed(3.14159265f);
        final int DEG180_Q = 180 << FIXED_SHIFT;
        int factor = FixedBaseMath.fixedDiv(PI_Q, DEG180_Q);
        int halfFov_deg = fovY >> 1;
        int halfFov_rad = FixedBaseMath.fixedMul(halfFov_deg, factor);
        int sinHalf = FixedTrigMath.sin(halfFov_rad);
        int cosHalf = FixedTrigMath.cos(halfFov_rad);
        int tanHalf = FixedBaseMath.fixedDiv(sinHalf, cosHalf);
        int top = FixedBaseMath.fixedMul(near, tanHalf);
        int bottom = -top;
        int right = FixedBaseMath.fixedMul(top, aspect);
        int left = -right;
        int twoN = FixedBaseMath.fixedMul(near, TWO_Q);
        int rl = right << 1;
        int tb = top << 1;
        int fn = far - near;
        int A = FixedBaseMath.fixedDiv(twoN, rl);
        int B = 0;
        int C = FixedBaseMath.fixedDiv(twoN, tb);
        int D = 0;
        int sumFarNear = far + near;
        int E = -FixedBaseMath.fixedDiv(sumFarNear, fn);
        int productFarNear = FixedBaseMath.fixedMul(far, near);
        int twoProduct = FixedBaseMath.fixedMul(productFarNear, TWO_Q);
        int F_val = -FixedBaseMath.fixedDiv(twoProduct, fn);
        int[] m = acquireMatrix();
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
        m[14] = -ONE_Q;
        m[15] = 0;
        return m;
    }

    public static void printMatrix(int[] m) {
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

    public static int[] transpose(int[] m) {
        int[] t = acquireMatrix();
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
