package smsqmulator;

/**
 * This is the screen object for a "mode 32" screen, where each pixels is a word in GGGBBBBB RRRRRGGG format.
 *
 * @author and copyright (c) 2012 -2016 Wolfgang Lenerz
 * @version 
 *  1.14 fillBlock and xorBlock totally rewritten, they are called from TrapDispatcher,not the cpu ; writeWord and writeLonbg 
 *       slightly improved ; readXXXXFromScreen and alphaBlock removed ; copyScreen moved to Screen object ; cleaned up makeClutColours.
 *  1.13 copyScreen implemented, fillBlock and xorBlock revamped.
 *  1.12 implement alphaBlock.
 *  1.11 The vrambuffer is now within the main memory, no longer a separate buffer created here, all operations involving 
 *       writing to the screeen memory adjusted accordingly ; implement getScreenSizeInBytes here, setVibrantColours takes 
 *       vram parameter. moveBlock streamlined, fillBlock and xorBlock never return error ; isQLScreen() introduced.
 *  1.10 copyMem32 streamlined.
 *  1.09 long word sized write: make sure it doesn't overshoot edge of screen.
 *  1.08 byte-sized reading/writing from/to screen is now allowed, optimized (I hope!) fillBlock, implemented xorBlock.
 *  1.07 moveBlock optimized. combineBlocks implemented (for wdw move with transparency). Part of screen initializing moved to super class.
 *  1.06 slightly brighter colours possibility added.
 *  1.05 first split version.
 * (note version up to 1.04 was for combined screens, see the Screen object)
 */
public class Screen32 extends Screen
{
    private static final int nbrOfBytesPerPixel=2;              // number of bytes per pixel in higher colour modes
    private final int [] clut=new int[65536];                   // colour look up table
    private int QLmode;                                         // QL screen colour if copy QL screen
    /**
     * Creates the object.
     * 
     * @param xsize the xsize of the screen in pixels.
     * @param ysize the ysize of the screen in pixels.
     * @param vrambase the start of the "videoram" as far as the CPU is concerned.
     * @param vibrantColours if true brighter colours are used (ie. the lover 3 bits of each R, G, B byte are set to 1).
     * @param monitor the monitor.
     * @param isMac true if screen is on a mac.
     */
    public Screen32 (int xsize,int ysize,int vrambase,boolean vibrantColours,Monitor monitor,boolean isMac)
    {
        super(xsize,ysize,monitor,isMac);
        this.mode=32;
        int p=xsize%8;
        if (p!=0)
            xsize=((xsize/8)+1)*8;                          // in this mode,the screen size always a multiple of 8 pixels wide.
        this.stopAddress=xsize*ysize*nbrOfBytesPerPixel;    // size of screen in bytes
        this.nbrOfBytesPerLine=xsize*Screen32.nbrOfBytesPerPixel;// nbr of bytes per line
        this.startAddress=vrambase;
        this.stopAddress+=this.startAddress-2;                // where screen stops
        this.vibrantColours=vibrantColours;
        makeClutColours(vibrantColours);
    } 
   
     /**
     * Writes a byte to the "screen memory" and paints the corresponding pixel on the screen.
     * 
     * Not implemented for this type of screen!
     * 
     * @param address where to write to. This is the SMSQE address, where it thinks video memory lies.
     * @param value the value to write.
     * @param val2 ignored
     */
    @Override
    public void writeByteToScreen(int address, int value,int val2)
    {
    }
    
    /**
     * Paints the pixel on the screen by setting the RGB value in the image dataBuffer.
     * One word = 1 pixel.
     * 
     * @param addr the address where to write to. It is presumed that it has been checked that this is in screen mem.
     * @param value the value to write to screen mem, a word in GGGBBBBB RRRRRGGG format.
     */
    @Override
    public void writeWordToScreen(int addr, final int value)
    {
        addr-=this.startAddress;                            // where in my buffer the address lies : address is relative to start of screen
        int y=addr/this.nbrOfBytesPerLine;                  // the line (y coord) we're on
        int x=addr-(y*this.nbrOfBytesPerLine);              // the initial column (x coord) of the pixel
        x/=Screen32.nbrOfBytesPerPixel;
        x+=y*this.xsize;
        int index=this.clut[value&0xffff];                  // RGB colour corresponding to the 
        if (this.dataBuffer.getElem(x)!=index)
        {
            this.dataBuffer.setElem(x, index);
            this.isDirty=true;
        }
    }
      
