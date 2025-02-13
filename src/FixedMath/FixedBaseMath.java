package FixedMath;

/*
 * FixedBaseMath
 *
 * This class implements fixed-point arithmetic using a resolution specified by FIXED_SHIFT,
 * and all internal arithmetic is performed using 64-bit long integers.
 *
 * Example partitions (units in meters, approximate values):
 *
 * 1. Q31.0 format (FIXED_SHIFT = 0, FIXED_SCALE = 1):
 *    - Range: approximately -9.22e18 to 9.22e18 meters.
 *    - Precision: 1 meter.
 *    - In kilometers: approximately -9.22e15 km to 9.22e15 km.
 *
 * 2. Q24.8 format (FIXED_SHIFT = 8, FIXED_SCALE = 256):
 *    - Range: approximately -3.59e16 to 3.59e16 meters.
 *    - Precision: 1/256 ≈ 0.0039 meters (≈ 4 mm).
 *    - In kilometers: approximately -3.59e13 km to 3.59e13 km.
 *
 * 3. Q22.10 format (FIXED_SHIFT = 10, FIXED_SCALE = 1024):
 *    - Range: approximately -8.59e15 to 8.59e15 meters.
 *    - Precision: 1/1024 ≈ 0.0009766 meters (≈ 1 mm).
 *    - In kilometers: approximately -8.59e12 km to 8.59e12 km.
 *
 * 4. Q20.12 format (FIXED_SHIFT = 12, FIXED_SCALE = 4096):
 *    - Range: approximately -2.15e15 to 2.15e15 meters.
 *    - Precision: 1/4096 ≈ 0.00024414 meters (≈ 0.244 mm).
 *    - In kilometers: approximately -2.15e12 km to 2.15e12 km.
 *
 * 5. Q16.16 format (FIXED_SHIFT = 16, FIXED_SCALE = 65,536):
 *    - Range: approximately -9.22e14 to 9.22e14 meters.
 *    - Precision: 1/65,536 ≈ 0.00001526 meters (≈ 15 µm).
 *    - In kilometers: approximately -9.22e11 km to 9.22e11 km.
 *
 * 6. Q8.24 format (FIXED_SHIFT = 24, FIXED_SCALE = 16,777,216):
 *    - Range: approximately -2.15e13 to 2.15e13 meters.
 *    - Precision: 1/16,777,216 ≈ 0.0000000596 meters (≈ 60 nm).
 *    - In kilometers: approximately -2.15e10 km to 2.15e10 km.
 *
 * 7. Q0.32 format (FIXED_SHIFT = 32, FIXED_SCALE = 4,294,967,296):
 *    - Range: approximately -9.22e9 to 9.22e9 meters.
 *    - Precision: 1/4,294,967,296 ≈ 2.33e-10 meters (≈ 0.23 nm).
 *    - In kilometers: approximately -9.22e6 km to 9.22e6 km.
 *
 * Note: These ranges are computed using the full 64-bit signed range. In practice, you may
 * want to clamp results to a smaller range.
 */
public final class FixedBaseMath {


    // Should be no bigger than 22, but not too small either.
    public static final int FIXED_SHIFT = 20;
    public static final long FIXED_SCALE = 1L << FIXED_SHIFT;
    public static final long MAX_FIXED = Long.MAX_VALUE;
    public static final long MIN_FIXED = Long.MIN_VALUE;
    
    public static final long FIXEDNEG1 = toFixed(-1.0f);
    public static final long FIXED1 = toFixed(1.0f);
    public static final long FIXED2 = toFixed(2.0f);
    public static final long FIXED6 = toFixed(6.0f);
    public static final long FIXED24 = toFixed(24.0f);
    public static final long FIXED120 = toFixed(120.0f);
    public static final long FIXED225 = toFixed(225.0f);
    public static final long FIXED720 = toFixed(720.0f);
    public static final long FIXED5040 = toFixed(5040.0f);

    public static long toFixed(float val) {
        float temp = val * FIXED_SCALE;
        if (temp > MAX_FIXED) {
            return MAX_FIXED;
        }
        if (temp < MIN_FIXED) {
            return MIN_FIXED;
        }
        return (long) temp;
    }

