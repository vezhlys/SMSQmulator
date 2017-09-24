package smsqmulator.cpu;

/**
 * Object derived from CPU, used only when emulating QL screen and copying from there to display.
 * 
 * @author and copyright (C) Wolfgang Lenerz 2016.
 * @version 
 * 1.00 derived from MC68000Cpu v.2.10.
 */
public class CPUforScreenEmulation extends MC68000Cpu
{
    private boolean copyQLScreen=false;
    
    /**
     * Creates the object.
     * 
     * @param size the size of ram in bytes.
     * @param screen the screen to use.
     * @param iniFile my .ini file with options to set.
     * @param romSize the size of the ROM image. Loaded after the RAM image.
     */
    public CPUforScreenEmulation(int size,smsqmulator.Screen screen,inifile.IniFile iniFile,int romSize)
    {
        super(size,screen,iniFile,romSize);
    }
    
    /**
     * Creates the object.
     * 
     * @param size the size of ram in bytes.
     * @param iniFile my .ini file with options to set.
     * @param romSize the size of the ROM image. Loaded after the RAM image.
     * @param videoRamSize size,in bytes, of screen memory to be allocated here.
     */
       public CPUforScreenEmulation(int size,inifile.IniFile iniFile,int romSize,int videoRamSize)
    {
        super(size,iniFile,romSize,videoRamSize);
    }
    
    /**
     * Creates the "naked" object, this isn't really useful except for testing.
     * 
     * @param size the size of ram in bytes. This is allocated when the object is created.
     * @param romSize the size of the ROM image. Loaded after the RAM image.
     * @param videoRamSize size,in bytes, of screen memory to be allocated here.
    */
    public CPUforScreenEmulation(int size,int romSize,int videoRamSize)
    {
        super(size,romSize,videoRamSize);
    }
        
    /**
     * Writes a byte to memory.
     * 
     * @param address where to write to.
     * 
     * @param value the byte to write (in the LSB of the int).
     */
    @Override
    public final void writeMemoryByte(int address, int value)
    {  
        address&=MC68000Cpu.cutOff;                             // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return;    
        int addr=address/2;
        int val=value;
        short res=(this.mainMemory[addr]);
        if((address&1)!=0)
        {
            res&=0xff00;
            value&=0xff;
        }
        else
        {
            res&=0x00ff;
            value=(value<<8)&0xff00;
        }
        value|=res;
        this.mainMemory[addr]=(short)(value);
        if(address>= this.screenStart)
        {
            this.screen.writeByteToScreen(address,val,value);       // trying to write to screen?
        }
        if (this.copyQLScreen && address>=0x20000 && address< 0x28000)
            this.screen.copyScreen(this, addr*2,value);
    }
    
