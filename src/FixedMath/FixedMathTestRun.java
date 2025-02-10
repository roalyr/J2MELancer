package FixedMath;

import javax.microedition.midlet.MIDlet;

/**
 * Main test runner MIDlet.
 */
public class FixedMathTestRun extends MIDlet {

    public void startApp() {
        FixedBaseMathExtensiveTest.main();
        FixedTrigMathEdgeCasesTest.main();
        FixedVecMathTest.main();
        FixedVecMathEdgeCasesTest.main();
        FixedMatMathTest.main();

        destroyApp(false);
        notifyDestroyed();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
}

// ====================================
// Extensive tests for FixedBaseMath
// ====================================
class FixedBaseMathExtensiveTest {

    public static void main() {
        System.out.println("=== FixedBaseMath Extensive Tests ===");
        System.out.println("sqrt(2.25) = " + testSqrt(2.25f) + " (expected ~1.5)");
        System.out.println("sqrt(16.0) = " + testSqrt(16.0f) + " (expected ~4.0)");
        // Add more tests as needed...
        System.out.println();
    }

    private static float testSqrt(float x) {
        int fixX = FixedBaseMath.toFixed(x);
        int sqX = FixedBaseMath.sqrt(fixX);
        return FixedBaseMath.toFloat(sqX);
    }
}

// ====================================
// Edge cases tests for FixedTrigMath
// ====================================
class FixedTrigMathEdgeCasesTest {

    private static final int[] TEST_DEGREES = {0, 15, 30, 45, 60, 90, 120, 135, 180, 225, 270, 315, 359};
    private static final float[] TEST_ACOS_X = {-1.0f, -0.7071f, -0.5f, 0.0f, 0.5f, 0.7071f, 1.0f};

    public static void main() {
        System.out.println("\n=== FixedTrigMath Angle Tests ===\n");
        System.out.println("Deg | angle(Q24.8) | sin(fixed->float) | sin(Math)   | cos(fixed->float) | cos(Math)   | tan(fixed->float) | tan(Math)");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (int i = 0; i < TEST_DEGREES.length; i++) {
            int deg = TEST_DEGREES[i];
            int angleQ = FixedTrigMath.degreesToRadians(deg);
            int sinFix = FixedTrigMath.sin(angleQ);
            int cosFix = FixedTrigMath.cos(angleQ);
            int tanFix = FixedTrigMath.tan(angleQ);
            float sinF = FixedBaseMath.toFloat(sinFix);
            float cosF = FixedBaseMath.toFloat(cosFix);
            float tanF = FixedBaseMath.toFloat(tanFix);
            double rad = Math.toRadians(deg);
            double sinActual = Math.sin(rad);
            double cosActual = Math.cos(rad);
            double tanActual = (Math.abs(cosActual) > 1e-12) ? Math.tan(rad) : Double.NaN;
            System.out.println(
                    pad(Integer.toString(deg), 3) + " | " +
                    pad(Integer.toString(angleQ), 11) + " | " +
                    pad(fmt(sinF), 18) + " | " +
                    pad(fmt((float) sinActual), 11) + " | " +
                    pad(fmt(cosF), 18) + " | " +
                    pad(fmt((float) cosActual), 11) + " | " +
                    pad(fmt(tanF), 18) + " | " +
                    pad(fmt((float) tanActual), 11));
        }

        System.out.println("\n=== FixedTrigMath ACOS Tests ===\n");
        System.out.println("  x      | x(Q24.8)  | acos(fixed->deg) | approx acos(deg)");
        System.out.println("--------------------------------------------------------------");
        for (int i = 0; i < TEST_ACOS_X.length; i++) {
            float x = TEST_ACOS_X[i];
            int xFix = FixedBaseMath.toFixed(x);
            int acosFix = FixedTrigMath.acos(xFix);
            float acosRad = FixedBaseMath.toFloat(acosFix);
            float acosDeg = (float) (acosRad * 180.0 / Math.PI);
            // Reference approximation using a simple formula for comparison.
            double approxRad = (Math.PI * 0.5) - x - (0.14159 * x * x * x);
            float approxDeg = (float) (approxRad * 180.0 / Math.PI);
            System.out.println(
                    pad(fmt(x), 8) + " | " +
                    pad(Integer.toString(xFix), 9) + " | " +
                    pad(fmt(acosDeg), 17) + " | " +
                    fmt(approxDeg));
        }
        System.out.println();
    }

