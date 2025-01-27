package Renderer;

public class SharedData {
    // Init values
    public static int display_width  = 240;
    public static int display_height = 320;
    public static int display_border_offset = display_width / 5;
    
    // map x,y to screen
    public static float halfW = display_width * 0.5f;
    public static float halfH = display_height * 0.5f;
    public static int leftPlaneX = 0 - display_border_offset;
    public static int rightPlaneX = display_width + display_border_offset;
    public static int upPlaneY = 0 - display_border_offset;
    public static int downPlaneY = display_height + display_border_offset;
    
}
