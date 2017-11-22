package smsqmulator;

/**
 * The GUI for the monitor/emulator when run as an application.
 * 
 * It contains a Screen object.
 * 
 * @author and copyright (c) Wolfgang Lenerz 2012-2017.
 * 
 * @version 
 * 1.33 when setting device names,, show to what "./" will be expanded
 * 1.32 setDeviceNames : when setting the ptr. pos. for opening the dad wdw, use a new point if ptr is outsie the wdw.
 * 1.31 device names set from within SMSQ/E are stored in the inifile ; new config item locked 
 *      qxl.win files may be read only ; SUSPEND-WHEN-ICONIFIED state is actually loaded at startup ;
 *      new config item action upon jva_popup ; ssssFrequencyMenuItem text is set accoring to language ; java version item added
 * 1.30 allowScreenEmulation config item added.
 * 1.29 Exit item in files menu and action routine ; provide for full screen modes:
 *      creation requires new parameters (window mode) ; if full screen window mode, wdw is undecorated ; call screen.setFullSize ; 
 *      new config items : window mode and display full screen on which monitor?, and corresponding action routines.
 * 1.28 Monitor.changeMemSize amended parameter used when changing mem size, screenSizeMenuItemActionPerformed makes new screen,
 *      screenMode variable introduced, monitor.reset() no longEr takes params, when screen mode/colours are changed,
 *      this takes effect immediately (setScreenModeForNextRestart), Throttle removed.
 * 1.27 getMonitor() implemented, screen update rate, devices sub-menu added.
 * 1.26 optimizations, italian language.
 * 1.25 use java.awt.event.InputEvent.BUTTON1_MASK instead of java.awt.event.InputEvent.BUTTON1_DOWN_MASK in doAltSpace,
 *      don't print ALT F3 to system.out
 * 1.24 new config item for mouse click delay and related action routine.
 * 1.23 new config item for qxl.win files lock error.
 * 1.22 only use alt-space kludge for windows, better ALT+SPACE handling when in windows. The systems menu is closed and the space key is marked as released!
 * 1.21 warning for read-only floppy drives.
 * 1.20 only call getOptions once wdw is open - shows errors in wdw ....
 * 1.19 new config menu item (use less CPU time when idle) ; made group of everything relating to energy/cpu saving
 * 1.18 this object is now split between this remaining object and the Screen object : routines relating to mouse/key presses moved to the screen object;
 *      use MonitorPanel object instead of creating these panels myself ; formWindowOpened starts the emulation, not the Monitor object when loading ROM.
 * 1.17 "warn if sound problem" config item and supporting code added.
 * 1.16 xsize must be multiple of 8 for all screen modes.
 * 1.15 cleanup (make sure screen object is removed when new screen is made). Screen size may be given as a * b instead of a x b.
 *      Slightly more bright colours menu item added. Set windowActivated status, ptr in wdw is only set when ptr "in screen" and wdw is activated.
 *      Provide for checkbox to suspend execution if window is minimized and call monitor.
 * 1.14 provide for 8 bit colours choice ; save rom file name before raising error.
 * 1.13 handle alt + cursor keys ; mouse wheel generates three keystrokes ; about shows java version for which this was compiled ; 
 *          when opening wdw, reposition wdw at its last position.
 * 1.12 set "pointer in screen" status; use new icon; changed some "config" menu item positions.
 * 1.11 correctly set status of read only warning item on startup; handle Shift tab, shift del, alt del, shift alt del. 
 * 1.10 better mouse handling when in double sized mode, warning for read only qxl.win drives added, set screen coordinates when wdw moved/resized ;
 *      ignore mouse moved event when mouse position in wdw is changed from within SMSQE via sptr.
 * 1.09 in memsize choice box current memsize is the default value. Max mem size = 96 MiB
 * 1.08 make sure screen is at least 512 x 256 pixels.
 * 1.07 when screen size is set, record true screen size in .ini file (could have been changed if in mode 4).
 *      Caps lock status is set in SMSQ/E. Show the SMSQ/E screen size when changing screen size, not the window size.
 *      Better handling of doubling/halving screen size.
 * 1.06 correct check whether screen was in double size upon launching or not. Warning items status set in ini file again.
 *      menu bar can be made invisible (use JVAMBAR Sbasic command to get it back).
 * 1.05 the time correction item really calls the time correction code.
 * 1.04 screen size (but not resolution) changes when the window is resized. Possibility to double the window size (not resolution) introduced.
 * 1.03 warn that machine will be reset when screen size is changed.
 * @version 1.02 provide for "time correction" option.
 * @version 1.01 handle some accented chars,"wake up" monitorGo thread when key typed.
 * @version 1.00 set warning checkboxes status on startup, added Throttle config item,only make new (QXL,SFA,NFA,FLP) filenames if OK was hit in DAD wdw.
 * @version 0.05 added some Mac specific code for keyboard handling and window appearance, courtesy of Tobias Fröschle.
 * @version 0.04 device usage configuration item : it set to "", device name is set back to original name.
 * @version 0.03 F12 = alt+space, first attempts at mac keyboard handling.
 * @version 0.02 many more configurable items.
 * @version 0.01 traps mouse movement and buttons.
 * @version 0.00
 */
public class MonitorGui extends javax.swing.JFrame implements MonitorHandler
{
    private Monitor monitor;
    private DriveAssignmentsDialog dad;                     // dialog to assign dirs to (nfa) drives	
    private String[]nfaNames={"","","","","","","",""};     // names of nfa drives
    private String[]sfaNames={"","","","","","","",""};     // names of sfa drives
    private String[]winNames={"","","","","","","",""};     // names of qxl(.win) drives
    private String[]flpNames={"","","","","","","",""};     // names of flp drives
    private String[]memNames={"","","","","","","",""};     // names of mem drives
    private final inifile.IniFile inifile;                  // the .ini file with saved options
    private final javax.swing.JFileChooser fc;
    private String romFile="";                              // name of romfile
    private int memSize=2;
    private Screen screen;                                  // the screen object for the emulator - this is also a swing.JPanel.
    private static final Object[]  memPossibilities = {"1","2","4","8","16","32","64","96","128","240"};// the memory possibilities we have
    private boolean isMac;                  
    private int language;                                   // preset english
    private final Warnings warnings;
    private boolean isDouble;                               // flip flop, if true screen size is doubled
    private boolean windowActivated=false;
    private final String version;                           // in the Localization object
    private final MonitorPanel monitorPanel;
    private java.awt.Robot robot=null;
    private int screenUpdateRate;                           // rate at which screen is updated
    private int screenMode;                                 // mode of the Ql screen (0,16,32)
    private int windowMode;                                 // mode of swing wdw: 0 =window, 1 = full size, 2 = special full size
    private java.awt.GraphicsDevice[] gds;                  // the monitors in a multi monitor environment
    private int currentMonitor;
    
