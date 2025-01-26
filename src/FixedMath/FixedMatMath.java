package FixedMath;

/**
 * A 4x4 matrix library in Q24.8, using row-major layout:
 *
 *   m[row*4 + col], row=0..3, col=0..3
 *
 * Translation is stored in the 4th column => m[row*4 + 3].
 *
 * So the transform of a point p=(x,y,z,1) is:
 *
 *   out[row] = sum_{col=0..3}( m[row*4 + col] * p[col] ),  Q24.8 multiply => shift>>8
 */
public final class FixedMatMath {

    public static final int Q24_8_SHIFT = 8;
    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    // Some convenient Q24.8 constants:
    private static final int ONE = FixedBaseMath.toQ24_8(1.0f);
    private static final int NEG_ONE = FixedBaseMath.toQ24_8(-1.0f);
    private static final int ZERO = 0;
    private static final int TWO = FixedBaseMath.toQ24_8(2.0f);

    // ---------------------------
    // Identity
    // ---------------------------
    public static int[] createIdentity4x4() {
        int[] m = new int[16];
        m[0]  = ONE;
        m[5]  = ONE;
        m[10] = ONE;
        m[15] = ONE;
        return m;
    }

    // ---------------------------
    // Translation in the 4th column
    // ---------------------------
    public static int[] createTranslation4x4(int tx, int ty, int tz) {
        int[] m = createIdentity4x4();
        // row=0,col=3 => m[0*4+3] = tx
        m[3]  = tx;
        m[7]  = ty;
        m[11] = tz;
        // m[15] = 1
        return m;
    }

    // ---------------------------
    // Scale on the diagonal
    // ---------------------------
    public static int[] createScale4x4(int sx, int sy, int sz) {
        int[] m = createIdentity4x4();
        m[0]  = sx;
        m[5]  = sy;
        m[10] = sz;
        return m;
    }

