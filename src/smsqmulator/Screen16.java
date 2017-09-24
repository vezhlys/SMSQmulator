package smsqmulator;

/**
 * An 8-bit Aurora compatible screen for SMSQmulator.
 * 
 * It sets up video memory (vram) as an array of bytes.
 * @version  
 *  1.10    xorBlock correct handling when trailing or leading, writeByte doesn't exceed max buffer size at bottom right.
 *  1.09    fillBlock and xorBlock totally rewritten, they are called from TrapDispatcher, not the cpu,   readXXXXFromScreen and 
 *          alphaBlock removed, copyScreen moved to Screen object.
 *  1.08    copyScreen implemented.
 *  1.07    implement alphaBlock
 *  1.05    The vrambuffer is now within the main memory, no longer a separate buffer created here, all operations involving 
 *          writing to the screeen memory adjusted accordingly ; implement getScreenSizeInBytes here, setVibrantColours takes 
 *          vram parameter. moveBlock streamlined, fillBlock and xorBlock never return error ; isQLScreen() introduced.
 *  1.04    moveBlock implemented for all sources and destinations.
 *  1.03    writeLongword : make sure it doesn't overshoot.
 *  1.02    optimized (I hope!) fillBlock, implemented xorBlock.
 *  1.01    slightly brighter colours possibility added. moveBlock optimized. combineBlocks implemented (for wdw move with transparency). Part of screen initializing moved to super class.
 *  1.00    adapted from Screen32.
 * 
 * @author and copyright (c) Wolfgang Lenerz 2013-2017.
 * 
 */
public class Screen16 extends Screen
{
    private final byte[] vramBuffer;
    
    // now the colour lookup table (aurora 8 bit -> 32 bit RGB) (Qxlpalsprite, taken from MK's SMSQE code).
    private static final int[] clut= {                      
        0x000000,0x200020,0x002000,0x202020,0x000048,0x200068,0x002048,0x202068,
        0x480000,0x680020,0x482000,0x682020,0x480048,0x680068,0x482048,0x682068,
        0x004800,0x204820,0x006800,0x206820,0x004848,0x204868,0x006848,0x206868,
        0x484800,0x684820,0x486800,0x686820,0x484848,0x684868,0x486848,0x686868,
        0x000090,0x2000B0,0x002090,0x2020B0,0x0000D8,0x2000F8,0x0020D8,0x2020F8,
        0x480090,0x6800B0,0x482090,0x6820B0,0x4800D8,0x6800F8,0x4820D8,0x6820F8,
        0x004890,0x2048B0,0x006890,0x2068B0,0x0048D8,0x2048F8,0x0068D8,0x2068F8,
        0x484890,0x6848B0,0x486890,0x6868B0,0x4848D8,0x6848F8,0x4868D8,0x6868F8,
        0x900000,0xB00020,0x902000,0xB02020,0x900048,0xB00068,0x902048,0xB02068,
        0xD80000,0xF80020,0xD82000,0xF82020,0xD80048,0xF80068,0xD82048,0xF82068,
        0x904800,0xB04820,0x906800,0xB06820,0x904848,0xB04868,0x906848,0xB06868,
        0xD84800,0xF84820,0xD86800,0xF86820,0xD84848,0xF84868,0xD86848,0xF86868,
        0x900090,0xB000B0,0x902090,0xB020B0,0x9000D8,0xB000F8,0x9020D8,0xB020F8,
        0xD80090,0xF800B0,0xD82090,0xF820B0,0xD800D8,0xF800F8,0xD820D8,0xF820F8,
        0x904890,0xB048B0,0x906890,0xB068B0,0x9048D8,0xB048F8,0x9068D8,0xB068F8,
        0xD84890,0xF848B0,0xD86890,0xF868B0,0xD848D8,0xF848F8,0xD868D8,0xF868F8,
        0x009000,0x209020,0x00B000,0x20B020,0x009048,0x209068,0x00B048,0x20B068,
        0x489000,0x689020,0x48B000,0x68B020,0x489048,0x689068,0x48B048,0x68B068,
        0x00D800,0x20D820,0x00F800,0x20F820,0x00D848,0x20D868,0x00F848,0x20F868,
        0x48D800,0x68D820,0x48F800,0x68F820,0x48D848,0x68D868,0x48F848,0x68F868,
        0x009090,0x2090B0,0x00B090,0x20B0B0,0x0090D8,0x2090F8,0x00B0D8,0x20B0F8,
        0x489090,0x6890B0,0x48B090,0x68B0B0,0x4890D8,0x6890F8,0x48B0D8,0x68B0F8,
        0x00D890,0x20D8B0,0x00F890,0x20F8B0,0x00D8D8,0x20D8F8,0x00F8D8,0x20F8F8,
        0x48D890,0x68D8B0,0x48F890,0x68F8B0,0x48D8D8,0x68D8F8,0x48F8D8,0x68F8F8,
        0x909000,0xB09020,0x90B000,0xB0B020,0x909048,0xB09068,0x90B048,0xB0B068,
        0xD89000,0xF89020,0xD8B000,0xF8B020,0xD89048,0xF89068,0xD8B048,0xF8B068,
        0x90D800,0xB0D820,0x90F800,0xB0F820,0x90D848,0xB0D868,0x90F848,0xB0F868,
        0xD8D800,0xF8D820,0xD8F800,0xF8F820,0xD8D848,0xF8D868,0xD8F848,0xF8F868,
        0x909090,0xB090B0,0x90B090,0xB0B0B0,0x9090D8,0xB090F8,0x90B0D8,0xB0B0F8,
        0xD89090,0xF890B0,0xD8B090,0xF8B0B0,0xD890D8,0xF890F8,0xD8B0D8,0xF8B0F8,
        0x90D890,0xB0D8B0,0x90F890,0xB0F8B0,0x90D8D8,0xB0D8F8,0x90F8D8,0xB0F8F8,
        0xD8D890,0xF8D8B0,0xD8F890,0xF8F8B0,0xD8D8D8,0xF8D8F8,0xD8F8D8,0xF8F8F8};
    
