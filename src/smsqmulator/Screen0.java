package smsqmulator;

/**
 * The emulated machine's screen - a QL mode 4 or 8 screen.
 * 
 * 
 * In ql screen mode 4/0, colours for 8 pixels are in 1 word - 4 pixels per byte.
 * The lower (even) address = green bits, the higer (odd) byte of the word = red bits
 * <p>
 * This object contains a "vramBufffer" containing the screen memory as SMSQE sees it, and a BufferedImage containing the
 * corresponding RGB data. "Drawing" on the screen is done by copying the image back to the screen.
 * The "video ram" data structure is an array of short : in QL modes 4 and 8, pixels are addressed as words (8 or 4 pixels per word)
 * 
 * @author and copyright (c) 2012 -2016 Wolfgang Lenerz
 * @version
 * 1.11 readXXXXFromScreen and alphaBlock removed, copyScreen moved to Screen object.
 * 1.10 copyScreen implemented.
 * 1.09 set stopAddress correctly, don't write beyond screen.
 * 1.08 implement getScreenSizeInBytes, setVibrantColours takes vram parameter, isQLScreen() introduced.
 * 1.07 better handling of edge condition (i.e. write at very last pixel on screen).
 * 1.06 slightly brighter colours possibility (not really) added - has no effect here. Part of screen initializing moved to super class.
 * 1.05 first split version. Fixed mode 8. When the object is created, the screen is set to mode 0, not mode 8.
 * (note versions up to 1.04 are for combined screens, see the Screen object).
 */
public class Screen0 extends Screen
{
    private int nbrOfPixelsPerByte=4;                     // number of pixels per byte IN MODE 4 
    private boolean isQLScreen=true;                      // are we emulating a standard QL screen?
    
    /**
     * Creates the object.
     * @param xsize the xsize of the screen in pixels
     * @param ysize the ysize of the screen in pixels. If they are both - 1, then use a QL screen.
     * @param vrambase the start of the "videoram" as far as the CPU is concerned. 
     * @param monitor the monitor running the cpu.
     * @param isMac true if we're running on a mac.
     */
    public Screen0 (int xsize,int ysize,int vrambase,Monitor monitor,boolean isMac)
    {
        super(xsize,ysize,monitor,isMac);
        this.mode=0;
        makeScreen(xsize,ysize,vrambase);
    }
    
    /**
     * This sets up the entire screen object, including the BufferedImage.
     * @param xsize the xsize of the screen, in pixels.
     * @param ysize the ysize of the screen, in pixels.
     * 
     * @param mode the screen mode (and hence the colour depth = the number of bytes per pixel) of the screen. 
     * This currently only handles the following:
     * <ul>
     *  <li>   mode = 0 : QL 4 colour mode  </li>
     *  <li>   mode = 8 : QL 8 colour mode  </li>
     * </ul>
     * @param buffer a buffer containing, amongst others, the screen memory.
     * @param vrambase the start of the screen ram as the CPU sees it. For the CPU, this will generally be above the ROM.
     * <p> NOTE : parameters <code>buffer</code> and <code>vrambase</code> are mutually exclusive. If buffer != null, then we are in a 
     * QL compatible mode where the screen ram is at $20000 and extends to $28000. The buffer then is the CPU's main memory.
     * 
     */
    private void makeScreen(int xsize,int ysize,int vrambase)
    {
        if (xsize==512 && ysize==256)                                 // emulate a standard QL screen at $20000  
        {
            this.startAddress=0x20000;
            this.stopAddress=0x28000-2;
            this.nbrOfBytesPerLine=128;                     // number of bytes per line IN MODES 4 & 8;
        }
        else
        {
            this.isQLScreen=false; 
            this.stopAddress=xsize*ysize/this.nbrOfPixelsPerByte;   // size of screen in bytes    
            this.nbrOfBytesPerLine=xsize/this.nbrOfPixelsPerByte;
            this.startAddress=vrambase;
            this.stopAddress=vrambase+this.getScreenSizeInBytes();            // where screen stops
        }   
       
            fillScreen (0);      
       }
    
