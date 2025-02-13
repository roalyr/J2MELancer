package Renderer;

import FixedMath.FixedMatMath;

public class Perspective {
    private long[] perspectiveMatrix;
    private long fovQ24_8;
    private long aspectQ24_8;
    private long nearQ24_8;
    private long farQ24_8;

    public Perspective(long fovQ24_8, long aspectQ24_8, long nearQ24_8, long farQ24_8) {
        this.fovQ24_8 = fovQ24_8;
        this.aspectQ24_8 = aspectQ24_8;
        this.nearQ24_8 = nearQ24_8;
        this.farQ24_8 = farQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public long[] getPerspectiveMatrix() {
        return perspectiveMatrix;
    }

    public void setFov(long fovQ24_8) {
        this.fovQ24_8 = fovQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public void setAspect(long aspectQ24_8) {
        this.aspectQ24_8 = aspectQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public void setNear(long nearQ24_8) {
        this.nearQ24_8 = nearQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }

    public void setFar(long farQ24_8) {
        this.farQ24_8 = farQ24_8;
        if (perspectiveMatrix != null) {
            FixedMatMath.releaseMatrix(perspectiveMatrix);
        }
        perspectiveMatrix = FixedMatMath.createPerspective4x4(fovQ24_8, aspectQ24_8, nearQ24_8, farQ24_8);
    }
}