    // 3*3 bit RGB to aurora colour table (taken from Marcel Kilgus's SMSQE code).
     private static final short []rgb2aur_tab={
        0x00,0x00,0x04,0x05,0x20,0x21,0x24,0x25,0x02,0x02,0x06,0x07,0x22,0x23,0x26,0x27,
        0x10,0x10,0x14,0x15,0x30,0x31,0x34,0x35,0x12,0x12,0x16,0x17,0x32,0x33,0x36,0x37,
        0x80,0x80,0x84,0x85,0xA0,0xA1,0xA4,0xA5,0x82,0x82,0x86,0x87,0xA2,0xA3,0xA6,0xA7,
        0x90,0x90,0x94,0x95,0xB0,0xB1,0xB4,0xB5,0x92,0x92,0x96,0x97,0xB2,0xB3,0xB6,0xB7,
        0x01,0x01,0x04,0x05,0x20,0x21,0x24,0x25,0x03,0x03,0x06,0x07,0x22,0x23,0x26,0x27,
        0x11,0x11,0x14,0x15,0x30,0x31,0x34,0x35,0x13,0x13,0x16,0x17,0x32,0x33,0x36,0x37,
        0x81,0x81,0x84,0x85,0xA0,0xA1,0xA4,0xA5,0x83,0x83,0x86,0x87,0xA2,0xA3,0xA6,0xA7,
        0x91,0x91,0x94,0x95,0xB0,0xB1,0xB4,0xB5,0x93,0x93,0x96,0x97,0xB2,0xB3,0xB6,0xB7,
        0x08,0x08,0x0C,0x05,0x28,0x21,0x2C,0x25,0x0A,0x0A,0x0E,0x07,0x2A,0x23,0x2E,0x27,
        0x18,0x18,0x1C,0x15,0x38,0x31,0x3C,0x35,0x1A,0x1A,0x1E,0x17,0x3A,0x33,0x3E,0x37,
        0x88,0x88,0x8C,0x85,0xA8,0xA1,0xAC,0xA5,0x8A,0x8A,0x8E,0x87,0xAA,0xA3,0xAE,0xA7,
        0x98,0x98,0x9C,0x95,0xB8,0xB1,0xBC,0xB5,0x9A,0x9A,0x9E,0x97,0xBA,0xB3,0xBE,0xB7,
        0x09,0x09,0x09,0x0D,0x28,0x29,0x2C,0x2D,0x0B,0x0B,0x0B,0x0F,0x2A,0x2B,0x2E,0x2F,
        0x19,0x19,0x19,0x1D,0x38,0x39,0x3C,0x3D,0x1B,0x1B,0x1B,0x1F,0x3A,0x3B,0x3E,0x3F,
        0x89,0x89,0x89,0x8D,0xA8,0xA9,0xAC,0xAD,0x8B,0x8B,0x8B,0x8F,0xAA,0xAB,0xAE,0xAF,
        0x99,0x99,0x99,0x9D,0xB8,0xB9,0xBC,0xBD,0x9B,0x9B,0x9B,0x9F,0xBA,0xBB,0xBE,0xBF,
        0x40,0x40,0x44,0x44,0x60,0x29,0x64,0x2D,0x42,0x42,0x46,0x46,0x62,0x2B,0x66,0x2F,
        0x50,0x50,0x54,0x54,0x70,0x39,0x74,0x3D,0x52,0x52,0x56,0x56,0x72,0x3B,0x76,0x3F,
        0xC0,0xC0,0xC4,0xC4,0xE0,0xA9,0xE4,0xAD,0xC2,0xC2,0xC6,0xC6,0xE2,0xAB,0xE6,0xAF,
        0xD0,0xD0,0xD4,0xD4,0xF0,0xB9,0xF4,0xBD,0xD2,0xD2,0xD6,0xD6,0xF2,0xBB,0xF6,0xBF,
        0x41,0x41,0x41,0x45,0x45,0x61,0x64,0x65,0x43,0x43,0x43,0x47,0x47,0x63,0x66,0x67,
        0x51,0x51,0x51,0x55,0x55,0x71,0x74,0x75,0x53,0x53,0x53,0x57,0x57,0x73,0x76,0x77,
        0xC1,0xC1,0xC1,0xC5,0xC5,0xE1,0xE4,0xE5,0xC3,0xC3,0xC3,0xC7,0xC7,0xE3,0xE6,0xE7,
        0xD1,0xD1,0xD1,0xD5,0xD5,0xF1,0xF4,0xF5,0xD3,0xD3,0xD3,0xD7,0xD7,0xF3,0xF6,0xF7,
        0x48,0x48,0x4C,0x4C,0x68,0x68,0x6C,0x65,0x4A,0x4A,0x4E,0x4E,0x6A,0x6A,0x6E,0x67,
        0x58,0x58,0x5C,0x5C,0x78,0x78,0x7C,0x75,0x5A,0x5A,0x5E,0x5E,0x7A,0x7A,0x7E,0x77,
        0xC8,0xC8,0xCC,0xCC,0xE8,0xE8,0xEC,0xE5,0xCA,0xCA,0xCE,0xCE,0xEA,0xEA,0xEE,0xE7,
        0xD8,0xD8,0xDC,0xDC,0xF8,0xF8,0xFC,0xF5,0xDA,0xDA,0xDE,0xDE,0xFA,0xFA,0xFE,0xF7,
        0x49,0x49,0x49,0x4D,0x4D,0x69,0x69,0x6D,0x4B,0x4B,0x4B,0x4F,0x4F,0x6B,0x6B,0x6F,
        0x59,0x59,0x59,0x5D,0x5D,0x79,0x79,0x7D,0x5B,0x5B,0x5B,0x5F,0x5F,0x7B,0x7B,0x7F,
        0xC9,0xC9,0xC9,0xCD,0xCD,0xE9,0xE9,0xED,0xCB,0xCB,0xCB,0xCF,0xCF,0xEB,0xEB,0xEF,
        0xD9,0xD9,0xD9,0xDD,0xDD,0xF9,0xF9,0xFD,0xDB,0xDB,0xDB,0xDF,0xDF,0xFB,0xFB,0xFF,
    };
    
