package Renderer;

import FixedMath.FixedBaseMath;

public final class RenderEffects {

    public static final int TYPE_EDGES = 0;
    public static final int TYPE_VERTICES = 1;
    
    public static final int SHAPE_P = 0;
    public static final int SHAPE_H = 1;
    public static final int SHAPE_X = 2;
    public static final int SHAPE_S = 3;
    
    private static final int ALPHA_THRESHOLD_LOCAL_HIGH = 250;
    private static final int ALPHA_THRESHOLD_LOCAL_LOW = 50;
    private static final int SRC_MAX_ALPHA = 50; // Maximum allowed alpha sum for soft blending

    private RenderEffects() {
    }
    
    private static int clamp(int value) {
        return (value > 255) ? 255 : value;
    }
    
    public static int blendPixel(int dstPixel, int srcPixel, int srcMaxAlpha) {
        // Do not blend if destination is our opaque black background.
        if (dstPixel == Renderer.BACKGROUND_COLOR) {
            return srcPixel;
        }
        
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
        
        // Calculate weight factor based on summed alpha.
        // When sumA is low (dark parts) we want full addition.
        // When sumA is high (bright parts) we fall back to averaging.
        float weight = 1.0f - Math.min(1.0f, (float) sumA / (2 * SRC_MAX_ALPHA));
        float factor = 0.5f + 0.5f * weight;
        
        int finalA = clamp((int) (sumA * factor));
        int finalR = clamp((int) (sumR * factor));
        int finalG = clamp((int) (sumG * factor));
        int finalB = clamp((int) (sumB * factor));
        
        if (finalA < ALPHA_THRESHOLD_LOCAL_LOW) {
            finalA = ALPHA_THRESHOLD_LOCAL_LOW;
        }
        
        return (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
    }
    
    private static void setPixel(int width, int height, int[] frameBuffer, int px, int py, int color) {
        if (color == Renderer.BACKGROUND_COLOR) {
            return;
        }
        if (px < 0 || px >= width || py < 0 || py >= height) {
            return;
        }
        int index = py * width + px;
        int dstColor = frameBuffer[index];
        if (dstColor == Renderer.BACKGROUND_COLOR) {
            frameBuffer[index] = color;
        } else {
            frameBuffer[index] = blendPixel(dstColor, color, SRC_MAX_ALPHA);
        }
    }
    
    public static void drawMarker(int shape, int width, int height, int[] frameBuffer, int x, int y, int color) {
        switch (shape) {
            case SHAPE_P:
                setPixel(width, height, frameBuffer, x, y, color);
                break;
            case SHAPE_H:
                setPixel(width, height, frameBuffer, x, y, color);
                setPixel(width, height, frameBuffer, x, y + 1, color);
                setPixel(width, height, frameBuffer, x + 1, y, color);
                break;
            case SHAPE_X:
                setPixel(width, height, frameBuffer, x, y, color);
                setPixel(width, height, frameBuffer, x + 1, y - 1, color);
                setPixel(width, height, frameBuffer, x + 1, y + 1, color);
                setPixel(width, height, frameBuffer, x - 1, y + 1, color);
                setPixel(width, height, frameBuffer, x - 1, y - 1, color);
                break;
            case SHAPE_S:
                setPixel(width, height, frameBuffer, x, y, color);
                setPixel(width, height, frameBuffer, x, y + 1, color);
                setPixel(width, height, frameBuffer, x + 1, y, color);
                setPixel(width, height, frameBuffer, x + 1, y + 1, color);
                
                break;
            default:
                setPixel(width, height, frameBuffer, x, y, color);
                break;
        }
    }
    
    public static int interpolateColor(long z, long z0, long z1, int color0, int color1) {
        long rangeQ = FixedBaseMath.fixedSub(z1, z0);
        if (rangeQ <= 0) {
            return color1;
        }
        long distQ = FixedBaseMath.fixedSub(z, z0);
        long ratioQ = FixedBaseMath.fixedDiv(distQ, rangeQ);
        if (ratioQ < 0) {
            ratioQ = 0;
        }
        long oneQ = FixedBaseMath.toFixed(1.0f);
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
        
        a = (a < 0) ? 0 : ((a > 255) ? 255 : a);
        r = (r < 0) ? 0 : ((r > 255) ? 255 : r);
        g = (g < 0) ? 0 : ((g > 255) ? 255 : g);
        b = (b < 0) ? 0 : ((b > 255) ? 255 : b);
        
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
            ratioQ = (ratioQ < 0) ? 0 : ((ratioQ > FixedBaseMath.toFixed(1.0f)) ? FixedBaseMath.toFixed(1.0f) : ratioQ);
            return ratioQToAlpha(ratioQ);
        }
        long farMinusFade = FixedBaseMath.fixedSub(farQ, fadeFarQ);
        if (z >= farMinusFade) {
            long dz = FixedBaseMath.fixedSub(farQ, z);
            long ratioQ = FixedBaseMath.fixedDiv(dz, fadeFarQ);
            ratioQ = (ratioQ < 0) ? 0 : ((ratioQ > FixedBaseMath.toFixed(1.0f)) ? FixedBaseMath.toFixed(1.0f) : ratioQ);
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
        ratioQ = (ratioQ < 0) ? 0 : ((ratioQ > FixedBaseMath.toFixed(1.0f)) ? FixedBaseMath.toFixed(1.0f) : ratioQ);
        long invertedRatioQ = FixedBaseMath.fixedSub(FixedBaseMath.toFixed(1.0f), ratioQ);
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
        long twoR = FixedBaseMath.fixedMul(FixedBaseMath.toFixed(2.0f), R);
        long ratioQ = FixedBaseMath.fixedDiv(shifted, twoR);
        long oneQ = FixedBaseMath.toFixed(1.0f);
        long invRatioQ = FixedBaseMath.fixedSub(oneQ, ratioQ);
        return ratioQToAlpha(invRatioQ);
    }
    
    public static int ratioQToAlpha(long ratioQ) {
        long alphaQ = FixedBaseMath.fixedMul(ratioQ, FixedBaseMath.toFixed(255.0f));
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