    private static String fmt(float val) {
        int scaled = (int) (val * 10000f);
        float shown = (float) scaled / 10000f;
        return Float.toString(shown);
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}

// ====================================
// Tests for FixedVecMath basic operations
// ====================================
class FixedVecMathTest {

    public static void main() {
        System.out.println("\n=== FixedVecMath Basic Tests ===\n");
        int[] vecA = {FixedBaseMath.toFixed(1.0f), 0, 0};
        int[] vecB = {0, FixedBaseMath.toFixed(2.0f), 0};


        int angleFix = FixedVecMath.angleBetweenVectors(vecA, vecB);
        float angleDeg = (float) Math.toDegrees(FixedBaseMath.toFloat(angleFix));
        System.out.println("Angle between A and B: " + angleDeg + " deg (expected 90 deg)");


    }
}

// ====================================
// Edge cases tests for FixedVecMath
// ====================================
class FixedVecMathEdgeCasesTest {

    public static void main() {
        System.out.println("\n=== FixedVecMath Edge Cases Tests ===\n");
        int[] zeroVec = {0, 0, 0};

        int[] v1 = {FixedBaseMath.toFixed(15.0f), 0, 0};
        int[] v2 = {FixedBaseMath.toFixed(50.0f), 0, 0};
        testAngle("Parallel vectors (15,0,0) vs (50,0,0)", v1, v2);

        int[] v3 = {FixedBaseMath.toFixed(-20.0f), FixedBaseMath.toFixed(-1.0f), 0};
        testAngle("Anti-parallel vectors (15,0,0) vs (-20,-1,0)", v1, v3);

        int bigComp = FixedBaseMath.toFixed(32768.0f);
        int[] bigVec = {bigComp, bigComp, bigComp};
        int[] shortVec = {FixedBaseMath.toFixed(3.0f), FixedBaseMath.toFixed(-4.0f)};
        int[] longVec = {FixedBaseMath.toFixed(1.0f), 0, 0, 0};
        System.out.println();
    }

    private static void testAngle(String label, int[] vA, int[] vB) {
        int angFix = FixedVecMath.angleBetweenNormalized(vA, vB);
        float angDeg = (float) Math.toDegrees(FixedBaseMath.toFloat(angFix));
        System.out.println(label + " => " + angDeg + " deg");
    }