    /**
     * Paints two pixels on the screen by setting the RGB values in the image dataBuffer.
     * One word = 1 pixel.
     * 
     * @param addr where to write to.
     * @param value the value for 2 pixels (high word|low word, both in GGGBBBBB RRRRRGGG format).
     */
    @Override
    public void writeLongToScreen(int addr,int value)
    {
        if (addr>this.stopAddress-3)
            return;
        int value1=value>>>16;                              // separate the two colours.
        value&=0xffff;
        addr-=this.startAddress;                            // where in my buffer the address lies : address is relative to start of screen
        int y=addr/this.nbrOfBytesPerLine;                  // the line (y coord) we're on
        int x=addr-(y*this.nbrOfBytesPerLine);              // the initial column (x coord) of the pixel
        x/=Screen32.nbrOfBytesPerPixel;
        x+=y*this.xsize;
        int colour=this.clut[value1];
        if (this.dataBuffer.getElem(x)!=colour)
        {
            this.dataBuffer.setElem(x, colour);
            this.isDirty=true;
        } 
        colour=this.clut[value];
        x++;
        if (this.dataBuffer.getElem(x)!=colour)
        {
            this.dataBuffer.setElem(x, colour);
            this.isDirty=true;
        }
    } 
   
          
    /**
     * Clears the entire screen, on screen as well as in VramBuffer
     */
    @Override
    public void clearScreen()
    {
        int[]colArray=new int[this.xsize*this.ysize];
        this.raster.setDataElements(0,0, this.xsize, this.ysize, colArray);
        this.isDirty=true;
    }
    