 /**
     * This sets up the entire screen object, including the BufferedImage.
     * 
     * @param xsize the xsize of the screen, in pixels.
     * @param ysize the ysize of the screen, in pixels.
     * @param vramBase the start of the screen ram as the CPU sees it. For the CPU, this will generally be above the ROM.
     * @param vibrantColours if <code>true</code>, use full 8 bits per colour, else only the upper 5 (6 for green). 
     * @param monitor the monitor.
     * @param isMac true if screen is on a mac.
     */
    public Screen16(int xsize,int ysize,int vramBase,boolean vibrantColours,Monitor monitor,boolean isMac)
    {
        super(xsize,ysize,monitor,isMac);
        this.mode=16;
        this.divisior=2;
        this.vibrantColours=vibrantColours;
        if (this.vibrantColours)
        {
            makeBrightness(false,null);
        }
        this.stopAddress=xsize*ysize;                       // size of screen in bytes
        this.nbrOfBytesPerLine=xsize;                       // nbr of bytes per line
        this.startAddress=vramBase;
        this.vramBuffer=new byte[xsize*ysize];         // the vram buffer is an array of bytes
    }
    
    /**
     * Writes a byte to the "screen memory" and paints the corresponding pixel on the screen.
     * 
     * @param addr where to write to. This is the SMSQE address, where it thinks video memory lies.
     * @param value the value to write
     */
    @Override
    public void writeByteToScreen(int addr, int value,int wordValue)
    {
        addr-=this.startAddress;                                // where in my buffer the address lies : address is relative to start of screen
        if (addr>=this.totsize)
            return;
        value=Screen16.clut[value&0xff];
        if (this.dataBuffer.getElem(addr)!=value)
        {
            this.dataBuffer.setElem(addr, value);
            this.isDirty=true;
        }
    }
    
    /**
     * Writes a word to screen memory, and paints the corresponding pixel on the screen.
     * 
     * @param addr the address where to write to. 
     * It is presumed that it has been checked that this is in screen mem.
     * @param value the value to write to screen mem.
     */
    @Override
    public void writeWordToScreen(int addr, int value)
    {   
        writeByteToScreen(addr,value>>>8,0);
        addr++;
        writeByteToScreen(addr,value,0);
    }
      
    /**
     * Writes a long word to the java screen, and paints the corresponding pixel on the screen..
     * It is presumed that it has been checked that this is in screen mem.
     * 
     * @param addr where to write to
     * @param value the value
     */
    @Override
    public void writeLongToScreen(int addr,int value)
    {
        writeByteToScreen(addr++,value>>>24,0);
        writeByteToScreen(addr++,value>>>16,0);
        writeByteToScreen(addr++,value>>>8,0);
        writeByteToScreen(addr,value,0);
    }
      