    private static String vecToStr(int[] v) {
        StringBuffer sb = new StringBuffer("(");
        for (int i = 0; i < v.length; i++) {
            sb.append(FixedBaseMath.toFloat(v[i]));
            if (i < v.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

// ====================================
// Extensive tests for FixedMatMath
// ====================================
class FixedMatMathTest {

    public static void main() {
        System.out.println("\n=== FixedMatMath Extensive Tests ===\n");

        // Test 1: Identity Matrix
        int[] ident = FixedMatMath.createIdentity4x4();
        printMatrix("Identity Matrix", ident);

        // Test 2: Translation Matrix
        int tx = FixedBaseMath.toFixed(3.0f);
        int ty = FixedBaseMath.toFixed(-2.0f);
        int tz = FixedBaseMath.toFixed(10.0f);
        int[] transM = FixedMatMath.createTranslation4x4(tx, ty, tz);
        printMatrix("Translation Matrix (3, -2, 10)", transM);

        int[] point123 = {
            FixedBaseMath.toFixed(1.0f),
            FixedBaseMath.toFixed(2.0f),
            FixedBaseMath.toFixed(3.0f)
        };

        // Test 3: Scale Matrix
        int sx = FixedBaseMath.toFixed(2.0f);
        int sy = FixedBaseMath.toFixed(0.5f);
        int sz = FixedBaseMath.toFixed(-1.0f);
        int[] scaleM = FixedMatMath.createScale4x4(sx, sy, sz);
        printMatrix("Scale Matrix (2, 0.5, -1)", scaleM);

        int[] vec345 = {
            FixedBaseMath.toFixed(3.0f),
            FixedBaseMath.toFixed(4.0f),
            FixedBaseMath.toFixed(5.0f)
        };
        int[] scaledVec = FixedMatMath.transformVector4x4(scaleM, vec345);
        printVec3("Scaled Vector (3,4,5)", scaledVec); // Expected: (6,2,-5)

        // Test 4: Rotation about X-axis (90 degrees)
        int angle90 = FixedBaseMath.toFixed((float) Math.toRadians(90.0f));
        int[] rotX = FixedMatMath.createRotationX4x4(angle90);
        printMatrix("RotationX Matrix (90 deg)", rotX);

        int[] vec020 = {0, FixedBaseMath.toFixed(2.0f), 0};
        int[] rotatedVec = FixedMatMath.transformVector4x4(rotX, vec020);
        printVec3("Rotated Vector (0,2,0) about X", rotatedVec);

        // Test 5: Matrix Multiplication (RotationZ 45 deg then Translation)
        int angle45 = FixedBaseMath.toFixed((float) Math.toRadians(45.0f));
        int[] rotZ45 = FixedMatMath.createRotationZ4x4(angle45);
        int[] comboM = FixedMatMath.multiply4x4(rotZ45, transM);
        printMatrix("Combo Matrix (RotZ 45 deg * Translation)", comboM);

        // Test 6: LookAt Matrix
        int[] eye = {0, 0, FixedBaseMath.toFixed(10.0f)};
        int[] target = {0, 0, 0};
        int[] up = {0, FixedBaseMath.toFixed(1.0f), 0};
        int[] lookAtM = FixedMatMath.createLookAt4x4(eye, target, up);
        printMatrix("LookAt Matrix (eye=(0,0,10), target=(0,0,0))", lookAtM);

        // Test 7: Perspective Projection Matrix
        int fovDeg = 60;
        int fovRad = FixedBaseMath.toFixed((float) Math.toRadians(fovDeg));
        int aspect = FixedBaseMath.toFixed(1.333f);
        int nearQ = FixedBaseMath.toFixed(1.0f);
        int farQ = FixedBaseMath.toFixed(100.0f);
        int[] perspM = FixedMatMath.createPerspective4x4(fovRad, aspect, nearQ, farQ);
        printMatrix("Perspective Projection Matrix", perspM);

        // Test 8: Edge Cases
        int[] rotZ0 = FixedMatMath.createRotationZ4x4(0);
        printMatrix("RotationZ Matrix (0 deg)", rotZ0);

        int[] scaleZero = FixedMatMath.createScale4x4(0, 0, 0);
        printMatrix("Scale Matrix (0,0,0)", scaleZero);

        int bigX = FixedBaseMath.toFixed(10000.0f);
        int bigY = FixedBaseMath.toFixed(-9999.0f);
        int bigZ = FixedBaseMath.toFixed(1.0f);
        int[] bigTrans = FixedMatMath.createTranslation4x4(bigX, bigY, bigZ);
        printMatrix("Translation Matrix (10000, -9999, 1)", bigTrans);

        System.out.println("\n=== End of FixedMatMath Tests ===\n");
    }

    private static String fmtQ(int q) {
        float f = FixedBaseMath.toFloat(q);
        int scaled = (int) (f * 10000f);
        float shown = (float) scaled / 10000f;
        return Float.toString(shown);
    }

    private static void printMatrix(String label, int[] m) {
        System.out.println(label + ":");
        for (int row = 0; row < 4; row++) {
            StringBuffer sb = new StringBuffer("  ");
            for (int col = 0; col < 4; col++) {
                int val = m[row * 4 + col];
                sb.append(fmtQ(val));
                if (col < 3) {
                    sb.append(",\t");
                }
            }
            System.out.println(sb.toString());
        }
        System.out.println();
    }

    private static void printVec4(String label, int[] vec4) {
        System.out.print(label + " = (");
        for (int i = 0; i < 4; i++) {
            System.out.print(fmtQ(vec4[i]));
            if (i < 3) {
                System.out.print(", ");
            }
        }
        System.out.println(")");
    }

    private static void printVec3(String label, int[] vec3) {
        System.out.print(label + " = (");
        for (int i = 0; i < 3; i++) {
            System.out.print(fmtQ(vec3[i]));
            if (i < 2) {
                System.out.print(", ");
            }
        }
        System.out.println(")");
    }
}