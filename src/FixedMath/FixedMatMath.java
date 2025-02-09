package FixedMath;

import java.util.Vector;

public final class FixedMatMath {

    public static final int Q24_8_SHIFT = 8;
    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    // Convenient Q24.8 constants
    private static final int ONE = FixedBaseMath.toQ24_8(1.0f);
    private static final int NEG_ONE = FixedBaseMath.toQ24_8(-1.0f);

    // ---------------------------
    // Dynamic Object Pool for 4x4 Matrices (using Vector for Java 1.3)
    // ---------------------------
    private static final Vector matrixPool = new Vector();
    // Maximum number of idle matrices to keep in the pool.
    private static final int MAX_IDLE_POOL_SIZE = 1024;

    /**
     * Acquires a 4x4 matrix (an int[16]) from the pool if available,
     * or creates a new one if not.
     */
    public static synchronized int[] acquireMatrix() {
        if (!matrixPool.isEmpty()) {
            int size = matrixPool.size();
            int[] m = (int[]) matrixPool.elementAt(size - 1);
            matrixPool.removeElementAt(size - 1);
            return m;
        }
        return new int[16];
    }

    /**
     * Releases a 4x4 matrix (an int[16]) back to the pool for reuse.
     * After adding, the pool is trimmed if it exceeds MAX_IDLE_POOL_SIZE.
     */
    public static synchronized void releaseMatrix(int[] m) {
        if (m == null || m.length != 16) {
            return;
        }
        matrixPool.addElement(m);
        while (matrixPool.size() > MAX_IDLE_POOL_SIZE) {
            matrixPool.removeElementAt(matrixPool.size() - 1);
        }
    }

    // ---------------------------
    // Identity Matrix
    // ---------------------------
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

    // ---------------------------
    // Translation Matrix (4th column)
    // ---------------------------
    public static int[] createTranslation4x4(int tx, int ty, int tz) {
        int[] m = createIdentity4x4();
        m[3] = tx;
        m[7] = ty;
        m[11] = tz;
        return m;
    }

    // ---------------------------
    // Scale Matrix (diagonal)
    // ---------------------------
    public static int[] createScale4x4(int sx, int sy, int sz) {
        int[] m = createIdentity4x4();
        m[0] = sx;
        m[5] = sy;
        m[10] = sz;
        return m;
    }