    /**
     * Clears the entire screen, but not the vramBuffer.
     */
    @Override
    public void clearScreen()
    {
        int[]colArray=new int[this.xsize*this.ysize];
        this.raster.setDataElements(0,0, this.xsize, this.ysize, colArray);
        this.isDirty=true;
    }
    
    /**
     * Write a byte at a certain address in the vramBuffer.
     * 
     * @param val  the byte to write
     * @param address where to write it.
     * @param vram the vramBuffer.
     */
    private static void writeByte(final byte val, final int address,short[] vram)
    {
        int addr=address/2;                                     // vram is an arrzy of shorts, index into it
        short res=(vram[addr]);                                 // word at this addres
        int value=val;
        if((address&1)==1)
        {
            res&=0xff00;
            value&=0xff;
        }
        else
        {
            res&=0x00ff;
            value=(value<<8)&0xff00;
        }
        vram[addr]=(short)(res|value);
    }
    
    /**
     * Read a byte from an address in the vramBuffer.
     * 
     * @param address where to read from
     * @param vram the video ram.
     * 
     * @return the byte read, ANDed off.
     */
    private static int readByte(final int address,final short[]vram)
    {
         if ((address & 1) == 1)
            return vram[address/2]&0xff;
        else
            return (vram[address/2]>>>8)&0xff;
    }
    /**
     * Makes two colour long words from the colours passed as parameter.
     * 
     * @param colours colours[0] = stipple nbr, colours [1] = d7 = stipple | main colour.
     * 
     * On return colours[0] = d6, colours[1] = D7 (even/odd rows).
     */
    private void getColoursFromStipple(int[]colours)
    {
        int D6=colours[0]&0x3;              // colours[0] = d6 ^stipple
        int D7 = colours[1];                // d7 = stipple | main
        int mainC=D7&0xff;                // main col in lower word of d7
        int stippleC=(D7>>>8)&0xff;               // stipple colour in high word of d7
        colours[0]=D7;                      // d6 = d7 means that this now already set up for solid colour & vertical stripes
        switch (D6)
        {
            case 0:                                             // 1 of four - high byte of d6 into low byte of d7
                colours[1]&=0xff00;
                colours[1]|=stippleC;
                break;
            case 1:                                             // horizontal stripes
                colours[0]=(stippleC<<8)| stippleC;  
                colours[1]=(mainC<<8)| mainC;   
                break;
                
            case 3:                                             // checkerboard
                colours[0]=(mainC<<8)| stippleC;               // swap d6
                break;
        }
        D7 = (colours[0]&0xffff)<<16;
        colours[0]&=0xffff;
        colours[0]|=D7;
        D7 = (colours[1]&0xffff)<<16;
        colours[1]&=0xffff;
        colours[1]|=D7;
    }
    
