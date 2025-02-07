package Renderer;

import FixedMath.*;

public class PhysicalCamera {

    private int massQ;
    private int inertiaQ;

    private int[] position; // [x, y, z]
    private int pitchQ, yawQ, rollQ;

    private int[] velocity;        // [vx, vy, vz]
    private int[] angularVelocity; // [wx, wy, wz]

    private int[] forceAccum;
    private int[] torqueAccum;

    public PhysicalCamera() {
        massQ    = FixedBaseMath.toQ24_8(1.0f);
        inertiaQ = FixedBaseMath.toQ24_8(1.0f);

        position = new int[]{0, 0, 0};
        pitchQ   = 0;
        yawQ     = 0;
        rollQ    = 0;

        velocity        = new int[]{0, 0, 0};
        angularVelocity = new int[]{0, 0, 0};

        forceAccum  = new int[]{0, 0, 0};
        torqueAccum = new int[]{0, 0, 0};
    }

    public int[] getPosition() {
        return position;
    }
    public void setPosition(int x, int y, int z) {
        position[0] = x; position[1] = y; position[2] = z;
    }

    public int getPitchQ() {
        return pitchQ;
    }
    public int getYawQ() {
        return yawQ;
    }
    public int getRollQ() {
        return rollQ;
    }

    public void setPitchQ(int p) {
        pitchQ = p;
    }
    public void setYawQ(int y) {
        yawQ = y;
    }
    public void setRollQ(int r) {
        rollQ = r;
    }

    public int[] getVelocity() {
        return velocity;
    }
    public int[] getAngularVelocity() {
        return angularVelocity;
    }

    public void setMassQ(int m) {
        massQ = m;
    }
    public void setInertiaQ(int i) {
        inertiaQ = i;
    }

    public void clearForces() {
        forceAccum[0] = forceAccum[1] = forceAccum[2] = 0;
        torqueAccum[0] = torqueAccum[1] = torqueAccum[2] = 0;
    }
    public void addForce(int fx, int fy, int fz) {
        forceAccum[0] += fx;
        forceAccum[1] += fy;
        forceAccum[2] += fz;
    }
    public void addTorque(int tx, int ty, int tz) {
        torqueAccum[0] += tx;
        torqueAccum[1] += ty;
        torqueAccum[2] += tz;
    }

    public void integrate(int dtQ24_8) {
        int accX = FixedBaseMath.q24_8_div(forceAccum[0], massQ);
        int accY = FixedBaseMath.q24_8_div(forceAccum[1], massQ);
        int accZ = FixedBaseMath.q24_8_div(forceAccum[2], massQ);

        velocity[0] = FixedBaseMath.q24_8_add(velocity[0], FixedBaseMath.q24_8_mul(accX, dtQ24_8));
        velocity[1] = FixedBaseMath.q24_8_add(velocity[1], FixedBaseMath.q24_8_mul(accY, dtQ24_8));
        velocity[2] = FixedBaseMath.q24_8_add(velocity[2], FixedBaseMath.q24_8_mul(accZ, dtQ24_8));

        position[0] = FixedBaseMath.q24_8_add(position[0], FixedBaseMath.q24_8_mul(velocity[0], dtQ24_8));
        position[1] = FixedBaseMath.q24_8_add(position[1], FixedBaseMath.q24_8_mul(velocity[1], dtQ24_8));
        position[2] = FixedBaseMath.q24_8_add(position[2], FixedBaseMath.q24_8_mul(velocity[2], dtQ24_8));

        int ax = FixedBaseMath.q24_8_div(torqueAccum[0], inertiaQ);
        int ay = FixedBaseMath.q24_8_div(torqueAccum[1], inertiaQ);
        int az = FixedBaseMath.q24_8_div(torqueAccum[2], inertiaQ);

        angularVelocity[0] = FixedBaseMath.q24_8_add(angularVelocity[0], FixedBaseMath.q24_8_mul(ax, dtQ24_8));
        angularVelocity[1] = FixedBaseMath.q24_8_add(angularVelocity[1], FixedBaseMath.q24_8_mul(ay, dtQ24_8));
        angularVelocity[2] = FixedBaseMath.q24_8_add(angularVelocity[2], FixedBaseMath.q24_8_mul(az, dtQ24_8));

        pitchQ = FixedBaseMath.q24_8_add(pitchQ, FixedBaseMath.q24_8_mul(angularVelocity[0], dtQ24_8));
        yawQ   = FixedBaseMath.q24_8_add(yawQ, FixedBaseMath.q24_8_mul(angularVelocity[1], dtQ24_8));
        rollQ  = FixedBaseMath.q24_8_add(rollQ, FixedBaseMath.q24_8_mul(angularVelocity[2], dtQ24_8));
    }

    public int[] getCameraMatrix() {
        int[] mat = FixedMatMath.createIdentity4x4();
        int[] rxInv = FixedMatMath.createRotationX4x4(-pitchQ);
        int[] ryInv = FixedMatMath.createRotationY4x4(-yawQ);
        int[] rzInv = FixedMatMath.createRotationZ4x4(-rollQ);

        mat = FixedMatMath.multiply4x4(mat, rzInv);
        mat = FixedMatMath.multiply4x4(mat, ryInv);
        mat = FixedMatMath.multiply4x4(mat, rxInv);

        int[] tInv = FixedMatMath.createTranslation4x4(-position[0], -position[1], -position[2]);
        mat = FixedMatMath.multiply4x4(mat, tInv);
        return mat;
    }

    public int[] getRotationMatrix() {
        int[] mat = FixedMatMath.createIdentity4x4();

        int[] rx = FixedMatMath.createRotationX4x4(pitchQ);
        int[] ry = FixedMatMath.createRotationY4x4(yawQ);
        int[] rz = FixedMatMath.createRotationZ4x4(rollQ);

        mat = FixedMatMath.multiply4x4(mat, rx);
        mat = FixedMatMath.multiply4x4(mat, ry);
        mat = FixedMatMath.multiply4x4(mat, rz);

        return mat;
    }
}