    public static float toFloat(long fixedVal) {
        return ((float) fixedVal) / FIXED_SCALE;
    }

    public static int toInt(long fixedVal) {
        return (int) (fixedVal >> FIXED_SHIFT);
    }

    public static long fixedAdd(long a, long b) {
        long result = a + b;
        if (result > MAX_FIXED) {
            //System.out.print("\n Addition overflow \n");
            return MAX_FIXED;
        }
        if (result < MIN_FIXED) {
            //System.out.print("\n Addition underflow \n");
            return MIN_FIXED;
        }
        return result;
    }

    public static long fixedSub(long a, long b) {
        long result = a - b;
        if (result > MAX_FIXED) {
            //System.out.print("\n Subtraction overflow \n");
            return MAX_FIXED;
        }
        if (result < MIN_FIXED) {
            //System.out.print("\n Subtraction underflow \n");
            return MIN_FIXED;
        }
        return result;
    }

    public static long fixedMul(long a, long b) {
        long result = (a * b) >> FIXED_SHIFT;
        if (result > MAX_FIXED) {
            //System.out.print("\n Multiplication overflow \n");
            return MAX_FIXED;
        }
        if (result < MIN_FIXED) {
            //System.out.print("\n Multiplication underflow \n");
            return MIN_FIXED;
        }
        return result;
    }

    public static long fixedDiv(long a, long b) {
        if (b == 0) {
            //System.out.print("\n Division by zero \n");
            return (a >= 0) ? MAX_FIXED : MIN_FIXED;
        }
        long result = (a << FIXED_SHIFT) / b;
        if (result > MAX_FIXED) {
            //System.out.print("\n Division overflow \n");
            return MAX_FIXED;
        }
        if (result < MIN_FIXED) {
            //System.out.print("\n Division underflow \n");
            return MIN_FIXED;
        }
        return result;
    }

    public static long sqrt(long x) {
        if (x <= 0) {
            return 0;
        }
        int F = FixedBaseMath.FIXED_SHIFT; // Supports arbitrary shift F

        double value = (double) x / (1L << F); // Convert fixed to float

        double sqrtValue = Math.sqrt(value);  // Compute sqrt in floating-point

        return (long) (sqrtValue * (1L << F)); // Convert back to fixed

    }

    public static long pow(long base, float exponent) {
        if (exponent == 0) {
            return FIXED1;
        }
        if (base == 0) {
            return 0;
        }
        int expInt = (int) exponent;
        float expFrac = exponent - expInt;
        long resultInt = FIXED1;
        for (int i = 0; i < expInt; i++) {
            resultInt = fixedMul(resultInt, base);
        }
        long resultFrac = powFractional(base, expFrac);
        return fixedMul(resultInt, resultFrac);
    }

    private static long powFractional(long base, float expFrac) {
        long oneFixed = FIXED1;
        long twoFixed = FIXED2;
        long baseNorm = base;
        while (baseNorm >= twoFixed) {
            baseNorm = fixedDiv(baseNorm, twoFixed);
        }
        long x = fixedSub(baseNorm, oneFixed);
        long term1 = oneFixed;
        long term2 = fixedMul(toFixed(expFrac), x);
        long term3 = fixedMul(toFixed(expFrac * (expFrac - 1) / 2), fixedMul(x, x));
        long term4 = fixedMul(toFixed(expFrac * (expFrac - 1) * (expFrac - 2) / 6), fixedMul(x, fixedMul(x, x)));
        return fixedAdd(fixedAdd(term1, term2), fixedAdd(term3, term4));
    }

    public static long fixedHypot3D(long x, long y, long z) {
        long squareX = fixedMul(x, x);
        long squareY = fixedMul(y, y);
        long squareZ = fixedMul(z, z);
        long sumSquares = fixedAdd(fixedAdd(squareX, squareY), squareZ);
        return sqrt(sumSquares);
    }

    private FixedBaseMath() {
    }
}
