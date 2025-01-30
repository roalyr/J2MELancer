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
        return (int)(val * Q24_8_SCALE);
    }

    /** Q24.8 => float.  e.g. 256 => 1.0 */
    public static float toFloat(int fixedVal) {
        return (float)fixedVal / (float)Q24_8_SCALE;
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
        long tmp = ((long)a * (long)b) >> Q24_8_SHIFT;
        return (int)tmp;
    }

    public static int q24_8_div(int a, int b) {
        if (b == 0) {
            // Handle division by zero (you might want to throw an exception instead)
            return (a >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        // Perform division using 64-bit intermediate value to avoid overflow
        return (int)(((long)a << Q24_8_SHIFT) / b);
    }

    // ----------------------------
    // sqrt(value) - Approximation using bit manipulation
    // ----------------------------
   public static int sqrt(int x) {
        if (x <= 0) return 0; // Handle non-positive input

        int iterations = 10; // Adjust for desired accuracy

        // Initial guess for the square root in Q24.8 format
        int guess = x;

        // Newton-Raphson iteration
        for (int i = 0; i < iterations; i++) {
            guess = (guess + q24_8_div(x, guess)) >> 1;
        }

        return guess;
    }

    private FixedBaseMath() {} // Non-instantiable class
}