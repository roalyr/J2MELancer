package Interfaces;

import javax.microedition.lcdui.Graphics;

public interface IRenderable {
    /**
     * Render this object.
     *   @param g the Graphics to draw on
     *   @param viewMatrix the world->view->projection 4x4 in Q24.8
     */
    void render(Graphics g, int[] viewMatrix);
}
