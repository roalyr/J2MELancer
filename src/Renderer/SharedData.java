package Renderer;

import FixedMath.FixedBaseMath;

public class SharedData {
    public static int display_width = 128;
    public static int display_height = 128;
    public static long halfW_Q24_8 = FixedBaseMath.toFixed(display_width / 2);
    public static long halfH_Q24_8 = FixedBaseMath.toFixed(display_height / 2);
    public static int renderables_num = 0;
}