     /**
     * Writes a byte to the "screen memory" and paints the corresponding pixel on the screen.
     * 
     * @param address where to write to. This is the SMSQE address, where it thinks video memory lies.
     * It has already been checked that the address is in my memory space.
     * 
     * @param value the value to write.
     */
    @Override
    public void writeByteToScreen(int address, int value, int wordValue)
    {
        writeWordToScreen (address&0xfffffffe,wordValue);
    }
    
    /**
     * Writes a word to screen memory, and paints the corresponding pixel on the screen.
     * 
     * @param addr the address where to write to. It is presumed that it has been checked that this is in screen mem.
     * 
     * @param value the value to write to screen mem.
     */
    @Override
    public void writeWordToScreen(int addr, int value)
    {   
        if (addr>=this.stopAddress)
            return;
        // in the QL screen, the screen memory is organized in words - we have to find the word in question
        value&=0xffff;
        addr-=this.startAddress;                            // where in my buffer the address lies : address is relative to start of screen
        int y=addr/this.nbrOfBytesPerLine;                  // the line (y coord) we're on
        int x=addr-(y*this.nbrOfBytesPerLine);              // the initial column (x coord) of the pixel
        x*=this.nbrOfPixelsPerByte;                         // these are the x & y coordinates of the pixel on the screen
        y*=this.xsize;
        int colour,col;  
       
        switch (this.mode)
        {
            case 0:                     
                x+=7;
                for (int i=0; i<8;i++)                      // 1 word = 8 pixels
                {
                    colour=(value & this.pixelmask4[i])>>>i;// blot out unwanted bits and shift to right for each pixel
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
                this.isDirty=true;
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
                        if (x*2+1<=this.xsize)
                        {
                            this.dataBuffer.setElem(x*2+y, col);
                            this.dataBuffer.setElem(x*2+y+1, col);
                        }
                    --x;
                }
                this.isDirty=true;
                break;
        }
    }
      
    /**
     * Writes a long word to the java screen.
     * 
     * @param addr where to write to
     * 
     * @param value the value
     */
    @Override
    public void writeLongToScreen(int addr,int value)
    {
        writeWordToScreen(addr,value>>>16);
        writeWordToScreen(addr+2,value);
    }

    /**
     * Sets the screen mode when in Ql compatible mode.
     * ATM only modes 4 and 8 are supported.
     * 
     * @param mode 0 for mode 4 ,8 for mode 8
     */
    @Override
    public void setMode(int mode)
    {
        if ((mode!= 0 && mode !=8 ) || mode==this.mode)
            return;
        this.mode=mode;
        switch (mode)
        {
            case 0:
            //    this.xsize*=2;
                this.nbrOfPixelsPerByte=4;                  // number of pixels per byte IN MODE 4;
                break;
            case 8:
             //   this.xsize/=2;
                this.nbrOfPixelsPerByte=2;                  // number of pixels per byte IN MODE 8
                break;
        }
    }
  
    /* -------------------------------------- Component painting ------------------------------------------*/
   
    /**
     * Clears the entire screen
     */
    @Override
    public void clearScreen()
    {
        int[]colArray=new int[this.xsize*this.ysize];
        this.raster.setDataElements(0,0, this.xsize, this.ysize, colArray);
        this.isDirty=true;
    }
    
    /**
     * Fills a block with colour - not implemented here.
     * 
     * @param cpu the Cpu used.
     * @param resolveStipple ignored.
     */
    @Override
    public void fillBlock (smsqmulator.cpu.MC68000Cpu cpu,boolean resolveStipple)
    {
        cpu.data_regs[0]=Types.ERR_NIMP;
        cpu.reg_sr&=~4;
    }
    
