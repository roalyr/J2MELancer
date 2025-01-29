package Renderer;

import FixedMath.*;

public class SharedData {
    // Init values
    public static int display_width  = 240;
    public static int display_height = 320;
    public static int display_border_offset = display_width / 5;
    
    // map x,y to screen
    public static int halfW = display_width /2;
    public static int halfH = display_height /2;
    public static int halfW_Q24_8 = FixedBaseMath.toQ24_8(halfW);
    public static int halfH_Q24_8 = FixedBaseMath.toQ24_8(halfH);
    
    public static int leftPlaneX = 0 - display_border_offset;
    public static int rightPlaneX = display_width + display_border_offset;
    public static int upPlaneY = 0 - display_border_offset;
    public static int downPlaneY = display_height + display_border_offset;
    
}
