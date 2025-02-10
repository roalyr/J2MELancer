package FixedMath;

public final class FixedTrigMath {

    public static final int PI = FixedBaseMath.toFixed(3.14159265f);
    public static final int TWO_PI = FixedBaseMath.toFixed(6.2831853f);
    public static final int HALF_PI = FixedBaseMath.toFixed(1.57079633f);

    private static final int P0 = FixedBaseMath.toFixed(1.5707288f);
    private static final int P1 = FixedBaseMath.toFixed(-0.2121144f);
    private static final int P2 = FixedBaseMath.toFixed(0.0742610f);
    private static final int P3 = FixedBaseMath.toFixed(-0.0187293f);

    public static int sin(int angle) {
        int a = angle % TWO_PI;
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
        int x = a;
        int x2 = FixedBaseMath.fixedMul(x, x);
        int x3 = FixedBaseMath.fixedMul(x2, x);
        int x5 = FixedBaseMath.fixedMul(x3, x2);
        int x7 = FixedBaseMath.fixedMul(x5, x2);
        int term1 = x;
        int term2 = FixedBaseMath.fixedDiv(x3, FixedBaseMath.toFixed(6.0f));
        int term3 = FixedBaseMath.fixedDiv(x5, FixedBaseMath.toFixed(120.0f));
        int term4 = FixedBaseMath.fixedDiv(x7, FixedBaseMath.toFixed(5040.0f));
        int result = term1 - term2 + term3 - term4;
        return sign * result;
    }

    public static int cos(int angle) {
        int a = angle % TWO_PI;
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
        int x2 = FixedBaseMath.fixedMul(x, x);
        int x4 = FixedBaseMath.fixedMul(x2, x2);
        int x6 = FixedBaseMath.fixedMul(x4, x2);
        int one = FixedBaseMath.toFixed(1.0f);
        int term1 = one;
        int term2 = FixedBaseMath.fixedDiv(x2, FixedBaseMath.toFixed(2.0f));
        int term3 = FixedBaseMath.fixedDiv(x4, FixedBaseMath.toFixed(24.0f));
        int term4 = FixedBaseMath.fixedDiv(x6, FixedBaseMath.toFixed(720.0f));
        int result = term1 - term2 + term3 - term4;
        if (flip) {
            result = -result;
        }
        return result;
    }

    public static int tan(int angle) {
        int sinVal = sin(angle);
        int cosVal = cos(angle);
        if (cosVal == 0) {
            return (sinVal >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        return FixedBaseMath.fixedDiv(sinVal, cosVal);
    }

    public static int acos(int x) {
        int one = FixedBaseMath.toFixed(1.0f);
        if (x > one) {
            x = one;
        }
        if (x < -one) {
            x = -one;
        }
        if (x < 0) {
            return PI - acos(-x);
        }
        int delta = one - x;
        int sqrtDelta = FixedBaseMath.sqrt(delta);
        int x2 = FixedBaseMath.fixedMul(x, x);
        int x3 = FixedBaseMath.fixedMul(x2, x);
        int poly = P0 + FixedBaseMath.fixedMul(P1, x) +
                   FixedBaseMath.fixedMul(P2, x2) +
                   FixedBaseMath.fixedMul(P3, x3);
        return FixedBaseMath.fixedMul(sqrtDelta, poly);
    }

    public static int degreesToRadians(int degrees) {
        return FixedBaseMath.toFixed((float) (degrees * Math.PI / 180.0));
    }

    public static float radiansToDegrees(int radians) {
        return FixedBaseMath.toFloat(radians) * (180.0f / (float) Math.PI);
    }

    private FixedTrigMath() {
    }
}
