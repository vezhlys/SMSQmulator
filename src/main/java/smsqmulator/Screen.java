package smsqmulator;

import java.awt.event.KeyEvent;

/**
 * The emulated machine's screen, to be subclassed.
 * 
 * <b>This object contains abstract methods and MUST be subclassed for each type of screen.</b>
 * <p>
 * The methods herein convert from the emulated machine's screen data to java screen data.
 * <p>
 * If we're not in QL compatible mode (screen located at 0x20000 and 0x8000 bytes long), then the CPU passes a vrambase parameter to 
 * indicate at what address it thinks the videoram starts (generally above the "ROM"). This will be address 0 if this object's vramBuffer.
 * <p>
 * This now (as of 1.08) handles keypresses and mouse movements/buttons on the emulated screen.
 * 
 * Screen redrawing should be kicked off by the "50 Hz" QL50HzInterrupt object only.
 * 
 * @author and copyright (c) 2012 -2017 Wolfgang Lenerz
 * @version 
 *  1.21    diffetent way of handling mousewheel ; setMousewheelAccel created.
 *  1.20    CTRL + mousewheel procudes left/right scroll keycodes.
 *  1.19    CTRl-Shift +a..z produces keys 160+ ; mac : ctrl shift c is not the same as ctrl c.
 *  1.18    totsize variable introduced.
 *  1.17    readXXXXFromScreen, alphaBlock and getCopyMode removed, copyScreen implemented here, accept "automatic" QL
 *          screen emulation  (field QLmode) ; µ,£ and ¤ keys corrected.
 *  1.16    copyScreen abstracted, setDisplayRegion implemented.
 *  1.15    isFullSize field, getPreferredSize returns screen size if isFullSize.
 *  1.14    setVramBase only takes one parameter, getScreenSizeInBytes made abstract, abstract isQLScreen() introduced.
 *  1.13    better handling of 0 count scroll wheel action; mouse click delay added.
 *  1.12    faster handling of key row.
 *  1.11    paint sets isDirty status before redrawing the screen from the image, so that the main thread can set tis to true whilst redrawing is taking place
 *  1.10    SHIFT SPACE produces char $FC
 *  1.09    changed key codes generated for home, end, page up and page down
 *  1.08    this object now handles the key presses etc on the screen (this is split off from MonitorGui 1.17).
 *  1.07    correctly set the dirty flag to false once repainting is done.
 *  1.06    paintComponent no longer uses a G2d. Part of screen initializing moved here.
 *  1.05    before this version, there was only 1 screen object handling modes 0/4/8 and 32, all in one object. With this version, this is split
 *          off into this abstract object and the screen0 and screen32objects.
 *  1.04    optimized paintBlock
 *  1.03    moveBlock no longer doubles characters.
 *  1.02    correct fill block parameters for one line blocks
 *  1.01    this doesn't call repaint() itself anymore, just sets a flag that it is dirty. repaint is called by tan independent thread.
 *          clut initialized at component creation, general cleanup.
 *          overriding method getPreferredSize returns true or doubled the image size.
 *          faster moveblock and fillblock routines.
 *          attempt at general speedup by writing to the image.raster.databuffer directly.
 *  1.00    if we're in QL colours mode, the x-size must be a multiple of 8, if it isn't it is INCREASED to be so.
 *  0.01    1 in 4 colour stipples handles stipples correctly.
 * @version 0.00
 */
public abstract class Screen extends javax.swing.JPanel
{
    protected int startAddress;                             // start address of screen = 1rst byte in screen memory
                                                            // this is where, in the memory map, the vrambase is supposed to be
    protected int stopAddress;                              // stop address of screen = 1rst byte AFTER screen memory
    protected int xsize=512,ysize=256;                      // size of screen in pixels
    protected int mode=0;                                   // Screen mode, determines number of bits (or bytes) per pixel
                                                            // * mode 0(4) = 2 bits per bixel  (1 word = 8 pixels)
                                                            // * mode 8 = 4 bits per pixel  (1 word = 4 pixels)
                                                            // * mode 32= 2 bytes per pixel (1 word = 1 pixel)
    public volatile boolean isDirty=false;                  // is set to true if screen should be redrawn, alse false
    protected boolean isDouble=false;                       // is set to true if screen displayed size should be doubled
    protected java.awt.image.BufferedImage screenImage;     // drawing on the screen is done through a buffered image.
    protected java.awt.image.WritableRaster raster;         // raster of image
    protected java.awt.image.DataBufferInt dataBuffer;         // and its data buffer
    protected int nbrOfBytesPerLine;                        // number of bytes per line;
    protected boolean vibrantColours =false;
    protected int divisior=1;                               // used when moving blocks of mem about
    protected Monitor monitor;
    protected boolean isMac;
    private int oldX=-1,oldY=-1;
   
    private boolean isFullSize=false;
    
    private final java.awt.Cursor myCursor;                 // an invisible mouse pointer.
    private boolean ignoreMoveEvent=false;                  // is true if the next mouse move event should be ignored
                                                            // this is the case if the mouse position has changed from within SMSQ/E
    private boolean mouseIsInScreen=true;                   // pointer in screen status = true if pointer is "in" my screen object.
    
