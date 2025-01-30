package FixedMath;

/**
 * Q24.8 Basic Math with a small 256-entry table for reciprocal and sqrt.
 *
 * - Always uses fallback for inputs < 1.0 in Q24.8 in reciprocal(...),
 *   so 1/0.4 => ~2.5 exactly.
 *
 * - sqrtTable[i] = sqrt(i<<8) in Q24.8, i=0..table_size-1
 * - reciprocalTable[i] = 1/(i<<8) in Q24.8, i=1..table_size-1
 *
 * "1.0" in Q24.8 is integer 256.  So (256 << 8)=65536 is "1.0" in Q24.16.
 */
public final class FixedBaseMath {

    // Q24.8 Format
    public static final int Q24_8_SHIFT = 8;           // 1<<8 = 256

    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    // A small 256-entry table
    private static final int TABLE_SIZE = 256;
    private static final int TABLE_SIZE_MINUS_ONE = TABLE_SIZE - 1;

    // sqrtTable[i] = sqrt(i<<8) in Q24.8
    private static final int[] sqrtTable = new int[TABLE_SIZE];
    // reciprocalTable[i] = 1/(i<<8) in Q24.8, i=1..table_size-1
    private static final int[] reciprocalTable = new int[TABLE_SIZE];
    

    static {
        initializeTables();
    }

    private static void initializeTables() {
        // 1) Build sqrt table
        for (int i = 0; i < TABLE_SIZE; i++) {
            int xQ24_8 = (i << Q24_8_SHIFT);
            float xF = (float) xQ24_8 / (float) Q24_8_SCALE;
            float sF = (float) Math.sqrt(xF);
            sqrtTable[i] = toQ24_8(sF);
        }

        // 2) Build reciprocal table for i=1..255 => 1/(i<<8)
        for (int i = 1; i < TABLE_SIZE; i++) {
            int xQ24_8 = (i << Q24_8_SHIFT);
            float xF = (float) xQ24_8 / (float) Q24_8_SCALE;
            float rF = 1.0f / xF;
            reciprocalTable[i] = toQ24_8(rF);
        }
    // index=0 remains 0
    }

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

    public static int q24_8_div_(int a, int b) {
        // a / b = a * (1/b)
        return q24_8_mul(a, reciprocal(b));
    }

    public static int q24_8_div(int a, int b) {
        // Handle division by zero
        if (b == 0) {
            return (a >= 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }

        // Use the sign of the result for final adjustment
        int sign = (a > 0) ^ (b > 0) ? -1 : 1;

        // Make operands positive
        a = Math.abs(a);
        b = Math.abs(b);

        // Handle cases where the divisor is less than 1.0 in Q24.8
        if (b < Q24_8_SCALE) {
            long numerator = ((long) a) << Q24_8_SHIFT;
            int result = (int) (numerator / b);
            return sign * result;
        }

        // Scale down if needed for table lookup
        int scale = 0;
        while (b > (TABLE_SIZE_MINUS_ONE << Q24_8_SHIFT)) {
            b >>= 1;
            scale++;
        }

        // Perform table lookup
        int index = (b >> Q24_8_SHIFT);
        if (index < 1) {
            index = 1; // Prevent index out of bounds

        }
        int reciprocal = reciprocalTable[index];

        // Adjust result based on scaling
        reciprocal <<= scale;

        // Perform the division as a multiplication
        long result = ((long) a * reciprocal) >> Q24_8_SHIFT;

        // Adjust the sign and return
        return sign * (int) result;
    }

    // ----------------------------
    // sqrt(value)
    // ----------------------------
    /**
     * sqrt(value), Q24.8 => Q24.8
     * - negative => 0
     * - if large => repeatedly divide by 4 => table => multiply by 2 for each step
     */
    public static int sqrt(int value) {
        if (value <= 0) {
            return 0;
        }

        int scale = 0;
        int temp = value;
        // while out of range [0..table_size-1<<8], shift down by 2 bits (divide by 4)
        while (temp > (TABLE_SIZE_MINUS_ONE << Q24_8_SHIFT)) {
            temp >>= 2;
            scale++;
        }

        int index = temp >> Q24_8_SHIFT; // [0..table_size-1]

        int result = sqrtTable[index];
        // each ">>2" => sqrt => <<1 on the result
        result <<= scale;
        return result;
    }

    // ----------------------------
    // reciprocal(value) = 1/value
    // ----------------------------
    /**
     * - If value=0 => returns 0x7fffffff sentinel
     * - If |value|<1.0 => fallback integer method => yields e.g. 1/0.4 => 2.5
     * - Else use small table for [1..table_size-1], scaling if needed for large input
     */
    public static int reciprocal(int value) {
        if (value == 0) {
            return 0x7fffffff;
        }

        // sign
        int sign = 1;
        if (value < 0) {
            sign = -1;
            value = -value;
        }

        // 1) if |value| < 1.0 in Q24.8 => fallback => (1.0 in Q24.8 <<8) / value
        //    that means 1<<8=256 => (256<<8)=65536 => 1 in Q24.16
        if (value < Q24_8_SCALE) {
            // numerator = 65536
            long numerator = (long) (Q24_8_SCALE << Q24_8_SHIFT); // 256<<8=65536

            long res = numerator / value; // final is Q24.8

            int intRes = (int) res;
            return (sign < 0) ? -intRes : intRes;
        }

        // 2) else table approach
        int scale = 0;
        int temp = value;
        // scale down if needed
        while (temp > (TABLE_SIZE_MINUS_ONE << Q24_8_SHIFT)) {
            temp >>= 1;
            scale++;
            if (scale > 24) {
                // extremely large => ~0
                return (sign > 0) ? 1 : -1;
            }
        }

        int index = (temp >> Q24_8_SHIFT);
        if (index < 1) {
            index = 1;
        }
        int result = reciprocalTable[index];

        // shift left by 'scale'
        result <<= scale;

        return (sign < 0) ? -result : result;
    }

    private FixedBaseMath() {
    }
}
