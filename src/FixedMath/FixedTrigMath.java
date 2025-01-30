package FixedMath;

public final class FixedTrigMath {

    // ================
    // Q24.8 Format
    // ================
    public static final int Q24_8_SHIFT = 8;         // 1<<8 = 256
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
    // Sine, Cosine, Tangent
    // Angles in radians, represented in Q24.8 format
    // ================

    public static int sin(int angleQ24_8) {
        // Convert Q24.8 angle to float radians
        float angleRadians = toFloat(angleQ24_8);

        // Calculate sine using Math.sin (which expects radians)
        float sinValue = (float) Math.sin(angleRadians);

        // Convert the result back to Q24.8
        return toQ24_8(sinValue);
    }

    public static int cos(int angleQ24_8) {
        // Convert Q24.8 angle to float radians
        float angleRadians = toFloat(angleQ24_8);

        // Calculate cosine using Math.cos
        float cosValue = (float) Math.cos(angleRadians);

        // Convert the result back to Q24.8
        return toQ24_8(cosValue);
    }

    public static int tan(int angleQ24_8) {
        // Convert Q24.8 angle to float radians
        float angleRadians = toFloat(angleQ24_8);

        // Calculate tangent using Math.tan
        float tanValue = (float) Math.tan(angleRadians);

        // Convert the result back to Q24.8
        return toQ24_8(tanValue);
    }

    // ================
    // ACOS (Inverse Cosine) - Approximation
    // ================

    /**
     * Calculates the inverse cosine (acos) of a value in Q24.8 format.
     * The input value should be in the range [-256, 256] (representing -1.0 to 1.0).
     * Returns the angle in radians, in Q24.8 format, in the range [0, PI].
     */
    public static int acos(int cosValueQ24_8) {
        // Convert Q24.8 to float for the calculation
        float x = toFloat(cosValueQ24_8);

        // Clamp the input value to the valid range [-1, 1]
        if (x > 1.0f) x = 1.0f;
        if (x < -1.0f) x = -1.0f;

        // Polynomial approximation for acos(x)
        float x2 = x * x;
        float x3 = x * x2;
        float result = 1.57079632679f - x - 0.2145988f * x3; // Approximation for acos

        // Convert the result back to Q24.8 format
        return toQ24_8(result);
    }

    // ================
    // Helper Functions
    // ================

    /**
     * Converts an angle in degrees to radians in Q24.8 format.
     */
    public static int degreesToRadiansQ24_8(float degrees) {
        return toQ24_8((float)(degrees * Math.PI / 180.0));
    }
    
    /**
     * Converts an angle in radians (Q24.8 format) to degrees.
     */
    public static float radiansToDegreesQ24_8(int radiansQ24_8) {
        return toFloat(radiansQ24_8) * (180.0f / (float)Math.PI);
    }

    private FixedTrigMath() {} // Non-instantiable class
}