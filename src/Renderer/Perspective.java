package Renderer;

import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;

public class Perspective {
    private int[] perspectiveMatrix;
    private int fovQ24_8;
    private int aspectQ24_8;
    private int nearQ24_8;
    private int farQ24_8;

    public Perspective(int fovQ24_8, int aspectQ24_8, int nearQ24_8, int farQ24_8) {
        this.fovQ24_8 = fovQ24_8;
        this.aspectQ24_8 = aspectQ24_8;
        this.nearQ24_8 = nearQ24_8;
        this.farQ24_8 = farQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public int[] getPerspectiveMatrix() {
        return perspectiveMatrix;
    }

    // Updates the field of view and recalculates the perspective matrix.
    public void setFov(int fovQ24_8) {
        this.fovQ24_8 = fovQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    // (Optional) Update the aspect ratio and recalc the matrix.
    public void setAspect(int aspectQ24_8) {
        this.aspectQ24_8 = aspectQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    // (Optional) Update the near clipping plane and recalc the matrix.
    public void setNear(int nearQ24_8) {
        this.nearQ24_8 = nearQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    // (Optional) Update the far clipping plane and recalc the matrix.
    public void setFar(int farQ24_8) {
        this.farQ24_8 = farQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }
}