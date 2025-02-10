package Renderer;

import javax.microedition.lcdui.Graphics;

public class RendererUI {

    private String stringA;
    private long fpsStartTime;
    private int framesRendered;
    private int currentFPS;

    public RendererUI() {
        fpsStartTime = System.currentTimeMillis();
        framesRendered = 0;
        currentFPS = 0;
    }

    public void updateFPS() {
        framesRendered++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - fpsStartTime >= 1000) {
            currentFPS = framesRendered;
            framesRendered = 0;
            fpsStartTime = currentTime;
        }
    }

    public void printFPS(Graphics g) {
        g.setColor(0xFFFFFFFF);
        int ren = SharedData.renderables_num;
        stringA = "FPS: " + currentFPS + " Renderables: " + ren;
        g.drawString(stringA, 2, 2, Graphics.TOP | Graphics.LEFT);
    }
}