    /**
     * Creates new form MonitorGui.
     * 
     * @param screen the <code>Screen</code> object for displaying things.
     * @param inifile the <code>Inifile</code> object containing configured values.
     * @param warnings object with warning flags.
     * @param jversion the java version this is compiled for.
     * @param vibrantCols <code>true</code> if colours should be a little bit brighter. 
     * @param isApplet whether this is an applet (if yes, don't set exit_on_close).
     * @param windowMode 0,1,or 2 : window, full screen, special full screen.
     * @param gd the graphics configuration of the current monitor screen we're on.
     * @param gds the info about all monitors that the user has connected.
     * @param currentMonitor nbr of monitor the screen is displayed on in a multi-monitor environment.
     * 
     */
    public MonitorGui(Screen screen,final inifile.IniFile inifile,final Warnings warnings,int jversion,boolean vibrantCols, 
            final boolean isApplet, final int windowMode,java.awt.GraphicsConfiguration gd,java.awt.GraphicsDevice[] gds,
            int currentMonitor) 
    {
        super (gd);
        this.windowMode=windowMode;
        this.inifile=inifile;
        this.screen=screen;
        this.screenMode=screen.getMode();
        this.warnings=warnings;
        this.gds=gds;
        this.currentMonitor=currentMonitor;   
        
        String os=System.getProperty("os.name").toLowerCase();
        if (this.windowMode>0) 
        {
            java.awt.Rectangle bounds = gd.getBounds();
            this.setLocation(bounds.x,bounds.y);
            java.awt.Dimension size=new java.awt.Dimension (bounds.width,bounds.height);
            this.setSize(size);                                 // make window as big as screen
            setUndecorated(true);
         //   this.setAlwaysOnTop(true);
         //   this.setResizable(false);
            try
            {
                java.awt.GraphicsDevice device = gd.getDevice();
                
                /*if (os.contains("mac"))  
                {
                    try
                    {
                        com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(this,true);
                        com.apple.eawt.Application.getApplication().requestToggleFullScreen(this);
                    }
                    catch (Exception ex)
                    {
                        this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); 
                        device.setFullScreenWindow(this);
                    }
                }
                else */
                if (os.contains("windows"))
                {
                    this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); 
                }
                else if (device.isFullScreenSupported())
                {
                    device.setFullScreenWindow(this);
                }
                else
                    this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); 
            }
            catch (Exception e)
            {
                //e.printStackTrace();
            }
        }   
        else
            this.setLocation(this.inifile.getOptionAsInt("WDW-XPOS",0),this.inifile.getOptionAsInt("WDW-YPOS",0));
        
        /*if (os.contains("mac"))                          // code contributed by Tobias Fröschle
        {
            final MonitorGui mon = this;
            // 1. get the Mac application
            com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
            // 3. Set a specific <CMD>+"Q" handler. We don't want the application 
            // to end if CMD-Q is pressed inadvertantly.
            app.setQuitHandler(new com.apple.eawt.QuitHandler() 
            {
                @Override
                public void handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent qe, com.apple.eawt.QuitResponse qr) 
                {
                    // Intention would be that when activated from the menu, then we should quit, 
                    // when activated through <CMD>-Q, inject the keypress but that's not quite soooo simple.....
                    if (qe.getSource() != mon)
                        qr.cancelQuit();
                    else
                        qr.performQuit();
                }
            });
            this.isMac = true;
        }*/
        screen.setIsMac(this.isMac);
        
        initComponents();                                   // setup netbeans created components
        setTitle("SMSQmulator v."+Localization.getVersion());
        this. deutschRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("images/flag-germany.gif")));
        this. englishRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("images/flag-uk.gif")));
        this. francaisRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("images/flag-france.gif")));
        this. espanolRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("images/flag-spain.gif")));
        this. italianRadioButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("images/flag-italy.png")));
        this.setIconImage(getFDImage("images/smsqmulator2.png"));// fame at last...
        
        this.monitorPanel=new MonitorPanel (this);
        this.vibrantColoursMenuItem.setSelected(vibrantCols);
        this.isDouble=this.inifile.getTrueOrFalse("DOUBLE-SIZE");
        if (!this.inifile.getTrueOrFalse("MENUBAR-VISIBLE"))
            this.jMenuBar1.setVisible(false);
        if (this.gds.length<2)
            this.fullScreenMonitorMenuItem.setVisible(false);
        
        setupScreen();                                      // setup the screen panel, causes pack()
        if (!isApplet)
            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        this.language=Localization.getLanguage();
        setLanguage(0);
        this.fc=new javax.swing.JFileChooser();
        this.version=""+jversion+")";
        
        // fiddling about with the keyboard
        // 1 - Stop tab as change focus key
        java.util.HashSet<java.awt.AWTKeyStroke> set = new java.util.HashSet<>
                 (java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getDefaultFocusTraversalKeys
                    (java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        set.clear();                                        // no more change focus keys!- this means that TAB is treated correctly
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set);
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().setDefaultFocusTraversalKeys(java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
        
        // 2 - Stop F10 bringing up strange menus (under linux & windws)
        javax.swing.Action doNothing = new javax.swing.AbstractAction() 
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) 
            {
             //  System.out.println("ALT F3");//just do nothing
            }
        };
        javax.swing.JPanel tcontent = (javax.swing.JPanel) getContentPane();
        javax.swing.InputMap inputMap = tcontent.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.KeyStroke stroke = javax.swing.KeyStroke.getKeyStroke("F10");
        inputMap.put(stroke, "F10");
        tcontent.getActionMap().put("F10", doNothing);  
  
       /*    
        
        stroke = javax.swing.KeyStroke.getKeyStroke("alt F3");
        
        inputMap = this.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "alt_F3");
        inputMap = this.getRootPane().getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
        inputMap.put(stroke, "alt_F3");
        inputMap = this.getRootPane().getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(stroke, "alt_F3");
        this.getRootPane().getActionMap().put("alt_F3", doNothing);
       */
        
        // 3 - stop Alt+Space bringing up a submenu in windows (actually, I can't stop if from  opening it but I close it again)
        javax.swing.Action actionListenerAltSpace = new javax.swing.AbstractAction() 
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) 
            {
               doaltSpace(false);
            }
        };  
        javax.swing.Action actionListenerAltShiftSpace = new javax.swing.AbstractAction() 
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) 
            {
               doaltSpace(true);
            }
        };
        if (os.contains("windows"))
        {
            inputMap = this.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);   
            stroke = javax.swing.KeyStroke.getKeyStroke("alt SPACE");
            inputMap.put(stroke, "xxx");
            this.getRootPane().getActionMap().put("xxx", actionListenerAltSpace);
            
            stroke = javax.swing.KeyStroke.getKeyStroke("shift alt SPACE");
            inputMap.put(stroke, "xsp");
            this.getRootPane().getActionMap().put("xsp", actionListenerAltShiftSpace);
            try
            {
                this.robot=new java.awt.Robot();
            }
            catch (Exception e)
            {
                /*nop*/
            }
        }
        // set warning checkboxes status                                          
        this.nonStandardQXLDriveCheckBox.setSelected(this.warnings.warnIfQXLDriveNotCompliant);                                                        
        this.nonexistingQxlFileCheckBox.setSelected(this.warnings.warnIfNonexistingQXLDrive);                                              
        this.qxlDriveFullCheckBox.setSelected(this.warnings.warnIfQXLDriveFull);                                           
        this.qxlDriveIsReadOnlyCheckBox.setSelected(this.warnings.warnIfQXLDriveIsReadOnly);                          
        this.soundProblemCheckBox.setSelected(this.warnings.warnIfSoundProblem);
        this.flpReadOnlyCheckBoxMenuItem.setSelected(this.warnings.warnIfFLPDriveIsReadOnly);
        this.nonexistingFlpFileCheckBoxMenuItem.setSelected(this.warnings.warnIfNonexistingFLPDrive);
        
        // set drives status items
        this.qxlDisableCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("DISABLE-WIN-DEVICE"));
        this.qxlIgnoreFilelockErrorCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("IGNORE-QXLWIN-LOCK-ERROR"));
        this.nfaDisableCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("DISABLE-NFA-DEVICE"));
        this.sfaDisableCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("DISABLE-sFA-DEVICE"));
        this.flpDisableCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("DISABLE-FLP-DEVICE"));
        this.memDisableCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("DISABLE-MEM-DEVICE"));
     
        // set idle time status item
        this.idleMenuItem.setSelected(this.inifile.getTrueOrFalse("LESS-CPU-WHEN-IDLE"));
        this.suspendIconifiedMenuItem.setSelected(this.inifile.getTrueOrFalse("SUSPEND-WHEN-ICONIFIED"));
        this.allowScreenEmulationjCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("QL-SCREEN-EMULATION"));
        int ppause=inifile.getOptionAsInt("MOUSE-CLICK-DELAY", 0);
        this.mouseClickDelayMenuItem.setText(Localization.Texts[127]+ppause);
        this.screen.setMouseClickDelay(ppause);
        this.screenUpdateRate=inifile.getOptionAsInt("SCREEN-UPDATE-RATE", 50);
        
        // set window mode status items
        switch (this.windowMode)
        {
            case 1:
                this.fullSizeMenuItem.setSelected(true);
                break;
            case 2:
                this.specialFullSizeMenuItem.setSelected(true);
                break;
            default:
                this.windowMenuItem.setSelected(true);
                break;
        }
        
        // set status of unlockable2ReadOnlyMenuItem.
        this.unlockable2ReadOnlyCheckBoxMenuItem.setSelected(this.inifile.getTrueOrFalse("MAKE-UNLOCKABLE-QXLWIN-READONLY"));
        this.unlockable2ReadOnlyCheckBoxMenuItem.setEnabled(inifile.getTrueOrFalse("IGNORE-QXLWIN-LOCK-ERROR"));
        
        // Set action after JVA_POPUP
        if (this.inifile.getTrueOrFalse("POPUP-ACTION"))
            this.popupBlinkCheckBoxMenuItem.setSelected(true);
        else
            this.popupOpenWdwCheckBoxMenuItem.setSelected(true);
    
        if (inifile.getOptionValue("SSSS-FREQUENCY").equals("20"))
            this.ssss20KHzCheckBoxMenuItem.setSelected(true);   
        else
            this.ssss22KHzCheckBoxMenuItem.setSelected(true); 
        
        int accel=inifile.getOptionAsInt("MOUSEWHEEL-ACCEL", 0);
        if (accel>0 & accel<10)
            this.screen.setMousewheelAccel(accel);
    }
    
    /**
     * Used when in windows. If I can't stop the LAF from opening a systems menu when ALT+SPACE
     * is hit, at least I can try to close that window again immediately.
     */
    private void doaltSpace(boolean shift)
    {
       java.awt.event.KeyEvent evt;
       if (shift)
           evt=new java.awt.event.KeyEvent(getRootPane(),java.awt.event.KeyEvent.VK_SPACE,System.currentTimeMillis(),
               java.awt.event.InputEvent.SHIFT_DOWN_MASK+java.awt.event.InputEvent.ALT_DOWN_MASK,java.awt.event.KeyEvent.VK_SPACE,' ');
       else 
           evt=new java.awt.event.KeyEvent(getRootPane(),java.awt.event.KeyEvent.VK_SPACE,
                System.currentTimeMillis(),java.awt.event.InputEvent.ALT_DOWN_MASK,java.awt.event.KeyEvent.VK_SPACE,' ');
        screen.screenPanelKeyTyped(evt);        // type in ALT+SPACE
        screen.requestFocus();
        if (this.robot != null)
        {
            this.robot.mousePress( java.awt.event.InputEvent.BUTTON1_MASK);    // push left mousebutton (gets rid of menu)
        }
        screen.screenPanelKeyReleased(evt);     // now release alt+space keys again!!!!!!
        if (this.robot != null)
        {
            this.robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);  // release left mouse button again
        }
    }
    
    /**
     * Adds the screen object to this.
     */
    private void setupScreen()
    {
        if (this.screen!=null)
            this.remove(this.screen);                       
        this.add(this.screen,java.awt.BorderLayout.CENTER);
        this.add(this.monitorPanel,java.awt.BorderLayout.PAGE_END);
        if (this.inifile.getTrueOrFalse("MONITOR-VISIBLE"))
        {
            this.monitorPanel.setVisible(true);
            this.monitorVisibleCheckBoxMenuItem.setSelected(true);
        }
        else
        {
            this.monitorPanel.setVisible(false);
            this.monitorVisibleCheckBoxMenuItem.setSelected(false);
        }
        java.awt.Dimension d=this.screen.getImageSize();// this method gets the image size
        if (this.isDouble)
        {
            d.width*=2;
            d.height*=2;
        }
        this.screen.setDoubleSize(this.isDouble);
        this.screen.setFullSize(this.windowMode>0);
        this.screen.setMinimumSize(d);
        this.screen.setMaximumSize(d);
        this.screen.setPreferredSize(d);
        this.screen.setSize(d);
        this.screen.setMonitor(this.monitor);
        this.invalidate();
        this.screen.setVisible(true);
        this.screen.setFocusable(true);
        this.pack();
    }
    
    /************* Getters/ setters ************************/
    /**
     * Sets the monitor for this wdw.
     * 
     * @param monitor the monitor to use.
     * @param getOpts true if options should be set.
     */
    public void setMonitor (Monitor monitor,boolean getOpts)
    {
        this.monitor=monitor;
        this.screen.setMonitor(monitor);
        if (getOpts)
            getOptions();
    }
    
    /**
     * Gets the left monitor "wdw".
     * 
     * @return the <code>javax.swing.JTextArea</code> corresponds to the left wdw.
     */
    public javax.swing.JTextArea getDataLogger()
    {
        return this.monitorPanel.getDataLogger();
    } 
    
    /**
     * Gets the right monitor wdw.
     * 
     * @return the <code>javax.swing.JTextArea</code> corresponds to the right wdw.
     */
    public javax.swing.JTextArea getRegLogger()
    {
        return this.monitorPanel.getRegLogger();
    }
    
    /**
     * Gets the command line wdw.
     * 
     * @return the command line "wdw", a <code>javax.swing.JTextField</code>.
     */
    public javax.swing.JTextField getInputWindow()
    {
        return this.monitorPanel.getInputWindow();
    }
       
    /**
     * Sets the name of the romfile.
     * 
     * @param s the name of the romfile.
     */
    public void setRomfile(String s)
    {
        this.romFile=s;
    }

    /**
     * Sets the focus to the screen object.
     */
    public void setFocus()
    {
        if (this.screen!=null)
        {
            this.screen.requestFocus();
            this.screen.requestFocusInWindow();
        }
    }
  
  /*********************** Actions for Monitor wdw JTextfield *******************************/
    /**
     * Sends a command to the monitor.
     * 
     * @param command the command string to send.
     */
    @Override
    public void monitorCommand (String command)
    {     
        if (this.monitor!=null)
        {
            this.monitor.executeMonitorCommand (command);
        }
    }

    /**
     * Sets the new screen coordinates when window was resized.
     * 
     * @param evt ignored
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) 
    {//GEN-FIRST:event_formComponentResized
        formComponentMoved(evt);
    }//GEN-LAST:event_formComponentResized

    /**
     * Sets the new screen coordinates when window has moved.
     * 
     * @param evt ignored
     */
    private void formComponentMoved(java.awt.event.ComponentEvent evt) 
    {//GEN-FIRST:event_formComponentMoved
        if (this.screen.isShowing())
        {
            java.awt.Point p=this.screen.getLocationOnScreen(); 
            this.monitor.setScreenCoordinates(p.x,p.y);
        }
        java.awt.Point p =this.getLocation();
        if (this.windowMode<1)
        {
            this.inifile.setOptionValue("WDW-XPOS",""+p.x);
            setOptionAndSave("WDW-YPOS",""+p.y);
        }
    }//GEN-LAST:event_formComponentMoved

    /**
     * Called when window is initialized, sets screen coordinates.
     * 
     * @param evt ignored
     */
    private void formWindowActivated(java.awt.event.WindowEvent evt) 
    {//GEN-FIRST:event_formWindowActivated
        formComponentMoved(evt);
        this.windowActivated=true;
    }//GEN-LAST:event_formWindowActivated


  /************************* Menu items action ********************************/

      /****  1 - Menu items in the "config" menu *************/

    /**
     * Menu item action : Get the names for the NFA drives & set them in the inifile and in the NFA device itself.
     * 
     * @param evt ignored
     */
    private void setNFANameMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_setNFANameMenuItemActionPerformed
        setDeviceNames(this.nfaNames,"NFA",Types.NFADriver,true);
    }//GEN-LAST:event_setNFANameMenuItemActionPerformed

   /**
     * Menu item action : Get the names for the SFA drives & set them in the inifile and in the SFA device itself.
     * 
     * @param evt ignored.
     */
    private void setSFANameMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_setSFANameMenuItemActionPerformed
        setDeviceNames(this.sfaNames,"SFA",Types.SFADriver,true);                                     
    }//GEN-LAST:event_setSFANameMenuItemActionPerformed

    /**
     * Menu item action : Get the names for the QXL drives & set them in the inifile and in the QXL device itself.
     * 
     * @param evt ignored.
     */
    private void setQxlNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_setQxlNameMenuItemActionPerformed
        setDeviceNames(this.winNames,"WIN",Types.WINDriver,false);
    }//GEN-LAST:event_setQxlNameMenuItemActionPerformed

   /**
     * Menu item action : Get the names for the FLP drives & set them in the inifile and in the FLP device itself.
     * 
     * @param evt ignored.
     */
    private void setFlpNamesMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_setFlpNamesMenuItemActionPerformed
        setDeviceNames(this.flpNames,"FLP",0,false);
        this.monitor.setFloppyNames(this.flpNames);      
    }//GEN-LAST:event_setFlpNamesMenuItemActionPerformed

   /**
     * Menu item action : Get the names for the MEM drives & set them in the inifile and in the MEM device itself.
     * 
     * @param evt ignored.
     */
    private void setMemNamesMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_setMemNamesMenuItemActionPerformed
    {//GEN-HEADEREND:event_setMemNamesMenuItemActionPerformed
        setDeviceNames(this.memNames,"MEM",Types.MEMDriver,false);
    }//GEN-LAST:event_setMemNamesMenuItemActionPerformed
 
    /**
     * Get & set names of dirs/files for qxl, sfa, etc.
     * 
     * @param oldNames the array currently holding the names
     * @param deviceName string name of device, 3 letters, uppercased.
     * @param dType which device.
     * @param mustBeDir true if file to be chosen must be a dir.
     */
    private void setDeviceNames(String[] oldNames,String deviceName,int dType,boolean mustBeDir) 
    {                                               
        this.dad=new DriveAssignmentsDialog(this,true,oldNames,deviceName,mustBeDir,this.inifile.getOptionValue("EXPANDED_DIR"));
        java.awt.Point p = this.getMousePosition();
        if (p==null)
            p=new java.awt.Point();
        java.awt.Point p1= this.getLocationOnScreen();
        if (!this.dad.adoptChanges(oldNames,p.x+p1.x+10,p.y+p1.y+10))
            return;
        
        String []newNames = this.dad.getOptions();
        setAndStoreDeviceNames(deviceName,oldNames,newNames,mustBeDir);// changes content of oldNames
        this.monitor.setNamesForDrives(dType,oldNames,false);
    }
    
    /**
     * Set new names for a device and save them to the inifile.
     * 
     * @param oldNames the array currently holding the names.
     * @param newNames the array holding the new names.
     * @param deviceName string name of device, 3 letters, uppercased.
     * @param dType which device.
     * @param mustBeDir  true if file to be chosen must be a dir0
    */
    private void setAndStoreDeviceNames(String deviceName,String []oldNames,String[] newNames,boolean mustBeDir) 
    {
        for (int i=0;i<8;i++) 
        {
            if (mustBeDir && newNames[i]!=null && !newNames[i].isEmpty() && (!newNames[i].endsWith(java.io.File.separator)))
               oldNames[i]=newNames [i]+ java.io.File.separator;
            else
                oldNames[i]=newNames[i];
            this.inifile.setOptionValue(deviceName+(i+1), oldNames[i]);
        }
        
        try
        {
            this.inifile.writeIniFile();
        }
        catch (Exception e)
        {/* NOP */}     
    }
    
    /**
     * Get some data for device, set new names for a device and save them to the inifile.
     * 
     * @param dType the type of device
     * @param newNames the 8-elements String array with the new names
     */
    public void setNewDeviceNames(int dType,String[]newNames)
    {
        String[] oldNames;
        boolean mustBeDir;
        String dName;
        switch (dType)
        {
            case Types.WINDriver:
                oldNames=this.winNames;
                mustBeDir=false;
                dName="WIN";
                break;
                
            case 0:                                             // floppy!!!!!!
                oldNames=this.flpNames;
                mustBeDir=false;
                dName="FLP";
                break;
                
            case Types.NFADriver:
                oldNames=this.nfaNames;
                mustBeDir=true;
                dName="NFA";
                break;
                
            case Types.SFADriver:
                oldNames=this.sfaNames;
                mustBeDir=true;
                dName="SFA";
                break;
                
            case Types.MEMDriver:
                oldNames=this.memNames;
                mustBeDir=false;
                dName="MEM";
                break;
                
            default:
                return;
        }
        setAndStoreDeviceNames(dName,oldNames,newNames,mustBeDir);
    }
    
    /**
     * Menu item action : Sets whether the names of files should be changed (on the NFA native file system).
     * 
     * @param change the type of change wished : 0 don't change the names, 1 - set names to upper case, 2 =set names to ower case.
     */ 
    private void setNfaNameChange(int change)
    {
        this.monitor.setFilenameChange(Types.NFADriver,change);
        setOptionAndSave("NFA-FILENAME-CHANGE", ""+change);
    }
    private void nfaNameUnchangedRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nfaNameUnchangedRadioButtonActionPerformed
        setNfaNameChange(0);
    }//GEN-LAST:event_nfaNameUnchangedRadioButtonActionPerformed
    private void nfaNameUpperCaseRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nfaNameUpperCaseRadioButtonActionPerformed
        setNfaNameChange(1);
    }//GEN-LAST:event_nfaNameUpperCaseRadioButtonActionPerformed
    private void nfaNameLowerCaseRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nfaNameLowerCaseRadioButtonActionPerformed
        setNfaNameChange(2);
    }//GEN-LAST:event_nfaNameLowerCaseRadioButtonActionPerformed

    /**
     * Menu item action : Sets whether the names of files should be changed (on the SFA native file system).
     * 
     * @param change the type of change wished : 0 don't change the names, 1 - set names to upper case, 2 =set names to ower case.
     */   
    private void setSfaNameChange(int change)
    {
        this.monitor.setFilenameChange(Types.SFADriver,change);
        setOptionAndSave("SFA-FILENAME-CHANGE", ""+change);
    }
    private void sfaNameUnchangedRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_sfaNameUnchangedRadioButtonActionPerformed
        setSfaNameChange(0);
    }//GEN-LAST:event_sfaNameUnchangedRadioButtonActionPerformed
    private void sfaNameUpperCaseRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_sfaNameUpperCaseRadioButtonActionPerformed
        setSfaNameChange(1);
    }//GEN-LAST:event_sfaNameUpperCaseRadioButtonActionPerformed
    private void sfaNameLowerCaseRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_sfaNameLowerCaseRadioButtonActionPerformed
        setSfaNameChange(2);
    }//GEN-LAST:event_sfaNameLowerCaseRadioButtonActionPerformed

    /**
     * Menu item actions : Sets the "USE" names of NFA/SFA/WIN/FLP devices.
     * This only will be active upon the next reset.
     * 
     * @param change the type of change wished : 0 don't change the names, 1 - set names to upper case, 2 =set names to ower case.
     */  
    private void nfaUseMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nfaUseMenuItemActionPerformed
        getUsageName("NFA");
    }//GEN-LAST:event_nfaUseMenuItemActionPerformed

    private void sfaUseMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_sfaUseMenuItemActionPerformed
        getUsageName("SFA");
    }//GEN-LAST:event_sfaUseMenuItemActionPerformed
   
    private void qxlUseMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_qxlUseMenuItemActionPerformed
        getUsageName("WIN");
    }//GEN-LAST:event_qxlUseMenuItemActionPerformed

    private void flpUseMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_flpUseMenuItemActionPerformed
        getUsageName("FLP");
    }//GEN-LAST:event_flpUseMenuItemActionPerformed

    private void memUseMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_memUseMenuItemActionPerformed
        getUsageName("MEM");
    }//GEN-LAST:event_memUseMenuItemActionPerformed
    
    
    /**
     * Gets the device usage name from the user and sets it.
     * 
     * @param device the device for which to set the usage name
     */
    private void getUsageName(String device)
    {
        device=device.toUpperCase();
        String s=this.inifile.getOptionValue(device+"_USE");    // try to get current use option from ini file
        if (s==null || s.isEmpty())
            s=device;
        s = (String)javax.swing.JOptionPane.showInputDialog(this,Localization.Texts[36]+"\n"+ device +Localization.Texts[37],
                    Localization.Texts[38]+device,javax.swing.JOptionPane.PLAIN_MESSAGE,null,null,s);// get new name from user
        if (s==null || s.isEmpty())
            return;
        if (s.length()!=3)
            Helper.reportError(Localization.Texts[30], Localization.Texts[39]+s, this);
        else
           setOptionAndSave(device+"_USE",s.toUpperCase());
    }
   
    /** 
     * Menu item action : This prompts for a new screen size, changes the screen size and restarts the emulation.
     * 
     * @param evt ignored.
     */
    private void screenSizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_screenSizeMenuItemActionPerformed
        java.awt.Dimension size=this.screen.getImageSize();
        String s = (String)javax.swing.JOptionPane.showInputDialog(
                    this,
                    Localization.Texts[40]+"\n"
                    + Localization.Texts[41]+" '512 x 256' "+Localization.Texts[42]+" '1024 x 512'\n"+
                    Localization.Texts[98]+"\n"+
                    Localization.Texts[43]+size.width+" x "+size.height+"\n"+
                    Localization.Texts[95]+"\n"+
                    "<html><font color=red>"+Localization.Texts[29]+"</font></html>",
                    Localization.Texts[44],
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);                                      // get user input
        if (s==null || s.isEmpty())
            return;                                             // nothing, just leave
        try
        { 
            String []options=s.split("x");                      // input size as a x b
            if (options.length!=2)
            {
                options=s.split("\\*");                         // input size as a * b
                if (options.length!=2)
                {
                    throw new java.io.IOException();            // this leaves the try clause
                }
            }
            int xsize=Integer.parseInt(options[0].trim());
            int ysize=Integer.parseInt(options[1].trim());
            if (xsize<512)
                xsize=512;
            if (ysize<256)
                ysize=256;                                      // screen has 512 x 256 as minimum size
            xsize=xsize+  ((xsize % 8)==0?  0 : (8-(xsize % 8)));// make xsize a multiple of 8
            this.inifile.setOptionValue("WDW_XSIZE", ""+xsize);
            this.inifile.setOptionValue("WDW_YSIZE", ""+ysize);
            this.inifile.writeIniFile();                        // write new sizes into ini file
            changeScreen (xsize,ysize);
        } 
        catch (Exception e)
        {
            Helper.reportError(Localization.Texts[45], Localization.Texts[40], this);
        }
    }
    private void changeScreen(int xsize,int ysize)
    {
        boolean vibrant=false;
        if (this.screen!=null)
            vibrant=this.screen.isVibrantColours();
        this.screen=null;                                   // prepare for garbage collection
        switch (this.screenMode)
        {
            case 32:
                this.screen=new smsqmulator.Screen32(xsize,ysize,0,vibrant,this.monitor,false);// make entirely new screen, mode 32
                break;  
            case 16:
                this.screen=new smsqmulator.Screen16(xsize,ysize,0,vibrant,this.monitor,false);// make entirely new screen, mode 32
                break;
            default:
                if (xsize == 512 &&  ysize == 256)              // make a QL compatible screen at $20000
                    this.screen=new smsqmulator.Screen0(512,256,-1,null,false);// these are the addresses of the QL screen 
                else
                    this.screen=new smsqmulator.Screen0(xsize,ysize,0,null,false);// make entirely new screen
        }
        setupScreen();                                      // set up new screen    formComponentMoved (null);                          // remake
        pack();   
        this.monitor.changeMemSize(-1, this.screen,this.inifile.getTrueOrFalse("QL-SCREEN-EMULATION"));
        
    }//GEN-LAST:event_screenSizeMenuItemActionPerformed

    /**
     * Menu item action : Sets/unsets fast mode.
     * 
     * @param evt ignored
     */
    private void fastModeMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_fastModeMenuItemActionPerformed
        this.monitor.setFastMode(this.fastModeMenuItem.isSelected()); 
        if (this.fastModeMenuItem.isSelected())
            setOptionAndSave("FAST-MODE", "1");
        else
            setOptionAndSave("FAST-MODE", "0");
    }//GEN-LAST:event_fastModeMenuItemActionPerformed

    /**
     * Sets the languages.
     */
    private void deutschRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_deutschRadioButtonActionPerformed
        setLanguage(1);
        setOptionAndSave("LANGUAGE","1");
    }//GEN-LAST:event_deutschRadioButtonActionPerformed

    private void englishRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_englishRadioButtonActionPerformed
        setLanguage(2);
        setOptionAndSave("LANGUAGE","2");
    }//GEN-LAST:event_englishRadioButtonActionPerformed

    private void espanolRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_espanolRadioButtonActionPerformed
        setLanguage(3);
        setOptionAndSave("LANGUAGE","3");
    }//GEN-LAST:event_espanolRadioButtonActionPerformed

    private void francaisRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_francaisRadioButtonActionPerformed
        setLanguage(4);
        setOptionAndSave("LANGUAGE","4");
    }//GEN-LAST:event_francaisRadioButtonActionPerformed


    /**
     * Sets all (visual) elements' texts to the current language.
     * 
     * @param language the language to use.
     */
    private void setLanguage(int language)
    {
        if (language>0 & language<6)
        {
            this.language=language;
            Localization.setLanguage(language);
        }

        // menu names
        this.filesMenu.setText(Localization.Texts[1]);
        this.devicesMenu.setText(Localization.Texts[132]);
        this.configMenu.setText(Localization.Texts[3]);

        // menu items
        this.aboutMenuItem.setText(Localization.Texts[4]);
        // file menu items
        this.loadSmsqeFileMenuItem.setText(Localization.Texts[6]);
        this.resetMenuItem.setText(Localization.Texts[7]);
        // config menu items
        this.setNFANameMenuItem.setText(Localization.Texts[8]);
        this.changeNfaNamesMenu.setText(Localization.Texts[9]);
        this.nfaUseMenuItem.setText(Localization.Texts[10]);
        this.setSFANameMenuItem.setText(Localization.Texts[11]);
        this.changeSfaNamesMenu.setText(Localization.Texts[12]);
        this.sfaUseMenuItem.setText(Localization.Texts[13]);
        this.setQxlNameMenuItem.setText(Localization.Texts[14]);
        this.qxlUseMenuItem.setText(Localization.Texts[15]);
        this.setFlpNamesMenuItem.setText(Localization.Texts[79]);
        this.flpUseMenuItem.setText(Localization.Texts[80]);
        this.setMemNamesMenuItem.setText(Localization.Texts[118]);
        this.memUseMenuItem.setText(Localization.Texts[119]);
        this.screenSizeMenuItem.setText(Localization.Texts[16]);
        this.screenColoursMenu.setText(Localization.Texts[17]);
        this.vibrantColoursMenuItem.setText(Localization.Texts[109]);
        this.screenUpdateRateMenuItem.setText(Localization.Texts[129]);
        this.windowModeMenuItem.setText(Localization.Texts[134]);
            this.windowMenuItem.setText(Localization.Texts[135]);
            this.fullSizeMenuItem.setText(Localization.Texts[136]);
            this.specialFullSizeMenuItem.setText(Localization.Texts[137]);
        this.fullScreenMonitorMenuItem.setText(Localization.Texts[138]);
        this.exitMenuItem.setText(Localization.Texts[141]);
        this.memSizeMenuItem.setText(Localization.Texts[18]);
        this.monitorVisibleCheckBoxMenuItem.setText(Localization.Texts[19]);
        this.fastModeMenuItem.setText(Localization.Texts[20]);
        this.nfaNameUnchangedRadioButton.setText(Localization.Texts[21]);
        this.nfaNameUpperCaseRadioButton.setText(Localization.Texts[22]);
        this.nfaNameLowerCaseRadioButton.setText(Localization.Texts[23]);
        this.sfaNameUnchangedRadioButton.setText(Localization.Texts[21]);
        this.sfaNameUpperCaseRadioButton.setText(Localization.Texts[22]);
        this.sfaNameLowerCaseRadioButton.setText(Localization.Texts[23]);
        
        this.wordColoursRadioButton.setText(Localization.Texts[24]);
        this.QLColoursRadioButton.setText(Localization.Texts[25]);
        this.byteColoursRadioButton.setText(Localization.Texts[107]);
        switch (this.language)
        {
            case 1:
                this.deutschRadioButton.setSelected(true);
                break;
            case 2:
            default:
                this.englishRadioButton.setSelected(true);
                break;
            case 3:
                this.espanolRadioButton.setSelected(true);
                break;
            case 4:
                this.francaisRadioButton.setSelected(true);
                break;
            case 5:
                this.italianRadioButton.setSelected(true);
                break;
        }
        
      //  this.throttleMenuItem.setText(Localization.Texts[87]);
        this.beepVolumeItem.setText(Localization.Texts[113]);
        this.suspendIconifiedMenuItem.setText(Localization.Texts[110]);
        this.dateOffsetMenuItem.setText(Localization.Texts[93]);
        this.menuBarInvisibleMenuItem.setText(Localization.Texts[71]);
        if (this.isDouble)
        {
            this.doubleSizeMenuItem.setText(Localization.Texts[97]);
        }
        else
        {
            this.doubleSizeMenuItem.setText(Localization.Texts[96]);
        }
        
        this.warningsMenu.setText(Localization.Texts[82]);
        this.nonStandardQXLDriveCheckBox.setText(Localization.Texts[83]);
        this.nonexistingQxlFileCheckBox.setText(Localization.Texts[84]);
        this.qxlDriveFullCheckBox.setText(Localization.Texts[85]);
        this.qxlDriveIsReadOnlyCheckBox.setText(Localization.Texts[104]);
        this.flpReadOnlyCheckBoxMenuItem.setText(Localization.Texts[122]);
        this.soundProblemCheckBox.setText(Localization.Texts[114]);       
        this.ssssFrequencyMenuItem.setText(Localization.Texts[143]);
        this.nonexistingFlpFileCheckBoxMenuItem.setText(Localization.Texts[124]);
        
        this.qxlDisableCheckBoxMenuItem.setText(Localization.Texts[99]);
        this.qxlIgnoreFilelockErrorCheckBoxMenuItem.setText(Localization.Texts[125]);
        this.unlockable2ReadOnlyCheckBoxMenuItem.setText(Localization.Texts[144]);
        this.nfaDisableCheckBoxMenuItem.setText(Localization.Texts[100]);
        this.sfaDisableCheckBoxMenuItem.setText(Localization.Texts[101]);
        this.flpDisableCheckBoxMenuItem.setText(Localization.Texts[102]);
        this.memDisableCheckBoxMenuItem.setText(Localization.Texts[120]);
        this.idleMenuItem.setText(Localization.Texts[121]);
        this.mouseClickDelayMenuItem.setText(Localization.Texts[127]);
        this.mousewheelAccelItem.setText(Localization.Texts[148]+ ": "+this.inifile.getOptionValue("MOUSEWHEEL-ACCEL"));
        this.allowScreenEmulationjCheckBoxMenuItem.setText(Localization.Texts[142]);
        this.popupActionMenu.setText(Localization.Texts[145]);
            this.popupOpenWdwCheckBoxMenuItem.setText(Localization.Texts[146]);
            this.popupBlinkCheckBoxMenuItem.setText(Localization.Texts[147]);
        pack();
    }
    
    /**
     * Sets the screen mode for the next restart of the emulation.
     * 
     * @param choice =0 for QL mode, =2 for 8bit aurora mode, =3 for 16 bit colours.
     */
    private void setScreenModeForNextRestart(int choice)
    {
        setOptionAndSave("SCREEN-MODE", ""+choice);
        switch (choice)
        {
            case 0:
                this.screenMode=0;
                break;
            case 2:
                this.screenMode=16;
                break;
            case 3:
                this.screenMode=32;
                break;
        }
        changeScreen(this.screen.xsize,this.screen.ysize);
    }
    private void wordColoursRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_wordColoursRadioButtonActionPerformed
        setScreenModeForNextRestart (3);
    }//GEN-LAST:event_wordColoursRadioButtonActionPerformed
    private void QLColoursRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_QLColoursRadioButtonActionPerformed
         setScreenModeForNextRestart (0);
    }//GEN-LAST:event_QLColoursRadioButtonActionPerformed
    private void byteColoursRadioButtonActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_byteColoursRadioButtonActionPerformed
        setScreenModeForNextRestart (2);
    }//GEN-LAST:event_byteColoursRadioButtonActionPerformed

   
    /**
     * Menu item action : Shows / hides the monitor window.
     * 
     * @param evt ignored.
     */
    private void monitorVisibleCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_monitorVisibleCheckBoxMenuItemActionPerformed
        this.monitorPanel.setVisible(this.monitorVisibleCheckBoxMenuItem.isSelected());
        this.pack();
        if (this.monitorVisibleCheckBoxMenuItem.isSelected())
            setOptionAndSave("MONITOR-VISIBLE", "1");
        else
            setOptionAndSave("MONITOR-VISIBLE", "0");
    }//GEN-LAST:event_monitorVisibleCheckBoxMenuItemActionPerformed

   /**
     * Menu item action : Change the size of the memory to be used.
     *<p> This implies a change of the CPU object!
     * 
     * @param evt ignored.
     */
    private void memSizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_memSizeMenuItemActionPerformed
        int mode=this.screen!=null?this.screen.getMode():0; 
        Object[]  possibilities=new Object[MonitorGui.memPossibilities.length-3]; // memory sizes proposed to the user
        if (mode>8)
        {
           System.arraycopy(memPossibilities,3,possibilities,0,MonitorGui.memPossibilities.length-3);// less choice if higher colours, we need more mem
        }
        else
        {
            possibilities=memPossibilities;                     // when in higher screen modes, use at least 8 MiB of mem
        }
        String s = (String)javax.swing.JOptionPane.showInputDialog(this, Localization.Texts[26]+"\n"+Localization.Texts[27]+
                this.memSize+"MB\n"+"<html><font color=red>"+Localization.Texts[29]+"</font></html>",
                Localization.Texts[28],javax.swing.JOptionPane.PLAIN_MESSAGE, null, possibilities, ""+this.memSize);// get mem size now
        if ((s != null) && (!s.isEmpty()))
        {
            try
            {
                this.memSize=Integer.parseInt(s);  
                setOptionAndSave("MEM_SIZE", s);                
                this.monitor.changeMemSize(this.memSize*1024*1024,this.screen,this.inifile.getTrueOrFalse("QL-SCREEN-EMULATION"));
            }
            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, Localization.Texts[30], s+Localization.Texts[31],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_memSizeMenuItemActionPerformed

    /**
     * Menu item action : Doubles/halves the window x & y sizes WITHOUT increasing resolution.
     * 
     * @param evt ignored
     */
    private void doubleSizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_doubleSizeMenuItemActionPerformed
        java.awt.Dimension t =this.screen.getImageSize();
        if (this.isDouble)
        {
            this.doubleSizeMenuItem.setText(Localization.Texts[96]);
        }
        else
        {
            t.width*=2;
            t.height*=2;
            this.doubleSizeMenuItem.setText(Localization.Texts[97]);
        }
        
        this.isDouble=!this.isDouble;
        this.screen.setDoubleSize(this.isDouble);
        this.screen.setSize(t);
        this.screen.setMinimumSize(t);
        this.screen.setMaximumSize(t);
        this.screen.setPreferredSize(t);
        invalidate();
        pack();
        setOptionAndSave("DOUBLE-SIZE", this.isDouble?"1":"0");
    }//GEN-LAST:event_doubleSizeMenuItemActionPerformed

    /**
     * Menu item actions : Check/uncheck the warning items.
     *
     * @param evt ignored. 
     */
    private void nonStandardQXLDriveCheckBoxActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nonStandardQXLDriveCheckBoxActionPerformed
        this.warnings.warnIfQXLDriveNotCompliant=this.nonStandardQXLDriveCheckBox.isSelected();
        setOptionAndSave("WARN-ON-NONSTANDARD-WINDRIVE",  this.warnings.warnIfQXLDriveNotCompliant?"1":"0");
    }//GEN-LAST:event_nonStandardQXLDriveCheckBoxActionPerformed

    private void nonexistingQxlFileCheckBoxActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_nonexistingQxlFileCheckBoxActionPerformed
        this.warnings.warnIfNonexistingQXLDrive=this.nonexistingQxlFileCheckBox.isSelected();
        setOptionAndSave("WARN-ON-NONEXISTING-WINDRIVE",  this.warnings.warnIfNonexistingQXLDrive?"1":"0");
    }//GEN-LAST:event_nonexistingQxlFileCheckBoxActionPerformed

    private void qxlDriveFullCheckBoxActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_qxlDriveFullCheckBoxActionPerformed
        this.warnings.warnIfQXLDriveFull=this.qxlDriveFullCheckBox.isSelected();
        setOptionAndSave("WARN-ON-WINDRIVE-FULL",  this.warnings.warnIfQXLDriveFull ?"1":"0");
    }//GEN-LAST:event_qxlDriveFullCheckBoxActionPerformed

    private void qxlDriveIsReadOnlyCheckBoxActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_qxlDriveIsReadOnlyCheckBoxActionPerformed
        this.warnings.warnIfQXLDriveIsReadOnly=this.qxlDriveIsReadOnlyCheckBox.isSelected();
        setOptionAndSave("WARN-ON-WINDRIVE-READ-ONLY",  this.warnings.warnIfQXLDriveIsReadOnly ?"1":"0");
    }//GEN-LAST:event_qxlDriveIsReadOnlyCheckBoxActionPerformed

    private void soundProblemCheckBoxActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_soundProblemCheckBoxActionPerformed
        this.warnings.warnIfSoundProblem=this.soundProblemCheckBox.isSelected();
        setOptionAndSave("WARN-ON-SOUND-PROBLEM",  this.warnings.warnIfSoundProblem ?"1":"0");
    }//GEN-LAST:event_soundProblemCheckBoxActionPerformed
    
    private void flpReadOnlyCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)                         
    {                                                                
        this.warnings.warnIfFLPDriveIsReadOnly=this.flpReadOnlyCheckBoxMenuItem.isSelected();
        setOptionAndSave("WARN-ON-FLPDRIVE-READ-ONLY",  this.warnings.warnIfFLPDriveIsReadOnly ?"1":"0");
    }                                                                          
    private void nonexistingFlpFileCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)                                                                   
    {//GEN-FIRST:event_nonexistingFlpFileCheckBoxMenuItemActionPerformed
        this.warnings.warnIfNonexistingFLPDrive=this.nonexistingFlpFileCheckBoxMenuItem.isSelected();
        setOptionAndSave("WARN-ON-NONEXISTING-FLPDRIVE",  this.warnings.warnIfNonexistingFLPDrive ?"1":"0");
    }//GEN-LAST:event_nonexistingFlpFileCheckBoxMenuItemActionPerformed


    /**
     * Menu item action : Sets the value for the date offset.
     * 
     * @param evt ignored.
     */
    private void dateOffsetMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_dateOffsetMenuItemActionPerformed
        boolean errValue=true;
        String s = this.inifile.getOptionValue("TIME-OFFSET") ;
        while (errValue)
        { 
            s = (String)javax.swing.JOptionPane.showInputDialog(
                    this,
                    Localization.Texts[94],
                    Localization.Texts[93],
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    s);
            if (s==null || s.isEmpty())
                return;
            try

            {
                int throt=Integer.parseInt(s);
                this.monitor.setTimeOffset (throt);
                this.setOptionAndSave("TIME-OFFSET",""+throt);
                errValue=false;
            }

            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, s+Localization.Texts[90], Localization.Texts[45],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_dateOffsetMenuItemActionPerformed
   
    /**
     * Menu item action : Click in "make menubar invisible" item.
     *
     * @param evt ignored.
     */
    private void menuBarInvisibleMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_menuBarInvisibleMenuItemActionPerformed
       menuBarVisible(false);
    }//GEN-LAST:event_menuBarInvisibleMenuItemActionPerformed

    /**
     * Makes the menu bar visible/invisible.
     * Note: making it visible is done from within SMSQ/E, JVAMBAR command.
     *
     * @param state = true if menu bar is to be visible, false if not.
     */
    public void menuBarVisible(boolean state)
    {
        this.jMenuBar1.setVisible(state);
        pack();
        setOptionAndSave("MENUBAR-VISIBLE",  state ?"1":"0");
    }

    /**
     * Checks whether menu bas is visible.
     * 
     * @return 1 if menu bar is visible, 0 if not.
     */
    public int menuBarIsVisible()
    {
        return this.jMenuBar1.isVisible()?1:0;
    }
    
    /**
     * Menu item actions : sets/unsets the "suspend when iconified" property.
     *
     * @param evt ignored.
     */
    private void suspendIconifiedMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_suspendIconifiedMenuItemActionPerformed
        setOptionAndSave("SUSPEND-WHEN-ICONIFIED",this.suspendIconifiedMenuItem.isSelected()?"1":"0");
    }//GEN-LAST:event_suspendIconifiedMenuItemActionPerformed
    
    /**
     * Menu item actions : disable device.
     *
     * @param evt ignored.
     */
    private void flpDisableCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_flpDisableCheckBoxMenuItemActionPerformed
        setOptionAndSave("DISABLE-FLP-DEVICE",  this.flpDisableCheckBoxMenuItem.isSelected() ?"1":"0");
    }//GEN-LAST:event_flpDisableCheckBoxMenuItemActionPerformed

    private void qxlDisableCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_qxlDisableCheckBoxMenuItemActionPerformed
        setOptionAndSave("DISABLE-WIN-DEVICE", this.qxlDisableCheckBoxMenuItem.isSelected() ?"1":"0");
    }//GEN-LAST:event_qxlDisableCheckBoxMenuItemActionPerformed

    private void sfaDisableCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_sfaDisableCheckBoxMenuItemActionPerformed
        setOptionAndSave("DISABLE-SFA-DEVICE", this.sfaDisableCheckBoxMenuItem.isSelected() ?"1":"0");
    }//GEN-LAST:event_sfaDisableCheckBoxMenuItemActionPerformed

    private void nfaDisableCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {                                                           
        setOptionAndSave("DISABLE-NFA-DEVICE", this.nfaDisableCheckBoxMenuItem.isSelected() ?"1":"0");
    }                                                              
    
    private void memDisableCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_memDisableCheckBoxMenuItemActionPerformed
        setOptionAndSave("DISABLE-MEM-DEVICE", this.memDisableCheckBoxMenuItem.isSelected() ?"1":"0");
    }//GEN-LAST:event_memDisableCheckBoxMenuItemActionPerformed
    
    /**
     * Menu item action : set vibrant colours on/off
     *
     * @param evt ignored.
     */
     private void vibrantColoursMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
     {//GEN-FIRST:event_vibrantColoursMenuItemActionPerformed
        this.screen.setVibrantColours(this.vibrantColoursMenuItem.isSelected(),this.monitor.cpu.getMemory());
        setOptionAndSave("VIBRANT-COLOURS",this.vibrantColoursMenuItem.isSelected()?"1":"0");
    }//GEN-LAST:event_vibrantColoursMenuItemActionPerformed
    
    /**
     * Menu item action : Sets the volume of the beep sound.
     
     * @param evt ignored
     */
    private void beepVolumeItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_beepVolumeItemActionPerformed
        boolean errValue=true;
        String s = this.inifile.getOptionValue("SOUND-VOLUME") ;
        while (errValue)
        { 
            s = (String)javax.swing.JOptionPane.showInputDialog(
                    this,
                    Localization.Texts[112],
                    Localization.Texts[113],
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    s);
            if (s==null || s.isEmpty())
                return;
            try
            {
                int vol=Integer.parseInt(s);
                this.monitor.setSoundVolume (vol);
                this.setOptionAndSave("SOUND-VOLUME",""+vol);
                errValue=false;
            }
            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, s+Localization.Texts[90], Localization.Texts[45],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_beepVolumeItemActionPerformed
    
    /**
     * Menu item action : Sets/unsets the "use less cpu when idle" setting.
     
     * @param evt ignored
     */
    private void idleMenuItemActionPerformed(java.awt.event.ActionEvent evt)
    {//GEN-FIRST:event_idleMenuItemActionPerformed
         setOptionAndSave("LESS-CPU-WHEN-IDLE", this.idleMenuItem.isSelected() ?"1":"0");
    }//GEN-LAST:event_idleMenuItemActionPerformed

     /**
     * Menu item action : Sets/unsets the "ignore when a qxl.win file is locked" setting.
     *
     * @param evt ignored
     */
   private void qxlIgnoreFilelockErrorCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_qxlIgnoreFilelockErrorCheckBoxMenuItemActionPerformed
        setOptionAndSave("IGNORE-QXLWIN-LOCK-ERROR", this.qxlIgnoreFilelockErrorCheckBoxMenuItem.isSelected() ?"1":"0");
        this.unlockable2ReadOnlyCheckBoxMenuItem.setEnabled(this.qxlIgnoreFilelockErrorCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_qxlIgnoreFilelockErrorCheckBoxMenuItemActionPerformed
 
   /**
     * Menu item action : Sets the delay when a mouse button is clicked.
     
     * @param evt ignored
     */
   
    private void mouseClickDelayMenuItemActionPerformed(java.awt.event.ActionEvent evt)                                                        
    {//GEN-FIRST:event_mouseClickDelayMenuItemActionPerformed
        String s=this.inifile.getOptionValue("MOUSE-CLICK-DELAY");
        if (s==null || s.isEmpty())
            s="0";                                              // get current value, default to 0 if none
        boolean errValue=true;
        while (errValue)                                        // do until valid value or nothing input by user
        {
            s = (String)javax.swing.JOptionPane.showInputDialog(this,Localization.Texts[128],
                        Localization.Texts[127],javax.swing.JOptionPane.PLAIN_MESSAGE,null,null,s);// get user input
            if (s==null || s.isEmpty())                         // none, do nothing
                return;
            try
            {
                int ppause=Integer.parseInt(s);
                this.setOptionAndSave("MOUSE-CLICK-DELAY",""+ppause);
                this.mouseClickDelayMenuItem.setText(Localization.Texts[127]+ppause);
                this.screen.setMouseClickDelay(ppause);
                errValue=false;
            }
            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, s+Localization.Texts[90], Localization.Texts[45],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_mouseClickDelayMenuItemActionPerformed
     
    
      /****  2 - Menu items in the "help" menu *************/
    /**
     * Menu item action : Shows the about menu item.
     * 
     * @param evt ignored.
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_aboutMenuItemActionPerformed
        java.net.URL imgURL = MonitorGui.class.getResource("images/combined.png");// get my icon
        if (imgURL!=null)
        {
            javax.swing.ImageIcon image = new javax.swing.ImageIcon(imgURL);    // and make it into correct icon
            javax.swing.JOptionPane.showMessageDialog(this,Localization.Texts[5]+Localization.Texts[106]+ this.version,"SMSQmulator ",1,image);
        }
        else
        {
            javax.swing.JOptionPane.showMessageDialog(this,Localization.Texts[5]+Localization.Texts[106]+this.version,"SMSQmulator ",1);
        }
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    
      /****  3 - Menu items in the "files" menu *************/

    /**
     * Item action : Loads the SMSQE file, stops the emulation & re-starts the emulation.
     * 
     * @param evt ignored
     */
    private void loadSmsqeFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_loadSmsqeFileMenuItemActionPerformed
        String fn=this.inifile.getOptionValue("ROM_IMAGE_FILE");
        if (fn!=null && !fn.isEmpty())
            fc.setCurrentDirectory(new java.io.File(fn));
        if (fc.showOpenDialog(this)==javax.swing.JFileChooser.APPROVE_OPTION)// get a filename
        {
            java.io.File f= fc.getSelectedFile();
            setOptionAndSave("ROM_IMAGE_FILE", f.getAbsolutePath());
            if (this.monitor.loadRom(f))
                this.monitor.goCommand(null,-1); 
        }
    }//GEN-LAST:event_loadSmsqeFileMenuItemActionPerformed


    /** 
     * Resets the emulator: restarts at the beginning.
     * 
     * @param evt ignored
     */
    private void resetMenuItemActionPerformed(java.awt.event.ActionEvent evt) 
    {//GEN-FIRST:event_resetMenuItemActionPerformed
  /*
        try
        {
            final java.util.Enumeration< java.net.NetworkInterface > interfaces =java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements())
            {
                Thread.sleep(1000);
                final java.net.NetworkInterface cur = interfaces.nextElement( );
                if ( cur.isLoopback( ) )
                {
                    continue;
                }
                
                System.out.println( "interface " + cur.getName( ) );
                for ( final java.net.InterfaceAddress addr : cur.getInterfaceAddresses())
                {
                    final java.net.InetAddress inet_addr = addr.getAddress();
                    if ( !( inet_addr instanceof java.net.Inet4Address ) )
                    {
                        continue;
                    }
                    System.out.println("  address: " + inet_addr.getHostAddress( ) +"/" + addr.getNetworkPrefixLength());
                    System.out.println("  broadcast address: " +addr.getBroadcast( ).getHostAddress( ));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
*/
        this.monitor.reset();
    }//GEN-LAST:event_resetMenuItemActionPerformed

  
  /******************************** Form opening / iconifying / de-iconifying ********************************/
    /**
     * When form opens, load rom if possible, shift focus to screen window and start emulation.
     * 
     * @param evt ignored
     */
    private void formWindowOpened(java.awt.event.WindowEvent evt) 
    {//GEN-FIRST:event_formWindowOpened
        getOptions();                                          // gets the options from the inifile and sets them.
        if (!this.romFile.isEmpty())
        {
            this.screen.requestFocusInWindow();
            this.screen.requestFocus();
            if (this.monitor.loadRom(this.romFile,null))
                this.monitor.goCommand(null,-1);
            else
                 Helper.reportError(Localization.Texts[45], Localization.Texts[32]+" ("+this.romFile+")", null);
        }
        else
        {
            Helper.reportError(Localization.Texts[32], Localization.Texts[33]+"\n"+Localization.Texts[34], this);
        }
        formComponentMoved(null);
    }//GEN-LAST:event_formWindowOpened

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) 
    {//GEN-FIRST:event_formWindowDeactivated
        this.windowActivated=false;
    }//GEN-LAST:event_formWindowDeactivated


    private void formWindowIconified(java.awt.event.WindowEvent evt) 
    {//GEN-FIRST:event_formWindowIconified
        if (this.suspendIconifiedMenuItem.isSelected())
            this.monitor.suspendExecution();
    }//GEN-LAST:event_formWindowIconified

    private void formWindowDeiconified(java.awt.event.WindowEvent evt) 
    {//GEN-FIRST:event_formWindowDeiconified
        if (this.suspendIconifiedMenuItem.isSelected())            
            this.monitor.resumeExecution();
    }//GEN-LAST:event_formWindowDeiconified
    


  /******************************** Misc ******************************/

    /**
     * Gets options from the inifile and sets them.
     * 
     * Also shows the state of the options in the GUI Config menu.
     */
    private void getOptions()
    {
        String nfa =this.inifile.getOptionValue("EXPANDED_DIR");
        int temp;
       
        getDriveNames("NFA",this.nfaNames,true,nfa);
        getDriveNames("SFA",this.sfaNames,true,nfa);
        getDriveNames("WIN",this.winNames,false,nfa);
        getDriveNames("MEM",this.memNames,false,nfa);
        getDriveNames("FLP",this.flpNames,false,nfa);               // get names for drives
       
        if (this.monitor!=null)
        {
            this.monitor.setNamesForDrives(Types.SFADriver,this.sfaNames,true); // and set them
            this.monitor.setNamesForDrives(Types.NFADriver,this.nfaNames,true);
            this.monitor.setNamesForDrives(Types.WINDriver,this.winNames,true);
            this.monitor.setNamesForDrives(Types.MEMDriver,this.memNames,true); 
            this.monitor.setFloppyNames(this.flpNames);
            
            temp=this.inifile.getOptionAsInt("NFA-FILENAME-CHANGE", 0);// NFA change names cases?
            this.monitor.setFilenameChange(Types.NFADriver,temp); // set options
            switch (temp)                                   // and show it in Gui
            {
                default:
                case 0:
                    this.nfaNameUnchangedRadioButton.setSelected(true);
                    break;
                case 1:
                    this.nfaNameUpperCaseRadioButton.setSelected(true);
                    break;
                case 2:
                    this.nfaNameLowerCaseRadioButton.setSelected(true);
                    break;
            }
            
            temp=this.inifile.getOptionAsInt("SFA-FILENAME-CHANGE", 0);// SFA change names cases?
            this.monitor.setFilenameChange(Types.SFADriver,temp); //
            switch (temp)
            {
                default:
                case 0:
                    this.sfaNameUnchangedRadioButton.setSelected(true);
                    break;
                case 1:
                    this.sfaNameUpperCaseRadioButton.setSelected(true);
                    break;
                case 2:
                    this.sfaNameLowerCaseRadioButton.setSelected(true);
                    break;
            }
            
            // set the usage names of inbuilt devices (no need to set name for flp!)
            nfa=this.inifile.getOptionValue("NFA_USE");
            if (nfa!=null && !nfa.isEmpty())
                this.monitor.setUsageForDrive(Types.NFADriver, nfa);  
            nfa=this.inifile.getOptionValue("SFA_USE");
            if (nfa!=null && !nfa.isEmpty())
                this.monitor.setUsageForDrive(Types.SFADriver, nfa);  
            nfa=this.inifile.getOptionValue("WIN_USE");
            if (nfa!=null && !nfa.isEmpty())
                this.monitor.setUsageForDrive(Types.WINDriver, nfa);
            nfa=this.inifile.getOptionValue("MEM_USE");
            if (nfa!=null && !nfa.isEmpty())
                this.monitor.setUsageForDrive(Types.MEMDriver, nfa);
        }
        
        this.memSize=this.inifile.getOptionAsInt("MEM_SIZE",8); // memory size
        
        if (this.inifile.getTrueOrFalse("MONITOR-VISIBLE"))     // monitor window visible or not
        {
            this.monitorPanel.setVisible(true);
            this.monitorVisibleCheckBoxMenuItem.setSelected(true);
        }
        else
        {
            this.monitorPanel.setVisible(false);
            this.monitorVisibleCheckBoxMenuItem.setSelected(false);
        }
        
        temp=this.inifile.getOptionAsInt("SCREEN-MODE",0);      // screen mode
        switch (temp)
        {
            default:
            case 0:
                this.QLColoursRadioButton.setSelected(true);
                break;
            case 2:
                this.byteColoursRadioButton.setSelected(true);
                break;
            case 3:
                this.wordColoursRadioButton.setSelected(true);
                break;
        }
        
        temp=this.inifile.getOptionAsInt("DIVIDER-LOCATION",0);
        if (temp!=0)
            this.monitorPanel.setDividerLocation(temp);
        if (this.inifile.getTrueOrFalse("FAST-MODE"))
            this.fastModeMenuItem.setSelected(true);
        else
            this.fastModeMenuItem.setSelected(false);
        this.pack();
    }
  
    /**
     * Get dir/file names from ini file for devices (eg dir name for NFA0, fienale for WIN1 etc...) and set them in the array.
     * 
     * @param drive what device we're getting them for
     * @param driveNames the array to fill in with the names
     * @param addSep true if a file separator should be added at end (yes if were looking for a dir, no if we're looking for a file)
     */
    private void getDriveNames(String drive,String [] driveNames, boolean addSep,String expandTo)
    {
        String p;
        for (int i=0;i<8;i++)
        {
            p=this.inifile.getOptionValue(drive+(i+1));
            if (p!=null)
            {
                if ( addSep &&!p.isEmpty() && !p.endsWith(java.io.File.separator))
                    p+=java.io.File.separator;
                if (p.startsWith("./"))
                    p=expandTo+p.substring(2);
                driveNames[i]=p;                                // set the names for the drives
            }
        }
    }
    
    /**
     * Pops the window up if iconified or flashes the taskbar..
     */
    public void deIconify()
    {
        if ((this.getExtendedState()&ICONIFIED)!=0)
        {
            if (inifile.getTrueOrFalse("POPUP-ACTION"))
            {
               java.awt.Image ic = getFDImage("images/smsqmulatorinv.png");
               TBFlasher mflasher=new TBFlasher(this,ic);
               mflasher.execute();
            }
            else
            {
                this.setExtendedState(NORMAL);
            }
        }
    }
           
    public void iconify()
    {
        this.setExtendedState(ICONIFIED);
    }
    /**
     * Sets the option in the inifile and saves the inifile.
     * (Options in inifile are (name = value) pairs).
     * 
     * @param optionName name of the option to set.
     * @param optionValue value of that option.
     */
    private void setOptionAndSave(String optionName,String optionValue)
    {
        this.inifile.setOptionValue(optionName,optionValue);
        try
        {
            this.inifile.writeIniFile();
        }
        catch (Exception e)
        {/*nop*/}
    }
   
    /**
     * Returns an Image or null.
     * 
     * @param filename where to get the image from.
     * 
     * @return the Image or null.
     */
    public static java.awt.Image getFDImage(String filename)
    {
        java.net.URL imgURL = MonitorGui.class.getResource(filename);
        if (imgURL != null)
            return new javax.swing.ImageIcon(imgURL).getImage();
        else
        {
        //    System.err.println("No image got");
            return null;
        }
    }
    
    /**
     * Gets the monitor.
     * 
     * @return  the monitor.
     */
    public final Monitor getMonitor()
    {
        return this.monitor;
    }
    
    /**
     * This is called whenever the divider location is changed, saves it to ini file.
     * 
     * @param newLocation the new divider location.
     */
    @Override
    public void dividerLocationChanged(int newLocation)
    {
        setOptionAndSave("DIVIDER-LOCATION",""+newLocation);
    }
    
    /**
     * Returns whether the mouse is "in" the Screen object or not.
     * 
     * @return <code>true</code> if the mouse pointer is "in" the Screen object, else <code>false</code>.
     */
    public boolean getMouseIsInScreen()
    {
        return this.screen.mouseInScreen() & this.windowActivated;
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
       return this.screen.setMousePosition(x, y);
    }
    
    
    /**************************************************************** Netbeans generated code ******************************************/
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        buttonGroup6 = new javax.swing.ButtonGroup();
        buttonGroup7 = new javax.swing.ButtonGroup();
        jMenuBar1 = new javax.swing.JMenuBar();
        filesMenu = new javax.swing.JMenu();
        loadSmsqeFileMenuItem = new javax.swing.JMenuItem();
        resetMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        configMenu = new javax.swing.JMenu();
        jMenu6 = new javax.swing.JMenu();
        deutschRadioButton = new javax.swing.JRadioButtonMenuItem();
        englishRadioButton = new javax.swing.JRadioButtonMenuItem();
        espanolRadioButton = new javax.swing.JRadioButtonMenuItem();
        francaisRadioButton = new javax.swing.JRadioButtonMenuItem();
        italianRadioButton = new javax.swing.JRadioButtonMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        devicesMenu = new javax.swing.JMenu();
        setNFANameMenuItem = new javax.swing.JMenuItem();
        changeNfaNamesMenu = new javax.swing.JMenu();
        nfaNameUnchangedRadioButton = new javax.swing.JRadioButtonMenuItem();
        nfaNameUpperCaseRadioButton = new javax.swing.JRadioButtonMenuItem();
        nfaNameLowerCaseRadioButton = new javax.swing.JRadioButtonMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        nfaUseMenuItem = new javax.swing.JMenuItem();
        nfaDisableCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        setSFANameMenuItem = new javax.swing.JMenuItem();
        changeSfaNamesMenu = new javax.swing.JMenu();
        sfaNameUnchangedRadioButton = new javax.swing.JRadioButtonMenuItem();
        sfaNameUpperCaseRadioButton = new javax.swing.JRadioButtonMenuItem();
        sfaNameLowerCaseRadioButton = new javax.swing.JRadioButtonMenuItem();
        sfaUseMenuItem = new javax.swing.JMenuItem();
        sfaDisableCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        setQxlNameMenuItem = new javax.swing.JMenuItem();
        qxlUseMenuItem = new javax.swing.JMenuItem();
        qxlDisableCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        qxlIgnoreFilelockErrorCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        unlockable2ReadOnlyCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        setFlpNamesMenuItem = new javax.swing.JMenuItem();
        flpUseMenuItem = new javax.swing.JMenuItem();
        flpDisableCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        flpUseNameMenuItem = new javax.swing.JPopupMenu.Separator();
        setMemNamesMenuItem = new javax.swing.JMenuItem();
        memUseMenuItem = new javax.swing.JMenuItem();
        memDisableCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        screenSizeMenuItem = new javax.swing.JMenuItem();
        screenColoursMenu = new javax.swing.JMenu();
        wordColoursRadioButton = new javax.swing.JRadioButtonMenuItem();
        byteColoursRadioButton = new javax.swing.JRadioButtonMenuItem();
        QLColoursRadioButton = new javax.swing.JRadioButtonMenuItem();
        windowModeMenuItem = new javax.swing.JMenu();
        windowMenuItem = new javax.swing.JRadioButtonMenuItem();
        fullSizeMenuItem = new javax.swing.JRadioButtonMenuItem();
        specialFullSizeMenuItem = new javax.swing.JRadioButtonMenuItem();
        vibrantColoursMenuItem = new javax.swing.JCheckBoxMenuItem();
        doubleSizeMenuItem = new javax.swing.JMenuItem();
        screenUpdateRateMenuItem = new javax.swing.JMenuItem();
        allowScreenEmulationjCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        fullScreenMonitorMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        beepVolumeItem = new javax.swing.JMenuItem();
        ssssFrequencyMenuItem = new javax.swing.JMenu();
        ssss22KHzCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        ssss20KHzCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        memSizeMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        monitorVisibleCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        fastModeMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        idleMenuItem = new javax.swing.JCheckBoxMenuItem();
        suspendIconifiedMenuItem = new javax.swing.JCheckBoxMenuItem();
        popupActionMenu = new javax.swing.JMenu();
        popupOpenWdwCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        popupBlinkCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        mouseClickDelayMenuItem = new javax.swing.JMenuItem();
        mousewheelAccelItem = new javax.swing.JMenuItem();
        dateOffsetMenuItem = new javax.swing.JMenuItem();
        menuBarInvisibleMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        warningsMenu = new javax.swing.JMenu();
        nonStandardQXLDriveCheckBox = new javax.swing.JCheckBoxMenuItem();
        nonexistingQxlFileCheckBox = new javax.swing.JCheckBoxMenuItem();
        qxlDriveFullCheckBox = new javax.swing.JCheckBoxMenuItem();
        qxlDriveIsReadOnlyCheckBox = new javax.swing.JCheckBoxMenuItem();
        soundProblemCheckBox = new javax.swing.JCheckBoxMenuItem();
        flpReadOnlyCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        nonexistingFlpFileCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jMenu3 = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        javaVersionMenuItem = new javax.swing.JMenuItem();

        jMenu1.setText("File");
        jMenuBar2.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar2.add(jMenu2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentMoved(java.awt.event.ComponentEvent evt)
            {
                formComponentMoved(evt);
            }
            public void componentResized(java.awt.event.ComponentEvent evt)
            {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowActivated(java.awt.event.WindowEvent evt)
            {
                formWindowActivated(evt);
            }
            public void windowDeactivated(java.awt.event.WindowEvent evt)
            {
                formWindowDeactivated(evt);
            }
            public void windowDeiconified(java.awt.event.WindowEvent evt)
            {
                formWindowDeiconified(evt);
            }
            public void windowIconified(java.awt.event.WindowEvent evt)
            {
                formWindowIconified(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                formWindowOpened(evt);
            }
        });

        filesMenu.setText("File");

        loadSmsqeFileMenuItem.setText("Load SMSQE File");
        loadSmsqeFileMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                loadSmsqeFileMenuItemActionPerformed(evt);
            }
        });
        filesMenu.add(loadSmsqeFileMenuItem);

        resetMenuItem.setText("Reset");
        resetMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                resetMenuItemActionPerformed(evt);
            }
        });
        filesMenu.add(resetMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                exitMenuItemActionPerformed(evt);
            }
        });
        filesMenu.add(exitMenuItem);

        jMenuBar1.add(filesMenu);

        configMenu.setText("Config");

        jMenu6.setText("Language/Sprache/Lengua/Langue/Lingua");
        jMenu6.setActionCommand("Lnaguage/Sprache/Lenguaje/Langue");

        buttonGroup4.add(deutschRadioButton);
        deutschRadioButton.setSelected(true);
        deutschRadioButton.setText("Deutsch");
        deutschRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                deutschRadioButtonActionPerformed(evt);
            }
        });
        jMenu6.add(deutschRadioButton);

        buttonGroup4.add(englishRadioButton);
        englishRadioButton.setText("English");
        englishRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                englishRadioButtonActionPerformed(evt);
            }
        });
        jMenu6.add(englishRadioButton);

        buttonGroup4.add(espanolRadioButton);
        espanolRadioButton.setText("Español");
        espanolRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                espanolRadioButtonActionPerformed(evt);
            }
        });
        jMenu6.add(espanolRadioButton);

        buttonGroup4.add(francaisRadioButton);
        francaisRadioButton.setText("Français");
        francaisRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                francaisRadioButtonActionPerformed(evt);
            }
        });
        jMenu6.add(francaisRadioButton);

        buttonGroup4.add(italianRadioButton);
        italianRadioButton.setText("Italiano");
        italianRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                italianRadioButtonActionPerformed(evt);
            }
        });
        jMenu6.add(italianRadioButton);

        configMenu.add(jMenu6);
        configMenu.add(jSeparator6);

        devicesMenu.setText("Devices");

        setNFANameMenuItem.setText("Set dirs for NFA drives");
        setNFANameMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setNFANameMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(setNFANameMenuItem);

        changeNfaNamesMenu.setText("NFA file name changes");

        buttonGroup1.add(nfaNameUnchangedRadioButton);
        nfaNameUnchangedRadioButton.setText("Leave as they are");
        nfaNameUnchangedRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nfaNameUnchangedRadioButtonActionPerformed(evt);
            }
        });
        changeNfaNamesMenu.add(nfaNameUnchangedRadioButton);

        buttonGroup1.add(nfaNameUpperCaseRadioButton);
        nfaNameUpperCaseRadioButton.setText("Set all NFA names to upper case");
        nfaNameUpperCaseRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nfaNameUpperCaseRadioButtonActionPerformed(evt);
            }
        });
        changeNfaNamesMenu.add(nfaNameUpperCaseRadioButton);

        buttonGroup1.add(nfaNameLowerCaseRadioButton);
        nfaNameLowerCaseRadioButton.setText("Set all NFA names to lower case");
        nfaNameLowerCaseRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nfaNameLowerCaseRadioButtonActionPerformed(evt);
            }
        });
        changeNfaNamesMenu.add(nfaNameLowerCaseRadioButton);
        changeNfaNamesMenu.add(jSeparator2);

        devicesMenu.add(changeNfaNamesMenu);

        nfaUseMenuItem.setText("NFA USE name");
        nfaUseMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nfaUseMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(nfaUseMenuItem);

        nfaDisableCheckBoxMenuItem.setSelected(true);
        nfaDisableCheckBoxMenuItem.setText("NFA disable");
        nfaDisableCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nfaDisableCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(nfaDisableCheckBoxMenuItem);
        devicesMenu.add(jSeparator1);

        setSFANameMenuItem.setText("Set dirs for SFA drives");
        setSFANameMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setSFANameMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(setSFANameMenuItem);

        changeSfaNamesMenu.setText("SFA file name changes");

        buttonGroup2.add(sfaNameUnchangedRadioButton);
        sfaNameUnchangedRadioButton.setText("Leave as they are");
        sfaNameUnchangedRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sfaNameUnchangedRadioButtonActionPerformed(evt);
            }
        });
        changeSfaNamesMenu.add(sfaNameUnchangedRadioButton);

        buttonGroup2.add(sfaNameUpperCaseRadioButton);
        sfaNameUpperCaseRadioButton.setText("Set all SFA names to upper case");
        sfaNameUpperCaseRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sfaNameUpperCaseRadioButtonActionPerformed(evt);
            }
        });
        changeSfaNamesMenu.add(sfaNameUpperCaseRadioButton);

        buttonGroup2.add(sfaNameLowerCaseRadioButton);
        sfaNameLowerCaseRadioButton.setText("Set all SFA names to lower case");
        sfaNameLowerCaseRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sfaNameLowerCaseRadioButtonActionPerformed(evt);
            }
        });
        changeSfaNamesMenu.add(sfaNameLowerCaseRadioButton);

        devicesMenu.add(changeSfaNamesMenu);

        sfaUseMenuItem.setText("SFA USE name");
        sfaUseMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sfaUseMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(sfaUseMenuItem);

        sfaDisableCheckBoxMenuItem.setSelected(true);
        sfaDisableCheckBoxMenuItem.setText("SFA disable");
        sfaDisableCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sfaDisableCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(sfaDisableCheckBoxMenuItem);
        devicesMenu.add(jSeparator5);

        setQxlNameMenuItem.setText("Set files for QXL drives");
        setQxlNameMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setQxlNameMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(setQxlNameMenuItem);

        qxlUseMenuItem.setText("QXL USE name");
        qxlUseMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                qxlUseMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(qxlUseMenuItem);

        qxlDisableCheckBoxMenuItem.setSelected(true);
        qxlDisableCheckBoxMenuItem.setText("WIN disable");
        qxlDisableCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                qxlDisableCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(qxlDisableCheckBoxMenuItem);

        qxlIgnoreFilelockErrorCheckBoxMenuItem.setSelected(true);
        qxlIgnoreFilelockErrorCheckBoxMenuItem.setText("Ignore file lock error");
        qxlIgnoreFilelockErrorCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                qxlIgnoreFilelockErrorCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(qxlIgnoreFilelockErrorCheckBoxMenuItem);

        unlockable2ReadOnlyCheckBoxMenuItem.setSelected(true);
        unlockable2ReadOnlyCheckBoxMenuItem.setText("Make unlockable read only");
        unlockable2ReadOnlyCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                unlockable2ReadOnlyCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(unlockable2ReadOnlyCheckBoxMenuItem);
        devicesMenu.add(jSeparator3);

        setFlpNamesMenuItem.setText("Set files for FLP drives");
        setFlpNamesMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setFlpNamesMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(setFlpNamesMenuItem);

        flpUseMenuItem.setText("FLP USE name");
        flpUseMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                flpUseMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(flpUseMenuItem);

        flpDisableCheckBoxMenuItem.setSelected(true);
        flpDisableCheckBoxMenuItem.setText("FLP disable");
        flpDisableCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                flpDisableCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(flpDisableCheckBoxMenuItem);
        devicesMenu.add(flpUseNameMenuItem);

        setMemNamesMenuItem.setText("Set files for mem drives");
        setMemNamesMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                setMemNamesMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(setMemNamesMenuItem);

        memUseMenuItem.setText("Mem USE name");
        memUseMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                memUseMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(memUseMenuItem);

        memDisableCheckBoxMenuItem.setSelected(true);
        memDisableCheckBoxMenuItem.setText("MEM disble");
        memDisableCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                memDisableCheckBoxMenuItemActionPerformed(evt);
            }
        });
        devicesMenu.add(memDisableCheckBoxMenuItem);

        configMenu.add(devicesMenu);
        configMenu.add(jSeparator11);

        screenSizeMenuItem.setText("Set sceen size");
        screenSizeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                screenSizeMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(screenSizeMenuItem);

        screenColoursMenu.setText("Screen colours/mode");

        buttonGroup3.add(wordColoursRadioButton);
        wordColoursRadioButton.setSelected(true);
        wordColoursRadioButton.setText("16 bit colours");
        wordColoursRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                wordColoursRadioButtonActionPerformed(evt);
            }
        });
        screenColoursMenu.add(wordColoursRadioButton);

        buttonGroup3.add(byteColoursRadioButton);
        byteColoursRadioButton.setText("8 bit colours");
        byteColoursRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                byteColoursRadioButtonActionPerformed(evt);
            }
        });
        screenColoursMenu.add(byteColoursRadioButton);

        buttonGroup3.add(QLColoursRadioButton);
        QLColoursRadioButton.setText("QL colours");
        QLColoursRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                QLColoursRadioButtonActionPerformed(evt);
            }
        });
        screenColoursMenu.add(QLColoursRadioButton);

        configMenu.add(screenColoursMenu);

        windowModeMenuItem.setText("Window mode");

        buttonGroup5.add(windowMenuItem);
        windowMenuItem.setSelected(true);
        windowMenuItem.setText("Window");
        windowMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                windowMenuItemActionPerformed(evt);
            }
        });
        windowModeMenuItem.add(windowMenuItem);

        buttonGroup5.add(fullSizeMenuItem);
        fullSizeMenuItem.setText("Full size");
        fullSizeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fullSizeMenuItemActionPerformed(evt);
            }
        });
        windowModeMenuItem.add(fullSizeMenuItem);

        buttonGroup5.add(specialFullSizeMenuItem);
        specialFullSizeMenuItem.setText("Special full size");
        specialFullSizeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                specialFullSizeMenuItemActionPerformed(evt);
            }
        });
        windowModeMenuItem.add(specialFullSizeMenuItem);

        configMenu.add(windowModeMenuItem);

        vibrantColoursMenuItem.setSelected(true);
        vibrantColoursMenuItem.setText("Vibrant colours");
        vibrantColoursMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                vibrantColoursMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(vibrantColoursMenuItem);

        doubleSizeMenuItem.setText("Double window size");
        doubleSizeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                doubleSizeMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(doubleSizeMenuItem);

        screenUpdateRateMenuItem.setText("Screen update rate");
        screenUpdateRateMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                screenUpdateRateMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(screenUpdateRateMenuItem);

        allowScreenEmulationjCheckBoxMenuItem.setSelected(true);
        allowScreenEmulationjCheckBoxMenuItem.setText("allow screen emulation");
        allowScreenEmulationjCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                allowScreenEmulationjCheckBoxMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(allowScreenEmulationjCheckBoxMenuItem);

        fullScreenMonitorMenuItem.setText("Monitor for full screen");
        fullScreenMonitorMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fullScreenMonitorMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(fullScreenMonitorMenuItem);
        configMenu.add(jSeparator4);

        beepVolumeItem.setText("Set beep volume");
        beepVolumeItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                beepVolumeItemActionPerformed(evt);
            }
        });
        configMenu.add(beepVolumeItem);

        ssssFrequencyMenuItem.setText("SSSS Frequency");

        buttonGroup7.add(ssss22KHzCheckBoxMenuItem);
        ssss22KHzCheckBoxMenuItem.setSelected(true);
        ssss22KHzCheckBoxMenuItem.setText("22.05 KHz (22050 Hz)");
        ssss22KHzCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ssss22KHzCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ssssFrequencyMenuItem.add(ssss22KHzCheckBoxMenuItem);

        buttonGroup7.add(ssss20KHzCheckBoxMenuItem);
        ssss20KHzCheckBoxMenuItem.setText("20 KHz (20000 Hz)");
        ssss20KHzCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ssss20KHzCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ssssFrequencyMenuItem.add(ssss20KHzCheckBoxMenuItem);

        configMenu.add(ssssFrequencyMenuItem);
        configMenu.add(jSeparator10);

        memSizeMenuItem.setText("Memory Size");
        memSizeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                memSizeMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(memSizeMenuItem);
        configMenu.add(jSeparator8);

        monitorVisibleCheckBoxMenuItem.setSelected(true);
        monitorVisibleCheckBoxMenuItem.setText("Monitor visible");
        monitorVisibleCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                monitorVisibleCheckBoxMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(monitorVisibleCheckBoxMenuItem);

        fastModeMenuItem.setSelected(true);
        fastModeMenuItem.setText("Fast mode");
        fastModeMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fastModeMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(fastModeMenuItem);
        configMenu.add(jSeparator12);

        idleMenuItem.setSelected(true);
        idleMenuItem.setText("Less CPU when idle");
        idleMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                idleMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(idleMenuItem);

        suspendIconifiedMenuItem.setSelected(true);
        suspendIconifiedMenuItem.setText("Suspend when iconified");
        suspendIconifiedMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                suspendIconifiedMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(suspendIconifiedMenuItem);

        popupActionMenu.setText("Action when popup event");

        buttonGroup6.add(popupOpenWdwCheckBoxMenuItem);
        popupOpenWdwCheckBoxMenuItem.setSelected(true);
        popupOpenWdwCheckBoxMenuItem.setText("Open window");
        popupOpenWdwCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                popupOpenWdwCheckBoxMenuItemActionPerformed(evt);
            }
        });
        popupActionMenu.add(popupOpenWdwCheckBoxMenuItem);

        buttonGroup6.add(popupBlinkCheckBoxMenuItem);
        popupBlinkCheckBoxMenuItem.setText("Blink");
        popupBlinkCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                popupBlinkCheckBoxMenuItemActionPerformed(evt);
            }
        });
        popupActionMenu.add(popupBlinkCheckBoxMenuItem);

        configMenu.add(popupActionMenu);
        configMenu.add(jSeparator9);

        mouseClickDelayMenuItem.setText("mouselcikcdelay");
        mouseClickDelayMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mouseClickDelayMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(mouseClickDelayMenuItem);

        mousewheelAccelItem.setText("Mousewheel acceleration factor");
        mousewheelAccelItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                mousewheelAccelItemActionPerformed(evt);
            }
        });
        configMenu.add(mousewheelAccelItem);

        dateOffsetMenuItem.setText("Time correction");
        dateOffsetMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                dateOffsetMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(dateOffsetMenuItem);

        menuBarInvisibleMenuItem.setText("Menu bar invisible");
        menuBarInvisibleMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuBarInvisibleMenuItemActionPerformed(evt);
            }
        });
        configMenu.add(menuBarInvisibleMenuItem);
        configMenu.add(jSeparator7);

        warningsMenu.setText("Warnings");

        nonStandardQXLDriveCheckBox.setText("Non standard qxl drive");
        nonStandardQXLDriveCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nonStandardQXLDriveCheckBoxActionPerformed(evt);
            }
        });
        warningsMenu.add(nonStandardQXLDriveCheckBox);

        nonexistingQxlFileCheckBox.setText("Non existing qxl file");
        nonexistingQxlFileCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nonexistingQxlFileCheckBoxActionPerformed(evt);
            }
        });
        warningsMenu.add(nonexistingQxlFileCheckBox);

        qxlDriveFullCheckBox.setText("qxl Drive full");
        qxlDriveFullCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                qxlDriveFullCheckBoxActionPerformed(evt);
            }
        });
        warningsMenu.add(qxlDriveFullCheckBox);

        qxlDriveIsReadOnlyCheckBox.setSelected(true);
        qxlDriveIsReadOnlyCheckBox.setText("Drive is read only");
        qxlDriveIsReadOnlyCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                qxlDriveIsReadOnlyCheckBoxActionPerformed(evt);
            }
        });
        warningsMenu.add(qxlDriveIsReadOnlyCheckBox);

        soundProblemCheckBox.setSelected(true);
        soundProblemCheckBox.setText("Sound problem");
        soundProblemCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                soundProblemCheckBoxActionPerformed(evt);
            }
        });
        warningsMenu.add(soundProblemCheckBox);

        flpReadOnlyCheckBoxMenuItem.setSelected(true);
        flpReadOnlyCheckBoxMenuItem.setText("Flp read only");
        flpReadOnlyCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                flpReadOnlyCheckBoxMenuItemActionPerformed(evt);
            }
        });
        warningsMenu.add(flpReadOnlyCheckBoxMenuItem);

        nonexistingFlpFileCheckBoxMenuItem.setSelected(true);
        nonexistingFlpFileCheckBoxMenuItem.setText("jCheckBoxMenuItem1");
        nonexistingFlpFileCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nonexistingFlpFileCheckBoxMenuItemActionPerformed(evt);
            }
        });
        warningsMenu.add(nonexistingFlpFileCheckBoxMenuItem);

        configMenu.add(warningsMenu);

        jMenuBar1.add(configMenu);

        jMenu3.setText("?");

        aboutMenuItem.setText("About...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                aboutMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(aboutMenuItem);

        javaVersionMenuItem.setText("Java version");
        javaVersionMenuItem.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                javaVersionMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(javaVersionMenuItem);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void italianRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_italianRadioButtonActionPerformed
    {//GEN-HEADEREND:event_italianRadioButtonActionPerformed
        setLanguage(5);
        setOptionAndSave("LANGUAGE","5");
    }//GEN-LAST:event_italianRadioButtonActionPerformed

    private void screenUpdateRateMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_screenUpdateRateMenuItemActionPerformed
    {//GEN-HEADEREND:event_screenUpdateRateMenuItemActionPerformed
        String s = (String)javax.swing.JOptionPane.showInputDialog(this, Localization.Texts[130]+"\n"+Localization.Texts[131]+
                this.screenUpdateRate,
                Localization.Texts[129],javax.swing.JOptionPane.PLAIN_MESSAGE, null, null, ""+this.screenUpdateRate);// get mem size now
        if ((s != null) && (!s.isEmpty()))
        {
            try
            {
                this.screenUpdateRate=Integer.parseInt(s);  
                if (this.screenUpdateRate<0)
                    this.screenUpdateRate=0;
                if (this.screenUpdateRate>65535)
                    this.screenUpdateRate=65535;
                setOptionAndSave("SCREEN-UPDATE-RATE", s);  
                this.monitor.setScreenUpdateInterval(this.screenUpdateRate);
            }
            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, s+Localization.Texts[31], Localization.Texts[30],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_screenUpdateRateMenuItemActionPerformed

    private void fullSizeMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fullSizeMenuItemActionPerformed
    {//GEN-HEADEREND:event_fullSizeMenuItemActionPerformed
        this.windowMode=1;
        setOptionAndSave("Window-MODE", ""+this.windowMode);
        
    }//GEN-LAST:event_fullSizeMenuItemActionPerformed

    private void windowMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_windowMenuItemActionPerformed
    {//GEN-HEADEREND:event_windowMenuItemActionPerformed
        this.windowMode=0;
        setOptionAndSave("Window-MODE", ""+this.windowMode);
    }//GEN-LAST:event_windowMenuItemActionPerformed

    private void specialFullSizeMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_specialFullSizeMenuItemActionPerformed
    {//GEN-HEADEREND:event_specialFullSizeMenuItemActionPerformed
        this.windowMode=2;
        setOptionAndSave("Window-MODE", ""+this.windowMode);
    }//GEN-LAST:event_specialFullSizeMenuItemActionPerformed

    private void fullScreenMonitorMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fullScreenMonitorMenuItemActionPerformed
    {//GEN-HEADEREND:event_fullScreenMonitorMenuItemActionPerformed
       MultiMonitorDialog mmd= new MultiMonitorDialog(this,true,this.gds,this.currentMonitor);
       int choice=mmd.getChoice();
       if (choice!=-1)
       {
            setOptionAndSave("SCREEN-NUMBER", ""+this.currentMonitor);
       }
    }//GEN-LAST:event_fullScreenMonitorMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exitMenuItemActionPerformed
    {//GEN-HEADEREND:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void allowScreenEmulationjCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_allowScreenEmulationjCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_allowScreenEmulationjCheckBoxMenuItemActionPerformed
        setOptionAndSave("QL-SCREEN-EMULATION",this.allowScreenEmulationjCheckBoxMenuItem.isSelected()?"1":"0");
    }//GEN-LAST:event_allowScreenEmulationjCheckBoxMenuItemActionPerformed

    private void unlockable2ReadOnlyCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_unlockable2ReadOnlyCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_unlockable2ReadOnlyCheckBoxMenuItemActionPerformed
        setOptionAndSave("MAKE-UNLOCKABLE-QXLWIN-READONLY",unlockable2ReadOnlyCheckBoxMenuItem.isSelected()?"1":"0");
    }//GEN-LAST:event_unlockable2ReadOnlyCheckBoxMenuItemActionPerformed

    private void popupOpenWdwCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_popupOpenWdwCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_popupOpenWdwCheckBoxMenuItemActionPerformed
        setOptionAndSave("POPUP-ACTION","0");
    }//GEN-LAST:event_popupOpenWdwCheckBoxMenuItemActionPerformed

    private void popupBlinkCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_popupBlinkCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_popupBlinkCheckBoxMenuItemActionPerformed
        setOptionAndSave("POPUP-ACTION","1");
    }//GEN-LAST:event_popupBlinkCheckBoxMenuItemActionPerformed

    // Set sampledSound frequency
    private void ssss20KHzCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ssss20KHzCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_ssss20KHzCheckBoxMenuItemActionPerformed
        setOptionAndSave("SSSS-FREQUENCY","20");
    }//GEN-LAST:event_ssss20KHzCheckBoxMenuItemActionPerformed
    private void ssss22KHzCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ssss22KHzCheckBoxMenuItemActionPerformed
    {//GEN-HEADEREND:event_ssss22KHzCheckBoxMenuItemActionPerformed
        setOptionAndSave("SSSS-FREQUENCY","22");
    }//GEN-LAST:event_ssss22KHzCheckBoxMenuItemActionPerformed

   /**
     * Menu item action : Show java version.
     * 
     * @param evt ignored.
     */
    private void javaVersionMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_javaVersionMenuItemActionPerformed
    {//GEN-HEADEREND:event_javaVersionMenuItemActionPerformed
        java.net.URL imgURL = MonitorGui.class.getResource("images/combined.png");// get my icon
        if (imgURL!=null)
        {
            javax.swing.ImageIcon image = new javax.swing.ImageIcon(imgURL);    // and make it into correct icon
            javax.swing.JOptionPane.showMessageDialog(this," Java Version: " + System.getProperty("java.version")+
                           " from "+System.getProperty("java.vendor"),"SMSQmulator ",1,image);
        }
        else
        {
            javax.swing.JOptionPane.showMessageDialog(this," Java Version: " + System.getProperty("java.version")+
                           " from "+System.getProperty("java.vendor"),"SMSQmulator ",1);
        }
    }//GEN-LAST:event_javaVersionMenuItemActionPerformed

    /**
     * Menu item action : Sets the value for the mousewheel acceleration.
     * 
     * @param evt ignored.
     */
    private void mousewheelAccelItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_mousewheelAccelItemActionPerformed
    {//GEN-HEADEREND:event_mousewheelAccelItemActionPerformed
        boolean errValue=true;
        String s = this.inifile.getOptionValue("MOUSEWHEEL-ACCEL") ;
        while (errValue)
        { 
            s = (String)javax.swing.JOptionPane.showInputDialog(
                    this,
                    Localization.Texts[148]+ " (1 - 9)",
                    Localization.Texts[148],
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    s);
            if (s==null || s.isEmpty())
                return;
            try
            {
                int accel=Integer.parseInt(s);
                if (accel>0 && accel<10)
                {
                    this.screen.setMousewheelAccel(accel);
                    this.setOptionAndSave("MOUSEWHEEL-ACCEL",""+accel);
                    this.mousewheelAccelItem.setText(Localization.Texts[148]+ ": "+accel);
                    errValue=false;
                }
            }

            catch (Exception e)
            {
                javax.swing.JOptionPane.showMessageDialog(this, s+Localization.Texts[90], Localization.Texts[45],javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_mousewheelAccelItemActionPerformed
      
    
    
    
    
    /**
     * Sets the value shown as default value when the screen update interval menu item is actioned.
     * 
     * @param tim the value in milliseconds.
     */
    public void setScreenUpdateInterval(int tim)
    {
        this.screenUpdateRate=tim;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButtonMenuItem QLColoursRadioButton;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JCheckBoxMenuItem allowScreenEmulationjCheckBoxMenuItem;
    private javax.swing.JMenuItem beepVolumeItem;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.ButtonGroup buttonGroup6;
    private javax.swing.ButtonGroup buttonGroup7;
    private javax.swing.JRadioButtonMenuItem byteColoursRadioButton;
    private javax.swing.JMenu changeNfaNamesMenu;
    private javax.swing.JMenu changeSfaNamesMenu;
    private javax.swing.JMenu configMenu;
    private javax.swing.JMenuItem dateOffsetMenuItem;
    private javax.swing.JRadioButtonMenuItem deutschRadioButton;
    private javax.swing.JMenu devicesMenu;
    private javax.swing.JMenuItem doubleSizeMenuItem;
    private javax.swing.JRadioButtonMenuItem englishRadioButton;
    private javax.swing.JRadioButtonMenuItem espanolRadioButton;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JCheckBoxMenuItem fastModeMenuItem;
    private javax.swing.JMenu filesMenu;
    private javax.swing.JCheckBoxMenuItem flpDisableCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem flpReadOnlyCheckBoxMenuItem;
    private javax.swing.JMenuItem flpUseMenuItem;
    private javax.swing.JPopupMenu.Separator flpUseNameMenuItem;
    private javax.swing.JRadioButtonMenuItem francaisRadioButton;
    private javax.swing.JMenuItem fullScreenMonitorMenuItem;
    private javax.swing.JRadioButtonMenuItem fullSizeMenuItem;
    private javax.swing.JCheckBoxMenuItem idleMenuItem;
    private javax.swing.JRadioButtonMenuItem italianRadioButton;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JMenuItem javaVersionMenuItem;
    private javax.swing.JMenuItem loadSmsqeFileMenuItem;
    private javax.swing.JCheckBoxMenuItem memDisableCheckBoxMenuItem;
    private javax.swing.JMenuItem memSizeMenuItem;
    private javax.swing.JMenuItem memUseMenuItem;
    private javax.swing.JMenuItem menuBarInvisibleMenuItem;
    private javax.swing.JCheckBoxMenuItem monitorVisibleCheckBoxMenuItem;
    private javax.swing.JMenuItem mouseClickDelayMenuItem;
    private javax.swing.JMenuItem mousewheelAccelItem;
    private javax.swing.JCheckBoxMenuItem nfaDisableCheckBoxMenuItem;
    private javax.swing.JRadioButtonMenuItem nfaNameLowerCaseRadioButton;
    private javax.swing.JRadioButtonMenuItem nfaNameUnchangedRadioButton;
    private javax.swing.JRadioButtonMenuItem nfaNameUpperCaseRadioButton;
    private javax.swing.JMenuItem nfaUseMenuItem;
    private javax.swing.JCheckBoxMenuItem nonStandardQXLDriveCheckBox;
    private javax.swing.JCheckBoxMenuItem nonexistingFlpFileCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem nonexistingQxlFileCheckBox;
    private javax.swing.JMenu popupActionMenu;
    private javax.swing.JCheckBoxMenuItem popupBlinkCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem popupOpenWdwCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem qxlDisableCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem qxlDriveFullCheckBox;
    private javax.swing.JCheckBoxMenuItem qxlDriveIsReadOnlyCheckBox;
    private javax.swing.JCheckBoxMenuItem qxlIgnoreFilelockErrorCheckBoxMenuItem;
    private javax.swing.JMenuItem qxlUseMenuItem;
    private javax.swing.JMenuItem resetMenuItem;
    private javax.swing.JMenu screenColoursMenu;
    private javax.swing.JMenuItem screenSizeMenuItem;
    private javax.swing.JMenuItem screenUpdateRateMenuItem;
    private javax.swing.JMenuItem setFlpNamesMenuItem;
    private javax.swing.JMenuItem setMemNamesMenuItem;
    private javax.swing.JMenuItem setNFANameMenuItem;
    private javax.swing.JMenuItem setQxlNameMenuItem;
    private javax.swing.JMenuItem setSFANameMenuItem;
    private javax.swing.JCheckBoxMenuItem sfaDisableCheckBoxMenuItem;
    private javax.swing.JRadioButtonMenuItem sfaNameLowerCaseRadioButton;
    private javax.swing.JRadioButtonMenuItem sfaNameUnchangedRadioButton;
    private javax.swing.JRadioButtonMenuItem sfaNameUpperCaseRadioButton;
    private javax.swing.JMenuItem sfaUseMenuItem;
    private javax.swing.JCheckBoxMenuItem soundProblemCheckBox;
    private javax.swing.JRadioButtonMenuItem specialFullSizeMenuItem;
    private javax.swing.JCheckBoxMenuItem ssss20KHzCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem ssss22KHzCheckBoxMenuItem;
    private javax.swing.JMenu ssssFrequencyMenuItem;
    private javax.swing.JCheckBoxMenuItem suspendIconifiedMenuItem;
    private javax.swing.JCheckBoxMenuItem unlockable2ReadOnlyCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem vibrantColoursMenuItem;
    private javax.swing.JMenu warningsMenu;
    private javax.swing.JRadioButtonMenuItem windowMenuItem;
    private javax.swing.JMenu windowModeMenuItem;
    private javax.swing.JRadioButtonMenuItem wordColoursRadioButton;
    // End of variables declaration//GEN-END:variables
}

    
    /*    
     * unsuccesful attempts to stop alt + space under windows
        stroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE,java.awt.event.InputEvent.ALT_MASK);
        inputMap.put(stroke, "SPACE");
        tcontent.getActionMap().put("SPACE", actionListener);
        */
        
   /*
        javax.swing.InputMap inputMap = new javax.swing.InputMap();
        rootPane.setInputMap(1,null);
        * 
        *
        // try to stop altkey escaping the wdw, doesn't work (yet?)
        this.addFocusListener(new java.awt.event.FocusListener() {
            private final java.awt.KeyEventDispatcher altDisabler = new java.awt.KeyEventDispatcher() 
            {
                @Override
                public boolean dispatchKeyEvent(java.awt.event.KeyEvent e) 
                {
                    return e.getKeyCode() == 18;
                }
            };

            @Override
            public void focusGained(java.awt.event.FocusEvent e) 
            {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(altDisabler);
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) 
            {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(altDisabler);
            }
        });
      */
     //    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
     //    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().
    
    /* 
    @Override // trying to trap the alt + space key on windows
    public boolean dispatchKeyEvent(java.awt.event.KeyEvent e) 
    {
        //  if ( e.getKeyCode() == 18)//alt key
        if ( e.getKeyCode() == 32 && e.getModifiers()==java.awt.event.InputEvent.ALT_MASK)//alt key
        {
            e.consume();
            return true;
        }
        else
            return false;
    }
    */
       /*
        // 2 - Stop F10 bringing up strange menus (under linux) - this doesn't work
        javax.swing.Action actionListener = new javax.swing.AbstractAction() 
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) 
            {
              // System.out.println("ALT F3");//just do nothing
            }
        };

        javax.swing.JPanel tcontent = (javax.swing.JPanel) getContentPane();
        javax.swing.InputMap inputMap = tcontent.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        javax.swing.KeyStroke stroke = javax.swing.KeyStroke.getKeyStroke("F10");
        inputMap.put(stroke, "F10");
        tcontent.getActionMap().put("F10", actionListener);
        
        // 3 - stop Alt+Space bringing up a submenu in windows -this doesn't work!!!!!
        stroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE,java.awt.event.InputEvent.ALT_MASK);
        inputMap.put(stroke, "SPACE");
        tcontent.getActionMap().put("SPACE", actionListener);
        */
