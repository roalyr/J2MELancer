package Renderer;

public class RenderLine {
// Dither constants

    private static final byte[][] DITHER_2X2 = {
        {0, 2},
        {3, 1}
    };
    private static final byte[][] DITHER_4X4 = {
        {0, 8, 2, 10},
        {12, 4, 14, 6},
        {3, 11, 1, 9},
        {15, 7, 13, 5}
    };
    private static final byte[][] DITHER_8X8 = {
        {0, 32, 8, 40, 2, 34, 10, 42},
        {48, 16, 56, 24, 50, 18, 58, 26},
        {12, 44, 4, 36, 14, 46, 6, 38},
        {60, 28, 52, 20, 62, 30, 54, 22},
        {3, 35, 11, 43, 1, 33, 9, 41},
        {51, 19, 59, 27, 49, 17, 57, 25},
        {15, 47, 7, 39, 13, 45, 5, 37},
        {63, 31, 55, 23, 61, 29, 53, 21}
    };

    /** 
     * Draws a line using alpha dithering for 2x2, 4x4, or 8x8 patterns.
     * @param x0,y0 start
     * @param x1,y1 end
     * @param color  ARGB color; alpha in [0..255]
     * @param ditherSize must be 2,4, or 8
     */
    public static void drawLineDither(int shape, int width, int height, int[] frameBuffer, int x0, int y0, int x1, int y1, int color, int ditherSize) {
        // 1) Extract alpha
        int alpha = (color >>> 24) & 0xFF;

        // 2) Convert alpha => coverage depending on ditherSize
        // For 2x2 => coverage in [0..3] => alpha>>>6
        // For 4x4 => coverage in [0..15] => alpha>>>4
        // For 8x8 => coverage in [0..63] => alpha>>>2
        int coverageShift;
        byte[][] ditherMatrix;
        int mask;  // for indexing e.g. y0 & (size-1)

        switch (ditherSize) {
            case 2:
                coverageShift = 6;   // alpha>>>6 => [0..3]

                ditherMatrix = DITHER_2X2;
                mask = 1;           // y0 & 1

                break;
            case 4:
                coverageShift = 4;   // alpha>>>4 => [0..15]

                ditherMatrix = DITHER_4X4;
                mask = 3;           // y0 & 3

                break;
            case 8:
            default:
                coverageShift = 2;   // alpha>>>2 => [0..63]

                ditherMatrix = DITHER_8X8;
                mask = 7;           // y0 & 7

                break;
        }

        int coverage = alpha >>> coverageShift;
        if (coverage == 0) {
            // If coverage is zero => skip entirely
            return;
        }

        // Clip test for entire line (quick out)
        if ((x0 < 0 && x1 < 0) || (x0 >= width && x1 >= width) ||
                (y0 < 0 && y1 < 0) || (y0 >= height && y1 >= height)) {
            return;
        }

        // 3) Bresenham
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                // 4) Get threshold from the matrix
                int threshold = ditherMatrix[y0 & mask][x0 & mask];
                // If coverage > threshold => plot pixel
                if (coverage > threshold) {
                    RenderEffects.drawMarker(shape, width, height, frameBuffer, x0, y0, color);
                }
            }

            //if (x0 > 0 && x0 < width - 1 && y0 > 0 && y0 < height - 1) {
            //int threshold = ditherMatrix[y0 & mask][x0 & mask];
            // If coverage > threshold => plot pixel
            //if (coverage > threshold) {
            //RenderEffects.drawH(width, height, frameBuffer, x0, y0, color);
            //}
            //}


            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    public static void drawLine(int shape, int width, int height, int[] frameBuffer, int x0, int y0, int x1, int y1, int color) {

        if ((x0 < 0 && x1 < 0) || (x0 >= width && x1 >= width) ||
                (y0 < 0 && y1 < 0) || (y0 >= height && y1 >= height)) {
            return;
        }
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (x0 == x1 && y0 == y1) {
                break;
            }

            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                RenderEffects.drawMarker(shape ,width, height, frameBuffer, x0, y0, color);
            }

            //if (x0 > 0 && x0 < width-1 && y0 > 0 && y0 < height-1) {
            //    RenderEffects.drawX(width, height, frameBuffer, x0, y0, color);
            //}

            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private RenderLine() {
    }
}
