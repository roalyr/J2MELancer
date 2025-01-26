package FixedMath;

public final class FixedTrigMath {

    // ================
    // Table Parameters
    // ================
    private static final int TABLE_SIZE = 256;
    private static final float TWO_PI = (float)(2.0 * Math.PI);
    private static final int TABLE_SIZE_MINUS1 = TABLE_SIZE - 1;

    // ================
    // Q24.8 Format
    // ================
    public static final int Q24_8_SHIFT = 8;           // 1<<8 = 256
    public static final int Q24_8_SCALE = 1 << Q24_8_SHIFT;

    /** float -> Q24.8 (truncate). If desired, do (int)(val*256f+0.5f) for rounding. */
    public static int toQ24_8(float val) {
        return (int)(val * Q24_8_SCALE);
    }

    /** Q24.8 -> float. */
    public static float toFloat(int fixedVal) {
        return (float)fixedVal / (float)Q24_8_SCALE;
    }

    // ================
    // Tables
    // ================
    private static final int[] sinTable  = new int[TABLE_SIZE];
    private static final int[] cosTable  = new int[TABLE_SIZE];
    private static final int[] tanTable  = new int[TABLE_SIZE];
    private static final int[] acosTable = new int[TABLE_SIZE];

    // If you prefer to clamp tangent to ±(some large value) instead of letting it overflow:
    private static final int TAN_CLAMP_Q24_8 = toQ24_8(32767f);  // sentinel max magnitude

    static {
        initializeTables();
    }

    private static void initializeTables() {
        // 1) Build sin, cos, tan
        for (int i = 0; i < TABLE_SIZE; i++) {
            float angle = (i * TWO_PI) / TABLE_SIZE;
            float s = (float)Math.sin(angle);
            float c = (float)Math.cos(angle);

            sinTable[i] = toQ24_8(s);
            cosTable[i] = toQ24_8(c);

            // TAN edge-case clamp: tan = sin/cos
            float t;
            if (Math.abs(c) < 1.0e-7f) {
                // near vertical asymptote
                // sign depends on sign(sin)
                if      (s >  0) t =  32767f;
                else if (s <  0) t = -32767f;
                else             t = 0f;  // sin=0 => exactly 0/0 -> 0
            } else {
                t = s / c;
            }

            // store in Q24.8
            tanTable[i] = clampToQ24_8(t);
        }

        // 2) Build acos table for x in [-1..1], using polynomial in FLOAT
        for (int i = 0; i < TABLE_SIZE; i++) {
            float x = -1.0f + 2.0f * (i / (float)TABLE_SIZE_MINUS1);
            if (x < -1f) x = -1f;
            if (x >  1f) x =  1f;

            float x3   = x * x * x;
            float half = (float)(Math.PI * 0.5);
            float valF = half - x - (0.14159f * x3);
            acosTable[i] = toQ24_8(valF);
        }
    }

    /**
     * Helper: clamp the float to a valid Q24.8 range if you want to avoid integer overflow.
     * Java int min/max is ~ ±2.1e9.  Our float is smaller than that typically, but let's be safe.
     */
    private static int clampToQ24_8(float v) {
        // For typical usage, a float near ±3.4e38 can overflow.
        // We'll just clamp to some large integer range in Q24.8,
        // e.g. ±(1<<23) = ±8388608.  That's plenty big. 
        if (v >  8388608f) v =  8388608f;
        if (v < -8388608f) v = -8388608f;
        return (int)(v * Q24_8_SCALE);
    }

    // ================
    // Sine, Cosine, Tangent
    // Angles in [0..255], which maps to [0..2π).
    // ================
    public static int sin(int angle) {
        int idx = angle & 0xFF;
        return sinTable[idx];
    }

    public static int cos(int angle) {
        int idx = angle & 0xFF;
        return cosTable[idx];
    }

    public static int tan(int angle) {
        int idx = angle & 0xFF;
        return tanTable[idx];
    }

    // ================
    // ACOS
    // ================
    /**
     * acos(cosValue):
     *   - cosValue in Q24.8 for x in [-1..1].
     *   - returns Q24.8 angle in [0..π].
     *   - If x >= +1 => 0
     *   - If x <= -1 => π
     *   - Else do table lookup with polynomial approximation.
     */
    public static int acos(int cosValue) {
        // Convert to float
        float x = toFloat(cosValue);

        if (x >= 1.0f) {
            // acos(1) = 0
            return 0;
        }
        if (x <= -1.0f) {
            // acos(-1) = π
            return toQ24_8((float)Math.PI);
        }

        // In-between -1..1 => table index
        float frac = (x + 1f) * 0.5f;
        float fi   = frac * TABLE_SIZE_MINUS1;
        int i      = (int)(fi + 0.5f);
        if (i < 0) i = 0;
        if (i > TABLE_SIZE_MINUS1) i = TABLE_SIZE_MINUS1;
        return acosTable[i];
    }

    // ================
    // Degrees -> Angle Index
    // ================
    public static int degToAngle256(float degrees) {
        float frac = degrees / 360f;
        float fi   = frac * TABLE_SIZE;
        int i      = (int)(fi + 0.5f);
        if (i < 0) i = 0;
        if (i >= TABLE_SIZE) i = TABLE_SIZE - 1;
        return i;
    }

    private FixedTrigMath() {}
}