    /**
     * Makes two colour long words from the stipple/colour passed as parameter.
     * 
     * @param colours colours[0] = stipple nbr, colours [1] = d7 = stipple | main colour.
     * 
     * On return colours[0] = d6, colours[1] = D7 (colours for even/odd rows).
     */
    private void getColoursFromStipple(int[]colours)
    {
        int D6= colours[0]&0x3;                                 // colours[0] = d6
        int D7= colours[1];                                     // d7 = stipple | main
        int mainC=D7&0xffff;                                    // main col in lower word of d7
        int stippleC=D7>>>16;                                   // stipple colour in high word of d7
        colours[0]=D7;                                          // d6 = d7 means that this now already set up for solid colour & vertical stripes
        switch (D6)
        {
            case 0:                                             // 1 of four - high word of d6 into low word of d7
                colours[1]&=0xffff0000;
                colours[1]|=stippleC;
                break;
            case 1:                                             // horizontal stripes
                colours[0]=(stippleC<<16)| stippleC;  
                colours[1]=(mainC<<16)| mainC;
            case 3:                                             // checkerboard
                colours[0]=(mainC<<16)| stippleC;               // swap d6
                break;
        }
    }
  
    
    /**
     * Fills a block with colour : this updates the memory AND the underlying image data (if block i =s actually within screen).
     * Sometimes, the block is only within the main memory.
     * The main memory is an array of shorts, the image databuffer is an array of ints (ARGB).
     * 
     * @param cpu the CPU used.    
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
     * stipples are such that they affect a block of 2*2 pixels   even row : p1p2
     *                                                            odd  row : p3p4   
     * where each of the 4 pixels may be one of two colours.
     * Hence the colours in D6 and D7 are the colours for two consecutive pixels :p1 and p2 in D6, p3 and p4 in D7.
     * This is why there may be a leading and a trailing colour. And this is why there are two different colours 
     * for even and odd rows.
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
        int address = cpu.addr_regs[1];                         // base address of block
        boolean toScreen= (address>=this.startAddress && address<= this.stopAddress) ; // is block in screen memory ?

        int[]colours=new int [2];
        colours[0]=cpu.data_regs[6];                            // stipple or even row colour
        colours[1]=cpu.data_regs[7];                            // if D6 is a stipple nbr, make colours from these regs 
        if (resolveStipple)                             
            getColoursFromStipple(colours);                     // make colours if d6 was stipple number
        
        boolean leading=false,trailing=false;
        if ((xo & 1)!=0)                                        // xo is odd, so we might have leading stipple pixel
        {
            leading=true;   
            xs--;
        }
        if ((xs &1 )!=0)                                        // xsize is odd, we might have training stipple pixel
        {
            trailing=true;
            xs--;
        }
        
        int cols=xs/2;                                          // nbr of double pixels (i.e. long words)
        boolean even = (yo & 1)==0;                             // is start row odd or even?
        
        short col1H=(short)(colours[0]>>>16);                   // high word of colour for memory - short
        int colour1H=this.clut[col1H&0xffff];                   // high word of colour in iamge databuffer - int
        short col1L=(short)(colours[0]&0xffff);                 // same for low word
        int colour1L=this.clut[col1L&0xffff];                   // this is for EVEN rows
        short col2H=(short)(colours[1]>>>16);                   // same for odd rows
        int colour2H=this.clut[col2H&0xffff];              
        short col2L=(short)(colours[1]&0xffff);
        int colour2L=this.clut[col2L&0xffff];      

        int rowInc=cpu.addr_regs[2];                            // row increment in memory
        address+=yo*rowInc;                                     // start of row
        address+=xo*2;                                          // start pixel - each pixel takes 2 bytes
                                                                // address now is the address of the start of the block
        address/=2;                                             // index into memory array
        rowInc/=2;                                              // row incement in words, not bytes
        short[] mainMemory=cpu.getMemory();
        
        yo*=this.xsize;                                         // index into image databuffer for first pixel of this line
        yo+=xo;
        
        // as of here : address = index into memory array ; yo = ndex into image databuffer array       
       
        xs=(cpu.data_regs[1]>>>16);                             // original x size
        rowInc-=xs;                                             // deduct original xs size from row inc, after each loop
        int bufRowInc=this.xsize-xs;                            // iteration, we've already added xs to start address
        for (int iy=0;iy<ys;iy++)                               // do for all rows
        {
            if (leading)                                        // do we have a leading word?
            {      
                if (even)
                {
                    mainMemory[address++]=col1L;
                    if (toScreen)
                        this.dataBuffer.setElem(yo++, colour1L);
                }
                else
                {
                    mainMemory[address++]=col2L;
                    if (toScreen)
                        this.dataBuffer.setElem(yo++, colour2L);
                }

            }  
            for (int i=0;i<cols;i++)                            // do it for each double pixel in row
            {
                if (even)
                {
                    mainMemory[address++]=col1H;
                    mainMemory[address++]=col1L;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(yo++, colour1H); 
                        this.dataBuffer.setElem(yo++, colour1L);
                    }
                }
                else
                {
                    mainMemory[address++]=col2H;
                    mainMemory[address++]=col2L;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(yo++, colour2H);    
                        this.dataBuffer.setElem(yo++, colour2L);
                    }
                }
            }
            if (trailing)
            {     
                if (even)
                {
                    mainMemory[address++]=col1H;
                    if (toScreen)
                        this.dataBuffer.setElem(yo++, colour1H);
                }
                else
                {
                    mainMemory[address++]=col2H;
                    if (toScreen)
                        this.dataBuffer.setElem(yo++, colour2H);
                }

            }  
            address+=rowInc;                                    //  go to start of next row in mem
            yo+=bufRowInc;                                      // go to start of new row in image databuffer
            even=!even;
        }
        this.isDirty=true;
    }
    
    /**
     * XORs a block with colour : this updates the memory AND the underlying image data (if block i =s actually within screen).
     * Sometimes, the block is only within the main memory.
     * The main memory is an array of shorts, the image databuffer is an array of ints (ARGB).
     * 
     * @param cpu the CPU used.    
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
     * stipples are such that they affect a block of 2*2 pixels   even row : p1p2
     *                                                            odd  row : p3p4   
     * where each of the 4 pixels may be one of two colours.
     * Hence the colours in D6 and D7 are the colours for two consecutive pixels :p1 and p2 in D6, p3 and p4 in D7.
     * This is why there may be a leading and a trailing colour. And this is why there are two different colours 
     * for even and odd rows.
     */
    @Override
    public void xorBlock (smsqmulator.cpu.MC68000Cpu cpu,boolean resolveStipple)
    {
        //if (cpu.data_regs[1]<=0)
        if (cpu.data_regs[1]==0)
            return;
        
        int xo=cpu.data_regs[2]>>>16;                           // x origin of the block.
        int yo=cpu.data_regs[2]&0xffff;                         // y origin of the block.
        int xs=cpu.data_regs[1]>>>16;                           // x size of the block IN WORDS
        int ys=cpu.data_regs[1]&0xffff;                         // y size of the block
 //       if ((ys&0x8000)!=0)
   //         return;
        int address = cpu.addr_regs[1];                         // base address of block
 
        boolean toScreen= (address>=this.startAddress && address<= this.stopAddress) ; // is block in screen memory ?
        if (!toScreen)                                          // not to screen?
        {
            cpu.data_regs[0]=smsqmulator.Types.ERR_ORNG;        // yes returns to smsqe with error
            cpu.reg_sr&=~4;                                     // unset Z flag
            return;   
        }
        int[]colours=new int [2];
        colours[0]=cpu.data_regs[6];                            // stipple or even row colour
        colours[1]=cpu.data_regs[7];                            // if D6 is a stipple nbr, make colours from these regs 
        if (resolveStipple)                             
            getColoursFromStipple(colours);                     // make colours if d6 was stipple number
        
        boolean leading=false,trailing=false;
        if ((xo & 1)!=0)                                        // xo is odd, so we might have leading stipple pixel
        {
            leading=true;   
            xs--;
        }
        if ((xs &1 )!=0)                                        // xsize is odd, we might have training stipple pixel
        {
            trailing=true;
            xs--;
        }
        
        int cols=xs/2;                                          // nbr of double pixels (i.e. long words)
        boolean even = (yo & 1)==0;                             // is start row odd or even?
        
        short col1H=(short)(colours[0]>>>16);                   // high word of colour for memory - short
        int colour1H=this.clut[col1H&0xffff];                   // high word of colour in iamge databuffer - int
        short col1L=(short)(colours[0]&0xffff);                 // same for low word
        int colour1L=this.clut[col1L&0xffff];                   // this is for EVEN rows
        short col2H=(short)(colours[1]>>>16);                   // same for odd rows
        int colour2H=this.clut[col2H&0xffff];              
        short col2L=(short)(colours[1]&0xffff);
        int colour2L=this.clut[col2L&0xffff];
        
        short xorcol;
        
        int rowInc=cpu.addr_regs[2];                            // row increment in memory
        address+=yo*rowInc;                                     // start of row
        address+=xo*2;                                          // start pixel - each pixel takes 2 bytes
                                                                // address now is the address of the start of the block
        address/=2;                                             // index into memory array
        rowInc/=2;                                              // row incement in words, not bytes
        short[] mainMemory=cpu.getMemory();
        
        yo*=this.xsize;                                         // index into image databuffer for first pixel of this line
        yo+=xo;
        
        // as of here : address = index into memory array ; yo = index into image databuffer array       
        for (int iy=0;iy<ys;iy++)                               // do for all rows
        {
            int xxo = yo;                                       // start in the databuffer array
            int ix= address;                                    // start address in memeory
            if (leading)                                        // do we have a leading word?
            {      
                if (even)
                {
                    xorcol=(short)(mainMemory [ix] ^ col1L);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour1L);
                        xxo++;
                    }
                }
                else
                {
                    xorcol=(short)(mainMemory [ix] ^ col2L);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour2L);
                        xxo++;
                    }
                }
            }  
            for (int i=0;i<cols;i++)                            // do it for each double pixel in row
            {
                if (even)
                {
                    xorcol=(short)(mainMemory [ix] ^ col1H);
                    mainMemory[ix++]=xorcol;
                    xorcol=(short)(mainMemory [ix] ^ col1L);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour1H);
                        xxo++;
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour1L);
                        xxo++;
                    } 
                }
                else
                {
                    xorcol=(short)(mainMemory [ix] ^ col2H);
                    mainMemory[ix++]=xorcol;
                    xorcol=(short)(mainMemory [ix] ^ col2L);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour2H);
                        xxo++;
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour2L);
                        xxo++;
                    } 
                }
            }
            if (trailing)
            {     
                if (even)
                {
                    xorcol=(short)(mainMemory [ix] ^ col1H);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour1H);
                        xxo++;
                    }
                }
                else
                {
                    xorcol=(short)(mainMemory [ix] ^ col2H);
                    mainMemory[ix++]=xorcol;
                    if (toScreen)
                    {
                        this.dataBuffer.setElem(xxo,this.dataBuffer.getElem(xxo)^colour2H);
                        xxo++;
                    }
                }            
            }  
            address+=rowInc;
            yo+=this.xsize;
            even=!even;
        }
        this.isDirty=true;
    }
    
    
    private static short alphaBlend(int src1, int src2, double weight)
    {
        int rs,rd,gs,gd,bs,bd;
        rs=(src1&0xf8);                             // split source1 into RGB
        gs=((src1&0xe000)>>>11) + ((src1&7)<<5);    // green has 6 bits
        bs=((src1&0x1f00))>>>5;

        rd=(src2&0xf8);                             // split source2 into RGB
        gd=((src2&0xe000)>>>11) + ((src2&7)<<5);  
        bd=((src2&0x1f00))>>>5;

        rd= (int) (rs*weight+rd*(1.0-weight));      // typical blending op
        gd= (int) (gs*weight+gd*(1.0-weight));
        bd= (int) (bs*weight+bd*(1.0-weight));

        // rgb to mode32
        rd&=0xf8;                                   // 00000000 rrrrr000
        gd&=0xfc;                                   // 00000000 gggggg00
        gd=(gd>>>5)+((gd&0x1c)<<11);                // ggg00000 00000ggg
        bd&=0xf8;                                   
        bd<<=5;                                     // 000bbbbb 00000000

        rd|=gd;                                     
        rd|=bd;
        return (short) rd;
    }
    
    /**
     * This makes my colour lookup table of RGB colours out of 16 bit SMSQE colours (mode 32).
     * 
     * @param moreVibrantColours tries to brighten up the colours a bit.
     * 
     */
    private void makeClutColours(boolean moreVibrantColours)
    {
        int r,g,b; 
        if (!moreVibrantColours)
        {   
            for (int c=1;c<65536;c++)
            {
                g=((c&0xe000)>>>3) + ((c&7)<<13);        // green 6 bits
                r=c&0xf8;
                if (r!=0)
                {
                    r<<=16;
                }
                b=((c&0x1f00))>>>5;
                this.clut[c]=b|r|g;
            } 
        }
        else
        {
            for (int c=1;c<65536;c++)
            {
                g=((c&0xe000)>>>3) + ((c&7)<<13);           // green 6 bits
                if (g!=0)
                {
                    g+=0x300;                               // rGb
                }
                r=c&0xf8;
                if (r!=0)
                {
                    r<<=16;
                    r+=0x70000;                                     // Rgb
                }
                b=((c&0x1f00))>>>5;
                if (b!=0)
                    b+=7;                                           // rgB
                this.clut[c]=b|r|g;
            }
        }
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
        if (copyFromScreen)                                 // source and destination are n the screen
        {
            int xs = (cpu.data_regs[1]>>>16)&0xffff;
            int ys=cpu.data_regs[1]&0xffff;
            int srcXo=(cpu.data_regs[2]>>>16)&0xffff;
            int srcYo=cpu.data_regs[2]&0xffff;
            int destXo= (cpu.data_regs[3]>>>16) &0xffff;
            int destYo=cpu.data_regs[3]&0xffff ;
            
            int []colArray=new int[xs*ys];
            this.raster.getDataElements(srcXo,srcYo, xs, ys, colArray);
            this.raster.setDataElements(destXo,destYo, xs, ys, colArray);// copy the part of the screen about, in the BufferedImage 
        }
        else
        {
            paintBlock(cpu.data_regs[1],cpu.data_regs[3],cpu.getMemory());
        }
        this.isDirty=true;
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
        int lineIncrease=this.nbrOfBytesPerLine/2;
        int line=yo*lineIncrease +xo;                       // index into array for first pixel
        line+=this.startAddress/2;
        
        //short[]vramBuffer=cpu.getMemory();
        yo*=this.xsize;                                     // index into screenimage buffer for first pixel of this line

        for (int iy=0;iy<ys;iy++)
        {
            int xxo = xo+yo;
            int end = line+xs;
            for (int ix=line;ix<end;ix++)
            {
                int col =vramBuffer[ix]&0xffff;
                this.dataBuffer.setElem(xxo++, this.clut[col]);
            }
            line+=lineIncrease;
            yo+=this.xsize;
        }
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
       // nbrOfBytes/=2;                                      // prepare for word sized access
        int vramStart=start/2;
        start-=this.startAddress;                                // make sure address is even
        short[]vramBuffer=cpu.getMemory();
        int value;
        for (int i=0;i<nbrOfBytes/2;i++,start+=2,vramStart++)
        {
            value=vramBuffer[vramStart];
            paintPixel(start,value);
        }
        this.isDirty=true;
    }
    
    /**
     * Sets a word full of pixels in the screen image at a certain address from the vramBuffer, but DOES NOT repaint the screen afterwards.
     * 
     * @param addr where to take the screen data from.
     */
    private void paintPixel(int addr,int value)
    {
        int y=addr/this.nbrOfBytesPerLine;                  // the line (y coord) we're on        
        int x=addr-(y*this.nbrOfBytesPerLine);              // the initial col (y coord) of the pixel
        x/=Screen32.nbrOfBytesPerPixel;
        this.dataBuffer.setElem(x+y*this.xsize, this.clut[value]);
    }
    
    /**
     * This combines two blocks (source 1, source2) with alpha blending and puts the result into the destination array.
     * 
     * Once this is done, the screen is painted with the new data (if the destination is the screen).
     * 
     * @param cpu the CPU used.
     */
    
    @Override
    public void combineBlocks(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int xs=cpu.data_regs[6]&0xff;
        double weight =(double)((double)xs/(double)255);    // the alpha gradient
        short[]srcBuffer;
        short[]src2Buffer;
        short[]destBuffer;
        short[]vramBuffer = cpu.getMemory();
        boolean writeBack=false;
        
        xs = (cpu.data_regs[1]>>>16)&0xffff;
        int ys=cpu.data_regs[1]&0xffff;
        int srcXo=(cpu.data_regs[2]>>>16)&0xffff;
        int srcYo=cpu.data_regs[2]&0xffff;
        int destXo= (cpu.data_regs[3]>>>16) &0xffff;
        int destYo=cpu.data_regs[3]&0xffff ;                // split up the x,y sizes and the src and dest x and y origins
        int src2Xo=(cpu.data_regs[4]>>>16)&0xffff;
        int src2Yo=cpu.data_regs[4]&0xffff;
        
        int srcInc=cpu.addr_regs[2]/2;
        int srcStart=cpu.addr_regs[4];
        if (srcStart>=this.startAddress)
        {
            srcStart=srcYo*srcInc+srcXo + this.startAddress/2;                    // is in vram buffer
            srcBuffer = vramBuffer;
        }
        else
        {
            srcStart=(srcStart/2)+srcYo*srcInc+srcXo;       // is in main mem
            srcBuffer = vramBuffer;
        }
        
        int src2Inc=cpu.data_regs[7]/2;
        int src2Start=cpu.addr_regs[1]; 
        if (src2Start>=this.startAddress)
        {
            src2Start=src2Yo*src2Inc+src2Xo+this.startAddress/2;                // source2 is in vram buffer
            src2Buffer = vramBuffer;
        }
        else
        {
            src2Start=(src2Start/2)+src2Yo*src2Inc+src2Xo;  // source2 is in main mem
            src2Buffer = vramBuffer;
        }
        
        int destInc=cpu.addr_regs[3]/2;
        int destStart=cpu.addr_regs[5];                     // destination
        if (destStart>=this.startAddress)
        {
            destStart=destYo*destInc+destXo+this.startAddress/2;                // destination is in vram buffer
            destBuffer = vramBuffer;
            writeBack=true;
        }
        else
        {
            destStart=(destStart/2)+destYo*destInc+destXo;  // destination is in main mem
            destBuffer = vramBuffer;
        }
                
        int rs,rd,gs,gd,bs,bd,src2,src1;
        for (int iY=0;iY<ys;iY++)                           // do it for every line colour
        {
            for (int iX=0;iX<xs;iX++)                       // every pixel in a line
            {
                src1=srcBuffer[srcStart+iX];                // source1 :  gggbbbbb rrrrrggg
                src2=src2Buffer[src2Start+iX];              // same for source2
                
                rs=(src1&0xf8);                             // split source1 into RGB
                gs=((src1&0xe000)>>>11) + ((src1&7)<<5);    // green has 6 bits
                bs=((src1&0x1f00))>>>5;
                
                rd=(src2&0xf8);                             // split source2 into RGB
                gd=((src2&0xe000)>>>11) + ((src2&7)<<5);  
                bd=((src2&0x1f00))>>>5;
                
                rd= (int) (rs*weight+rd*(1.0-weight));      // typical blending op
                gd= (int) (gs*weight+gd*(1.0-weight));
                bd= (int) (bs*weight+bd*(1.0-weight));
                
                // rgb to mode32
                rd&=0xf8;                                   // 00000000 rrrrr000
                gd&=0xfc;                                   // 00000000 gggggg00
                gd=(gd>>>5)+((gd&0x1c)<<11);                // ggg00000 00000ggg
                bd&=0xf8;                                   
                bd<<=5;                                     // 000bbbbb 00000000

                rd|=gd;                                     
                rd|=bd;

                destBuffer[destStart+iX]=(short)(rd);       // insert into destination.
            }
            srcStart+=srcInc;                               // src1 down to next source line 
            destStart+=destInc;                             // same for destination line
            src2Start+=src2Inc;                             // I could put these into the for () structure but this is more legible
        }
        if (writeBack)
            paintBlock(cpu.data_regs[1],cpu.data_regs[3],cpu.getMemory());  // now actually paint the screen with the data from the vram.
        cpu.data_regs[0]=0;
        cpu.reg_sr|=4;
        this.isDirty=true;
    }
    
    /**
     * Sets the new Vrambase.
     * Adjust vramtop accordingly.
     * 
     * @param vrambase the new vrambase, the start address of the vram.
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
     * @param vramBuffer where data is stored.
     */
    @Override
    public void setVibrantColours(boolean b,short[]vramBuffer)
    {
        this.vibrantColours=b;
        makeClutColours(this.vibrantColours);
        int addr=this.startAddress/2;
        for (int iX=0;iX<this.xsize*this.ysize;iX++)
        {
           int col=vramBuffer[addr+iX]&0xffff;
           this.dataBuffer.setElem(iX, this.clut[col]); // strangely enough, profiling shows these two lines to be faster than the single line below:
        //   this.dataBuffer.setElem(iX, this.clut[this.vramBuffer[iX]&0xffff]);
        }
        this.isDirty=true;
    }
     
    /**
     * Gets size, in bytes, of the vram.
     * 
     * @return screen size, in number of bytes.
     */
    @Override
    public final int getScreenSizeInBytes()
    {
        return this.xsize*this.ysize*this.nbrOfBytesPerPixel;
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
