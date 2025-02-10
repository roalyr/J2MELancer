package Renderer;

import FixedMath.FixedBaseMath;

public class RenderEffects {

    // Primitive types
    public static final int TYPE_EDGES = 0;
    public static final int TYPE_VERTICES = 1;
    // Marker shape constants
    public static final int SHAPE_P = 0;
    public static final int SHAPE_H = 1;
    public static final int SHAPE_X = 2;
    public final int[] vertexTemplateA = new int[9];

    public static int blendPixel(int dstPixel, int srcPixel, int srcMaxAlpha) {
        int dstA = (dstPixel >> 24) & 0xFF;
        int dstR = (dstPixel >> 16) & 0xFF;
        int dstG = (dstPixel >> 8) & 0xFF;
        int dstB = dstPixel & 0xFF;
        int srcA = (srcPixel >> 24) & 0xFF;
        int srcR = (srcPixel >> 16) & 0xFF;
        int srcG = (srcPixel >> 8) & 0xFF;
        int srcB = srcPixel & 0xFF;
        int sumA = dstA + srcA;
        int sumR = dstR + srcR;
        int sumG = dstG + srcG;
        int sumB = dstB + srcB;
        if (sumA > srcMaxAlpha) {
            float scale = srcMaxAlpha / (float) sumA;
            sumA = srcMaxAlpha;
            sumR = (int) (sumR * scale);
            sumG = (int) (sumG * scale);
            sumB = (int) (sumB * scale);
        }
        if (sumA > 255) {
            sumA = 255;
        }
        if (sumR > 255) {
            sumR = 255;
        }
        if (sumG > 255) {
            sumG = 255;
        }
        if (sumB > 255) {
            sumB = 255;
        }
        return (sumA << 24) | (sumR << 16) | (sumG << 8) | sumB;
    }