    /**
     * Fills a block with colour : this updates the "screen memory" AND the underlying image.
     * 
     * @param cpu the CPu used.    
     * @param resolveStipple    true if we need to convert a stipple number in d6 into colours in d6/d7
     * 
       The block size and origin are assumed to have been adjusted for
       correct position within the area to be set.

       Registers:
               Entry                           Exit
       D0                                      smashed
       D1      block size                      smashed
       D2      block origin                    smashed
       D3-D5                                   smashed
       D6      stipple Nbr (cn_block) OR long word colour for even row (bm_block)
       D7      colour mask in long word (cn_block) 
               or long word colour for odd row (bm_block)   smashed
       A1      area base address               smashed
       A2      area row increment              smashed

     */
    @Override
    public void fillBlock (smsqmulator.cpu.MC68000Cpu cpu,boolean resolveStipple)
    {
        if (cpu.data_regs[1]==0)
            return;
        int xo=cpu.data_regs[2]>>>16;                           // x origin of the block.
        int yo=cpu.data_regs[2]&0xffff;                         // y origin of the block.
        int xs=cpu.data_regs[1]>>>16;                           // x size of the block IN WORDS
        int ys=cpu.data_regs[1]&0xffff;                         // y size of the block
        if (xo+xs>this.xsize)
        {
            xs=this.xsize-xo-1;
            if (xs<1)
            {
                return;
            }
        }
        if (yo+ys>this.ysize)
        {
            ys=this.ysize-yo-1;
            if (ys<1)
            {
                return;
            }
        }
        short[] mainMemory=cpu.getMemory();
        boolean toScreen=(cpu.addr_regs[1]>=this.startAddress && cpu.addr_regs[1]<= this.stopAddress) ;     // is block in main memory (generally window save area)?

        int[]colours=new int [2];
        colours[0]=cpu.data_regs[6];                        //0
        colours[1]=cpu.data_regs[7];                        // get colours from these regs 00f8 0000
        if (resolveStipple)                                 
        {
            getColoursFromStipple(colours);                 // make colours if d6 was stipple number
        }
        boolean leading=false,trailing=false;
        if ((xo & 1)!=0)
        {
            leading=true;
            xs--;
        }
        if ((xs &1 )!=0)
        {
            trailing=true;
            xs--;
        }
        int cols=xs/2;                                      // nbr of double pixels (i.e. words)
        boolean even = ((yo & 1)==0);                       // are we starting with an even or odd row?
 
        short col1H=(short)((colours[0]>>>8)&0xff);
        int colour1H=this.clut[col1H&0xff];      
        short col1L=(short)(colours[0]&0xff);
        int colour1L=this.clut[col1L&0xff];     
        short col1F=(short)(colours[0]&0xffff);

        short col2H=(short)((colours[1]>>>8)&0xff);
        int colour2H=this.clut[col2H&0xff];      
        short col2L=(short)(colours[1]&0xff);
        int colour2L=this.clut[col2L&0xff];
        short col2F=(short)(colours[1]&0xffff);

        int lineIncrease=cpu.addr_regs[2] ;                     // how much one row is, in bytes
        int line=yo*lineIncrease +xo;                           // index into memeory array for first pixel
        line+=cpu.addr_regs[1];                                 // 
        line/=2;                                                // mainMemory is an array of shorts
        lineIncrease/=2;                                        // increase 
        yo*=this.xsize;                                         // index into screen buffer for first pixel of this line

        int temp;
        for (int iy=0;iy<ys;iy++)                               // do it for every line            
        {
            int xxo = xo+yo;                                    // start index into screen buffer array
            int ix=line;                                        // start index in main memory array
            if (leading)                                        // I have a leading byte, this is the lower byte of a word in main memory
            {      
               if (even)                                        // treat an even line
                {
                    temp=mainMemory[ix]&0xff00;                 // get current colour word & eliminate existing lower byte
                    temp|=(col1L&0xff);                         // fill in my colour byte
                    mainMemory[ix++]=(short)(temp&0xffff);      // set it in main memory
                    if (toScreen)
                        this.dataBuffer.setElem(xxo++, colour1L);// set colour in screen buffer
                }
                else                                                // same for odd lines
                {
                    temp=mainMemory[ix]&0xff00;
                    temp|=(col2L&0xff);
                    mainMemory[ix++]=(short)(temp&0xffff);
                    if (toScreen)
                        this.dataBuffer.setElem(xxo++, colour2L);
                }  
            }  
            for (int i=0;i<cols;i++)                        // now treat all full words, i.e. 2 pixels at a time
            {
                if (even)
                {
                    mainMemory[ix++]=col1F;                 // set the colour word, for 2 pixels
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo++, colour1H);    
                        this.dataBuffer.setElem(xxo++, colour1L);
                    }
                }
                else
                {
                    mainMemory[ix++]=col2F;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo++, colour2H);   
                        this.dataBuffer.setElem(xxo++, colour2L);
                    }
                }
            }
            if (trailing)
            {  
               if (even)
                {
                    temp=mainMemory[ix]&0xff;                   //  current two colours
                    temp|=(col1H<<8)&0xff00;                    // replace higher byte with mine
                    mainMemory[ix++]=(short)(temp&0xffff);
                    if (toScreen)
                        this.dataBuffer.setElem(xxo++, colour1H);
                }
                else
                {
                    temp=mainMemory[ix]&0xff;                   //  current two colours
                    temp|=(col2H<<8)&0xff00;                    // replace higher byte with mine
                    mainMemory[ix++]=(short)(temp&0xffff);
                    if (toScreen)
                        this.dataBuffer.setElem(xxo++, colour2H);
                }
            }  
            line+=lineIncrease;
            yo+=this.xsize;
            even=!even;
        }
        this.isDirty=true;
    }
  
    /**
     * XORs a block with colour : this updates the "screen memory" (vrambuffer) AND the underlying image. 
     * 
     * @param cpu the CPu used.
     *  xo x origin of the block
     *  yo y origin of the block
     *  xs x size of the block
     *  ys y size of the block
     *  stipple  how we stipple the colour (0xffff -  solid colour, 0= 1 of 4 , 2 =vertical, 3 horiz., 3 checkerboard
     *  colour1x the stipple colour (in SMSQE format, mode 16).
     *  colour2x the main colour (in SMSQE format, mode 16).
     * @param resolveStipple    true if we need to convert a stipple number in d6 into colours in d6/d7
     */
    @Override
    public void xorBlock (smsqmulator.cpu.MC68000Cpu cpu, boolean resolveStipple)
    {
        int xo=cpu.data_regs[2]>>>16;                           // x origin of the block.
        int yo=cpu.data_regs[2]&0xffff;                         // y origin of the block.
        int xs=cpu.data_regs[1]>>>16;                           // x size of the block IN WORDS
        int ys=cpu.data_regs[1]&0xffff;                         // y size of the block
        if (cpu.data_regs[1]==0)
            return;
        if (xo+xs>this.xsize)
        {
            xs=this.xsize-xo-1;
            if (xs<1)
            {
                return;
            }
        }
        if (yo+ys>this.ysize)
        {
            ys=this.ysize-yo-1;
            if (ys<1)
            {
                return;
            }
        }
        short[] mainMemory=cpu.getMemory();
        boolean toScreen=!(cpu.addr_regs[1]<this.startAddress || cpu.addr_regs[1]>= this.stopAddress) ;     // is block in main memory (generally window save area)?

        int[]colours=new int [2];
        colours[0]=cpu.data_regs[6];                        //0
        colours[1]=cpu.data_regs[7];                        // get colours from these regs 00f8 0000
        if (resolveStipple)                                 
        {
            getColoursFromStipple(colours);                 // make colours if d6 was stipple number
        }
        boolean leading=false,trailing=false;
        if ((xo & 1)!=0)
        {
            leading=true;
            xs--;
        }
        if ((xs &1 )!=0)
        {
            trailing=true;
            xs--;
        }
        int cols=xs/2;                                      // nbr of double pixels (i.e. words)
        boolean even = ((yo & 1)==0);                       // are we treating even or odd row?
 
        short col1H=(short)((colours[0]>>>8)&0xff);
        int colour1H=this.clut[col1H&0xff];      
        short col1L=(short)(colours[0]&0xff);
        int colour1L=this.clut[col1L&0xff];     
        int colour1F=(colours[0]&0xffff);

        short col2H=(short)((colours[1]>>>8)&0xff);
        int colour2H=this.clut[col2H&0xff];      
        short col2L=(short)(colours[1]&0xff);
        int colour2L=this.clut[col2L&0xff];
        int colour2F=(colours[1]&0xffff);

        int lineIncrease=cpu.addr_regs[2] ;                     // how much one row is, in bytes
        int line=yo*lineIncrease +xo;                           // index into memeory array for first pixel
        line+=cpu.addr_regs[1];                                 // 
        line/=2;                                                // mainMemory is an array of shorts
        lineIncrease/=2;                                        // increase 
        yo*=this.xsize;                                         // index into screen buffer for first pixel of this line

        int temp,old;
        for (int iy=0;iy<ys;iy++)                               // do it for every line            
        {
            int xxo = xo+yo;                                    // start index into screen buffer array
            int ix=line;                                        // start index in main memory array
            if (leading)                                        // I have a leading byte, this is the lower byte of a word in main memory
            {      
               if (even)                                        // treat an even line
                {
                    old=mainMemory[ix]&0xff;
                    old^=(col1L&0xff);
                    temp=mainMemory[ix]&0xff00;                 // get current colour word & eliminate existing lower byte
                    temp|=(old&0xff);                           // fill in my colour byte
                    mainMemory[ix++]=(short)(temp&0xffff);      // set it in main memory
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour1L);// set colour in screen buffer
                    }
                }
                else                                                // same for odd lines
                {
                    old=mainMemory[ix]&0xff;
                    old^=(col2L&0xff);
                    temp=mainMemory[ix]&0xff00;                 // get current colour word & eliminate existing lower byte
                    temp|=(old&0xff);                           // fill in my colour byte
                    mainMemory[ix++]=(short)(temp&0xffff);      // set it in main memory
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour2L);// set colour in screen buffer
                    }
                }  
            }  
            for (int i=0;i<cols;i++)                            // now treat all full words, i.e. 2 pixels at a time
            {
                if (even)
                {
                    temp=mainMemory[ix]^(colour1F);
                    mainMemory[ix++]=(short)temp;               // set the colour word, for 2 pixels
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour1H);
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour1L);
                    }
                }
                else
                {
                    temp=mainMemory[ix]^(colour2F);
                    mainMemory[ix++]=(short)temp;               // set the colour word, for 2 pixels
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour2H);
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour2L);
                    }
                }
            }
            if (trailing)
            {  
               if (even)
                {
                    old=mainMemory[ix]&0xff00;
                    old^=((col1H<<8)&0xff00);
                    temp=mainMemory[ix]&0xff;                   // get current colour word & eliminate existing lower byte
                    temp|=old;                                  // fill in my colour byte
                    mainMemory[ix++]=(short)(temp&0xffff);      // set it in main memory
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour1H);// set colour in screen buffer
                    }
                }
                else
                {
                    old=mainMemory[ix]&0xff00;
                    old^=((col2H<<8)&0xff00);
                    temp=mainMemory[ix]&0xff;                   // get current colour word & eliminate existing lower byte
                    temp|=old;                                  // fill in my colour byte
                    mainMemory[ix++]=(short)(temp);             // set it in main memory
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo, this.dataBuffer.getElem(xxo++)^colour2H);// set colour in screen buffer
                    }
                }
            }  
            line+=lineIncrease;
            yo+=this.xsize;
            even=!even;
        }
        this.isDirty=true;
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
        if (copyFromScreen)
        {
            int xs = (cpu.data_regs[1]>>>16)&0xffff;
            int ys=cpu.data_regs[1]&0xffff;
            int srcXo=(cpu.data_regs[2]>>>16)&0xffff;
            int srcYo=cpu.data_regs[2]&0xffff;
            int destXo= (cpu.data_regs[3]>>>16) &0xffff;
            int destYo=cpu.data_regs[3]&0xffff ;
            int []colArray=new int[xs*ys];
            this.raster.getDataElements(srcXo,srcYo, xs, ys, colArray);
            this.raster.setDataElements(destXo,destYo, xs, ys, colArray);
            this.isDirty=true;
        }
        else
            paintBlock(cpu.data_regs[1],cpu.data_regs[3],cpu.getMemory());
    }
    
    /**
     * This combines two blocks (source 1, source2) with alpha blending and puts the result into the screen.
     * 
     * If destination is not the screen and source  1 and 2 are not in main memory, this returns to SMSQE with an error,
     * else this will alpha blend source1 and source2 and put the result into the "vram" array.
     * Once this is done, the screen is painted with the new data.
     * 
     * @param cpu the CPU used.
     */
    @Override
    public  void combineBlocks(smsqmulator.cpu.MC68000Cpu cpu)
    { 
        int srcStart=cpu.addr_regs[4];
        int src2Start=cpu.addr_regs[1];
        int destStart=cpu.addr_regs[5];
        if ((destStart<this.startAddress) ||(srcStart>=this.startAddress)|| src2Start>=this.startAddress)
        {
            cpu.data_regs[0]=-14;
            return;
        }
        
        int p=cpu.data_regs[6]&0xff;
        double weight =(double)((double)p/(double)255);
        
        int xs = (cpu.data_regs[1]>>>16)&0xffff;
        int ys=cpu.data_regs[1]&0xffff;
        int srcXo=(cpu.data_regs[2]>>>16)&0xffff;
        int srcYo=cpu.data_regs[2]&0xffff;
        int destXo= (cpu.data_regs[3]>>>16) &0xffff;
        int destYo=cpu.data_regs[3]&0xffff;
        
        // caculate source 1 address
        
        int srcInc=cpu.addr_regs[2];
        srcStart+=srcYo*srcInc + srcXo;
        
        // destination address, must be in screen
        int destInc=cpu.addr_regs[3];
        destStart=destYo*destInc+destXo+this.startAddress;               // destination is in vram buffer
        short[] vramBuffer=cpu.getMemory();
        src2Start+= (destYo*(destInc+4))+destXo;
        int src2Inc=cpu.data_regs[7];
        
        int src2Temp,srcTemp;
        
        int rs,rd,gs,gd,bs,bd,src2,src1;
        byte res;
        
        for (int iY=0;iY<ys;iY++)
        {
            srcTemp=srcStart;
            src2Temp=src2Start;
            for (int iX=0;iX<xs;iX++)
            {   
                src1=cpu.readMemoryByte(srcTemp++);         // source1 colour
                src2=cpu.readMemoryByte(src2Temp++);        // source2 colour
                src1=Screen16.clut [src1];                      // convert to rgb           
                src2=Screen16.clut [src2];
  
                // split colours into RGB components
                rs=src1>>>16;                               // source 1 R
                gs=(src1>>>8)&0xff;
                bs=src1&0xff;
                rd=src2>>>16;                               // source 2 R
                gd=(src2>>>8)&0xff;
                bd=src2&0xff;
             
                rd= (int) (rs*weight+rd*(1.0-weight));      // typical blending op
                gd= (int) (gs*weight+gd*(1.0-weight));
                bd= (int) (bs*weight+bd*(1.0-weight));
              
                // rgb to mode16  : just take the top 3 bits of each colour, and combine into 3 * 3 bit colour
                rd=(rd&0xe0)<<1;                            // 111 000 000
                gd=(gd>>>2)&0x38;
                bd=(bd>>>5)&0x7;                        
                rd|=gd;                                     // recombine
                rd|=bd;
                res=(byte)(Screen16.rgb2aur_tab[rd]&0x1ff); // use Marcel's table with that colour
                writeByte(res,destStart+iX,vramBuffer);
              //  vramBuffer[destStart+iX]=res;
            }
            srcStart+=srcInc;                               // src1 down to next source line 
            destStart+=destInc;                             // same for destination line
            src2Start+=src2Inc;                             // and source2
        }
        paintBlock(cpu.data_regs[1],cpu.data_regs[3],vramBuffer);      // now paint this in the BufferedImage
        cpu.data_regs[0]=0;
    }
    
    /**
     * This takes data from the vramBuffer to paint the corresponding pixels in the BufferedImage.
     * 
     * @param ys x and y sizes (x|y in long word) of block.
     * @param yo same for origin of block.
     */
    private void paintBlock(int ys,int yo,short[]vramBuffer)
    {
        int xs=ys>>>16;
        ys&=0xffff;
        int xo=yo>>>16;
        yo&=0xffff;                                         // now we've got the correct x,y, origins and sizes as java ints
        int start=yo*this.xsize+xo+this.startAddress;                         // index into vramBuffer of first pixel to paint
        int iX;
        for (int iY=0;iY<ys;iY++,start+=this.xsize)
        {
            for (iX=start;iX<xs+start;iX++)
            {
               int col=readByte(iX,vramBuffer)&0xff;
               this.dataBuffer.setElem(iX-this.startAddress, Screen16.clut[col]); 
            }
        }
        this.isDirty=true;
    }
    
    /**
     * Displays a region if bytes were loaded directly to the screen memory.
     * Used from CPU.
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
       
        short[]vramBuffer=cpu.getMemory();
        int value;
        for (int i=start;i<nbrOfBytes+start;i++)
        {
            value=readByte(i,vramBuffer);
            this.dataBuffer.setElem(i-this.startAddress, Screen16.clut[value]);
        }
        this.isDirty=true;
    }
    
    /**
     * Sets the new Vrambase and adjust vramtop and vrambase accordingly.
     * 
     * @param vrambase the new vrambase.
     * 
     */
    @Override
    public synchronized void setVramBase(int vrambase)
    {
        this.startAddress=vrambase;
        this.stopAddress=vrambase+getScreenSizeInBytes();
    }
    
    /**
     * Sets whether brighter colours should be used.
     * 
     * @param b <code>true</code> if brighter colours should be used.
     * @param vramBuffer the video ram buffer.
     */
    @Override
    public void setVibrantColours(boolean b,short[]vramBuffer)
    {
        this.vibrantColours=b;
        makeBrightness (true,vramBuffer);
    }
    
    /**
     * Makes "brighter" colours, or not depending on the vibrantColours flag.
     */
    private void makeBrightness(boolean redraw,short[]vramBuffer)
    {
        if (this.vibrantColours)
        {
            int add=0x070307;
            int col;
            for (int i=0;i<Screen16.clut.length;i++)
            {
                col=Screen16.clut[i]|add;
                Screen16.clut[i]=col;
            }
        }
        else
        {
            int add=0xf8fcf8;
            int col;
            for (int i=0;i<Screen16.clut.length;i++)
            {
                col=Screen16.clut[i]&add;
                Screen16.clut[i]=col;
            }
        }
        if (redraw)
        {
            for (int iX=0;iX<this.xsize*this.ysize;iX++)
            {
                int col=readByte(iX+this.startAddress,vramBuffer);
                this.dataBuffer.setElem(iX, Screen16.clut[col]); // strangely enough, profiling shows these two lines to be faster than the single line below:
            //   this.dataBuffer.setElem(iX, Screen16.clut[vramBuffer[iX]&0xffff]);
            }
        }
    }
     
     /**
     * Gets size, in bytes, of the video ram.
     * 
     * @return screen size, in number of bytes.
     */
    @Override
    public final int getScreenSizeInBytes()
    {
        return (this.xsize*this.ysize);
    }
   
    
    /**
     * Checks whether this is a QL type screen
     * 
     * @return true if this is a QL screen (512*256 i mode 4, 256*256 in mode 8), else false
     */
    @Override
    public boolean isQLScreen()
    {
        return false;
    }
}


