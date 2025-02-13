package Renderer;

import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import FixedMath.FixedQuatMath;

public class Camera {

    private long[] viewMatrix; // The view matrix (inverse of world transform)
    private long[] position;   // Position in world space (Q24.8)
    // Orientation as a quaternion [x, y, z, w] in Q24.8
    private long[] orientation;
    private long[] rotMatrix;
    private long[] rotMatrixT;
    private long[] trans;

    public Camera() {
        viewMatrix = FixedMatMath.createIdentity4x4();
        position = new long[]{0, 0, 0};
        // Persistent orientation starts as identity.
        orientation = new long[]{0, 0, 0, FixedBaseMath.FIXED1};
    }

    // Returns the view matrix used for rendering.
    public long[] getViewMatrix() {
        updateViewMatrix();
        return viewMatrix;
    }

    // Computes the view matrix as R^T * T(-position), where R is derived from orientation.
    private void updateViewMatrix() {
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

    public long[] getPosition() {
        return position;
    }

    public void setPosition(long x, long y, long z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public long[] getOrientation() {
        return orientation;
    }

    public void setOrientation(long[] q) {
        if (orientation != null) {
            FixedQuatMath.releaseQuaternion(orientation);
        }
        orientation = q;
    }

    // Applies a yaw rotation (about local up: [0,1,0]).
    public void addYaw(long angleQ) {
        long[] delta = FixedQuatMath.fromAxisAngle(new long[]{0, FixedBaseMath.FIXED1, 0}, angleQ);
        long[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        long[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Applies a pitch rotation (about local right: [1,0,0]).
    public void addPitch(long angleQ) {
        long[] delta = FixedQuatMath.fromAxisAngle(new long[]{FixedBaseMath.FIXED1, 0, 0}, angleQ);
        long[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        long[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Applies a roll rotation (about local forward: [0,0,1]).
    public void addRoll(long angleQ) {
        long[] delta = FixedQuatMath.fromAxisAngle(new long[]{0, 0, FixedBaseMath.FIXED1}, angleQ);
        long[] multResult = FixedQuatMath.multiply(orientation, delta);
        FixedQuatMath.releaseQuaternion(delta);
        long[] normResult = FixedQuatMath.normalize(multResult);
        FixedQuatMath.releaseQuaternion(multResult);
        FixedQuatMath.releaseQuaternion(orientation);
        orientation = normResult;
    }

    // Returns the current rotation matrix derived from orientation.
    public long[] getRotationMatrix() {
        return FixedQuatMath.toRotationMatrix(orientation);
    }

    // --- Translation Methods using current orientation ---

    public void moveForward(long amount) {
        long[] rot = getRotationMatrix();
        long[] localForward = new long[]{0, 0, -FixedBaseMath.FIXED1, 0};
        long[] worldForward = new long[4];
        FixedMatMath.transformPoint(rot, localForward, worldForward);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldForward[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldForward[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldForward[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }

    public void moveRight(long amount) {
        long[] rot = getRotationMatrix();
        long[] localRight = new long[]{FixedBaseMath.FIXED1, 0, 0, 0};
        long[] worldRight = new long[4];
        FixedMatMath.transformPoint(rot, localRight, worldRight);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldRight[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldRight[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldRight[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }

    public void moveUp(long amount) {
        long[] rot = getRotationMatrix();
        long[] localUp = new long[]{0, FixedBaseMath.FIXED1, 0, 0};
        long[] worldUp = new long[4];
        FixedMatMath.transformPoint(rot, localUp, worldUp);
        position[0] = FixedBaseMath.fixedAdd(position[0], FixedBaseMath.fixedMul(worldUp[0], amount));
        position[1] = FixedBaseMath.fixedAdd(position[1], FixedBaseMath.fixedMul(worldUp[1], amount));
        position[2] = FixedBaseMath.fixedAdd(position[2], FixedBaseMath.fixedMul(worldUp[2], amount));
        FixedMatMath.releaseMatrix(rot);
    }
}
