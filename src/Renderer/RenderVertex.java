package Renderer;

public class RenderVertex {

    public static void drawVertex(int shape, int width, int height, int[] frameBuffer,int x, int y, int color) {
        RenderEffects.drawMarker(shape,width, height, frameBuffer, x, y, color);
    }

    private RenderVertex() {
    }
}
