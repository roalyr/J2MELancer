package FixedMath;

/*
 * FixedBaseMath
 *
 * This class implements fixed-point arithmetic using a resolution specified by FIXED_SHIFT.
 *
 * Example formats (assuming units are in meters):
 *
 *
 * Fixed-Point Partitions (using 32-bit signed integers):
 *
 * 1. Q31.0 format (FIXED_SHIFT = 0, FIXED_SCALE = 1):
 *    - Range: approximately -2,147,483,648.0 to 2,147,483,647.0 meters.
 *    - Precision: 1 meter.
 *    - In kilometers: approximately -2,147,483.648 km to 2,147,483.647 km.
 *
 * 2. Q24.8 format (FIXED_SHIFT = 8, FIXED_SCALE = 256):
 *    - Range: approximately -8,388,608.0 to 8,388,607.99609375 meters.
 *    - Precision: 1/256 ≈ 0.0039 meters (≈ 4 mm).
 *    - In kilometers: approximately -8,388.608 km to 8,388.60799609375 km.
 *
 * 3. Q22.10 format (FIXED_SHIFT = 10, FIXED_SCALE = 1024):
 *    - Range: approximately -2,097,152.0 to 2,097,151.9990234375 meters.
 *    - Precision: 1/1024 ≈ 0.0009766 meters (≈ 1 mm).
 *    - In kilometers: approximately -2,097.152 km to 2,097.1519990234375 km.
 *
 * 4. Q20.12 format (FIXED_SHIFT = 12, FIXED_SCALE = 4096):
 *    - Range: approximately -524,288.0 to 524,287.9997558594 meters.
 *    - Precision: 1/4096 ≈ 0.00024414 meters (≈ 0.244 mm).
 *    - In kilometers: approximately -524.288 km to 524.2879997558594 km.
 *
 * 5. Q16.16 format (FIXED_SHIFT = 16, FIXED_SCALE = 65,536):
 *    - Range: approximately -32,768.0 to 32,767.99998474 meters.
 *    - Precision: 1/65,536 ≈ 0.00001526 meters (≈ 15 µm).
 *    - In kilometers: approximately -32.768 km to 32.76799998474 km.
 *
 * 6. Q8.24 format (FIXED_SHIFT = 24, FIXED_SCALE = 16,777,216):
 *    - Range: approximately -128.0 to 127.99999994 meters.
 *    - Precision: 1/16,777,216 ≈ 0.0000000596 meters (≈ 60 nm).
 *    - In kilometers: approximately -0.128 km to 0.12799999994 km.
 *
 * 7. Q0.32 format (FIXED_SHIFT = 32, FIXED_SCALE = 4,294,967,296):
 *    - Range: approximately -1.0 to 0.9999999997671694 meters.
 *    - Precision: 1/4,294,967,296 ≈ 2.33e-10 meters (≈ 0.23 nm).
 *    - In kilometers: approximately -0.001 km to 0.0009999999997671694 km.
 *
 * The maximum fixed-point value is determined by Integer.MAX_VALUE (2^31 - 1) and
 * the minimum by Integer.MIN_VALUE. Each arithmetic method checks for overflow and
 * clamps the result to Integer.MAX_VALUE or Integer.MIN_VALUE as appropriate.
 *
 * Change FIXED_SHIFT to switch the fixed-point resolution.
 */

public final class FixedBaseMath {

    // Change this value to set your fixed-point resolution.
    // For Q24.8, use 8; for Q22.10, use 10; for Q16.16, use 16.
    public static final int FIXED_SHIFT = 12;  
    public static final int FIXED_SCALE = 1 << FIXED_SHIFT;

    // Maximum and minimum representable fixed-point values.
    public static final int MAX_FIXED = Integer.MAX_VALUE;
    public static final int MIN_FIXED = Integer.MIN_VALUE;
    
    // ----------------------------
    // Conversions
    // ----------------------------
    public static int toFixed(float val) {
        // Multiply and round to nearest integer
        float temp = val * FIXED_SCALE;
        // Check for overflow:
        if (temp > MAX_FIXED) return MAX_FIXED;
        if (temp < MIN_FIXED) return MIN_FIXED;
        return (int) temp;
    }

    public static float toFloat(int fixedVal) {
        return ((float) fixedVal) / FIXED_SCALE;
    }

    public static int toInt(int fixedVal) {
        return fixedVal >> FIXED_SHIFT;
    }

    // ----------------------------
    // Basic Arithmetic with Overflow Checks
    // ----------------------------

    public static int fixedAdd(int a, int b) {
        long result = (long) a + (long) b;
        if (result > MAX_FIXED) return MAX_FIXED;
        if (result < MIN_FIXED) return MIN_FIXED;
        return (int) result;
    }

    public static int fixedSub(int a, int b) {
        long result = (long) a - (long) b;
        if (result > MAX_FIXED) return MAX_FIXED;
        if (result < MIN_FIXED) return MIN_FIXED;
        return (int) result;
    }

    public static int fixedMul(int a, int b) {
        long result = ((long) a * (long) b) >> FIXED_SHIFT;
        if (result > MAX_FIXED) return MAX_FIXED;
        if (result < MIN_FIXED) return MIN_FIXED;
        return (int) result;
    }

    public static int fixedDiv(int a, int b) {
        if (b == 0) {
            return (a >= 0) ? MAX_FIXED : MIN_FIXED;
        }
        long result = ((long) a << FIXED_SHIFT) / b;
        if (result > MAX_FIXED) return MAX_FIXED;
        if (result < MIN_FIXED) return MIN_FIXED;
        return (int) result;
    }

    // ----------------------------
    // Square Root using Newton-Raphson iteration with overflow check
    // ----------------------------
    public static int sqrt(int x) {
        if (x <= 0) {
            return 0;
        }
        int iterations = 10;
        int guess = x;
        for (int i = 0; i < iterations; i++) {
            int div = fixedDiv(x, guess);
            int sum = fixedAdd(guess, div);
            guess = sum >> 1;  // Divide by 2
        }
        return guess;
    }

    // ----------------------------
    // Power function (with fractional exponent approximation)
    // ----------------------------
    public static int pow(int base, float exponent) {
        if (exponent == 0) {
            return toFixed(1f);
        }
        if (base == 0) {
            return 0;
        }
        int expInt = (int) exponent;
        float expFrac = exponent - expInt;
        int resultInt = toFixed(1f);
        for (int i = 0; i < expInt; i++) {
            resultInt = fixedMul(resultInt, base);
        }
        int resultFrac = powFractional(base, expFrac);
        return fixedMul(resultInt, resultFrac);
    }

    private static int powFractional(int base, float expFrac) {
        int oneFixed = toFixed(1f);
        int twoFixed = toFixed(2f);
        int baseNorm = base;
        // Normalize base to [1,2)
        while (baseNorm >= twoFixed) {
            baseNorm = fixedDiv(baseNorm, twoFixed);
        }
        int x = fixedSub(baseNorm, oneFixed); // x in [0,1)
        int term1 = oneFixed;
        int term2 = fixedMul(toFixed(expFrac), x);
        int term3 = fixedMul(toFixed(expFrac * (expFrac - 1) / 2), fixedMul(x, x));
        int term4 = fixedMul(toFixed(expFrac * (expFrac - 1) * (expFrac - 2) / 6), fixedMul(x, fixedMul(x, x)));
        return fixedAdd(fixedAdd(term1, term2), fixedAdd(term3, term4));
    }

    private FixedBaseMath() {
    }
}
