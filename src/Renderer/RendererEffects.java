/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Renderer;


import FixedMath.FixedBaseMath;


/**
 *
 * @author ROAL
 */
public class RendererEffects {

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
            int color0, int color1,
            float exponent) {
        int rangeQ = FixedBaseMath.q24_8_sub(z1, z0);
        if (rangeQ <= 0) {
            return color1;
        }
        int distQ = FixedBaseMath.q24_8_sub(z, z0);
        int ratioQ = FixedBaseMath.q24_8_div(distQ, rangeQ);
        if (ratioQ < 0) {
            ratioQ = 0;
        }
        int oneQ = FixedBaseMath.toQ24_8(1.0f);
        if (ratioQ > oneQ) {
            ratioQ = oneQ;
        }
        int adjustedRatioQ = FixedBaseMath.pow(ratioQ, exponent);
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
        int a = a0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaA), adjustedRatioQ));
        int r = r0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaR), adjustedRatioQ));
        int g = g0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaG), adjustedRatioQ));
        int b = b0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaB), adjustedRatioQ));
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

    public static int computeFadeAlpha(int z, int nearQ, int farQ, int fadeQ) {
        if (z < nearQ || z > farQ) {
            return 0;
        }
        if (fadeQ <= 0) {
            return 255;
        }
        int nearPlusFade = FixedBaseMath.q24_8_add(nearQ, fadeQ);
        if (z <= nearPlusFade) {
            int dz = FixedBaseMath.q24_8_sub(z, nearQ);
            int ratioQ = FixedBaseMath.q24_8_div(dz, fadeQ);
            if (ratioQ < 0) {
                ratioQ = 0;
            }
            if (ratioQ > FixedBaseMath.toQ24_8(1.0f)) {
                ratioQ = FixedBaseMath.toQ24_8(1.0f);
            }
            return ratioQToAlpha(ratioQ);
        }
        int farMinusFade = FixedBaseMath.q24_8_sub(farQ, fadeQ);
        if (z >= farMinusFade) {
            int dz = FixedBaseMath.q24_8_sub(farQ, z);
            int ratioQ = FixedBaseMath.q24_8_div(dz, fadeQ);
            if (ratioQ < 0) {
                ratioQ = 0;
            }
            if (ratioQ > FixedBaseMath.toQ24_8(1.0f)) {
                ratioQ = FixedBaseMath.toQ24_8(1.0f);
            }
            return ratioQToAlpha(ratioQ);
        }
        return 255;
    }

    public static int ratioQToAlpha(int ratioQ) {
        int alphaQ = FixedBaseMath.q24_8_mul(ratioQ, FixedBaseMath.toQ24_8(255.0f));
        int alphaI = FixedBaseMath.toInt(alphaQ);
        if (alphaI < 0) {
            alphaI = 0;
        }
        if (alphaI > 255) {
            alphaI = 255;
        }
        return alphaI;
    }
}