    /**
     * XORS a block with colour - not implemented here.
     * 
     * @param cpu the CPu used.
     * @param resolveStrip  ignored
     */
    @Override
    public void xorBlock (smsqmulator.cpu.MC68000Cpu cpu, boolean resolveStrip)
    {    
        cpu.data_regs[0]=Types.ERR_ORNG;
        cpu.reg_sr&=~4;
    }
  
    
    /**
     * This combines two blocks with alpha blending - not implemented here.
     * Not used here.
     * @param cpu the CPU used
     */
    @Override
    public  void combineBlocks(smsqmulator.cpu.MC68000Cpu cpu)
    {
        cpu.data_regs[0]=Types.ERR_NIMP;
        cpu.reg_sr&=~4;
    }
    
    
    /**
     * This is called when a block of memory was copied to the vram..
     * It must copy the data from the vram into the image.
     * 
     * @param cpu the CPU used.
     * @param copyFromScreen = true if the source is also the screen.
      */
    @Override
    public void moveBlock(smsqmulator.cpu.MC68000Cpu cpu,boolean copyFromScreen)
    {   
        cpu.data_regs[0]=Types.ERR_NIMP;
        cpu.reg_sr&=~4;
    }
    
    /**
     * Displays a region if bytes were loaded directly to the screen memory.
     * Used from CPU.
     * 
     * @param cpu the cpu used.
     * @param start where the bytes were loaded to.
     * @param nbrOfBytes how many bytes were loaded.
     */
    @Override
    public void displayRegion(smsqmulator.cpu.MC68000Cpu cpu,int start,int nbrOfBytes)
    {
        // first calculate the real start/stop adresses, making sure that we don't overstep the screen vram.
        if (this.stopAddress<start+nbrOfBytes)              // would be shoot over the top?
            nbrOfBytes-=start+nbrOfBytes-this.stopAddress;  // yes, don't
        if (start<this.startAddress)                        // would we start before the start address,?
        {
            nbrOfBytes-=(this.startAddress-start);  
            start=this.startAddress;                        // yes, don't
        }
        if (nbrOfBytes<1)                                   // now that **shouldn't** happen at all.
            return;
        // when bytes are loaded like this into screen mem, they don't necessarily make up a rectangular block.
        nbrOfBytes/=2;                                      // prepare for word sized access
        start-=this.startAddress;
        start&=0xfffffffe;                                  // make sure address is even=
        short []vramBuffer=cpu.getMemory();
        int incr=this.nbrOfPixelsPerByte;
        for (int i=0;i<nbrOfBytes;i+=incr)
        {
            paintPixels(start+i,vramBuffer);
        }
        this.isDirty=true;
    }
    
    /**
     * Sets a word full of pixels in the screen image at a certain address from the vrambfuffer, but DOES NOT repaint the screen afterwards.
     * 
     * @param addr where to take the screen data from.
     */
    private void paintPixels(int addr,short[]vramBuffer)
    {
        int index=addr/2;                                   // indax into the array
        short value =vramBuffer[index];                     // get the value from the screen mem
        int y=addr/this.nbrOfBytesPerLine;                  // the line (y coord) we're on
        int x=addr-(y*this.nbrOfBytesPerLine);              // the initial col (y coord) of the pixel
        x*=this.nbrOfPixelsPerByte;                         // these are the x & y coordinates of the pixel on the screen
        setPixels (x,y,value,this.mode);
    }
       
      
    /**
     * Sets the new Vrambase for the screen buffer if we are in QL compatible mode (this implies that the CPU or its memory changed).
     * Adjust vramtop accordingly.
     * If this is a Ql screen, sets the vram object 
     * 
     * @param vrambase the new video ram base.
     * 
     */
    @Override
    public synchronized void setVramBase(int vrambase)
    {
        if (!this.isQLScreen)
        {
            this.startAddress=vrambase;
            this.stopAddress=vrambase+getScreenSizeInBytes();
        }
    }
    
    /**
     * Sets whether brighter colours should be used.
     * This does nothing here.
     * 
     * @param b <code>true</code> if brighter colours should be used.
     * @param s ignored
     */
    @Override
    public void setVibrantColours(boolean b,short[] s)
    {      
    }
    
     /**
     * Gets size, in bytes, of the vram.
     * 
     * @return screen size, in number of bytes.
     */
    @Override
    public final int getScreenSizeInBytes()
    {
        return (this.xsize*this.ysize)/this.nbrOfPixelsPerByte;
    }
    
    /**
     * Checks whether this is a QL type screen
     * 
     * @return true if this is a QL screen (512*256 i mode 4, 256*256 in mode 8), else false
     */
    @Override
    public boolean isQLScreen()
    {
        return this.isQLScreen;
    }
}
