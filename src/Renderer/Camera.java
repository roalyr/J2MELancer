package Renderer;

import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import FixedMath.FixedQuatMath;

public class Camera {

    private int[] viewMatrix; // The view matrix (inverse of world transform)
    private int[] position;   // Position in world space (Q24.8)
    // Orientation as a quaternion [x, y, z, w] in Q24.8
    private int[] orientation;
    private int[] rotMatrix;
    private int[] rotMatrixT;
    private int[] trans;

    public Camera() {
        viewMatrix = FixedMatMath.createIdentity4x4();
        position = new int[]{0, 0, 0};
        // Persistent orientation starts as identity.
        orientation = new int[]{0, 0, 0, FixedBaseMath.toFixed(1.0f)};
    }

    // Returns the view matrix used for rendering.
    public int[] getViewMatrix() {
        updateViewMatrix();
        return viewMatrix;
    }

    // Computes the view matrix as R^T * T(-position), where R is derived from orientation.
    private void updateViewMatrix() {
        // Release previous viewMatrix if any.
        if (viewMatrix != null) {
            FixedMatMath.releaseMatrix(viewMatrix);
        }
        rotMatrix = FixedQuatMath.toRotationMatrix(orientation);
        rotMatrixT = FixedMatMath.transpose(rotMatrix);
        trans = FixedMatMath.createTranslation4x4(-position[0], -position[1], -position[2]);
        viewMatrix = FixedMatMath.multiply4x4(rotMatrixT, trans);
        FixedMatMath.releaseMatrix(rotMatrix);
        FixedMatMath.releaseMatrix(rotMatrixT);
        FixedMatMath.releaseMatrix(trans);
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
        // If the current orientation was acquired from the pool, release it.
        if (orientation != null) {
            FixedQuatMath.releaseQuaternion(orientation);
        }
        orientation = q;
    }

    // --- Camera Turning Methods (update persistent orientation in place) ---

    // Applies a yaw rotation (about local up: [0,1,0]).
    public void addYaw(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{0, FixedBaseMath.toFixed(1.0f), 0}, angleQ);
        int[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        int[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        // Release the old persistent orientation.
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Applies a pitch rotation (about local right: [1,0,0]).
    public void addPitch(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{FixedBaseMath.toFixed(1.0f), 0, 0}, angleQ);
        int[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        int[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Applies a roll rotation (about local forward: [0,0,1]).
    public void addRoll(int angleQ) {
        int[] delta = FixedQuatMath.fromAxisAngle(new int[]{0, 0, FixedBaseMath.toFixed(1.0f)}, angleQ);
        int[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        int[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Returns the current rotation matrix derived from orientation.
    public int[] getRotationMatrix() {
        return FixedQuatMath.toRotationMatrix(orientation);
    }

    // --- Translation Methods using current orientation ---

    public void moveForward(int amount) {
        int[] rot = getRotationMatrix();
        int[] localForward = new int[]{0, 0, -FixedBaseMath.toFixed(1.0f), 0};
        int[] worldForward = new int[4]; // Temporary (not pooled)
        FixedMatMath.transformPoint(rot, localForward, worldForward);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldForward[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldForward[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldForward[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }

    public void moveRight(int amount) {
        int[] rot = getRotationMatrix();
        int[] localRight = new int[]{FixedBaseMath.toFixed(1.0f), 0, 0, 0};
        int[] worldRight = new int[4];
        FixedMatMath.transformPoint(rot, localRight, worldRight);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldRight[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldRight[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldRight[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }

    public void moveUp(int amount) {
        int[] rot = getRotationMatrix();
        int[] localUp = new int[]{0, FixedBaseMath.toFixed(1.0f), 0, 0};
        int[] worldUp = new int[4];
        FixedMatMath.transformPoint(rot, localUp, worldUp);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldUp[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldUp[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldUp[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }
}