    private int mouseButtons=0;                             // the state of the mouse buttons
    private int mouseClickDelay=0;
    private static final int[] CONV_CHAR=                   // smsqe<-> java char conversion     
    {
	0,1,2,3,4,5,6,7,
	8,9,10,11,12,13,14,15,
	16,17,18,19,20,21,22,23,
	24,25,26,27,28,29,30,31,
	32,33,34,35,36,37,38,39,
	40,41,42,43,44,45,46,47,
	48,49,50,51,52,53,54,55,
	56,57,58,59,60,61,62,63,
	64,65,66,67,68,69,70,71,
	72,73,74,75,76,77,78,79,
	80,81,82,83,84,85,86,87,
	88,89,90,91,92,93,94,95,
	96,97,98,99,100,101,102,103,
	104,105,106,107,108,109,110,111,
	112,113,114,115,116,117,118,119,
	120,121,122,123,124,125,126,127,
	181,129,130,131,132,133,134,135,
	136,137,138,139,171,141,142,143,
	144,145,146,147,148,149,150,151,
	152,153,154,155,139,157,158,159,
	160,179,157,96,183,158,166,182,
	168,127,170,184,172,173,174,175,
	186,177,178,179,159,176,182,183,
	184,185,186,185,188,189,190,180,
	192,193,162,161,160,197,170,168,
	200,163,202,203,204,205,206,207,
	208,169,210,211,212,165,164,215,
	166,217,218,219,167,221,222,156,
	141,140,142,129,128,130,138,136,
	144,131,145,143,148,147,149,146,
	240,137,151,150,152,133,132,187,
	134,154,153,155,135,253,254,255,
    };  
    
    protected final int [] pixelmask4={0x101,0x202,0x404,0x808,0x1010,0x2020,0x4040,0x8080};// the pixel masks for each individual pixel in the word in QL mode 4
    protected final int [] pixelmask8={0x203,0x80c,0x2030,0x80c0};//ignore flash bit
    protected final int black,blue,red,green,magenta,yellow,orange,white,cyan; // colours in QL compatible modes   
    protected int copyMode;                     // what mode we're supposed to be in when copying QL screen
    protected int QLmode;                       // mode set with mode command
    protected static int totsize;
    /**
     * For Smsqe keyrow emulation. (an 8x8 matrix for keys)
     */
     private final static int[][] KEYROW={
                {KeyEvent.VK_F4,            KeyEvent.VK_F1,     KeyEvent.VK_5,      KeyEvent.VK_F2,     KeyEvent.VK_F3,  KeyEvent.VK_F5,        KeyEvent.VK_4,      KeyEvent.VK_7},
                {KeyEvent.VK_ENTER,         KeyEvent.VK_LEFT,   KeyEvent.VK_UP,     KeyEvent.VK_ESCAPE, KeyEvent.VK_RIGHT,KeyEvent.VK_BACK_SLASH,KeyEvent.VK_SPACE, KeyEvent.VK_DOWN},
                {KeyEvent.VK_CLOSE_BRACKET, KeyEvent.VK_Z,      KeyEvent.VK_PERIOD, KeyEvent.VK_C,      KeyEvent.VK_B,  KeyEvent.VK_DOLLAR,     KeyEvent.VK_M,      KeyEvent.VK_QUOTE},
                {KeyEvent.VK_OPEN_BRACKET,  KeyEvent.VK_CAPS_LOCK,KeyEvent.VK_K,    KeyEvent.VK_S,      KeyEvent.VK_F,  KeyEvent.VK_EQUALS,     KeyEvent.VK_G,      KeyEvent.VK_SEMICOLON},
                {KeyEvent.VK_L,             KeyEvent.VK_3,      KeyEvent.VK_H,      KeyEvent.VK_1,       KeyEvent.VK_A,  KeyEvent.VK_P,         KeyEvent.VK_D,      KeyEvent.VK_J},
                {KeyEvent.VK_9,             KeyEvent.VK_W,      KeyEvent.VK_I,      KeyEvent.VK_TAB,    KeyEvent.VK_R,  KeyEvent.VK_MINUS,      KeyEvent.VK_Y,      KeyEvent.VK_O},
                {KeyEvent.VK_8,             KeyEvent.VK_2,      KeyEvent.VK_6,      KeyEvent.VK_Q,      KeyEvent.VK_E,  KeyEvent.VK_AT,         KeyEvent.VK_T,      KeyEvent.VK_U},
                {KeyEvent.VK_SHIFT,         KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,    KeyEvent.VK_X,      KeyEvent.VK_V,  KeyEvent.VK_SLASH,      KeyEvent.VK_N,      KeyEvent.VK_COMMA}};
    
    private int [][] keyrowTable;
    private int mouseWheelAccel;
    
    /**
     * Creates the object.
     * Subclasses should always call this.
     * @param xsize screen x size in pixels.
     * @param ysize screen y size in pixels.
     * @param monitor the monitor running the emulation.
     * @param isMac <code>true</code> if we are running on a mac.
     */
    public Screen(int xsize,int ysize,Monitor monitor,boolean isMac)
    {
        super();                                                // initialize the JPanel 
        this.xsize=xsize;
        this.ysize=ysize;
        this.totsize=xsize*ysize;
        this.screenImage = new java.awt.image.BufferedImage(xsize,ysize, java.awt.image.BufferedImage.TYPE_INT_RGB);
        this.raster=this.screenImage.getRaster();
        this.dataBuffer=(java.awt.image.DataBufferInt)raster.getDataBuffer();
        java.awt.Dimension d=new java.awt.Dimension(xsize,ysize);
        this.setSize(d);
        this.setPreferredSize(d);
        this.setMinimumSize(d);
        this.setMaximumSize(d); 
        this.isDirty=true;
        this.monitor=monitor;
        this.isMac=isMac;
        
        byte[]imageByte=new byte[0];                        // empty byte array for cursor creation
        java.awt.Point myPoint=new java.awt.Point(0,0);
        java.awt.Image cursorImage=java.awt.Toolkit.getDefaultToolkit().createImage(imageByte);//Create image for cursor using empty array
        this.myCursor=java.awt.Toolkit.getDefaultToolkit().createCustomCursor(cursorImage,myPoint,"invisible_cursor");
        setMouseAndKeys();
        setupKeyrowArray();
        this.black=java.awt.Color.black.getRGB();
        this.red=java.awt.Color.red.getRGB();
        this.green=java.awt.Color.green.getRGB();
        this.white=java.awt.Color.white.getRGB();
        this.cyan=java.awt.Color.cyan.getRGB();
        this.blue=java.awt.Color.blue.getRGB();
        this.magenta=java.awt.Color.magenta.getRGB();
        this.yellow=java.awt.Color.yellow.getRGB();
        this.orange=java.awt.Color.orange.getRGB();
        this.mouseWheelAccel = 1;                           // default value
    }
    