    // ---------------------------
    // Rotation X
    // ---------------------------
    public static int[] createRotationX4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m  = createIdentity4x4();
        // For row-major, typical rotation about X:
        //  [1   0    0    0]
        //  [0  cos  -sin   0]
        //  [0  sin   cos   0]
        //  [0   0    0     1]
        m[5]  = cosQ;                         // row=1,col=1
        m[6]  = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);  // row=1,col=2 => -sin
        m[9]  = sinQ;                         // row=2,col=1
        m[10] = cosQ;                         // row=2,col=2
        return m;
    }

    public static int[] createRotationY4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m  = createIdentity4x4();
        // [ cos  0  sin  0]
        // [ 0    1   0   0]
        // [-sin 0  cos  0]
        // [ 0    0   0   1]
        m[0]  = cosQ;
        m[2]  = sinQ;
        m[8]  = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);
        m[10] = cosQ;
        return m;
    }

    public static int[] createRotationZ4x4(int angleQ24_8) {
        int sinQ = FixedTrigMath.sin(angleQ24_8);
        int cosQ = FixedTrigMath.cos(angleQ24_8);
        int[] m  = createIdentity4x4();
        // [cos -sin  0  0]
        // [sin  cos  0  0]
        // [ 0    0   1  0]
        // [ 0    0   0  1]
        m[0] = cosQ;
        m[1] = FixedBaseMath.q24_8_mul(NEG_ONE, sinQ);
        m[4] = sinQ;
        m[5] = cosQ;
        return m;
    }

    // ---------------------------
    // Multiply 4x4
    // ---------------------------
    public static int[] multiply4x4(int[] m1, int[] m2) {
        int[] r = new int[16];
        for (int row=0; row<4; row++) {
            for (int col=0; col<4; col++) {
                long sum = 0;
                for (int k=0; k<4; k++) {
                    sum += (long)m1[row*4 + k] * m2[k*4 + col];
                }
                r[row*4 + col] = (int)(sum >> Q24_8_SHIFT);
            }
        }
        return r;
    }

    // ---------------------------
    // Transform a point (x,y,z,1)
    // ---------------------------
    public static int[] transformPoint4x4(int[] m, int[] xyz) {
        // xyz has length=3 => (x,y,z)
        // We treat as (x,y,z,1).
        int[] out4 = new int[4];
        for (int row=0; row<4; row++) {
            long sum = 0;
            for (int col=0; col<3; col++) {
                sum += (long) m[row*4 + col] * xyz[col];
            }
            // plus the w component => *1
            sum += (long)m[row*4 + 3] * (long)FixedBaseMath.toQ24_8(1.0f);
            out4[row] = (int)(sum >> Q24_8_SHIFT);
        }
        return out4;
    }

    // ---------------------------
    // Transform a vector (x,y,z,0)
    // i.e. ignoring translation
    // ---------------------------
    public static int[] transformVector4x4(int[] m, int[] xyz) {
        int[] out3 = new int[3];
        for (int row=0; row<3; row++) {
            long sum = 0;
            for (int col=0; col<3; col++) {
                sum += (long) m[row*4 + col] * xyz[col];
            }
            // no translation => skip m[row*4 + 3]
            out3[row] = (int)(sum >> Q24_8_SHIFT);
        }
        return out3;
    }

    // ---------------------------
    // createLookAt4x4
    //   row0 => xAxis
    //   row1 => yAxis
    //   row2 => zAxis
    //   row3 => translation
    // ---------------------------
    public static int[] createLookAt4x4(int[] eye, int[] target, int[] up) {
        // zAxis = normalize(eye - target)
        int[] zAxis = FixedVecMath.q24_8_sub(eye, target);
        zAxis = FixedVecMath.q24_8_normalize(zAxis);

        // xAxis = normalize( cross(up, zAxis) )
        int[] xAxis = FixedVecMath.q24_8_crossProduct(up, zAxis);
        xAxis = FixedVecMath.q24_8_normalize(xAxis);

        // yAxis = cross(zAxis, xAxis)
        int[] yAxis = FixedVecMath.q24_8_crossProduct(zAxis, xAxis);

        int[] m = new int[16];
        // row0 => xAxis
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[3] = 0;

        // row1 => yAxis
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[7] = 0;

        // row2 => zAxis
        m[8]  = zAxis[0];
        m[9]  = zAxis[1];
        m[10] = zAxis[2];
        m[11] = 0;

        // row3 => translation => -dot(xAxis,eye), -dot(yAxis,eye), -dot(zAxis,eye)
        int negDotX = FixedVecMath.q24_8_dotProduct(xAxis, eye);
        int negDotY = FixedVecMath.q24_8_dotProduct(yAxis, eye);
        int negDotZ = FixedVecMath.q24_8_dotProduct(zAxis, eye);
        negDotX = FixedBaseMath.q24_8_mul(NEG_ONE, negDotX);
        negDotY = FixedBaseMath.q24_8_mul(NEG_ONE, negDotY);
        negDotZ = FixedBaseMath.q24_8_mul(NEG_ONE, negDotZ);

        m[12] = negDotX;
        m[13] = negDotY;
        m[14] = negDotZ;
        m[15] = ONE;
        return m;
    }

    // ---------------------------
    // createPerspective4x4
    // fovY, aspect, near, far are Q24.8
    // row-major
    // ---------------------------
    public static int[] createPerspective4x4(
    int fovY_Q24_8, 
    int aspect_Q24_8,
    int near_Q24_8,
    int far_Q24_8
) {
    // Convert Q24.8 => float
    float fovY  = FixedBaseMath.toFloat(fovY_Q24_8);
    float aspect= FixedBaseMath.toFloat(aspect_Q24_8);
    float nearF = FixedBaseMath.toFloat(near_Q24_8);
    float farF  = FixedBaseMath.toFloat(far_Q24_8);

    // top = near * tan(fov/2)
    float halfFov = (float)Math.toRadians(fovY/2.0f);
    float topF = (float)( nearF * Math.tan(halfFov) );
    float bottomF = -topF;
    float rightF  = topF * aspect;
    float leftF   = -rightF;

    // We'll compute the 12 needed float terms then convert to Q24.8
    float twoN = 2.0f * nearF;
    float rl   = (rightF - leftF);
    float tb   = (topF - bottomF);
    float fn   = (farF  - nearF);

    float A = twoN / rl;  // = 2n/(r-l)
    float B = (rightF + leftF)/ rl;  // (r+l)/(r-l)
    float C = twoN / tb;  // (2n)/(t-b)
    float D = (topF + bottomF)/ tb;  // (t+b)/(t-b)
    float E = -(farF + nearF)/ fn;   // -(f+n)/(f-n)
    float F = -(2.0f*farF*nearF)/ fn;// -(2fn)/(f-n)

    // row-major 4x4
    int[] m = new int[16];
    // row0
    m[0] = FixedBaseMath.toQ24_8(A);
    m[1] = 0;
    m[2] = FixedBaseMath.toQ24_8(B);
    m[3] = 0;
    // row1
    m[4] = 0;
    m[5] = FixedBaseMath.toQ24_8(C);
    m[6] = FixedBaseMath.toQ24_8(D);
    m[7] = 0;
    // row2
    m[8] = 0;
    m[9] = 0;
    m[10] = FixedBaseMath.toQ24_8(E);
    m[11] = FixedBaseMath.toQ24_8(F);
    // row3
    m[12] = 0;
    m[13] = 0;
    m[14] = FixedBaseMath.toQ24_8(-1.0f);
    m[15] = 0;

    return m;
}


    private FixedMatMath() {}
}
