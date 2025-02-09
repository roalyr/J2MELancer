package Renderer;

import FixedMath.*;

public class SharedData {
    // Init values
    public static int display_width = 128; // Overwritten upon start
    public static int display_height = 128; // Overwritten upon start
    
    // map x,y to screen
    public static int halfW_Q24_8 = FixedBaseMath.toQ24_8(display_width /2);
    public static int halfH_Q24_8 = FixedBaseMath.toQ24_8(display_height /2);

    public static int renderables_num = 0;
    
}
