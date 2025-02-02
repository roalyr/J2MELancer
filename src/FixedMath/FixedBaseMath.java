package FixedMath;

/**
 * Q24.8 Basic Math without Lookup Tables
 */
public final class FixedBaseMath {

    // Q24.8 Format
    public static final int Q24_8_SHIFT = 8;         // 1<<8 = 256

    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    // ----------------------------
    // Q24.8 Conversions
    // ----------------------------
    /** float => Q24.8 (truncate).  e.g. 1.0 => 256. */
    public static int toQ24_8(float val) {
        return (int) (val * Q24_8_SCALE);
    }

    /** Q24.8 => float.  e.g. 256 => 1.0 */
    public static float toFloat(int fixedVal) {
        return (float) fixedVal / (float) Q24_8_SCALE;
    }

    /** Q24.8 => int (truncate). e.g. 256 => 1 */
    public static int toInt(int fixedVal) {
        return fixedVal >> Q24_8_SHIFT; // Right-shift by 8 bits to remove the fractional part

    }

    // ----------------------------
    // Basic Arithmetic
    // ----------------------------
    public static int q24_8_add(int a, int b) {
        return a + b;
    }

    public static int q24_8_sub(int a, int b) {
        return a - b;
    }

    public static int q24_8_mul(int a, int b) {
        // (a * b) >> 8
        long tmp = ((long) a * (long) b) >> Q24_8_SHIFT;
        return (int) tmp;
    }

    public static int q24_8_div(int a, int b) {
        if (b == 0) {
            // Handle division by zero (you might want to throw an exception instead)
            return (a >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        // Perform division using 64-bit intermediate value to avoid overflow
        return (int) (((long) a << Q24_8_SHIFT) / b);
    }

    // ----------------------------
    // sqrt(value) - Approximation using bit manipulation
    // ----------------------------
    public static int sqrt(int x) {
        if (x <= 0) {
            return 0; // Handle non-positive input

        }
        int iterations = 10; // Adjust for desired accuracy

        // Initial guess for the square root in Q24.8 format
        int guess = x;

        // Newton-Raphson iteration
        for (int i = 0; i < iterations; i++) {
            guess = (guess + q24_8_div(x, guess)) >> 1;
        }

        return guess;
    }

    public static int pow(int base, float exponent) {
        if (exponent == 0) {
            return FixedBaseMath.toQ24_8(1f);
        }
        if (base == 0) {
            return 0;
        }

        // Separate integer and fractional parts of the exponent
        int expInt = (int) exponent;
        float expFrac = exponent - expInt;

        // Calculate the integer part of the power
        int resultInt = FixedBaseMath.toQ24_8(1f); // Initialize to 1.0 in Q24.8

        for (int i = 0; i < expInt; i++) {
            resultInt = FixedBaseMath.q24_8_mul(resultInt, base);
        }

        // Calculate the fractional part of the power using a Taylor series approximation
        int resultFrac = powFractional(base, expFrac);

        // Combine the integer and fractional parts
        return FixedBaseMath.q24_8_mul(resultInt, resultFrac);
    }

    private static int powFractional(int base, float expFrac) {
        // Normalize base to be in the range of [0, 2) in Q24.8
        int oneQ24_8 = FixedBaseMath.toQ24_8(1f);
        int baseNorm = base;
        while (baseNorm >= FixedBaseMath.toQ24_8(2f)) {
            baseNorm = FixedBaseMath.q24_8_div(baseNorm, FixedBaseMath.toQ24_8(2f));
        }

        // Calculate (baseNorm - 1) which is in the range of [0, 1)
        int x = FixedBaseMath.q24_8_sub(baseNorm, oneQ24_8);

        // Use a Taylor series expansion to approximate (1 + x)^expFrac
        int term1 = oneQ24_8; // 1 in Q24.8

        int term2 = FixedBaseMath.q24_8_mul(toQ24_8(expFrac), x);
        int term3 = FixedBaseMath.q24_8_mul(toQ24_8(expFrac * (expFrac - 1) / 2), FixedBaseMath.q24_8_mul(x, x));
        int term4 = FixedBaseMath.q24_8_mul(toQ24_8(expFrac * (expFrac - 1) * (expFrac - 2) / 6), FixedBaseMath.q24_8_mul(x, FixedBaseMath.q24_8_mul(x, x)));

        int result = FixedBaseMath.q24_8_add(FixedBaseMath.q24_8_add(term1, term2), FixedBaseMath.q24_8_add(term3, term4));

        return result;
    }

    private FixedBaseMath() {
    } // Non-instantiable class

}