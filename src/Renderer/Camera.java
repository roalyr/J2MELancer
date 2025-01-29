package Renderer;

import FixedMath.*;

public class Camera {

    private int[] cameraMatrix;
    private int[] position;  // Camera position (x, y, z) in Q24.8
    private int[] rotation;  // Camera rotation (Euler angles: x, y, z) in Q24.8
    private int[] upVector; // Camera up vector (x, y, z) in Q24.8


    public Camera() {
        cameraMatrix = FixedMatMath.createIdentity4x4();
        position = new int[]{0, 0, 0}; // Initially at origin
        rotation = new int[]{0, 0, 0}; // Initially no rotation
        upVector = new int[]{0, FixedBaseMath.toQ24_8(1.0f), 0}; // Y-axis is often "up"

    }

    public int[] getCameraMatrix() {
        updateCameraMatrix(); // Update matrix before returning
        return cameraMatrix;
    }

    public void setPosition(int x, int y, int z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public void setRotation(int rotX, int rotY, int rotZ) {
        rotation[0] = rotX;
        rotation[1] = rotY;
        rotation[2] = rotZ;
    }

    // Methods to update the camera's position, orientation, etc.
    public void translate(int tx, int ty, int tz) {
        position[0] += tx;
        position[1] += ty;
        position[2] += tz;
    }

    public void rotate(int rotX, int rotY, int rotZ) {
        rotation[0] += rotX;
        rotation[1] += rotY;
        rotation[2] += rotZ;
    }

    private void updateCameraMatrix() {
        // 1. Reset to the identity matrix
        cameraMatrix = FixedMatMath.createIdentity4x4();

        // 2. Apply rotation (consider adjusting order if needed)
        int[] rotXMatrix = FixedMatMath.createRotationX4x4(rotation[0]);
        int[] rotYMatrix = FixedMatMath.createRotationY4x4(rotation[1]);
        int[] rotZMatrix = FixedMatMath.createRotationZ4x4(rotation[2]);

        cameraMatrix = FixedMatMath.multiply4x4(cameraMatrix, rotZMatrix); // Z
        cameraMatrix = FixedMatMath.multiply4x4(cameraMatrix, rotYMatrix); // Y
        cameraMatrix = FixedMatMath.multiply4x4(cameraMatrix, rotXMatrix); // X

        // 3. Apply translation
        int[] translationMatrix = FixedMatMath.createTranslation4x4(-position[0], -position[1], -position[2]);
        cameraMatrix = FixedMatMath.multiply4x4(cameraMatrix, translationMatrix);
    }

    // ... other camera control methods ...
}