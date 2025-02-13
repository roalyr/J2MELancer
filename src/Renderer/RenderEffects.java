package Renderer;

import FixedMath.FixedBaseMath;

public final class RenderEffects {

    public static final int TYPE_EDGES = 0;
    public static final int TYPE_VERTICES = 1;

    public static final int SHAPE_P = 0;
    public static final int SHAPE_H = 1;
    public static final int SHAPE_X = 2;
    public static final int SHAPE_S = 3;

    // Used for local alpha thresholds in your fade logic
    private static final int ALPHA_THRESHOLD_LOCAL_HIGH = 250;
    private static final int ALPHA_THRESHOLD_LOCAL_LOW = 50;

    private RenderEffects() { }

    // ---------------------------------------------------------
    // Depth-test pixel setting: NO BLENDING with background
    // ---------------------------------------------------------

    /**
     * Draws marker at (x, y), performing depth test with 'z'.
     * If passes, overwrites the framebuffer pixel with 'color'.
     */
    public static void drawMarkerDepthTest(
            int shape,
            int width,
            int height,
            int[] frameBuffer,
            long[] depthBuffer,
            int x,
            int y,
            int color,
            long z
    ) {
        switch (shape) {
            case SHAPE_P:
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y, color, z);
                break;
            case SHAPE_H:
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y + 1, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x + 1, y, color, z);
                break;
            case SHAPE_X:
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x + 1, y - 1, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x + 1, y + 1, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x - 1, y + 1, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x - 1, y - 1, color, z);
                break;
            case SHAPE_S:
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y + 1, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x + 1, y, color, z);
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x + 1, y + 1, color, z);
                break;
            default:
                // default to single pixel
                setPixelDepthTest(width, height, frameBuffer, depthBuffer, x, y, color, z);
                break;
        }
    }

    /**
     * Write pixel if newZ < storedZ in the depth buffer. 
     * No alpha blending is done; we simply overwrite the color.
     */
    private static void setPixelDepthTest(
            int width, int height,
            int[] frameBuffer,
            long[] depthBuffer,
            int px, int py,
            int color,
            long newZ
    ) {
        if (px < 0 || px >= width || py < 0 || py >= height) {
            return; // out of bounds
        }
        int idx = py * width + px;

        // Depth test
        long oldZ = depthBuffer[idx];
        if (newZ < oldZ) {
            depthBuffer[idx] = newZ;    // update stored depth
            frameBuffer[idx] = color;   // overwrite color (no blending)
        }
    }

    // ---------------------------------------------------------
    // Below here remain your original fade/darkening methods
    // ---------------------------------------------------------

    public static int _interpolateColor_(long z, long z0, long z1, int color0, int color1) {
        long rangeQ = FixedBaseMath.fixedSub(z1, z0);
        if (rangeQ <= 0) {
            return color1;
        }
        long distQ = FixedBaseMath.fixedSub(z, z0);
        long ratioQ = FixedBaseMath.fixedDiv(distQ, rangeQ);
        if (ratioQ < 0) ratioQ = 0;
        long oneQ = FixedBaseMath.FIXED1;
        if (ratioQ > oneQ) ratioQ = oneQ;

        int a0 = (color0 >>> 24) & 0xFF;
        int r0 = (color0 >>> 16) & 0xFF;
        int g0 = (color0 >>>  8) & 0xFF;
        int b0 =  color0         & 0xFF;

        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>>  8) & 0xFF;
        int b1 =  color1         & 0xFF;

        int da = (a1 - a0);
        int dr = (r1 - r0);
        int dg = (g1 - g0);
        int db = (b1 - b0);

        long daQ = FixedBaseMath.toFixed(da);
        long drQ = FixedBaseMath.toFixed(dr);
        long dgQ = FixedBaseMath.toFixed(dg);
        long dbQ = FixedBaseMath.toFixed(db);

        int a = a0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(daQ, ratioQ));
        if (a < 0) a = 0; else if (a > 255) a = 255;
        int r = r0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(drQ, ratioQ));
        if (r < 0) r = 0; else if (r > 255) r = 255;
        int g = g0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(dgQ, ratioQ));
        if (g < 0) g = 0; else if (g > 255) g = 255;
        int b = b0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(dbQ, ratioQ));
        if (b < 0) b = 0; else if (b > 255) b = 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int computeFadeAlpha(long z, long nearQ, long farQ, long fadeNearQ, long fadeFarQ) {
        if (z < nearQ || z > farQ) {
            return 0;
        }
        if (fadeNearQ <= 0 || fadeFarQ <= 0) {
            return 255;
        }
        long nearPlusFade = FixedBaseMath.fixedAdd(nearQ, fadeNearQ);
        if (z <= nearPlusFade) {
            long dz = FixedBaseMath.fixedSub(z, nearQ);
            long ratioQ = FixedBaseMath.fixedDiv(dz, fadeNearQ);
            if (ratioQ < 0) ratioQ = 0;
            if (ratioQ > FixedBaseMath.FIXED1) ratioQ = FixedBaseMath.FIXED1;
            return ratioQToAlpha(ratioQ);
        }
        long farMinusFade = FixedBaseMath.fixedSub(farQ, fadeFarQ);
        if (z >= farMinusFade) {
            long dz = FixedBaseMath.fixedSub(farQ, z);
            long ratioQ = FixedBaseMath.fixedDiv(dz, fadeFarQ);
            if (ratioQ < 0) ratioQ = 0;
            if (ratioQ > FixedBaseMath.FIXED1) ratioQ = FixedBaseMath.FIXED1;
            return ratioQToAlpha(ratioQ);
        }
        return 255;
    }

    public static int computeLocalAlpha(long localZ, long localZmin, long localZmax) {
        if (localZ <= localZmin) {
            return 255;
        }
        if (localZ >= localZmax) {
            return 0;
        }
        long rangeQ = FixedBaseMath.fixedSub(localZmax, localZmin);
        long diffQ = FixedBaseMath.fixedSub(localZ, localZmin);
        long ratioQ = FixedBaseMath.fixedDiv(diffQ, rangeQ);
        if (ratioQ < 0) ratioQ = 0;
        if (ratioQ > FixedBaseMath.FIXED1) ratioQ = FixedBaseMath.FIXED1;
        long invertedRatioQ = FixedBaseMath.fixedSub(FixedBaseMath.FIXED1, ratioQ);
        return ratioQToAlpha(invertedRatioQ);
    }

    public static int computeLocalAlphaFromCameraSpace(long vertexCamZ, long centerCamZ, long boundingRadiusQ) {
        long localZ = FixedBaseMath.fixedSub(vertexCamZ, centerCamZ);
        long R = boundingRadiusQ;
        if (localZ < -R) {
            localZ = -R;
        } else if (localZ > R) {
            localZ = R;
        }
        long shifted = FixedBaseMath.fixedAdd(localZ, R);
        long twoR = FixedBaseMath.fixedMul(FixedBaseMath.FIXED2, R);
        long ratioQ = FixedBaseMath.fixedDiv(shifted, twoR);
        if (ratioQ < 0) ratioQ = 0;
        if (ratioQ > FixedBaseMath.FIXED1) ratioQ = FixedBaseMath.FIXED1;
        long invRatioQ = FixedBaseMath.fixedSub(FixedBaseMath.FIXED1, ratioQ);
        return ratioQToAlpha(invRatioQ);
    }

    public static int ratioQToAlpha(long ratioQ) {
        long alphaQ = FixedBaseMath.fixedMul(ratioQ, FixedBaseMath.FIXED225);
        int alphaI = FixedBaseMath.toInt(alphaQ);
        if (alphaI < ALPHA_THRESHOLD_LOCAL_LOW) {
            alphaI = ALPHA_THRESHOLD_LOCAL_LOW;
        }
        if (alphaI > ALPHA_THRESHOLD_LOCAL_HIGH) {
            alphaI = 255;
        }
        return alphaI;
    }
}
