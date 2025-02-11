package FixedMath;

public final class FixedTrigMath {

    public static final long PI = FixedBaseMath.toFixed(3.14159265f);
    public static final long TWO_PI = FixedBaseMath.toFixed(6.2831853f);
    public static final long HALF_PI = FixedBaseMath.toFixed(1.57079633f);

    private static final long P0 = FixedBaseMath.toFixed(1.5707288f);
    private static final long P1 = FixedBaseMath.toFixed(-0.2121144f);
    private static final long P2 = FixedBaseMath.toFixed(0.0742610f);
    private static final long P3 = FixedBaseMath.toFixed(-0.0187293f);

    public static long sin(long angle) {
        long a = angle % TWO_PI;
        if (a < 0) {
            a += TWO_PI;
        }
        if (a > PI) {
            a -= TWO_PI;
        }
        int sign = 1;
        if (a < 0) {
            sign = -1;
            a = -a;
        }
        if (a > HALF_PI) {
            a = PI - a;
        }
        long x = a;
        long x2 = FixedBaseMath.fixedMul(x, x);
        long x3 = FixedBaseMath.fixedMul(x2, x);
        long x5 = FixedBaseMath.fixedMul(x3, x2);
        long x7 = FixedBaseMath.fixedMul(x5, x2);
        long term1 = x;
        long term2 = FixedBaseMath.fixedDiv(x3, FixedBaseMath.toFixed(6.0f));
        long term3 = FixedBaseMath.fixedDiv(x5, FixedBaseMath.toFixed(120.0f));
        long term4 = FixedBaseMath.fixedDiv(x7, FixedBaseMath.toFixed(5040.0f));
        long result = term1 - term2 + term3 - term4;
        return sign * result;
    }

    public static long cos(long angle) {
        long a = angle % TWO_PI;
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
        long x = a;
        long x2 = FixedBaseMath.fixedMul(x, x);
        long x4 = FixedBaseMath.fixedMul(x2, x2);
        long x6 = FixedBaseMath.fixedMul(x4, x2);
        long one = FixedBaseMath.toFixed(1.0f);
        long term1 = one;
        long term2 = FixedBaseMath.fixedDiv(x2, FixedBaseMath.toFixed(2.0f));
        long term3 = FixedBaseMath.fixedDiv(x4, FixedBaseMath.toFixed(24.0f));
        long term4 = FixedBaseMath.fixedDiv(x6, FixedBaseMath.toFixed(720.0f));
        long result = term1 - term2 + term3 - term4;
        if (flip) {
            result = -result;
        }
        return result;
    }

    public static long tan(long angle) {
        long sinVal = sin(angle);
        long cosVal = cos(angle);
        if (cosVal == 0) {
            return (sinVal >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return FixedBaseMath.fixedDiv(sinVal, cosVal);
    }

    public static long acos(long x) {
        long one = FixedBaseMath.toFixed(1.0f);
        if (x > one) {
            x = one;
        }
        if (x < -one) {
            x = -one;
        }
        if (x < 0) {
            return PI - acos(-x);
        }
        long delta = one - x;
        long sqrtDelta = FixedBaseMath.sqrt(delta);
        long x2 = FixedBaseMath.fixedMul(x, x);
        long x3 = FixedBaseMath.fixedMul(x2, x);
        long poly = P0 + FixedBaseMath.fixedMul(P1, x) +
                    FixedBaseMath.fixedMul(P2, x2) +
                    FixedBaseMath.fixedMul(P3, x3);
        return FixedBaseMath.fixedMul(sqrtDelta, poly);
    }

    public static float degreesToRadians(float degrees) {
        return (degrees * (float) Math.PI / 180.0f);
    }

    public static float radiansToDegrees(float radians) {
        return (radians) * (180.0f / (float) Math.PI);
    }

    private FixedTrigMath() {
    }
}
