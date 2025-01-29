package FixedMath;

import javax.microedition.midlet.*;

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
class FixedBaseMathExtensiveTest {

    public static void main() {
        System.out.println("1) 1/0.4 = " + testOneOverX(0.4f) + " (expected ~2.5)");
        System.out.println("2) sqrt(2.25) = " + testSqrt(2.25f) + " (expected ~1.5)");
        System.out.println("3) sqrt(16.0) = " + testSqrt(16.0f) + " (expected ~4.0)");
        System.out.println("4) 1/0 => " + testOneOverX(0.0f) + " (sentinel)");
    // etc...
    }

    private static float testOneOverX(float x) {
        int fixX = FixedBaseMath.toQ24_8(x);
        int recX = FixedBaseMath.reciprocal(fixX);
        return FixedBaseMath.toFloat(recX);
    }

    private static float testSqrt(float x) {
        int fixX = FixedBaseMath.toQ24_8(x);
        int sqX = FixedBaseMath.sqrt(fixX);
        return FixedBaseMath.toFloat(sqX);
    }
}

// ============================================
class FixedTrigMathEdgeCasesTest {

    // We'll test these degree angles for sin/cos/tan
    private static final int[] TEST_DEGREES = {
        0, 15, 30, 45, 60, 90, 120, 135, 180, 225, 270, 315, 359
    };

    // We'll test these x values in [-1..1] for acos
    private static final float[] TEST_ACOS_X = {
        -1.0f, -0.7071f, -0.5f, 0.0f, 0.5f, 0.7071f, 1.0f
    };

    public static void main() {
        System.out.println("\n=== Testing Multiple Angles for sin/cos/tan ===\n");

        System.out.println("Deg | angle256  | sin(fixed->float)  sin(actual)   " + "cos(fixed->float)  cos(actual)   tan(fixed->float)  tan(actual)");
        System.out.println("-------------------------------------------------------------------------------------");

        // Old-style loop
        for (int i = 0; i < TEST_DEGREES.length; i++) {
            int deg = TEST_DEGREES[i];
            // convert degrees => [0..255]
            int angle256 = FixedTrigMath.degToAngle256((float) deg);

            int sinFix = FixedTrigMath.sin(angle256);
            int cosFix = FixedTrigMath.cos(angle256);
            int tanFix = FixedTrigMath.tan(angle256);

            float sinVal = FixedTrigMath.toFloat(sinFix);
            float cosVal = FixedTrigMath.toFloat(cosFix);
            float tanVal = FixedTrigMath.toFloat(tanFix);

            // Compare to Java's built-in math (if available). If Math.toRadians is absent, do deg*(Math.PI/180)
            double rad = Math.toRadians(deg);
            double sinActual = Math.sin(rad);
            double cosActual = Math.cos(rad);
            double tanActual = 0.0;
            if (Math.abs(cosActual) > 1e-12) {
                tanActual = Math.tan(rad);
            }

            // Print row
            System.out.println(
                    pad(Integer.toString(deg), 3) + " | " +
                    pad(Integer.toString(angle256), 9) + " | " +
                    pad(fmt(sinVal), 17) + "  " +
                    pad(fmt((float) sinActual), 11) + "  " +
                    pad(fmt(cosVal), 17) + "  " +
                    pad(fmt((float) cosActual), 11) + "  " +
                    pad(fmt(tanVal), 17) + "  " +
                    pad(fmt((float) tanActual), 11));
        }

        System.out.println("\n=== Testing ACOS for multiple inputs in [-1..1] ===\n");
        System.out.println(" x       | x(Q24.8)   | acos(fixed->deg)  approx poly(deg)");
        System.out.println("----------------------------------------------------------");

        for (int i = 0; i < TEST_ACOS_X.length; i++) {
            float x = TEST_ACOS_X[i];

            // convert x => Q24.8
            int xFix = FixedTrigMath.toQ24_8(x);
            // table-based approximation
            int aFix = FixedTrigMath.acos(xFix);
            float aRad = FixedTrigMath.toFloat(aFix);
            float aDeg = (float) (aRad * 180.0 / Math.PI);

            // "reference" polynomial in double
            //   acos(x) ~ pi/2 - x - 0.14159*x^3
            // We'll just do that for comparison, since no Math.acos in J2ME
            double x3 = x * x * x;
            double approxRad = (Math.PI * 0.5) - x - (0.14159 * x3);
            float approxDeg = (float) (approxRad * 180.0 / Math.PI);

            System.out.println(
                    pad(fmt(x), 8) + " | " +
                    pad(Integer.toString(xFix), 10) + " | " +
                    pad(fmt(aDeg), 16) + "    " +
                    fmt(approxDeg));
        }
    }

