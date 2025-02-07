package Renderer;

import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import FixedMath.FixedQuatMath;

public class Camera {

    private int[] viewMatrix; // The view matrix (inverse of world transform)
    private int[] position;   // Position in world space (Q24.8)
    // Orientation as a quaternion [x, y, z, w] in Q24.8
    private int[] orientation;

    public Camera() {
        viewMatrix = FixedMatMath.createIdentity4x4();
        position = new int[]{0, 0, 0};
        // Identity quaternion: no rotation.
        orientation = new int[]{0, 0, 0, FixedBaseMath.toQ24_8(1.0f)};
    }

    // Returns the view matrix used for rendering.
    public int[] getViewMatrix() {
        updateViewMatrix();
        return viewMatrix;
    }

    // Computes the view matrix as R^T * T(-position), where R is derived from the orientation.
    private void updateViewMatrix() {
        int[] rotMatrix = FixedQuatMath.toRotationMatrix(orientation);
        int[] rotMatrixT = FixedMatMath.transpose(rotMatrix);
        int[] trans = FixedMatMath.createTranslation4x4(-position[0], -position[1], -position[2]);
        viewMatrix = FixedMatMath.multiply4x4(rotMatrixT, trans);
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int x, int y, int z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public int[] getOrientation() {
        return orientation;
    }

    public void setOrientation(int[] q) {
        orientation = q;
    }

    // --- Adjusted Controls: Pitch and Yaw are applied relative to the camera's local (canonical) axes ---

    // Applies a yaw rotation (rotation about the local up axis).
    // Uses the canonical local up vector [0, 1, 0] in camera space.
    public void addYaw(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{0, FixedBaseMath.toQ24_8(1.0f), 0}, angleQ);
        // Right-multiply to apply rotation in the local coordinate system.
        orientation = FixedQuatMath.multiply(orientation, delta);
        orientation = FixedQuatMath.normalize(orientation);
    }

    // Applies a pitch rotation (rotation about the local right axis).
    // Uses the canonical local right vector [1, 0, 0] in camera space.
    public void addPitch(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{FixedBaseMath.toQ24_8(1.0f), 0, 0}, angleQ);
        // Right-multiply to apply rotation in the local coordinate system.
        orientation = FixedQuatMath.multiply(orientation, delta);
        orientation = FixedQuatMath.normalize(orientation);
    }

    // (Optional) Applies a roll rotation about the local forward axis.
    public void addRoll(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{0, 0, FixedBaseMath.toQ24_8(1.0f)}, angleQ);
        orientation = FixedQuatMath.multiply(orientation, delta);
        orientation = FixedQuatMath.normalize(orientation);
    }

    // Returns the current rotation matrix derived from the quaternion.
    public int[] getRotationMatrix() {
        return FixedQuatMath.toRotationMatrix(orientation);
    }

    // --- Translation Methods using current orientation ---

    // Moves the camera along its local forward axis (canonical: [0, 0, -1]).
    public void moveForward(int amount) {
        int[] rot = getRotationMatrix();
        int[] localForward = new int[]{0, 0, -FixedBaseMath.toQ24_8(1.0f), 0};
        int[] worldForward = new int[4];
        FixedMatMath.transformPoint(rot, localForward, worldForward);
        position[0] = FixedBaseMath.q24_8_add(position[0], FixedBaseMath.q24_8_mul(worldForward[0], amount));
        position[1] = FixedBaseMath.q24_8_add(position[1], FixedBaseMath.q24_8_mul(worldForward[1], amount));
        position[2] = FixedBaseMath.q24_8_add(position[2], FixedBaseMath.q24_8_mul(worldForward[2], amount));
    }

    // Moves the camera along its local right axis (canonical: [1, 0, 0]).
    public void moveRight(int amount) {
        int[] rot = getRotationMatrix();
        int[] localRight = new int[]{FixedBaseMath.toQ24_8(1.0f), 0, 0, 0};
        int[] worldRight = new int[4];
        FixedMatMath.transformPoint(rot, localRight, worldRight);
        position[0] = FixedBaseMath.q24_8_add(position[0], FixedBaseMath.q24_8_mul(worldRight[0], amount));
        position[1] = FixedBaseMath.q24_8_add(position[1], FixedBaseMath.q24_8_mul(worldRight[1], amount));
        position[2] = FixedBaseMath.q24_8_add(position[2], FixedBaseMath.q24_8_mul(worldRight[2], amount));
    }

    // Moves the camera along its local up axis (canonical: [0, 1, 0]).
    public void moveUp(int amount) {
        int[] rot = getRotationMatrix();
        int[] localUp = new int[]{0, FixedBaseMath.toQ24_8(1.0f), 0, 0};
        int[] worldUp = new int[4];
        FixedMatMath.transformPoint(rot, localUp, worldUp);
        position[0] = FixedBaseMath.q24_8_add(position[0], FixedBaseMath.q24_8_mul(worldUp[0], amount));
        position[1] = FixedBaseMath.q24_8_add(position[1], FixedBaseMath.q24_8_mul(worldUp[1], amount));
        position[2] = FixedBaseMath.q24_8_add(position[2], FixedBaseMath.q24_8_mul(worldUp[2], amount));
    }
}