    /**
     * Writes a word to memory.
     * @param address where to write to.
     * @param value the word to write (in the LSW of the int).
     */
    @Override
    public final void writeMemoryWord(int address, int value)
    {   
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForWord)
            return;  
        this.mainMemory[address/2]= (short)(value);
        if(address>= this.screenStart)
        {
            this.screen.writeWordToScreen(address,value);   // trying to write screen?     
        }
        if (this.copyQLScreen && address>=0x20000 && address< 0x28000)
            this.screen.copyScreen(this, address,value);
    }
    
    /**
     * Writes a short to memory.
     * 
     * @param address where to write to.
     * @param value the short to write.
     */
    @Override
    public final void writeMemoryShort(int address, short value)
    {   
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForWord)
            return;  
        this.mainMemory[address/2]= value;
        if(address>= this.screenStart)
        {
            this.screen.writeWordToScreen(address,value);   // trying to write screen?
        }
        if (this.copyQLScreen && address>=0x20000 && address< 0x28000)
            this.screen.copyScreen(this, address,value);
    }
    
    /**
     * Writes a long to the memory.
     * @param address the address where to write to.
     * @param value the value to write.
     */
    @Override
    public final void writeMemoryLong(int address, int value)
    {  
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForLong)
            return;  
        int addr=address/2;
        this.mainMemory[addr]= (short)(value>>>16); 
        this.mainMemory[addr+1]= (short)(value); 
        
        if(address>= this.screenStart)
        {
            this.screen.writeLongToScreen(address,value);   // trying to write screen?
        }
        if (this.copyQLScreen && address>=0x20000 && address< 0x27ffe)
        {
            this.screen.copyScreen(this, address,value>>>16);
            this.screen.copyScreen(this, address+2,value&0xffff);
        }
    }
     
    /**
     * This switches QL screen emulation on or off. If on, any writes to the QL screen area are copied to the true display.
     * 
     * @param origins origins in y screen which should correspond to (0,0) in QL screen.
     * @param QLScreenMode the screen mode the QL screen is supposed to be in.
     */
    @Override
    public void setCopyScreen(int QLScreenMode,int origins)
    {
        this.copyQLScreen=QLScreenMode!=0;
        this.screen.setQLEmulationMode (QLScreenMode);
        this.data_regs[0]=0;
        this.reg_sr|=4;
    }
    
    /**
     * Reads bytes from a ByteBuffer and writes them into memory. Possibly copies QL screen memory to display.
     * 
     * @param address where to read to.
     * @param nbrOfBytes how many bytes are to be read.
     * @param buffer the buffer to read from.
     * @param startInBuffer where in the buffer to start reading from.
     * @param specialRead =<code>true</code> ONLY when loading the OS, must be <code>false</code> at all other times!
     * 
     * @return nbr of bytes read.
     */
    @Override
    protected int readFromBuffer(int address,int nbrOfBytes,java.nio.ByteBuffer buffer,int startInBuffer,boolean specialRead)
    {
       if (nbrOfBytes<1)
            return 0;                                       // there is nothing to read!
        address&=MC68000Cpu.cutOff;
        if (specialRead)
        {
            if ((address+nbrOfBytes)>this.totMemSize)       // don't write above max writable ROM address
            {
                nbrOfBytes=this.totMemSize-address;
                if (nbrOfBytes<1)
                    return 0;
            }
        }
        else
        {
            if ((address+nbrOfBytes)>this.totRamSize)       // don't write above max writable RAM address
            {
                nbrOfBytes=this.ramSize-address;
                if (nbrOfBytes<1)
                    return 0;                               // nothing was read
            }
        }
        boolean toScreen=false;
        if ((address>=this.screenStart && address<=this.screenStop) || (address+nbrOfBytes>=this.screenStart && address+nbrOfBytes<=this.screenStop)) 
        {
            if (!specialRead) 
            {
                toScreen=true;                              // check whether we're LBYTESing to the screen
            }
        }
        int start=address;
        buffer.limit(buffer.capacity());
        if (nbrOfBytes+startInBuffer>buffer.capacity()) 
        {
            nbrOfBytes = buffer.capacity()-startInBuffer;
        }
        boolean startAddressIsOdd=(address&1)==1;

        address/=2;
        if (startAddressIsOdd)
        {
           nbrOfBytes--; 
           short p=(short)(this.mainMemory[address]&0xff00);
           short m=(short) (buffer.get(startInBuffer)&0xff);
           this.mainMemory[address]=(short) (p|m);
           address++;
           startInBuffer++;
        }
       
        // now copy from byte buffer to my array in word sized chunks
        int i;
        for (i=startInBuffer;i<nbrOfBytes+startInBuffer-1;i+=2,address++)
        {
            this.mainMemory[address]=buffer.getShort(i);
        }
        
        if ((nbrOfBytes & 1) == 1)                          // handle possible last byte (not word)
        {
            short res=this.mainMemory[address];
            res&=0xff;
            res|=(buffer.get(i)<<8);
            this.mainMemory[address]=res;
        }
        if (startAddressIsOdd) 
        {
            nbrOfBytes++;
        }
        if (toScreen) 
        {
            this.screen.displayRegion(this,start,nbrOfBytes);
        }
        else if (this.copyQLScreen && !specialRead && (start>=0x20000 && address+nbrOfBytes<0x28000)) 
        {
            this.screen.setDisplayRegion(start,nbrOfBytes, this.mainMemory);
        }
        return nbrOfBytes;
    }
}