    public static int interpolateColor(int z, int z0, int z1,
            int color0, int color1) {
        int rangeQ = FixedBaseMath.fixedSub(z1, z0);
        if (rangeQ <= 0) {
            return color1;
        }
        int distQ = FixedBaseMath.fixedSub(z, z0);
        int ratioQ = FixedBaseMath.fixedDiv(distQ, rangeQ);
        if (ratioQ < 0) {
            ratioQ = 0;
        }
        int oneQ = FixedBaseMath.toFixed(1.0f);
        if (ratioQ > oneQ) {
            ratioQ = oneQ;
        }
        
        int a0 = (color0 >> 24) & 0xFF;
        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int deltaA = a1 - a0;
        int deltaR = r1 - r0;
        int deltaG = g1 - g0;
        int deltaB = b1 - b0;
        int a = a0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(FixedBaseMath.toFixed(deltaA), ratioQ));
        int r = r0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(FixedBaseMath.toFixed(deltaR), ratioQ));
        int g = g0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(FixedBaseMath.toFixed(deltaG), ratioQ));
        int b = b0 + FixedBaseMath.toInt(FixedBaseMath.fixedMul(FixedBaseMath.toFixed(deltaB), ratioQ));
        if (a < 0) {
            a = 0;
        } else if (a > 255) {
            a = 255;
        }
        if (r < 0) {
            r = 0;
        } else if (r > 255) {
            r = 255;
        }
        if (g < 0) {
            g = 0;
        } else if (g > 255) {
            g = 255;
        }
        if (b < 0) {
            b = 0;
        } else if (b > 255) {
            b = 255;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int computeFadeAlpha(int z, int nearQ, int farQ, int fadeNearQ, int fadeFarQ) {
        if (z < nearQ || z > farQ) {
            return 0;
        }
        if (fadeNearQ <= 0 || fadeFarQ <= 0) {
            return 255;
        }
        int nearPlusFade = FixedBaseMath.fixedAdd(nearQ, fadeNearQ);
        if (z <= nearPlusFade) {
            int dz = FixedBaseMath.fixedSub(z, nearQ);
            int ratioQ = FixedBaseMath.fixedDiv(dz, fadeNearQ);
            if (ratioQ < 0) {
                ratioQ = 0;
            }
            if (ratioQ > FixedBaseMath.toFixed(1.0f)) {
                ratioQ = FixedBaseMath.toFixed(1.0f);
            }
            return ratioQToAlpha(ratioQ);
        }
        int farMinusFade = FixedBaseMath.fixedSub(farQ, fadeFarQ);
        if (z >= farMinusFade) {
            int dz = FixedBaseMath.fixedSub(farQ, z);
            int ratioQ = FixedBaseMath.fixedDiv(dz, fadeFarQ);
            if (ratioQ < 0) {
                ratioQ = 0;
            }
            if (ratioQ > FixedBaseMath.toFixed(1.0f)) {
                ratioQ = FixedBaseMath.toFixed(1.0f);
            }
            return ratioQToAlpha(ratioQ);
        }
        return 255;
    }

    /**
     * Low-level pixel setter that checks boundaries. 
     */
    private static void setPixel(int width, int height, int[] frameBuffer,
            int px, int py, int color) {
        if (px < 0 || px >= width || py < 0 || py >= height) {
            return;
        }
        frameBuffer[py * width + px] = color;
    }

    /**
     * Unified method to draw a small marker symbol (P, H, or X) at (x,y).
     * Ensures no out-of-bounds. 
     * @param shape One of SHAPE_P, SHAPE_H, or SHAPE_X
     */
    public static void drawMarker(int shape, int width, int height,
            int[] frameBuffer, int x, int y, int color) {

        switch (shape) {
            case SHAPE_P:
                // Single pixel
                setPixel(width, height, frameBuffer, x, y, color);
                break;

            case SHAPE_H:
                // Plots (x,y), (x, y+1), (x+1, y)
                setPixel(width, height, frameBuffer, x, y, color);
                setPixel(width, height, frameBuffer, x, y + 1, color);
                setPixel(width, height, frameBuffer, x + 1, y, color);
                break;

            case SHAPE_X:
                // Center (x,y) + diagonals (y±1,x±1)
                setPixel(width, height, frameBuffer, x, y, color);
                setPixel(width, height, frameBuffer, x + 1, y - 1, color);
                setPixel(width, height, frameBuffer, x + 1, y + 1, color);
                setPixel(width, height, frameBuffer, x - 1, y + 1, color);
                setPixel(width, height, frameBuffer, x - 1, y - 1, color);
                break;

            default:
                // If unknown shape, do nothing or default to P
                break;
        }
    }

    public static int computeLocalAlpha(int localZ, int localZmin, int localZmax) {
        // If localZ is at or closer than the model's nearest point, full opacity.
        if (localZ <= localZmin) {
            return 255;
        }
        // If at or beyond the model's farthest point, completely transparent.
        if (localZ >= localZmax) {
            return 0;
        }
        // Compute the fraction along the z-range.
        int rangeQ = FixedBaseMath.fixedSub(localZmax, localZmin);
        int diffQ = FixedBaseMath.fixedSub(localZ, localZmin);
        int ratioQ = FixedBaseMath.fixedDiv(diffQ, rangeQ);
        if (ratioQ < 0) {
            ratioQ = 0;
        } else if (ratioQ > FixedBaseMath.toFixed(1.0f)) {
            ratioQ = FixedBaseMath.toFixed(1.0f);
        }
        // Invert the ratio: 0 (closest) becomes 1, and 1 (farthest) becomes 0.
        int invertedRatioQ = FixedBaseMath.fixedSub(FixedBaseMath.toFixed(1.0f), ratioQ);
        return ratioQToAlpha(invertedRatioQ);
    }

    public static int computeLocalAlphaFromCameraSpace(int vertexCamZ, int centerCamZ, int boundingRadiusQ) {
        // Compute the vertex's depth relative to the object center.
        int localZ = FixedBaseMath.fixedSub(vertexCamZ, centerCamZ);
        int R = boundingRadiusQ; // expected half depth extent (in Q24.8)

        // Clamp localZ to [-R, R]
        if (localZ < -R) {
            localZ = -R;
        } else if (localZ > R) {
            localZ = R;
        }
        // Normalize localZ from [-R, R] to a ratio in Q24.8: 0 means farthest, 1 means closest.
        // First, shift so that -R becomes 0 and R becomes 2R:
        int shifted = FixedBaseMath.fixedAdd(localZ, R);
        // Divide by 2R:
        int twoR = FixedBaseMath.fixedMul(FixedBaseMath.toFixed(2.0f), R);
        int ratioQ = FixedBaseMath.fixedDiv(shifted, twoR);
        // Invert the ratio so that a vertex with localZ == -R (closest) yields 1.0, and one with localZ == R yields 0.
        int oneQ = FixedBaseMath.toFixed(1.0f);
        int invRatioQ = FixedBaseMath.fixedSub(oneQ, ratioQ);
        return ratioQToAlpha(invRatioQ);
    }

    // Helper to convert a Q24.8 ratio (0 to 1) into an integer alpha [0,255].
    public static int ratioQToAlpha(int ratioQ) {
        int alphaQ = FixedBaseMath.fixedMul(ratioQ, FixedBaseMath.toFixed(255.0f));
        int alphaI = FixedBaseMath.toInt(alphaQ);
        if (alphaI < 0) {
            alphaI = 0;
        }
        if (alphaI > 255) {
            alphaI = 255;
        }
        return alphaI;
    }

    private RenderEffects() {
    }
}
