package smsqmulator;

import java.io.IOException;

/**
 * The main object, it doesn't do much other than setup and start the monitor.
 * @author and copyright (c) Wolfgang Lenerz 2012-2017.
 * 
 * @version 
 * the scheme now is : the smsqe file is set by default to be found in the install dir. This may be overwritten by the ini file.
 * The inifile is searched for, in this order, in the command line, in the exec dir, in the install dir, in the user's dir.
 * 
 * 
 * 1.23 set new parameter for SoundDevice ; config item MOUSEWHEEL-ACCEL ; new way of handling finding of ini file ; 
 *      ./ expansion added ; may use command line parameter = entire ini file path.
 * 1.22 new config option to make unlockable qxl.win files read only ; correct window mode settings (when did they get 
 *      changed back?) ; new config entry for action after jva_popup.
 * 1.21 may use CPUforScreenEmulation.
 * 1.20 correct window mode settings.
 * 1.19 new config option "WINDOW-MODE" ; set Screen x, y sizes according to window mode.
 * 1.18 new config item for mouse click delay.
 * 1.17 new config item for qxl.win files lock error.
 * 1.16 added inifile items for inexistent and read-only floppy image files.
 * 1.15 don't use fast mode if this is disabled in config file.
 * 1.14 new config item use less cpu when idle.
 * 1.13 preset options for screen colours is set to 0 (when did that disappear)?
 * 1.12 changes to accommodate embedded applet, check for major verion corrected, will now display versions beyond 7.
 * 1.11 added WARN-ON-SOUND-PROBLEM config item.
 * 1.10 xsize must be multiple of 8 for all screen modes ; provides for BEEP volume in ini file.
 * 1.09 New config option suspend execution if wdw iconified.
 * 1.09 New config option: more vibrant colours. Optimisations. Cpu uses MB and not KB for mem size.
 * 1.08 try to get the java version for which these classes were compiled, some cleanup, new ini options window x and y positions.
 * 1.07 provide for "disable xxx device" ini file options.
 * 1.06 fixed bootDir for windows where the filename doesn't always contain java.io.File.separators but "/" instead. If an ini file
 *      is present on the boot device, always use that, even for saving the ini file.
 * 1.05 try to read ini file from install dir first, then from home dir.
 * 1.04 make sure screen is at least 512 x 256 pixels.
 * 1.03 deleted doubled LAF settings for mac.
 * 1.02 use WIN_USE instead of QXL_USE
 * 1.01 provide for time offset option, use it for monitor.
 * 1.00 check for ini file, if none, use default qxl.win file.
 * 0.02 added some Mac specific code for application appearance, courtesy of Tobias Fröschle.
 * 0.01 try to load SMSQE from start dir if not configured.
 * 0.00
 * 
 */
public class SMSQmulator 
{
    private smsqmulator.cpu.MC68000Cpu cpu;
//    private String romFile;
    private Screen screen;;