    /**
     * Simple "round-ish" printing, shows ~4 decimals.
     */
    private static String fmt(float val) {
        int scaled = (int) (val * 10000f); // e.g. 1.23456 -> 12345

        float shown = (float) scaled / 10000f;
        return Float.toString(shown);
    }

    /**
     * Quick column aligner.
     */
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


// ===================================================
class FixedVecMathTest {

    public static void main() {
        // Vector A = (1.0, 0.0, 0.0) in Q24.8 => (256, 0, 0)
        int[] vecA = {FixedBaseMath.toQ24_8(1.0f), 0, 0};
        // Vector B = (0.0, 2.0, 0.0) => (0, 512, 0)
        int[] vecB = {0, FixedBaseMath.toQ24_8(2.0f), 0};

        // 1) magnitude(A) => 1.0 => Q24.8=256
        int magA = FixedVecMath.q24_8_magnitude(vecA);
        float magAf = FixedBaseMath.toFloat(magA);
        System.out.println("magnitude(A) => " + magAf + " (expected 1.0)");

        // 2) magnitude(B) => 2.0 => Q24.8=512
        int magB = FixedVecMath.q24_8_magnitude(vecB);
        float magBf = FixedBaseMath.toFloat(magB);
        System.out.println("magnitude(B) => " + magBf + " (expected 2.0)");

        // 3) angleBetween(A, B) => 90 degrees => ~1.5708 rad
        int angleFix = FixedVecMath.angleBetweenVectors(vecA, vecB);
        float angleRad = FixedBaseMath.toFloat(angleFix);
        float angleDeg = (float) Math.toDegrees(angleRad);
        System.out.println("angleBetween(A, B) => " + angleDeg + " degrees (expected 90)");

        // 4) cross(A, B) => (0, 0, A.x*B.y - A.y*B.x) => (0, 0, 1.0*2.0)= (0,0,2.0)
        int[] crossAB = FixedVecMath.q24_8_crossProduct(vecA, vecB);
        System.out.println("cross(A,B) => (" +
                FixedBaseMath.toFloat(crossAB[0]) + ", " +
                FixedBaseMath.toFloat(crossAB[1]) + ", " +
                FixedBaseMath.toFloat(crossAB[2]) + ") (expected (0,0,2))");

        // 5) normalize(B) => B / 2 => => (0,1,0)
        int[] normB = FixedVecMath.q24_8_normalize(vecB);
        System.out.println("normalize(B) => (" +
                FixedBaseMath.toFloat(normB[0]) + ", " +
                FixedBaseMath.toFloat(normB[1]) + ", " +
                FixedBaseMath.toFloat(normB[2]) + ") (expected (0,1,0))");
    }
}

class FixedVecMathEdgeCasesTest {