    // ---------------------------
    // Rotation Matrix about X-axis
    // ---------------------------
    public static int[] createRotationX4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m = createIdentity4x4();
        m[5] = cosQ;
        m[6] = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);
        m[9] = sinQ;
        m[10] = cosQ;
        return m;
    }

    // ---------------------------
    // Rotation Matrix about Y-axis
    // ---------------------------
    public static int[] createRotationY4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m = createIdentity4x4();
        m[0] = cosQ;
        m[2] = sinQ;
        m[8] = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);
        m[10] = cosQ;
        return m;
    }

    // ---------------------------
    // Rotation Matrix about Z-axis
    // ---------------------------
    public static int[] createRotationZ4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m = createIdentity4x4();
        m[0] = cosQ;
        m[1] = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);
        m[4] = sinQ;
        m[5] = cosQ;
        return m;
    }

    // ---------------------------
    // Create Rotation Matrix about an Arbitrary Axis
    // ---------------------------
    public static int[] createRotationAroundAxis4x4(int[] axis, int angleQ24_8) {
        int[] nAxis = FixedVecMath.q24_8_normalize(axis);
        int x = nAxis[0];
        int y = nAxis[1];
        int z = nAxis[2];

        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int t = ONE - cosQ;

        int[] m = acquireMatrix();
        // First row
        m[0] = cosQ + FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(x, x));
        m[1] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(x, y)) - FixedBaseMath.q24_8_mul(sinQ, z);
        m[2] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(x, z)) + FixedBaseMath.q24_8_mul(sinQ, y);
        m[3] = 0;
        // Second row
        m[4] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(x, y)) + FixedBaseMath.q24_8_mul(sinQ, z);
        m[5] = cosQ + FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(y, y));
        m[6] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(y, z)) - FixedBaseMath.q24_8_mul(sinQ, x);
        m[7] = 0;
        // Third row
        m[8] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(x, z)) - FixedBaseMath.q24_8_mul(sinQ, y);
        m[9] = FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(y, z)) + FixedBaseMath.q24_8_mul(sinQ, x);
        m[10] = cosQ + FixedBaseMath.q24_8_mul(t, FixedBaseMath.q24_8_mul(z, z));
        m[11] = 0;
        // Fourth row
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = ONE;
        return m;
    }

    // ---------------------------
    // 4x4 Matrix Multiplication (Unrolled)
    // ---------------------------
    public static int[] multiply4x4(int[] m1, int[] m2) {
        int[] r = acquireMatrix();
        for (int row = 0; row < 4; row++) {
            int rBase = row * 4;
            int m1_0 = m1[rBase + 0];
            int m1_1 = m1[rBase + 1];
            int m1_2 = m1[rBase + 2];
            int m1_3 = m1[rBase + 3];
            long sum0 = (long) m1_0 * m2[0] + (long) m1_1 * m2[4] + (long) m1_2 * m2[8] + (long) m1_3 * m2[12];
            long sum1 = (long) m1_0 * m2[1] + (long) m1_1 * m2[5] + (long) m1_2 * m2[9] + (long) m1_3 * m2[13];
            long sum2 = (long) m1_0 * m2[2] + (long) m1_1 * m2[6] + (long) m1_2 * m2[10] + (long) m1_3 * m2[14];
            long sum3 = (long) m1_0 * m2[3] + (long) m1_1 * m2[7] + (long) m1_2 * m2[11] + (long) m1_3 * m2[15];
            r[rBase + 0] = (int) (sum0 >> Q24_8_SHIFT);
            r[rBase + 1] = (int) (sum1 >> Q24_8_SHIFT);
            r[rBase + 2] = (int) (sum2 >> Q24_8_SHIFT);
            r[rBase + 3] = (int) (sum3 >> Q24_8_SHIFT);
        }
        return r;
    }

    /**
     * Transforms a point using a 4x4 matrix.
     * Supports both 3-element (x, y, z) arrays (assumed w = 1) and 4-element arrays.
     * The result is stored in the provided output array (must be at least length 4).
     */
    public static void transformPoint(int[] m4x4, int[] xyz, int[] out) {
        int w;
        if (xyz.length == 3) {
            w = FixedBaseMath.toQ24_8(1.0f);
        } else {
            w = xyz[3];
        }
        for (int row = 0; row < 4; row++) {
            int base = row * 4;
            long sum = (long) m4x4[base + 0] * xyz[0] +
                    (long) m4x4[base + 1] * xyz[1] +
                    (long) m4x4[base + 2] * xyz[2] +
                    (long) m4x4[base + 3] * w;
            out[row] = (int) (sum >> Q24_8_SHIFT);
        }
    }

    // ---------------------------
    // Transform a Vector (ignores translation)
    // ---------------------------
    public static int[] transformVector4x4(int[] m, int[] xyz) {
        int[] out3 = new int[3];
        for (int row = 0; row < 3; row++) {
            int base = row * 4;
            long sum = (long) m[base + 0] * xyz[0] +
                    (long) m[base + 1] * xyz[1] +
                    (long) m[base + 2] * xyz[2];
            out3[row] = (int) (sum >> Q24_8_SHIFT);
        }
        return out3;
    }

    // ---------------------------
    // Create LookAt Matrix
    // ---------------------------
    public static int[] createLookAt4x4(int[] eye, int[] target, int[] up) {
        int[] zAxis = FixedVecMath.q24_8_sub(eye, target);
        zAxis = FixedVecMath.q24_8_normalize(zAxis);
        int[] xAxis = FixedVecMath.q24_8_crossProduct(up, zAxis);
        xAxis = FixedVecMath.q24_8_normalize(xAxis);
        int[] yAxis = FixedVecMath.q24_8_crossProduct(zAxis, xAxis);
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
        int negDotX = FixedBaseMath.q24_8_mul(NEG_ONE, FixedVecMath.q24_8_dotProduct(xAxis, eye));
        int negDotY = FixedBaseMath.q24_8_mul(NEG_ONE, FixedVecMath.q24_8_dotProduct(yAxis, eye));
        int negDotZ = FixedBaseMath.q24_8_mul(NEG_ONE, FixedVecMath.q24_8_dotProduct(zAxis, eye));
        m[12] = negDotX;
        m[13] = negDotY;
        m[14] = negDotZ;
        m[15] = ONE;
        return m;
    }

    // ---------------------------
    // Create Perspective Projection Matrix
    // ---------------------------
    public static int[] createPerspective4x4(int fovY_Q24_8, int aspect_Q24_8, int near_Q24_8, int far_Q24_8) {
        float fovY = FixedBaseMath.toFloat(fovY_Q24_8);
        float aspect = FixedBaseMath.toFloat(aspect_Q24_8);
        float nearF = FixedBaseMath.toFloat(near_Q24_8);
        float farF = FixedBaseMath.toFloat(far_Q24_8);

        float halfFov = (float) Math.toRadians(fovY / 2.0f);
        float topF = nearF * (float) Math.tan(halfFov);
        float bottomF = -topF;
        float rightF = topF * aspect;
        float leftF = -rightF;
        float twoN = 2.0f * nearF;
        float rl = rightF - leftF;
        float tb = topF - bottomF;
        float fn = farF - nearF;

        float A = twoN / rl;
        float B = (rightF + leftF) / rl;
        float C = twoN / tb;
        float D = (topF + bottomF) / tb;
        float E = -(farF + nearF) / fn;
        float F = -(2.0f * farF * nearF) / fn;

        int[] m = acquireMatrix();
        m[0] = FixedBaseMath.toQ24_8(A);
        m[1] = 0;
        m[2] = FixedBaseMath.toQ24_8(B);
        m[3] = 0;
        m[4] = 0;
        m[5] = FixedBaseMath.toQ24_8(C);
        m[6] = FixedBaseMath.toQ24_8(D);
        m[7] = 0;
        m[8] = 0;
        m[9] = 0;
        m[10] = FixedBaseMath.toQ24_8(E);
        m[11] = FixedBaseMath.toQ24_8(F);
        m[12] = 0;
        m[13] = 0;
        m[14] = FixedBaseMath.toQ24_8(-1.0f);
        m[15] = 0;
        return m;
    }

    // ---------------------------
    // Print Matrix (for debugging)
    // ---------------------------
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

    // ---------------------------
    // Transpose Matrix
    // ---------------------------
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