    /**
     * This creates the main object.
     * 
     * @param compiledVersion 6,7 or 8  (or 0 if error) - the version for which this was compiled.
     * @param isApplet is this started as an applet?
     * @param appletAsApplication is this an applet started as an application?
     * @param iniFileName file name for an .ini file.
     */
    public SMSQmulator(int compiledVersion,boolean isApplet,boolean appletAsApplication,String iniFileName)
    {
 //       System.out.println("User dir : " +System.getProperty( "user.home" )+java.io.File.separator);
   //     System.out.println("Started from "+System.getProperty( "user.dir" )+java.io.File.separator);
     //   System.out.println("Started from " + java.nio.file.Paths.get("").toAbsolutePath().normalize().toString());    
       // System.out.println("Command line : "+bootFile); 
        String appDir = System.getProperty( "user.dir" )+java.io.File.separator; // this is where I was executed from APPDIR

        String installDir;                                          // where SMSQmulator is "installed" this is where the SMSQE file is by default (ow=verwritten by the ini file)
        try
        { 
            String path = SMSQmulator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");
            java.io.File file = new java.io.File(decodedPath);
            installDir = file.getAbsoluteFile().getParent()+java.io.File.separator; // this is where the app is running from (install dir)
        }
        catch (Exception e)
        {
            installDir = System.getProperty( "user.home" )+java.io.File.separator;
        }

        String iniDir=null;                                     // this is where I look for the ini file     
        if (iniFileName!=null && !iniFileName.isEmpty())         // no ini file was passed as parameter
        {
            java.io.File bFile = new java.io.File (iniFileName);   // ini file    
            if (!bFile.exists())
            {
                Helper.reportError("Error", "The ini file '"+iniFileName+"' doesn't exist!", null);
            }
            else
            {
                iniDir= bFile.getAbsoluteFile().getParent()+java.io.File.separator;  // ini directory
                iniFileName = bFile.getName();                     //ini filename
            }
        }
       
        if (iniDir==null)                                       // no ini file was passed as parameter, or it wasn't found
        {
            iniDir=appDir;                                      // use appdir
            iniFileName ="SMSQmulator.ini";
            java.io.File bFile = new java.io.File (iniDir+iniFileName);
             if (!bFile.exists())
                 iniDir=null;
        }
        
        if (iniDir==null)                                       // ini file still wasn't found, use install dir
        {
            iniDir=installDir;                                  // use install dir
            java.io.File bFile = new java.io.File (iniDir+iniFileName);
             if (!bFile.exists())
                 iniDir=null;
        }
        if (iniDir==null)                                       // ini file still wasn't found, use home dir
        {
            iniDir=System.getProperty( "user.home" )+java.io.File.separator;// use home dir
        }
        
        
        setLaF();                                               // nicer look and feel
        inifile.IniFile inifile=new inifile.IniFile();          // ini file with options
        presetOptions (inifile,installDir+"SMSQE");             // preset all options to default 
        inifile.setFilename(iniDir+iniFileName);
        try
        {
            inifile.readIniFile();                              // try to read in options from infile
        }
        catch (Exception e)                                     // couldn't read inifile, continue using default values & write them
        { 
            try
            {                                                   // try to read the ini file
                inifile.addOption("WIN1",appDir+"SMSQmulator.win","Directory name for WIN1_ drive");
                inifile.writeIniFile();
            }
            catch (Exception es)
            {/*nop*/}
        }
        
        inifile.addOption("EXPANDED_DIR",iniDir,"./ will be expanded to this - do not modify, will be overwritten!");
        int xsize=inifile.getOptionAsInt("WDW_XSIZE", 512);
        int ysize=inifile.getOptionAsInt("WDW_YSIZE", 256);
        if (xsize<512)
            xsize=512;
        if (ysize<256)
            ysize=256;
        xsize=xsize+ ( (xsize % 8)==0?  0 : (8-(xsize % 8))); // make this a multiple of 8
        int wdwMode=inifile.getOptionAsInt("WINDOW-MODE", 0);
        if (wdwMode>2 || wdwMode<0)
            wdwMode=0; 
        int screenNbr=inifile.getOptionAsInt("SCREEN-NUMBER", 0);
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.awt.GraphicsDevice[] gds = ge.getScreenDevices();
        int totalDisplays = gds.length;                         // numbere of screens the user has
        if (screenNbr>totalDisplays)                            // can we display on the screen that we used last time?...
            screenNbr=0;                                        // ...no, so make sure we display somewhere (there should always be a screen 0)
        java.awt.GraphicsDevice gd2 = gds[screenNbr];           // try to set the window to the same screen as that when it was used last time
      //  String test = gd2.getIDstring();
        java.awt.GraphicsConfiguration gd = gd2.getDefaultConfiguration();
        java.awt.Rectangle bounds = gd.getBounds();
        if (wdwMode>0)                                          
        {
            xsize=bounds.width;
            ysize=bounds.height;
            if (wdwMode==2 && inifile.getTrueOrFalse("DOUBLE-SIZE"))
            {
                xsize/=2;
                ysize/=2;
            }
        }
        
        int memSize=inifile.getOptionAsInt("MEM_SIZE",1);   // get configured mem, preset 1 MB memory
        switch (inifile.getOptionAsInt("SCREEN-MODE",0))    
        {
            case 2:                                         // mode 32
                this.screen=new Screen16(xsize,ysize,0,inifile.getTrueOrFalse("VIBRANT-COLOURS"),null,false);         // make entirely new screen 
                if (memSize<8) 
                    memSize=8;  
                break;
            case 3:                                         // mode 32
                this.screen=new Screen32(xsize,ysize,0,inifile.getTrueOrFalse("VIBRANT-COLOURS"),null,false);         // make entirely new screen 
                if (memSize<8) 
                    memSize=8;  
                break;
            default:                                        // mode 0 - this is also used when user configured rubbish in the ini file.
                if ( xsize == 512 &&  ysize == 256)         // make a QL compatible screen
                    this.screen=new Screen0(xsize,ysize,0,null,false);// these are the addresses of the QL screen 
                else
                    this.screen=new Screen0(xsize,ysize,0,null,false);// make entirely new screen
        }
        int language=inifile.getOptionAsInt("LANGUAGE", 0);
        Localization local=new Localization(language);          // initialise this once!
        Warnings warnings=new Warnings();
        warnings.setWarnings(inifile);
        if (inifile.getTrueOrFalse("QL-SCREEN-EMULATION"))
            this.cpu = new smsqmulator.cpu.CPUforScreenEmulation(memSize*1024*1024,this.screen,inifile,350000);// create the CPU, set its mem size & screen object
        else
            this.cpu = new smsqmulator.cpu.MC68000Cpu(memSize*1024*1024,this.screen,inifile,350000);// create the CPU, set its mem size & screen object
        String m=inifile.getOptionValue("SSSS-FREQUENCY");
        SampledSound sam =new SampledSound(this.cpu,inifile.getOptionAsInt("SOUND-VOLUME", 50),warnings,m);
     //   SoundDevice sound =new SoundDevice(inifile.getOptionAsInt("SOUND-VOLUME", 50),warnings,this.cpu,m);
        SoundDevice sound =new SoundDevice(sam);
     
        MonitorGui gui=null;
        Monitor moni;
        if (isApplet && !appletAsApplication)
        {
            boolean fastMode=inifile.getTrueOrFalse("FAST-MODE");
            moni=new Monitor(this.cpu,false,false,sam,null,null,fastMode,warnings,inifile.getOptionAsInt("THROTTLE", 0),
                                     inifile.getOptionAsInt("TIME-OFFSET", 0),null,inifile.getOptionAsInt("SOUND-VOLUME", 50),sound,inifile);
            this.screen.setMonitor(moni);
        }
        else
        {
            gui=new MonitorGui(this.screen,inifile,warnings,compiledVersion,inifile.getTrueOrFalse("VIBRANT-COLOURS"),isApplet,
                wdwMode,gd,gds,screenNbr);
            boolean watchBreakpoint=true;                       // monitor watches breakpoints
            boolean loginstr=false;                             // but doesn't log instructions
            boolean fastMode=inifile.getTrueOrFalse("FAST-MODE");
       //     moni=new Monitor(this.cpu,watchBreakpoint,loginstr,sam,gui.getRegLogger(),gui.getDataLogger(),fastMode,local,warnings,inifile.getOptionAsInt("THROTTLE", 0),
            moni=new Monitor(this.cpu,watchBreakpoint,loginstr,sam,gui.getRegLogger(),gui.getDataLogger(),fastMode,warnings,inifile.getOptionAsInt("THROTTLE", 0),
                             inifile.getOptionAsInt("TIME-OFFSET", 0),gui,inifile.getOptionAsInt("SOUND-VOLUME", 50),sound,inifile);
            moni.setInputWindow(gui.getInputWindow());
            gui.setMonitor(moni,false);
            gui.getRegLogger().append("Use the small window below to type your commands.\nType 'h' for help.\n");
        }
        
        if (gui!=null)
        {
            String imageFile=inifile.getOptionValue("ROM_IMAGE_FILE");// any smsqe file configured?...
            if (imageFile!=null  && !imageFile.isEmpty())
            { 
                gui.setRomfile(imageFile);                      // ... yes, try to set the rom file
            }
            gui.setVisible(true);
        }
    }
    /*
    public Screen getScreen()
    {
        return this.screen;
    }
    
    public String getRomFile()
    {
        return this.romfile;
    }
    
    */
    /**
     * This starts the emulator.
     * 
     * @param args the command line arguments - ignored.
     */
    public static void main(final String[] args) 
    {        
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {                 
                java.io.InputStream in= SMSQmulator.class.getResourceAsStream("SMSQmulator.class");
                int major=0;
                try
                {
                    byte[] buffer = new byte[8];            // the first 8 bytes of a class contain info  about java version
                    in.read(buffer);  
                    major=(buffer[7]&0xff) -44;
                } 
                catch (IOException e)
                {/*nop*/}
                finally 
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {/*nop*/}
                }
                
/*                String p = System.getProperty("os.name");
                p=p+" ";
                p+= System.getProperty("os.version");
                p=p+ " ";
                p+= System.getProperty("os.arch");
                p=p+".";
                String m =p;
                */
                if (args.length!=0)
                    new SMSQmulator(major,false,false,args[0]);
                else
                    new SMSQmulator(major,false,false,null);
            }
        });
    }
            
    /**
     * Preset all options the inifile may have.
     * 
     * @param inifile the inifile with the options to be set.
     */
    public final static void presetOptions( inifile.IniFile inifile, String romImage)
    {
        String[] options = {"NFA1","","Directory name for NFA1_ drive","NFA2","","","NFA3","","","NFA4","","","NFA5","","","NFA6","","","NFA7","","","NFA8","","","NFA_USE","","",
                            "SFA1","","Directory name for SFA1_ drive","SFA2","","","SFA3","","","SFA4","","","SFA5","","","SFA6","","","SFA7","","","SFA8","","","SFA_USE","","",
                            "WIN1","","File name for WIN1_ drive","WIN2","","","WIN3","","","WIN4","","","WIN5","","","WIN6","","","WIN7","","","WIN8","","","WIN_USE","","",
                            "MEM1","","File name for MEM1_ drive","MEM2","","","MEM3","","","MEM4","","","MEM5","","","MEM6","","","MEM7","","","MEM8","","","MEM_USE","","",
                            "FLP1","","File name for FLP1_ drive","FLP2","","","FLP3","","","FLP4","","","FLP5","","","FLP6","","","FLP7","","","FLP8","","","FLP_USE","","",
                            "DISABLE-WIN-DEVICE","0","Disable the WIN device (0 = no, 1 = yes)","DISABLE-NFA-DEVICE","0","Disable the NFA device (0 = no, 1 = yes)",
                            "DISABLE-SFA-DEVICE","0","Disable the SFA device (0 = no, 1 = yes)","DISABLE-FLP-DEVICE","0","Disable the FLP device (0 = no, 1 = yes)",
                            "DISABLE-MEM-DEVICE","1","Disable the MEM device (0 = no, 1 = yes)",
                            "WDW_XSIZE","512","X size of window: min size = 512","WDW_YSIZE","256","Y size of window: min size = 256",
                            "ROM_IMAGE_FILE","","File containing the SMSQE code",
                            "NFA-FILENAME-CHANGE","","Change case of files on NFA devices (0: no change 1:upper case 2:lower case)",
                            "SFA-FILENAME-CHANGE","","Change case of file names on SFA devices (0: no change 1:upper case 2:lower case)",
                            "MEM_SIZE","8","Size of memory in MiB","SCREEN-MODE","3","Screen colour mode (0 = QL mode, 2 = Aurora 8 bit colour mode, 3 = 16 bit colour mode)",
                            "MONITOR-VISIBLE","0","Is the debugging monitor panel visible? (0 = no;  1 = yes)","DIVIDER-LOCATION","200","X location of debugging monitor windows divider",
                            "FAST-MODE","1","Start emulation in fast mode? (0 = no, 1 = yes)","LANGUAGE","2","Language to be used by the emulator (NOT SMSQ/E) (1-DE  2-UK/US  3-ES  4-FR  5-IT)",
                            "WARN-ON-NONSTANDARD-WINDRIVE","1","Warn if a WIN drive has a non standard drive map (0 = no, 1 = yes)","WARN-ON-NONEXISTING-WINDRIVE","1","Warn if a WIN drive file doesn't exist (0 = no, 1 = yes)",
                            "WARN-ON-WINDRIVE-FULL","0","Warn when a WIN drive is full (0 = no, 1 = yes)",
                            "TIME-OFFSET","0","Number of seconds the date is off (+ or -, 1 hour = 3600 secs)","DOUBLE-SIZE","0","Double the size of SMSQ/E pixels? (0 = no, 1 = yes)",
                            "MENUBAR-VISIBLE","1","Menu bar is visible (1 = yes, 0 = no)","WARN-ON-WINDRIVE-READ-ONLY","1","Warn when a WIN drive is read only (0 = no, 1 = yes)",
                            "WDW-XPOS","0","Window x position on screen","WDW-YPOS","0","Window y position on screen","VIBRANT-COLOURS","0","(Try to) use more vibrant colours (0 = no, 1 = yes)",
                            "SUSPEND-WHEN-ICONIFIED","1","Suspend execution when window is iconified? (0 = no, 1 = yes)","SOUND-VOLUME","50","Sound volume from 0 (no sound) to 100 (loudest)",
                            "WARN-ON-SOUND-PROBLEM","1","Warn if sound can't be correctly initialised (0 = no, 1 = yes)","WARN-ON-NONEXISTING-FLPDRIVE","1","Warn if any floppy image file can't be opened (0 = no, 1 = yes)",
                            "LESS-CPU-WHEN-IDLE","1","Use less CPU time when idle (0 = no, 1 = yes)","WARN-ON-FLPDRIVE-READ-ONLY","1","Warn if an flp drive is read only (0 = no, 1 = yes)",
                            "IGNORE-QXLWIN-LOCK-ERROR","0","Ignore error if qxl.win files can't be locked (0 = no, 1 = yes)",
                            "MOUSE-CLICK-DELAY","0","Delay after mouse click before mouse button can be released, in milliseconds",
                            "SCREEN-UPDATE-RATE","50","Frequency at which screen is updated in milliseconds",
                            "WINDOW-MODE","0","SMSQmulator window mode 0 (=window, 1 = full size, 2 = special full size)",
                            "SCREEN-NUMBER","0","Number of screen to be used in multi-monitor environment",
                            "QL-SCREEN-EMULATION","0","Allow QL screen emulation (warning: may result in performance penalty)? (0 = no, 1 = yes)",
                            "SSSS-FREQUENCY","22.05","SSSS frequency in Khz, allowed are 20 and 22.05 only, default 22.05",
                            "MAKE-UNLOCKABLE-QXLWIN-READONLY","0","When ignoring file lock errors, make an unlockable file read only",
                            "POPUP-ACTION","1","Action after JVA_POPUP : 0 = open wdw, 1 = blink taskbar entry",
                            "MOUSEWHEEL-ACCEL","1","Speed of mouse scroll wheel (1 - 9 = normal to fast)",
                            "EXPANDED_DIR","","./ will be expanded to this - do not modify, will be overwritten!"
                            };
        for (int i=0;i<options.length;i+=3)
        {
            inifile.addOption(options[i+0], options[i+1],options[i+2]);
        }
        inifile.addOption("ROM_IMAGE_FILE",romImage,"File containing the SMSQE code");
    }
         
    /**
     * Standard LaF setting to something nicer.
     */
    private void setLaF()
    {        
        try
        {
            boolean isWindows=true;
            String intern = System.getProperty("os.name").toLowerCase();// get current OS
            if (intern.toLowerCase().contains("mac"))
            {                                                                       // *** code contributed by Tobias Fröschle
                // do some Mac specific stuff like
                // 1. get the Mac application
                com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();

                // 2. Set the dock image for the app
                java.net.URL imgURL = MonitorGui.class.getResource("images/WolfSoft2.gif");
                java.awt.Image image = null;
                try 
                {
                    image = javax.imageio.ImageIO.read(imgURL);
                } 
                catch (java.io.IOException ex) 
                {
                    // Logger.getLogger(SMSQmulator.class.getName()).log(Level.SEVERE, null, ex);
                }
                app.setDockIconImage(image);
                
                // Set some properties to be a bit more Apple Application Guidelines conformant
                // Screen menu bar instead of window menu bar (also saves a bit of screen real estate)
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.brushMetalLook", "true");
            }
            else if (!intern.equals("windows")) // "linux" "unix" "sunos"
            {
                isWindows=false;
                java.awt.Font myFont=new java.awt.Font("Verdana",java.awt.Font.PLAIN,11);
                javax.swing.UIManager.put("DefaultFont", myFont);
                javax.swing.UIManager.put("defaultFont", myFont);
                javax.swing.UIManager.put("ArrowButton.font", myFont);
                javax.swing.UIManager.put("Button.font", myFont);
                javax.swing.UIManager.put("CheckBox.font", myFont);
                javax.swing.UIManager.put("CheckBoxMenuItem.font", myFont);
                javax.swing.UIManager.put("ColorChooser.font", myFont);
                javax.swing.UIManager.put("ComboBox.font", myFont);
                javax.swing.UIManager.put("DesktopIcon.font", myFont);
                javax.swing.UIManager.put("DesktopPane.font", myFont);
                javax.swing.UIManager.put("EditorPane.font", myFont);
                javax.swing.UIManager.put("FileChooser.font", myFont);
                javax.swing.UIManager.put("FormattedTextField.font", myFont);
                javax.swing.UIManager.put("InternalFrame.font", myFont);
                javax.swing.UIManager.put("InternalFrame.titlefont", myFont);
                javax.swing.UIManager.put("InternalFrameTitlePane.font", myFont);
                javax.swing.UIManager.put("Label.font", myFont);
                javax.swing.UIManager.put("List.font", myFont);
                javax.swing.UIManager.put("Menu.font", myFont);
                javax.swing.UIManager.put("MenuBar.font", myFont);
                javax.swing.UIManager.put("MenuItem.font", myFont);
                javax.swing.UIManager.put("OptionPane.font", myFont);
                javax.swing.UIManager.put("Panel.font", myFont);
                javax.swing.UIManager.put("PasswordField.font", myFont);
                javax.swing.UIManager.put("PopupMenu.font", myFont);
                javax.swing.UIManager.put("PopupMenuSeparator.font", myFont);
                javax.swing.UIManager.put("ProgressBar.font", myFont);
                javax.swing.UIManager.put("RadioButton.font", myFont);
                javax.swing.UIManager.put("RadioButtonMenuItem.font", myFont);
                javax.swing.UIManager.put("RootPane.font", myFont);
                javax.swing.UIManager.put("ScrollBar.font", myFont);
                javax.swing.UIManager.put("ScrollBarThumb.font", myFont);
                javax.swing.UIManager.put("ScrollBarTrack.font", myFont);
                javax.swing.UIManager.put("ScrollPane.font", myFont);
                javax.swing.UIManager.put("Saparator.font", myFont);
                javax.swing.UIManager.put("Spinner.font", myFont);
                javax.swing.UIManager.put("SplitPane.font", myFont);
                javax.swing.UIManager.put("Slider.font", myFont);
                javax.swing.UIManager.put("SliderThumb.font", myFont);
                javax.swing.UIManager.put("SliderTrack.font", myFont);
                javax.swing.UIManager.put("Table.font", myFont);
                javax.swing.UIManager.put("TableHeader.font", myFont);
                javax.swing.UIManager.put("TabbedPane.font", myFont);
                javax.swing.UIManager.put("TextArea.font", myFont);
                javax.swing.UIManager.put("TextField.font", myFont);
                javax.swing.UIManager.put("TextPane.font", myFont);
                javax.swing.UIManager.put("TitledBorder.font", myFont);
                javax.swing.UIManager.put("ToggleButton.font", myFont);
                javax.swing.UIManager.put("ToolBar.font", myFont);
                javax.swing.UIManager.put("ToolTip.font", myFont);
                javax.swing.UIManager.put("Tree.font", myFont);
                javax.swing.UIManager.put("Viewport.font", myFont);
                intern="Nimbus";  
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) 
                {
                    if (intern.equals(info.getName()))             // Metal ; CDE/Motif ; GTK+ ; Nimbus; Windows
                    {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            }
        }
        catch ( javax.swing.UnsupportedLookAndFeelException e)
        {
            System.out.println("Oops, Look and feel inexistant");
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Oops, classe inconnue (?)!!!");
        }
        catch (InstantiationException e)
        {
            System.out.println("Oops, classe non construite!!!");
        }
        catch (IllegalAccessException e)
        {
            System.out.println("Oops, erreur inconnue (?)!!!");
        }
        
      /*
        javax.swing.UIDefaults udefaults = javax.swing.UIManager.getLookAndFeelDefaults();
        java.util.Set defaults = udefaults.entrySet();
        for (java.util.Iterator i = defaults.iterator(); i.hasNext();) 
        {
          java.util.Map.Entry entry = (java.util.Map.Entry) i.next();
          System.out.print(entry.getKey() + " = ");
          System.out.println(entry.getValue());
        }
        
        
         javax.swing.UIDefaults uidefs = javax.swing.UIManager.getLookAndFeelDefaults();
    String[] keys = (String[]) uidefs.keySet().toArray(new String[0]);
    for (int i = 0; i < keys.length; i++) {
      Object v = uidefs.get(keys[i]);
     if (v instanceof javax.swing.text.JTextComponent.KeyBinding[]) {
        javax.swing.text.JTextComponent.KeyBinding[] keyBindsVal = (javax.swing.text.JTextComponent.KeyBinding[]) uidefs
            .get(keys[i]);
      }
    }
        */
        
        

    }
}
