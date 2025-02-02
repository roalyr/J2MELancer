package FixedMath;

public final class FixedTrigMath {

    // Q24.8 Format constants
    public static final int Q24_8_SHIFT = 8;
    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    // Fundamental angle constants in Q24.8 format
    public static final int PI = FixedBaseMath.toQ24_8(3.14159265f);
    public static final int TWO_PI = FixedBaseMath.toQ24_8(6.2831853f);
    public static final int HALF_PI = FixedBaseMath.toQ24_8(1.57079633f);

    // Polynomial approximation degrees can be tuned via the following coefficients.
    // Coefficients for acos(x) approximation (for x in [0,1]):
    // acos(x) ≈ sqrt(1 - x) * (P0 + P1*x + P2*x² + P3*x³)
    private static final int P0 = FixedBaseMath.toQ24_8(1.5707288f);
    private static final int P1 = FixedBaseMath.toQ24_8(-0.2121144f);
    private static final int P2 = FixedBaseMath.toQ24_8(0.0742610f);
    private static final int P3 = FixedBaseMath.toQ24_8(-0.0187293f);

    // Polynomial approximation for sine using Taylor series up to x⁷:
    // sin(x) ≈ x - x³/6 + x⁵/120 - x⁷/5040
    public static int sin(int angleQ24_8) {
        // Range reduction to [-PI, PI]
        int a = angleQ24_8 % TWO_PI;
        if (a < 0) {
            a += TWO_PI;
        }
        if (a > PI) {
            a -= TWO_PI;
        }
        // Sine is odd: sin(-x) = -sin(x)
        int sign = 1;
        if (a < 0) {
            sign = -1;
            a = -a;
        }
        // Further reduce to [0, HALF_PI] using symmetry: sin(x) = sin(PI - x)
        if (a > HALF_PI) {
            a = PI - a;
        }
        int x = a;
        int x2 = FixedBaseMath.q24_8_mul(x, x);
        int x3 = FixedBaseMath.q24_8_mul(x2, x);
        int x5 = FixedBaseMath.q24_8_mul(x3, x2);
        int x7 = FixedBaseMath.q24_8_mul(x5, x2);
        int term1 = x;
        int term2 = FixedBaseMath.q24_8_div(x3, FixedBaseMath.toQ24_8(6.0f));
        int term3 = FixedBaseMath.q24_8_div(x5, FixedBaseMath.toQ24_8(120.0f));
        int term4 = FixedBaseMath.q24_8_div(x7, FixedBaseMath.toQ24_8(5040.0f));
        int result = term1 - term2 + term3 - term4;
        return sign * result;
    }

    // Polynomial approximation for cosine using Taylor series up to x⁶:
    // cos(x) ≈ 1 - x²/2 + x⁴/24 - x⁶/720
    public static int cos(int angleQ24_8) {
        int a = angleQ24_8 % TWO_PI;
        if (a < 0) {
            a += TWO_PI;
        }
        if (a > PI) {
            a = TWO_PI - a;
        }
        boolean flip = false;
        if (a > HALF_PI) {
            a = PI - a;
            flip = true;
        }
        int x = a;
        int x2 = FixedBaseMath.q24_8_mul(x, x);
        int x4 = FixedBaseMath.q24_8_mul(x2, x2);
        int x6 = FixedBaseMath.q24_8_mul(x4, x2);
        int one = FixedBaseMath.toQ24_8(1.0f);
        int term1 = one;
        int term2 = FixedBaseMath.q24_8_div(x2, FixedBaseMath.toQ24_8(2.0f));
        int term3 = FixedBaseMath.q24_8_div(x4, FixedBaseMath.toQ24_8(24.0f));
        int term4 = FixedBaseMath.q24_8_div(x6, FixedBaseMath.toQ24_8(720.0f));
        int result = term1 - term2 + term3 - term4;
        if (flip) {
            result = -result;
        }
        return result;
    }

    // Tangent approximated as sin(x)/cos(x)
    public static int tan(int angleQ24_8) {
        int sinVal = sin(angleQ24_8);
        int cosVal = cos(angleQ24_8);
        if (cosVal == 0) {
            return (sinVal >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        return FixedBaseMath.q24_8_div(sinVal, cosVal);
    }

    // Polynomial approximation for arccosine.
    // For x in [-1, 1] represented in Q24.8 (i.e., [-256, 256]):
    // If x < 0, use: acos(x) = PI - acos(-x)
    // For x in [0,1], use:
    //   acos(x) ≈ sqrt(1 - x) * (P0 + P1*x + P2*x² + P3*x³)
    public static int acos(int x) {
        int one = FixedBaseMath.toQ24_8(1.0f);
        // Clamp x to [-1, 1]
        if (x > one) {
            x = one;
        }
        if (x < -one) {
            x = -one;
        }
        if (x < 0) {
            return PI - acos(-x);
        }
        // Now x is in [0, one]
        int delta = one - x;
        int sqrtDelta = FixedBaseMath.sqrt(delta);
        int x2 = FixedBaseMath.q24_8_mul(x, x);
        int x3 = FixedBaseMath.q24_8_mul(x2, x);
        int poly = P0 + FixedBaseMath.q24_8_mul(P1, x) +
                FixedBaseMath.q24_8_mul(P2, x2) +
                FixedBaseMath.q24_8_mul(P3, x3);
        return FixedBaseMath.q24_8_mul(sqrtDelta, poly);
    }

    // Converts an angle in degrees to Q24.8 radians.
    public static int degreesToRadiansQ24_8(float degrees) {
        return FixedBaseMath.toQ24_8((float) (degrees * Math.PI / 180.0));
    }

    // Converts an angle in Q24.8 radians to degrees.
    public static float radiansToDegreesQ24_8(int radiansQ24_8) {
        return FixedBaseMath.toFloat(radiansQ24_8) * (180.0f / (float) Math.PI);
    }

    private FixedTrigMath() {
    }
}