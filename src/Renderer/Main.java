package Renderer;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class Main extends MIDlet implements CommandListener {

    private Display display;
    private Command exitCommand;
    private CanvasScene canvasScene;

    public Main() {
        display = Display.getDisplay(this);
        exitCommand = new Command("Exit", Command.EXIT, 1);
        canvasScene = new CanvasScene();
        canvasScene.addCommand(exitCommand);
        canvasScene.setCommandListener(this);
    }

    public void startApp() {
        display.setCurrent(canvasScene);
        SharedData.display_width = display.getCurrent().getWidth();
        SharedData.display_height = display.getCurrent().getHeight();
        new Thread(canvasScene).start();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command c, Displayable s) {
        if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        }
    }
}
