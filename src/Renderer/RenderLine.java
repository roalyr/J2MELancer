package Renderer;

public class RenderLine {

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

    public static void drawLineDither(int shape, int width, int height, int[] frameBuffer,
            int x0, int y0, int x1, int y1, int color, int ditherSize) {
        int alpha = (color >>> 24) & 0xFF;

        // Early return if dithering is off
        if (ditherSize == 0) {
            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = (x0 < x1) ? 1 : -1;
            int sy = (y0 < y1) ? 1 : -1;
            int err = dx - dy;

            while (true) {
                if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                        RenderEffects.drawMarker(shape, width, height, frameBuffer, x0, y0, color);
                }
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


        int coverageShift;
        byte[][] ditherMatrix;
        int mask;

        switch (ditherSize) {
            case 2:
                coverageShift = 6;
                ditherMatrix = DITHER_2X2;
                mask = 1;
                break;
            case 4:
                coverageShift = 4;
                ditherMatrix = DITHER_4X4;
                mask = 3;
                break;
            case 8:
            default:
                coverageShift = 2;
                ditherMatrix = DITHER_8X8;
                mask = 7;
                break;
        }

        int coverage = alpha >>> coverageShift;
        if (coverage == 0) {
            return;
        }

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
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                int threshold = ditherMatrix[y0 & mask][x0 & mask];
                if (coverage > threshold - 1) {
                    RenderEffects.drawMarker(shape, width, height, frameBuffer, x0, y0, color);
                }
            }
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
}