    public static void main() {
        // 1) Zero vector
        int[] zeroVec = {0, 0, 0};
        System.out.println("Zero vector magnitude => " + FixedBaseMath.toFloat(FixedVecMath.q24_8_magnitude(zeroVec)));
        System.out.println("Zero vector normalized => " + vecToStr(FixedVecMath.q24_8_normalize(zeroVec)));

        // TODO: Improve this
        // 2) Parallel vectors
        int[] v1 = {
            FixedBaseMath.toQ24_8(15.0f),
            0,
            0
        };
        int[] v2 = {
            FixedBaseMath.toQ24_8(50.00f),
            0,
            0
        };
        testAngle("Parallel (15,0,0) vs (50,0,0)", v1, v2);

        // 3) Anti-parallel within threshold?
        int[] v3 = {
            FixedBaseMath.toQ24_8(-20.0f),
            FixedBaseMath.toQ24_8(-1.00f),
            0
        };
        testAngle("Anti-parallel (15,0,0) vs (-20,-1,0)", v1, v3);

        // 4) Large magnitude
        // e.g. let's try something near 32768 as a float => ~8 million in Q24.8
        int bigComp = FixedBaseMath.toQ24_8(32768.0f);
        int[] bigVec = {bigComp, bigComp, bigComp};
        System.out.println("\nLarge vector magnitude => " + FixedBaseMath.toFloat(FixedVecMath.q24_8_magnitude(bigVec)));

        // 5) 2D mismatch example
        int[] shortVec = {FixedBaseMath.toQ24_8(3.0f), FixedBaseMath.toQ24_8(-4.0f)};
        int[] longVec = {FixedBaseMath.toQ24_8(1.0f), 0, 0, 0};
        int dotMismatch = FixedVecMath.q24_8_dotProduct(shortVec, longVec);
        System.out.println("\nDot product of (3,-4) and (1,0,0,0) => " + FixedBaseMath.toFloat(dotMismatch) + "  (only first 2 comps used)");
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

// ==================================
class FixedMatMathTest {

    // Quick helper for converting a Q24.8 to float and printing about 4 decimals
    private static String fmtQ(int q) {
        float f = FixedBaseMath.toFloat(q);
        int scaled = (int) (f * 10000f);
        float shown = (float) scaled / 10000f;
        return Float.toString(shown);
    }

    // Print matrix as 4 rows
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
    }

    private static void printVec4(String label, int[] vec4) {
        // vec4 has 4 elements
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
        // vec3 has 3 elements
        System.out.print(label + " = (");
        for (int i = 0; i < 3; i++) {
            System.out.print(fmtQ(vec3[i]));
            if (i < 2) {
                System.out.print(", ");
            }
        }
        System.out.println(")");
    }

    public static void main() {
        System.out.println("=== FixedMatMath Extensive Test ===\n");

        // 1) Identity
        int[] ident = FixedMatMath.createIdentity4x4();
        printMatrix("Identity Matrix", ident);

        // 2) Translation test: translate by (3, -2, 10).
        int tx = FixedBaseMath.toQ24_8(3.0f);
        int ty = FixedBaseMath.toQ24_8(-2.0f);
        int tz = FixedBaseMath.toQ24_8(10.0f);
        int[] transM = FixedMatMath.createTranslation4x4(tx, ty, tz);
        printMatrix("\nTranslation (3, -2, 10)", transM);

        // Transform a point (1,2,3).  Expect => (4,0,13).
        int[] point123 = {
            FixedBaseMath.toQ24_8(1.0f),
            FixedBaseMath.toQ24_8(2.0f),
            FixedBaseMath.toQ24_8(3.0f)
        };
        int[] resultP = FixedMatMath.transformPoint4x4(transM, point123);
        printVec4("Transformed (1,2,3)", resultP);

        // 3) Scale test: scale by (2,0.5,-1)
        int sx = FixedBaseMath.toQ24_8(2.0f);
        int sy = FixedBaseMath.toQ24_8(0.5f);
        int sz = FixedBaseMath.toQ24_8(-1.0f);
        int[] scaleM = FixedMatMath.createScale4x4(sx, sy, sz);
        printMatrix("\nScale (2, 0.5, -1)", scaleM);

        // Scale a vector (3,4,5) => (6,2, -5).
        int[] vec345 = {
            FixedBaseMath.toQ24_8(3.0f),
            FixedBaseMath.toQ24_8(4.0f),
            FixedBaseMath.toQ24_8(5.0f),
        };
        int[] resultV = FixedMatMath.transformVector4x4(scaleM, vec345);
        printVec3("Scaled (3,4,5)", resultV);

        // 4) Rotation test: rotate around X by 90° => (y=>z, z=> -y)
        int angle90 = FixedBaseMath.toQ24_8((float) Math.toRadians(90.0f));
        int[] rotX = FixedMatMath.createRotationX4x4(angle90);
        printMatrix("\nRotationX(90 deg)", rotX);

        // Transform (0,2,0) => (0,0,2)? Actually => (0,0,-2) or (0,0,2) depending on sign convention
        int[] vec020 = {0, FixedBaseMath.toQ24_8(2.0f), 0};
        int[] resultRot = FixedMatMath.transformVector4x4(rotX, vec020);
        printVec3("Rotate(0,2,0) around X=90 deg", resultRot);

        // 5) Multiply two transforms: rotateZ(45) then translate(1,2,3)
        int angle45 = FixedBaseMath.toQ24_8((float) Math.toRadians(45.0f));
        int[] rotZ45 = FixedMatMath.createRotationZ4x4(angle45);
        int[] combo = FixedMatMath.multiply4x4(rotZ45, transM);
        printMatrix("\nRotZ(45 deg)*Translate(3,-2,10)", combo);

        // 6) createLookAt(eye=(0,0,10), target=(0,0,0), up=(0,1,0))
        //    Then transform a point to see if it's in front/behind
        int[] eye = {0, 0, FixedBaseMath.toQ24_8(10.0f)};
        int[] target = {0, 0, 0};
        int[] up = {0, FixedBaseMath.toQ24_8(1.0f), 0};
        int[] camM = FixedMatMath.createLookAt4x4(eye, target, up);
        // TODO: investigate low precision and sign.
        printMatrix("\nLookAt eye(0,0,10) target(0,0,0) up(0,1,0)", camM);

        // 7) createPerspective => fov=60 deg => ~1.0472 rad, aspect=1.333, near=1, far=100
        int fovDeg = 60;
        int fovRad = FixedBaseMath.toQ24_8((float) Math.toRadians(fovDeg));
        int aspect = FixedBaseMath.toQ24_8(1.333f);
        int nearQ = FixedBaseMath.toQ24_8(1.0f);
        int farQ = FixedBaseMath.toQ24_8(100.0f);
        int[] perspM = FixedMatMath.createPerspective4x4(fovRad, aspect, nearQ, farQ);
        printMatrix("\nPerspective(60 deg, 1.333 aspect, near=1, far=100)", perspM);

        // 8) Edge Cases
        //    - rotate by 0 deg => should yield identity
        //    - scale(0,0,0) => everything collapses
        //    - translate(10000, -9999, 1) => big translation
        int angle0 = 0;
        int[] rotZ0 = FixedMatMath.createRotationZ4x4(angle0);
        printMatrix("\nRotationZ(0 deg)", rotZ0);

        int[] scaleZero = FixedMatMath.createScale4x4(0, 0, 0);
        printMatrix("\nScale(0,0,0) => everything collapses", scaleZero);

        int bigX = FixedBaseMath.toQ24_8(10000.0f);
        int bigY = FixedBaseMath.toQ24_8(-9999.0f);
        int bigZ = FixedBaseMath.toQ24_8(1.0f);
        int[] bigTrans = FixedMatMath.createTranslation4x4(bigX, bigY, bigZ);
        printMatrix("\nTranslate(10000, -9999, 1)", bigTrans);

        System.out.println("\n=== End of Matrix Tests ===");
    }
}
