package Renderer;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class RotatingCubeDemo extends MIDlet implements CommandListener {

    private Display display;
    private Command exitCommand;
    private RotatingCubeCanvas cubeCanvas;

    public RotatingCubeDemo() {
        display = Display.getDisplay(this);
        exitCommand = new Command("Exit", Command.EXIT, 1);
        cubeCanvas = new RotatingCubeCanvas();
        cubeCanvas.addCommand(exitCommand);
        cubeCanvas.setCommandListener(this);
    }

    public void startApp() {
        display.setCurrent(cubeCanvas);
        SharedData.display_width = display.getCurrent().getWidth();
        SharedData.display_height= display.getCurrent().getHeight();
        new Thread(cubeCanvas).start();
    }

    public void pauseApp() { }

    public void destroyApp(boolean unconditional) { }

    public void commandAction(Command c, Displayable s) {
        if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        }
    }
}