/**
     * Blend two colours with alpha blending.
     * 
     * @param alphacol colour 1, which is to be blended with the existing colour.
     * @param col colour 2, the existing colour
     * @param weight how much of colour 1 is to show in colour2 (0= nothing 255, colour1 becoms totally opaque).
     * 
     * @return the combined colour.
     *
    private static byte alphaPBlend(int alphacol, int col, double weight)
    {
        int src1=Screen16.clut [alphacol];                      // convert to rgb           
        int src2=Screen16.clut [col&0xff];
        int rs,rd,rg,gs,gd,bs, bd;
        
        // split colours into RGB components
        rs=src1>>>16;                                           // source 1 R
        gs=(src1>>>8)&0xff;
        bs=src1&0xff;
        rd=src2>>>16;                                           // source 2 R
        gd=(src2>>>8)&0xff;
        bd=src2&0xff;

        rd= (int) (rs*weight+rd*(1.0-weight));                  // typical blending op
        gd= (int) (gs*weight+gd*(1.0-weight));
        bd= (int) (bs*weight+bd*(1.0-weight));

        // rgb to mode16  : just take the top 3 bits of each colour, and combine into 3 * 3 bit colour
        rd=(rd&0xe0)<<1;                                        // 111 000 000
        gd=(gd>>>2)&0x38;
        bd=(bd>>>5)&0x7;                        
        rd|=gd;                                                 // recombine
        rd|=bd;
        return (byte) (Screen16.rgb2aur_tab[rd&0x1ff]);         // use Marcel's table with that colour
    }
    */