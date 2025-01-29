package Renderer;

import FixedMath.*;

public class Perspective {
    private int[] perspectiveMatrix;

    public Perspective(int fovQ24_8, int aspectQ24_8, int nearQ24_8, int farQ24_8) {
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public int[] getPerspectiveMatrix() {
        return perspectiveMatrix;
    }

    // Methods to adjust FOV, aspect ratio, near/far planes
    public void setFov(int fovQ24_8) {
        // ... update perspectiveMatrix ...
    }

    // ... other perspective adjustment methods ...
}