    /**
     * This is a lazy way of building an array for the keys for key row.
     */
    private void setupKeyrowArray()
    {
        int p=0;
        for (int row=0; row<8;row++)
        {
            for (int col=0;col<8;col++)
            {
                if (Screen.KEYROW[row][col]>p)
                {
                    p=(Screen.KEYROW[row][col]);
                }
            }
        }
        this.keyrowTable=new int[p+1][2];
        for (int row=0;row<p+1;row++)
             this.keyrowTable[row][0]=-1;
        for (int row=0; row<8;row++)
        {
            for (int col=0;col<8;col++)
            {
                p=Screen.KEYROW[row][col];
                this.keyrowTable[p][0]=row;
                this.keyrowTable[p][1]=col;
            }
        }
    }
    
    
    /**
     * This sets the mouse and key listeners for this object.
     */
    private void setMouseAndKeys()
    {
        this.addKeyListener(new java.awt.event.KeyAdapter() 
        {
            @Override
            /**
             * Get the standard keys typed in.
             */
            public void keyTyped(java.awt.event.KeyEvent evt) 
            {
                screenPanelKeyTyped(evt);
            }
            
            @Override
            /**
             * Input some keys that aren't caught via the keyTyped method.
             */
            public void keyPressed(java.awt.event.KeyEvent evt) 
            {
                screenPanelKeyPressed(evt);                     
            }

	    @Override
            public void keyReleased(java.awt.event.KeyEvent evt) 
            {
                screenPanelKeyReleased(evt);
            }

        });
        
        this.addMouseListener(new java.awt.event.MouseAdapter() 
        {
            /**
            * Mouse click on emulated screen - give screen input focus.
            * @param evt ignored.
            * */
            @Override 
            public void mouseClicked(java.awt.event.MouseEvent evt) 
            {
                requestFocusInWindow();
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) 
            {
                screenPanelMouseEntered(evt);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) 
            {
                screenPanelMouseExited(evt);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) 
            {
                screenPanelMousePressed(evt);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) 
            {
                screenPanelMouseReleased(evt);
            }
        });
        
        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() 
        {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent evt) 
            {
                screenPanelMouseMoved(evt);
            }
 	    @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) 
            {
                screenPanelMouseMoved(evt);
            }
        });
        
        // handle mouse scroll (now ignores 0 count scrolls)
        this.addMouseWheelListener(new java.awt.event.MouseWheelListener() 
        {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) 
            {
                int count=evt.getWheelRotation();
                int mschar;
                if (count==0)
                    return;
                if (count<0)
                {
                    if (evt.isControlDown())
                        mschar=0xc10000;
                    else
                        mschar=0xd10000;
                    count=count*-1;
                }
                else
                {
                    if (evt.isControlDown())
                        mschar=0xc90000;
                    else
                        mschar=0xd90000;
                }
                count*=mouseWheelAccel;
                count--;                                        // prepare for dbf in smsqes
                monitor.inputMouseWheel(count+mschar);                    // simulate skeystrokes
                evt.consume();
             /* int count=evt.getWheelRotation();
                
                if (count==0)
                    return;
                if (count<0)
                {
                    if (evt.isControlDown())
                        count=0xc1c1c1;
                    else
                        count=0xd1d1d1;
                }
                else if (count>0)
                {
                    if (evt.isControlDown())
                        count=0xc9c9c9;
                    else
                        count=0xd9d9d9;
                }
                monitor.inputKey(count);                    // simulate three keystrokes
                evt.consume();*/
//                test++;
            }
        });
    }
    
    /**
     * Gets the key typed in the <code>Screen</code> object into the monitor (and thus the emulation).
     * 
     * @param evt the key typed
     * 
     * NOTE this needs more key translations.
     */
    public void screenPanelKeyTyped(java.awt.event.KeyEvent evt) 
    {                        
        boolean doAlt;
        if (this.monitor!=null)
        {
            int key;
            if (this.isMac)
            {
                key=evt.getKeyCode();
                if (key==17 || key==18 || key==152)
                { 
                    evt.consume();
                    return;                                 // on the mac alt & ctrl may generate keytyped events on their own
                }
            }
            key= evt.getKeyChar();
            if (!this.isMac)
                doAlt=evt.isAltDown();
            else
                doAlt=evt.isMetaDown();
            switch (key)
            {
                case 8:                                     // backspace
                    key=0xc2;
                    if (doAlt)
                    {
                        doAlt=false;
                        key=0xc3;                    
                    }  
                    if (evt.isShiftDown())
                        key+=4;
                    break;
                case 9:                                     // tab
                    if (evt.isShiftDown())
                    {
                        if (doAlt && evt.isControlDown())   // ctrl, alt sift tab = reset
                        {
                            this.monitor.reset();
                            return;                         // PREMATURE EXIT!!!!!!!!!!!!!!!!!
                        }
                        else
                            key=253;
                    }
                    break;
                case 27:
                    return;                                 // already caught in key pressed !!!!!!!, don't do this twice
                case 32:                                    // space
                    if (evt.isControlDown())                // CTRL space is special
                        key=-1;
                    if (evt.isShiftDown())                  // shift space
                        key=0xfc;
                    break;
                case 0x7f:                                  // delete
                    key=0xca;                               // convert to ctrl + right    
                    if (doAlt)
                    {
                        doAlt=false;
                        key++;                    
                    }  
                    if (evt.isShiftDown())
                        key+=4;
                    break;
                case 0xb2:
                    key=0x23;                               // small 2 to #
                    break;
                case 8364:
                    key=181;
                    break;
                case 65535:                                 // That is the Mac's CTRL+Space!
                    if (this.isMac && evt.isControlDown())
                       key = -1;
                    break;
                default:
                    if (key>-1 && key<256)
                        key=Screen.CONV_CHAR[key];
                    break;
            }
            if (doAlt)
            {
                if (key==0xca)
                    key=203;
                else
                {
                    int mkey=key<<8;
                    key=mkey+0xff;
                }
            }
            if (key<27 && evt.isControlDown() && evt.isShiftDown())
                key+=160;
            this.monitor.inputKey(key);
            evt.consume();
        }
    }                                    

    /**
    * Input some keys that aren't caught via the keyTyped method:
    * <ul>
    * <li> Cursor keys</li>
    * <li> function keys</li>
    * <li> home, end page up page down</li>
    * <li> ESC key (not caught under osx)</li>
    * </ul>
    * 
    * @param evt ignored.
    */
    public void screenPanelKeyPressed(java.awt.event.KeyEvent evt) 
    {   
        int key=0;
        int kcode=evt.getKeyCode();
        this.monitor.stopThrottle();
        setKeyrow(kcode);
        switch (kcode)
        {
            case java.awt.event.KeyEvent.VK_ALT:
                evt.consume();
                return; //  
            case java.awt.event.KeyEvent.VK_ESCAPE:
                key=27;
                break;     
            case java.awt.event.KeyEvent.VK_PAUSE:
                key=-1;
                break;     
            case java.awt.event.KeyEvent.VK_SCROLL_LOCK:
                key=249;
                break;                                      // catch the escape key here!
            case java.awt.event.KeyEvent.VK_DOWN:
                key=0xd8;
                break;
            case java.awt.event.KeyEvent.VK_UP:
                key=0xd0;
                break;
            case java.awt.event.KeyEvent.VK_LEFT:
                key=0xc0;
                break;
            case java.awt.event.KeyEvent.VK_RIGHT:
                key=0xc8;
                break;
            case java.awt.event.KeyEvent.VK_F1:
                key=232;
                break;
            case java.awt.event.KeyEvent.VK_F2:
                key=236;
                break;
            case java.awt.event.KeyEvent.VK_F3:
                key=240;
                break;
            case java.awt.event.KeyEvent.VK_F4:
                key=244;
                break;
            case java.awt.event.KeyEvent.VK_F5:
                key=248;
                break;
            case java.awt.event.KeyEvent.VK_F6:
                key=234;
                break;
            case java.awt.event.KeyEvent.VK_F7:
                key=238;
                break;
            case java.awt.event.KeyEvent.VK_F8:
                key=242;
                break;
            case java.awt.event.KeyEvent.VK_F9:
                key=246;
                break;
            case java.awt.event.KeyEvent.VK_F10:
                key=250;
                break;
            case java.awt.event.KeyEvent.VK_F12:
                key=0x20ff;
           //     key=-1;
                break;
            case java.awt.event.KeyEvent.VK_HOME:
                key=193;//0xd5
                break;
            case java.awt.event.KeyEvent.VK_END:
                key=201;//0xdd;
                break;
            case java.awt.event.KeyEvent.VK_PAGE_UP:
                key=0xd4;//0xd4ff;
                break;  
            case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                key=0xdc;//0xdcff;
                break;
            case java.awt.event.KeyEvent.VK_CAPS_LOCK:
                try
                {
                    this.monitor.setCapsLockStatus(java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK));
                }
                catch (Exception e)
                {
                    //NOP
                }
                return;
            case java.awt.event.KeyEvent.VK_ENTER:
                if (evt.isShiftDown())
                    key=254;
                break;
       //     case 130:
         //       key=94;
           //     break;
            default:                                            
                if (this.isMac &&(evt.isAltDown() || evt.isControlDown()) || evt.isMetaDown()) // mac ALT, CTRL & META keypresses are special
                {                                           // code contributed by Tobias Fröschle.
                    char c;
                    c = evt.getKeyChar();               // get the character
                    key=evt.getKeyCode();
                    if (c==0 || key==18 || key==17 || key==157)
                    {
                        evt.consume();
                        return;                             // none, or the ALt & ctrl keypresses : just do nothing
                    }
                    if (!evt.isShiftDown()){
                        c=Character.toLowerCase(c);         // make into lower case if shift is not pressed
                        key = Character.toLowerCase(key);
                    }
                    else {
                        c = Character.toUpperCase(c);
                        key = Character.toUpperCase(key);
                    }
                    
                     if (evt.isControlDown() && (c==99) && !evt.isShiftDown())
                            c=3;                            // CTRL C is special
                    evt.setKeyChar(c);
                    evt.setKeyCode(key);
                    
                    screenPanelKeyTyped(evt);
                    return;
                }
                else
                    return;                                 // ***** premature exit if key is none of the above!!!!!!!
        
               
               /*
                if (this.isMac &&(evt.isAltDown() || evt.isControlDown())) // mac ALT & CTRL keypresses are special
                {
                    char c= evt.getKeyChar();               // get the character
                    key=evt.getKeyCode();
                    if (c==0 || key==18 || key==17)
                    {
                        evt.consume();
                        return;                             // none, or the ALt & ctrl keypresses : just do nothing
                    }
                    if (!evt.isShiftDown())
                        c=Character.toLowerCase(c);         // make into lower case if shift is not pressed
                    
                     if (evt.isControlDown() && (c==99) )
                            c=3;                            // CTRL C is special
                    evt.setKeyChar(c);
                    screenPanelKeyTyped(evt);
                    return;
                }
                else
                    return;                                 // ***** premature exit if key is none of the above!!!!!!!
                    * 
                    */
        }
        switch (key)                                        // now add crtl & shift keys
        {
            case 232:
            case 236:
            case 240:
            case 244:
            case 248:                                       // F1 - F5
                if (evt.isControlDown())
                    key++;
                if (evt.isShiftDown())
                    key+=2;
                break;
            case 0xc0:
            case 0xc8:
            case 0xd0:
            case 0xd8:                                      // cursor keys
                if (evt.isControlDown())
                    key+=2;
                if (evt.isShiftDown())
                    key+=4;
                break;
              
        }
        if (evt.isAltDown())                                // Altkey
        {
            switch (key)
            {   case 0xc0:
                case 0xc2:
                case 0xc4:
                case 0xc8:
                case 0xca:
                case 0xcc:
                case 0xd0:
                case 0xd2:
                case 0xd4:
                case 0xd8:        
                case 0xda:
                case 0xdc:                          // special case for alt+cursor keys
                    key++;
                    break;             
                case 0x20ff:
                    break;
                default:
                    if (key<0x100)
                    {
                        int mkey=key<<8;
                        key=mkey+0xff;
                    }
                    break;
            }
        }
        this.monitor.inputKey(key);
    }                      

    /**
     * When key is released, remove it from keyrow array.
     * 
     * @param evt the key vent of the key released.
     */
    public void screenPanelKeyReleased(java.awt.event.KeyEvent evt) 
    {
        int keyCode=(evt.getKeyCode());
        if (keyCode<this.keyrowTable.length && this.keyrowTable[keyCode][0]!=-1)
            this.monitor.removeKeyrow(this.keyrowTable[keyCode][0],this.keyrowTable[keyCode][1]);
        
    } 
    /*
    public void screenPanelKeyReleased(java.awt.event.KeyEvent evt) 
    {
        int keyCode=(evt.getKeyCode());
        for (int row=0; row<8;row++)
        {
            for (int col=0;col<8;col++)
            {
                if (Screen.KEYROW[row][col]==keyCode)
                {
                    this.monitor.removeKeyrow(row,col);
                    return;
                }
            }
        }
    }
        */          
    
    /**
     * Marks a key as pressed in the keyrow array.
     * 
     * @param keyCode 
     */
    private void setKeyrow(int keyCode)
    {
      if (keyCode<this.keyrowTable.length && this.keyrowTable[keyCode][0]!=-1)
        this.monitor.setKeyrow(this.keyrowTable[keyCode][0],this.keyrowTable[keyCode][1]);
    } 
    
   /*
    private void setKeyrow(int keyCode)
    {
        for (int row=0; row<8;row++)
        {
            for (int col=0;col<8;col++)
            {
                if (Screen.KEYROW[row][col]==keyCode)
                {
                    this.monitor.setKeyrow(row,col);
                    return;
                }
            }
        }
    }
    */
   
    /**
     * This is the method that catches the mouse movement and sends it to the emulator via the monitor.
     * 
     * @param evt the mouse event
     */
    public void screenPanelMouseMoved(java.awt.event.MouseEvent evt) 
    {                        
        if (this.ignoreMoveEvent)
        {
            this.ignoreMoveEvent=false;
            return;
        }
        int msx=evt.getX();
        int msy=evt.getY();
        if (this.oldX<0 || this.oldY<0)                         // right at the beginning, initialise 
        {
            this.oldX=msx;
            this.oldY=msy;
            return;
        }
        if (msx!=this.oldX || msy!=this.oldY)
        {
            int temp1=msx-this.oldX;                            // make mvt relative
            this.oldX=msx;                                      // and make new ptr position to old ptr position
            int temp2=msy-this.oldY;                            // make mvt relative
            this.oldY=msy;
            if (this.isDouble)
            {
                this.monitor.inputMouse(temp1/2,temp2/2,this.oldX/2,this.oldY/2);// input all to the emulator
            }
            else
            {
                this.monitor.inputMouse(temp1,temp2,this.oldX,this.oldY);// input all to the emulator
            }
        }
    }                                      
 
    /**
     * Sets a new mouse position if that has changed from within SMSQ/E.
     * 
     * @param x the new x position, relative to the screen object.
     * @param y same for y.
     * @return true if the screen is in double sized mode.
     */
    public boolean setMousePosition(int x,int y)
    {
        if (this.mouseIsInScreen)
        {
            if (this.isDouble)
            {
                this.oldX=x*2;
                this.oldY=y*2;
            }
            else
            {
                this.oldX=x;
                this.oldY=y;
            }
            this.ignoreMoveEvent=true;                          // ignore the next mouse move event, which is generated by the TrapDispatcher's robot moving the ptr
        }
        return this.isDouble;
    }
   
    /**
     * When mouse enters the emulated screen, this makes normal mouse cursor "disappear".
     * The cursor is replaced by an invisible one.
     * 
     * @param evt ignored
     */
    public void screenPanelMouseEntered(java.awt.event.MouseEvent evt) 
    {                                         
        this.setCursor(this.myCursor);
        this.mouseIsInScreen=true;
    }                                        

    /**
     * Sets the mouse cursor back to normal when mouse leaves the emulated screen.
     * 
     * @param evt ignored
     */
    public void screenPanelMouseExited(java.awt.event.MouseEvent evt) 
    {                                        
        this.mouseIsInScreen=false;
        this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
        this.monitor.inputMouseButton(0);
    }                                       

    /**
     * Handles mouse presses on the emulated screen - send mouse button pressed to SMSQE.
     * 
     * @param evt ignored
     */
    public void screenPanelMousePressed(java.awt.event.MouseEvent evt) 
    {                                         
       int btn;
       switch (evt.getButton())
       {
           case java.awt.event.MouseEvent.BUTTON1:          // left button -hit
               btn=1;
               break;
           case java.awt.event.MouseEvent.BUTTON2:          // middle button
               btn=4;                                       
               break;                                           
           case java.awt.event.MouseEvent.BUTTON3:          // right button - do
               btn=2;
               break;
           default :
               btn=0;
               break;
       }
       this.mouseButtons |=btn;
       this.monitor.inputMouseButton(this.mouseButtons);
       if (mouseClickDelay!=0)
       {
           try
           {
                Thread.sleep(mouseClickDelay);
           }
           catch (Exception e)
           { /* NOP*/ }
       }
    }    
   
    /**
     * Handles mouse releases on the emulated screen - send mouse button released to SMSQE.
     * 
     * @param evt ignored
     */                                 
    public void screenPanelMouseReleased(java.awt.event.MouseEvent evt) 
    {                                         
       int btn;    
       switch (evt.getButton())
       {
           case java.awt.event.MouseEvent.BUTTON1:          // left button -hit
               btn=1;
               break;
           case java.awt.event.MouseEvent.BUTTON2:          // middle button
               btn=4;;
               break;
           case java.awt.event.MouseEvent.BUTTON3:          // right button - do
               btn=2;
               break;
           default :
               btn=0;
               break;
       }
       this.mouseButtons &= ~btn;
       this.monitor.inputMouseButton(this.mouseButtons);
    }             
    /**
     * Sets whether this screen is run under a mac.
     * 
     * @param im <code>true</code> if this is run under a mac.
     */
    public void setIsMac(boolean im)
    {
        this.isMac=im;
    }
    /**
     * Sets the Monitor object for this.
     * 
     * @param m the new Monitor.
     */
    public void setMonitor(Monitor m)
    {
        this.monitor=m;
    }
    
    /**
     * sets the delay after a mouse click. 
     * 
     * @param delay delay in milliseconds.
     */
    public void setMouseClickDelay (int delay)
    {
        this.mouseClickDelay=delay;
    }
    /**
     * THE FOLLOWING ABSTRACT ROUTINES MUST BE IMPLEMENTED BY EACH SUBCALSS.
     */
    
     /**
     * Writes a byte to the "screen memory" and paints the corresponding pixel on the screen.
     * @param address where to write to. This is the SMSQE address, where it thinks video memory lies.
     * It has already been checked that the address is in my memory space.
     * @param value the value to write.
     * @param wordValue the value to write into the word this byte is part of
     */
    public abstract void writeByteToScreen(int address, int value,int wordValue );
    
    /**
     * Writes a word to screen memory, and paints the corresponding pixel on the screen.
     * @param addr the address where to write to. It is presumed that it has been checked that this is in screen mem.
     * @param value the value to write to screen mem.
     */
    public abstract void writeWordToScreen(int addr, int value);
    
     /**
     * Writes a long word to the java screen.
     * @param addr where to write to.
     * @param value the value.
     */
    public abstract void writeLongToScreen(int addr,int value);   
    
  
    /**
     * Sets the screen mode when in Ql compatible mode.
     * Is only overriden by screen0.
     * @param mode 0 for mode 4 ,8 for mode 8
     */
    public void setMode(int mode)
    {
    }
  /**
     * Sets the screen emulation mode.
     * ATM only modes 4 and 8 are supported.
     * @param mode 0 for mode 4 ,8 for mode 8
     */
    public void setEmuMode(int mode)
    {
        this.QLmode=mode==8?8:4;
    }
 
       
    /**
     * Clears the entire screen, on screen as well as in VramBuffer.
     */
    public abstract void clearScreen();
    
    /**
     * Xors a block with colour : this updates the "screen memory" (vrambuffer) AND the underlying image.
     * Implemented in modes 16 and 32.
     * 
     * @param cpu the CPu used.
     * @param resolveStipple true if colours must be derived from stipple.
     */
    public abstract void xorBlock (smsqmulator.cpu.MC68000Cpu cpu, boolean resolveStipple);
    
     /**
     * Fills a block with colour : this updates the "screen memory" (vrambuffer) AND the underlying image.
     * Implemented in modes 16 and 32.
     * 
     * @param cpu the CPU used.
     * @param resolveStipple true if colours must be derived from stipple.
     */
    public abstract void fillBlock (smsqmulator.cpu.MC68000Cpu cpu, boolean resolveStipple); 
    
    
    /**
     * This is called when a block of memory was copied to the vram..
     * It must copy the data from the vram into the image.
     * 
     * @param cpu the CPU used.
     * @param copyFromScreen = true if the source is also the screen.
     * 
     * d1 c  s size of section to move (x :pixels | y :lines) a pixel may take a variable amount of bytes
     * d2 c  s old origin in source area
     * d3 c  s new origin in destination area
     * d4    s scratch
     * d5    s scratch
     * d6/d7	preserved
     * a0/a1	preserved
     * a2 c	row increment of source area
     * a3 c	row increment of destination area
     * a4 c	base address of source area
     * a5 c	base address of destination area
     * a6/a7	preserved
     * 
     */
    public abstract void moveBlock(smsqmulator.cpu.MC68000Cpu cpu,boolean copyFromScreen);
    
    /**
     * This combines two blocks (source 1, source2) with alpha blending and puts the result into the screen.
     * The destination must be the screen: this MUST have been checked before this  method is called.
     * Source2 must have a row increment of destination row increment + 4.
     * 
     * @param cpu the CPU used.
     *    
        d0    s ? / some arbitrary value
        d1 c  s size of section to move / scratch
        d2 c  s old origin in source area1 / scratch
        d3 c  s new origin in destination area / scratch
        d4 c  s 
        d5    s scratch
        d6 c    alpha / preserved
        d7      preserved
        a1 c  s base address of source area2  / scratch
        a2 c  s row increment of source area1 / scratch
        a3 c  s row increment of destination area / scratch
        a4 c  s base address of source area1 / scratch
        a5 c  s base address of destination area / scratch
        a6/a7   preserved
     */
    public abstract void combineBlocks(smsqmulator.cpu.MC68000Cpu cpu);
    
    /**
     * Displays a region if bytes were loaded directly to the screen memory.
     * Used from CPU.
     * 
     * @param cpu .
     * @param start where the bytes were loaded to.
     * @param nbrOfBytes how many bytes were loaded.
     */
    public abstract void displayRegion(smsqmulator.cpu.MC68000Cpu cpu,int start,int nbrOfBytes);
      
   /**
     * Sets the new Vrambase or the screen buffer if we are in QL compatible mode (this implies that the CPU or its memory changed).
     * Adjust vramtop accordingly.
     * If this is a Ql screen, sets the vram object 
     * 
     * @param vrambase the "video ram" base.
     */
    public abstract void setVramBase(int vrambase);
    
    /**
     * Sets whether brighter colours should be used.
     * 
     * @param bright <code>true</code> if brighter colours should be used.
     * @param vram te array containing the "vram".
     */
    public abstract void setVibrantColours(boolean bright,short[]vram);
    
     /**
     * Gets size, in bytes, of the vram.
     * 
     * @return screen size, in number of bytes.
     */
    public abstract int getScreenSizeInBytes();
    
    /**
     * Checks whether this is a QL type screen
     * 
     * @return true if this is a QL screen (512*256 i mode 4, 256*256 in mode 8), else false
     */
    public abstract boolean isQLScreen();   
    
    
    /**
     * Sets the mode the emulated QL screen is supposed to be in when copying it in higher screen modes.
     * 
     * @param mode the QL mode (mode 4 = 0 or mode 8=8)
     */
    public final void setQLEmulationMode(int mode)
    {
        this.copyMode=mode;
    }
    
    /**
     * Gets the divisor necessary for moving memory about.
     * 
     * @return the divisor.
     */
    public int getDivisor()
    {
        return this.divisior;
    }
    
    /**
     * Shows whether "vibrant colours" were turned on or not.
     * 
     * @return <code>true</code> if yes.
     */
    public boolean isVibrantColours()
    {
        return this.vibrantColours;
    }
    
    /**
     * Gets the first address of screen memory.
     * @return the first address of screen memory.
     */
    public final int getScreenBase()
    {
        return this.startAddress;
    }
    
    /**
     * Gets the last address of screen memory.
     * @return the last address of screen memory.
     */
    public final int getScreenTop()
    {
        return this.stopAddress;
    }
        
    /**
     * Gets the x resolution in pixels.
     * 
     * @return the x resolution (width) in pixels.
     */
    public final int getXSize()
    {
        return this.xsize;
    }
    
    /**
     * Gets the y resolution in pixels.
     * 
     * @return the y resolution (height) in pixels.
     */
    public final int getYSize()
    {
        return this.ysize;
    }
    
    
   /**
     * Gets the number of bytes per line in high colour mode.
     * 
     * @return the size of the line in number of bytes per line (this obny makes sens for high colour modes)
     */
    public final int getLineSize()
    {
        return this.nbrOfBytesPerLine ;
    }
    
    /**
     * Gets the current screen mode.
     * 
     * @return the current screen mode 0, 8 or 32
     */
    public final int getMode()
    {
        return this.mode;
    }
   
     /**
     * Gets the preferred size of this object, which is that of the underlying image.
     * 
     * @return the java.awt.Dimension with the size.
     */
    @Override
    public final java.awt.Dimension getPreferredSize() 
    {
        if (this.isFullSize)
            return java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if (this.isDouble)
        {
            return new java.awt.Dimension(this.screenImage.getWidth()*2, this.screenImage.getHeight()*2);
        }
        else
        {
            return new java.awt.Dimension(this.screenImage.getWidth(), this.screenImage.getHeight());
        }
    }
    
    /**
     * Gets the SMSQE pixel size of the screen, which is that of the underlying image.
     * 
     * @return the java.awt.Dimension with the size.
     */
    public final java.awt.Dimension getImageSize() 
    {
        return new java.awt.Dimension(this.screenImage.getWidth(), this.screenImage.getHeight());
    }

    /**
     * Sets the double sized status of this screen (a pixel is doubled in every dimension).
     * 
     * @param d <code>true</code> if pixels should be doubled.
     */
    public final void setDoubleSize(boolean d)
    {
        this.isDouble=d;
    }
    
    /**
     * Gets whether mouse is in this screen.
     * 
     * @return true only if mouse is in this screen.
     */
    public boolean mouseInScreen()
    {
        return this.mouseIsInScreen;
    }
    
    /**
     * Gets the monitor this screen is attached to.
     * 
     * @return the monitor this screen is attached to.
     */
    public Monitor getMonitor()
    {
        return this.monitor;
    }
    
    /* -------------------------------------- Component painting ------------------------------------------*/
    /**
     * Paints the component.
     * 
     * @param g the <code>java.awt.Graphics</code> object to draw on.
     */
    @Override
    public  void paintComponent(java.awt.Graphics g) 
    {
        java.awt.Dimension t=this.getSize();   
        this.isDirty=false;                                     // set this right away so that another screen op can set this to dirty whilst it is repainting
        g.drawImage(this.screenImage,0,0,t.width,t.height,null);
    }
    
   /**
     * Fills the screen with a certain colour.
     * 
     * @param c the <code>java.awt.Color</code> to fill the screen with.
     */
    public final void fillScreen(java.awt.Color c) 
    {
        fillScreen (c.getRGB());
    }
    
    /**
     * Fills the visible screen with a certain colour.
     * Note this doesn't set the "screen memory" to that colour!
     * 
     * @param newRGB the int (RGB) to fill the screen with.
     */
    public final void fillScreen(int newRGB) 
    {
         int[]colArray=new int[this.xsize*this.ysize];
         for (int i=0;i<this.xsize* this.ysize;i++)
         {
             colArray[i]=newRGB;
         }
         this.raster.setDataElements(0,0, this.xsize, this.ysize, colArray);
         this.isDirty=true;
    }  
    
    /**
     * Sets whether screen should be full size. If yes, it also covers the task bar.
     * 
     * @param full true if screen should be fullsize.
     */
    public final void setFullSize(boolean full)
    {
        this.isFullSize=full;
    }
      
    /**
     * Copies a word's worth of pixels from the original QL screen to this screen.
     * These will be either 8 pixels (in mode 4) or 4 pixels (in mode 8).
     * 
     * @param cpu the CPU from which to get the data.
     * @param addr where in QL screen the pixels go.
     * @param value the colour of the pixels.
     */
    public void copyScreen(smsqmulator.cpu.MC68000Cpu cpu,int addr,int value)
    {
        if (this.xsize<512 || this.ysize<256) 
            return;
        int md;
        // translate the address from the QL screen into my address.
        addr-=0x20000;
        int y=addr/128;                                     // the line (y coord) we're on
        int x=addr-(y*128);                                 // the initial column (x coord) of the pixel
        if (this.copyMode==-1)                              // the copymode is not explicily set, used mode from last MODE command
            md=this.QLmode;
        else
            md=this.copyMode;
        if (md==4)
            x*=4;                                           // number of pixels per byte IN MODE 4;
        else
            x*=2;                                           // number of pixels per byte IN MODE 8
        setPixels (x,y,value,md);                           // set the pixels
        this.isDirty=true;
    }
    
    /**
     * Sets the colours of a QL screen mode word's worth of pixels..
     * This sets either 8 pixels (QL mode 4) or 4 pixels (Ql mode 8).
     * @param x x coordinate of pixel.
     * @param y y coordinate of pixel.
     * @param value the word containing the colours to set the pixels to, in QL mode 8 or 4.
     * @param Qlmode the QL colour mode (must be 4 or 8).
     */
    protected void setPixels (int x,int y,int value,int Qlmode)
    {
        int colour,col;
        y*=this.xsize;
        switch (Qlmode)
        {
            case 4:                     
                x+=7;
                for (int i=0; i<8;i++)                      // 1 word = 8 pixels
                {
                    colour=(value & this.pixelmask4[i])>>>i;// blot out unwanted bits and shift to left for each pixel
                    switch(colour)
                    {
                        case 0 :
                            col=this.black;
                            break;
                        case 1 :
                            col=this.red;
                            break;
                        case 0x100 :
                            col=this.green;
                            break;
                        case 0x101 :
                            col=this.white;
                            break;
                        default :
                            col=this.blue;        // error!
                            break;
                    }
                    this.dataBuffer.setElem(x+y, col);
                    --x;
                }
                break;
            case 8:                    
                x+=3;
                for (int i=0; i<4;i++)                  // 1 word = 4 pixels
                {
                    colour=(value & this.pixelmask8[i])>>>(i*2);
                    switch(colour)
                    {
                        case 0 :
                            col=this.black;
                            break;
                        case 1 :
                            col=this.blue;
                            break;
                        case 2 :
                            col=this.red;
                            break;
                        case 3 :
                            col=this.magenta;
                            break;
                        case 0x200:
                            col=this.green;
                            break;
                        case 0x201:
                            col=this.cyan;
                            break;
                        case 0x202:
                            col=this.yellow;
                            break;
                        case 0x203:
                            col=this.white;
                            break;
                        default :
                            col=this.orange;
                            break;
                    }
                    this.dataBuffer.setElem(x*2+y, col);
                    this.dataBuffer.setElem(x*2+y+1, col);
                    --x;
                }
        }
    }
    
    /**
     * Copies a block of screen memory from an original QL screen to this screen, converting the colours.
     * Used by CPUforScreenEmulation.
     * 
     * @param address start address of block in the screen memory.
     * @param nbrOfBytes nbr of bytes to treat.
     * @param mem the array with the "vram". the values in there correspond to QL mode colours.
     */
    public final void setDisplayRegion(int address,int nbrOfBytes, short[]mem)
    {
        address &= 0xfffffffe;
        int addr=address -0x20000;
        int y=addr/128;                                     // the line (y coord) we're on
        int x=addr-(y*128);                                 // the initial column (x coord) of the pixel
        int maxX,incX;
        int md;
        if (this.copyMode==-1)                              // the copymode is not explicily set, used mode from last MODE command
            md=this.QLmode;
        else
            md=this.copyMode;
        
        if (md==4)
        {
              x*=4;                                         // number of pixels per byte IN MODE 4;
              maxX=512;
              incX=8;
        }
        else
        {
              x*=2;                                         // number of pixels per byte IN MODE 8
              maxX=256;
              incX=4;
        }
        int maxY=nbrOfBytes/128+y;                          // number of lines starting from y
        address/=2;                                         // index into memory
        for (;y<maxY;y++)
        {
            for (;x<maxX;x+=incX)
            {
                int value=mem[address++]&0xffff;            // get one word
                setPixels(x,y,value,md);
            }
            x=0;
        }
    }   
    
    public void setMousewheelAccel(int mse)
    {
        this.mouseWheelAccel=mse;
    }
}