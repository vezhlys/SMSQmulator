package smsqmulator;

/**
 * Class to speed up some SMSQ/E floating point ari ops.
 * Some notes:
 * 
 * On the d1/d2 operations, d1 = mantissa, d2 - exponent.
 * 
 * tos=top of stack, pointed to by (a1)
 * nos=next on stack, pointed to by 6(a1)
 * 
 * SMSQ/E FPs have a 12 bits exponent, and a 31 bits mantissa + sign bit(bit 31 of the long word is the sign bit).
 * IEEE doubles (64 bits) "only" have an 11 bits exponent, rest is mantissa + sign bit (bit 63), so there might be some s
 * SMSQ/E can handle that IEEE can't : they are handed back to SMSQ/E.
 *  * 
 * THIS CLASS IS NOT THREAD SAFE!!!!!!!!!!!!!!!!!
 * Do not use one same object of this class by different threads concurrently.
 * 
 * @author and copyright (c) Wolfgang Lenerz 2015-2016
 * @version 
 * 1.08     better handling of negative mantissa, -32768 is no longer a special case
 * 1.07     -32768 is treated as NAN and handed back to smsq/e.
 * 1.06     0x7b,0x86a80000 is a special value = 360. I'm sure this is a bug in SMSQ/e somehow (sbext_ext_turtle_asm)
 * 1.05     Cosmetic changes ("nega" variable definition removed).
 * 1.04     Better fp handling.
 * 1.03     (very slightly) faster ql to IEEE fp conversion, correct handling of some negative values.
 * 1.02     don't use c68 QL to IEEE routine, too many pbs (e.g.with $80e,$100000). 
 * 1.01     if nbrs don't fit in an IEEE double, or pose other pbs, use SMSQ/E routines.
 * 1.00     initial version.
 */
public class Arith
{
    private final static int exponentOne=0x3ff00000;            // IEEE exponent so that (2^exponent)=1
    private int oldPC;                                          // value of old program cunter
    private int whatOp;                                         // what operation we're using
   
    /**
     * Handles the f.p. arithmetic operation.
     * 
     * @param cpu the cpu to handle the op for.
     * @param whatOp what operation to handle.
     */
    public final void handleOp(smsqmulator.cpu.MC68000Cpu cpu,int whatOp)
    {
        this.oldPC=cpu.pc_reg;                                  // keep old Program Counter if error
        cpu.data_regs[0]=0;                                     // preset no error
        cpu.reg_sr|=4;                                          // and set mk68 status register accordingly : preset everything was ok
        cpu.pc_reg=cpu.readMemoryLong(cpu.addr_regs[7])/2;      // set new PC after operation
        cpu.addr_regs[7] += 4;                                  // already do rts
        this.whatOp=whatOp;                                     // keep this for possible error returns
        
        switch (whatOp)
        {
            case 0 :                                            // addd : d1/d2 + tos -> tos   
                double testVal=qlFloat2Double(cpu.data_regs[2]&0xffff, cpu.data_regs[1]);
                double2QlFloat(testVal+qlFloat2Double(cpu),cpu);
                break;
                
            case 1:                                             // add : tos + nos -> nos
                testVal=qlFloat2Double(cpu);                    // tos
                cpu.addr_regs[1]+=6;                            // A1 is increased by 6 by this op
                double2QlFloat(testVal+qlFloat2Double(cpu),cpu);// get nos & do op
                break;
                
            case 2 :                                            // subd : tos - d1/d2 -> tos
                testVal=qlFloat2Double(cpu.data_regs[2]&0xffff, cpu.data_regs[1]);
                double2QlFloat(qlFloat2Double(cpu)-testVal,cpu);// tos - d1/d2
                break;
                
            case 3:                                             // sub :  nos - tos -> nos
                testVal=qlFloat2Double(cpu);                    // tos
                cpu.addr_regs[1]+=6;                            // point to nos
                double2QlFloat(qlFloat2Double(cpu)-testVal,cpu);// into what was nos
                break;
                
            case 4 :                                            // double : x -> 2x
                testVal=qlFloat2Double(cpu);
                double2QlFloat(testVal+testVal,cpu);
                break;
                
            case 5:                                             // halve : x -> x/2
                testVal=qlFloat2Double(cpu);
                double2QlFloat(testVal/2.0,cpu);
                break;
                
            case 6 :                                            // divd
                testVal=qlFloat2Double(cpu.data_regs[2]&0xffff, cpu.data_regs[1]);
                if (testVal==0)
                {
                    cpu.data_regs[0]=Types.ERR_OVFL;
                    cpu.reg_sr&=~4;
                }
                else
                {
                    double2QlFloat(qlFloat2Double(cpu)/testVal,cpu);
                }
                break;
                
            case 7:                                             // div : nos / tos ->nos
                testVal=qlFloat2Double(cpu);                    // tos
                if (testVal==0)
                {
                    cpu.data_regs[0]=Types.ERR_OVFL;
                    cpu.reg_sr&=~4;
                }        
                else
                {
                    cpu.addr_regs[1]+=6;
                    double2QlFloat(qlFloat2Double(cpu)/testVal,cpu);
                }
                break;
                
            case 8 :                                            // recip : x -> 1/x
                testVal=qlFloat2Double(cpu);
                if (testVal==0)
                {
                    cpu.data_regs[0]=Types.ERR_OVFL;
                    cpu.reg_sr&=~4;
                }
                else
                {
                    double2QlFloat(1.0/testVal,cpu);
                }
                break;
                
            case 9:                                             // muld : D1/D2 * TOS  -> TOS
                testVal=qlFloat2Double(cpu.data_regs[2]&0xffff, cpu.data_regs[1]);
                 double2QlFloat(qlFloat2Double(cpu)*testVal,cpu);
                break;   
                
            case 10 :                                           // mul : tos*nos -> nos
                testVal=qlFloat2Double(cpu);
                cpu.addr_regs[1]+=6;
                double2QlFloat(qlFloat2Double(cpu)*testVal,cpu);
                break;
                
            case 11:                                            // square : x -> x*x
                testVal=qlFloat2Double(cpu);
                double2QlFloat(testVal*testVal,cpu);
                break;
                
            case 12 :                                           // square root
                double2QlFloat(Math.sqrt(qlFloat2Double(cpu)),cpu);   
                break;
                
            case 13:                                            // cos
                double2QlFloat(Math.cos(qlFloat2Double(cpu)),cpu);
                break;
                
            case 14 :                                           // sin
                double2QlFloat(Math.sin(qlFloat2Double(cpu)),cpu);
                break;
                
            case 15:                                            // cotan
                double2QlFloat(1.0/Math.tan(qlFloat2Double(cpu)),cpu);
                break;   
                
            case 16:                                            // tan
                double2QlFloat(Math.tan(qlFloat2Double(cpu)),cpu);
                break;
     /*       
            case 17:
                test(cpu);
                break;
    */            
            default: 
                cpu.data_regs[0]=Types.ERR_NIMP;
                cpu.reg_sr&=~4;                                 // what kind of op was that??????????????????????????!
                break;
        }
    }
    
    /**
     * Convert SMSQ/E float at (A1) into java double.
     * 
     * @param cpu
     * 
     * @return the double
     */
    private static double qlFloat2Double(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int exponent=cpu.readMemoryWord(cpu.addr_regs[1]);
        int mantissa=cpu.readMemoryLong(cpu.addr_regs[1]+2);   
        if (exponent==0x7b && mantissa==0x86a80000)//329 1518  7ba6a8
            return 360;
        return qlFloat2Double(exponent,mantissa);    
        //return qlFloat2Double(cpu.readMemoryWord(cpu.addr_regs[1]),cpu.readMemoryLong(cpu.addr_regs[1]+2));
    }
     
    /**
     * Convert SMSQ/E float to java (IEEE) double.
     * Inspired partly by c68 routine. 
     * This gets the SMSQ/E mantissa and converts it into a double with an exponent of 1, so that the resulting value
     * is 1.mantissa. 1.0 is deducted from that and then this is multiplied by 2^(true exponent).
     * v1.03
     * 
     * @param exponent  SMSQ/E exponent
     * @param mantissa  SMSQ/E mantissa
     * 
     * @return the corresponding IEEE double,possibly Nan if out of range.
     */   
    private static double qlFloat2Double(int exponent, int mantissa)
    {
        if (mantissa==0)
            return 0;                                           // easy case 
        boolean neg=mantissa<0;                                                                                         
        
        if (neg)
        { 
            mantissa=-mantissa;
            exponent++;                                         // hmmm, why?, seems to work, though
        }
        else
            mantissa<<=1;                                       // remove SMSQ/E sign bit
        if (exponent <1027 || exponent>3070) 
            return Double.NaN;                                  // I can't handle that
        exponent-=0x800;                                        // SMSQ/E bias
        long lowerMantissa = (mantissa & 0xffff);               // keep
        lowerMantissa<<=20;                                     // lower part of the mantissa
        mantissa>>>=12;                                         // and upper bits
        mantissa|=Arith.exponentOne;                            // now combines special exponent + mantissa upper bits
        long interm1=mantissa;                                  // make into long &...
        interm1<<=32;                                           // shift into upper "integer" (long word)
        interm1|=lowerMantissa;                                 // and combine with lower mantissa parts
        double result= Double.longBitsToDouble(interm1) - 1.0;  // get the fractional value but deduct implied bit,i.e. 1
        return (neg)?-result*(Math.pow(2.0, (double)exponent)):result*(Math.pow(2.0, (double)exponent));
    }
    
    /**
     * Convert IEEE double back to SMSQ/E float and set it at (A1).
     * This converts the result of the operation back to an SMSQ/E float.
     * 
     * Based on C68 routine.
     * 
     * @param nbr the double to convert.
     * @param cpu
    */
    private void double2QlFloat(double nbr, smsqmulator.cpu.MC68000Cpu cpu)
    {
       if (Double.isInfinite(nbr) || Double.isNaN(nbr))//||nbr == -32768.0)        // did the op give an error?
        {
            errorReturnAfterResult (cpu);                       // yes, do an error return to smsqe and have smsqe do the operation
            return;
        }
        long interm=Double.doubleToLongBits(nbr);
        int exponent, mantissa;
        if (nbr!=0)
        {
            mantissa=(int)interm; 
            interm>>>=32;
            exponent=(int)interm;
            boolean neg=exponent<0;
            exponent+=exponent;
            exponent>>>=1;
            if (exponent==0)
            {
                errorReturnAfterResult(cpu);
                return;
            }
            int temp=exponent;
            exponent>>>=20;
            exponent+=0x402;
            temp&=0xfffff;
            temp|=0x100000;
            temp<<=10;
            mantissa>>>=22;
            mantissa|=temp;
           
            if (neg)
            {
                if (mantissa != 0 && (mantissa & (mantissa-1)) == 0)// only one bit set in mantisa?
                {                   
                    mantissa<<=1;                                   // yes, normalize
                    mantissa|=0x80000000;                           
                    exponent-=1;
                }
                
                    mantissa=0-mantissa;
            }
        }        
        else
        {
            exponent=0;
            mantissa=0;                                                     
        }
        cpu.writeMemoryWord(cpu.addr_regs[1], exponent);                   
        cpu.writeMemoryLong(cpu.addr_regs[1]+2, mantissa);                 
    }
    
    /**
     * For some reason, I couldn't really handle the operation (e.g. result of op = value outside an IEEE double).
     * Now return to SMSQ/E and do the operation there!
     * To do this, I must execute the instruction that originally sat at the (now) patched location.
     * 
     * @param cpu 
     */
    private void errorReturnAfterResult(smsqmulator.cpu.MC68000Cpu cpu)
    {
        cpu.addr_regs[7]-=4;                                    // undo rts
        switch (this.whatOp)
        {
            case 0 :                                            // addd 
            case 2 :                                            // subd : tos - d1/d2 -> tos
            case 12 :                                           // square root
                resetCpu3Regs(cpu,0);
                break;
                
            case 1:                                             // add : 
                resetCpu3Regs(cpu,6);
                break;
                            
            case 3:                                             // sub :  nos - tos -> nos ****************
                cpu.pc_reg=this.oldPC;                          // actually do the jsr
                int s=this.oldPC *2+ cpu.readMemoryWordPCSignedInc();
                cpu.addr_regs[7] -= 4;
                cpu.writeMemoryLong(cpu.addr_regs[7],cpu.pc_reg*2);               
		cpu.pc_reg=(s&smsqmulator.cpu.MC68000Cpu.cutOff)/2;
               break; 
                
            case 4 :                                            // double : x -> 2x
                cpu.data_regs[0]=cpu.readMemoryWordSigned(cpu.addr_regs[1])&0xffff;// abridged move.w (a1),d0
                cpu.pc_reg =this.oldPC;                         // start at where we were 
                break;
                
            case 5:                                             // halve : x -> x/2
                int d=cpu.readMemoryWordSigned(cpu.addr_regs[1]);//  do a subq.w   #1,(a1)
                int r = d-1;
                cpu.writeMemoryWord(cpu.addr_regs[1],r);
                cpu.reg_sr&=0xffe0;                             // all flags 0
                boolean Sm = false;                             // source is never neg
                boolean Dm = (d & 0x8000) != 0;                 // dist is neg
                boolean Rm = (r & 0x8000) != 0;                 // result is neg
                //n z v c                                       // flags, x will be set to c
                //8 4 2 1
                if (r == 0)
                {
                    cpu.reg_sr+=4;                              // set Z flag
                }
                else if (Rm)
                {
                    cpu.reg_sr+=8;                              // set N flag
                }
                if (!Sm && Dm && !Rm)
                {
                    cpu.reg_sr+=2;                              // set V flag
                }
                if  (Rm && !Dm)
                {
                    cpu.reg_sr+=0x11;                           // set C & X flags
                }
                cpu.pc_reg =this.oldPC;                         // start at where we were 
                break;
                
            case 6 :                                            // divd
            case 9:                                             // muld : D1/D2 * TOS  -> TOS
            case 11:                                            // square : x -> x*x
                resetCpu5Regs(cpu,0);
                break;
                
            case 7:                                             // div : nos / tos ->nos
            case 10 :                                           // mul : tos*nos -> nos
                resetCpu5Regs(cpu,6);
                break;
                
            case 8 :                                            // recip : x -> 1/x
                cpu.addr_regs[1]-=2;                            // do a qa_push1
                cpu.writeMemoryWord(cpu.addr_regs[1], 1);
                cpu.addr_regs[1]-=4;
                cpu.writeMemoryLong(cpu.addr_regs[1], 0x08014000);
                cpu.pc_reg =this.oldPC+1;                       // start at where we were    
                break;
          
            case 13:                                            // cos
            case 14 :                                           // sin
                resetCpu4Regs(cpu);
                break;
                
            case 15:                                            // cotan
            case 16:                                            // tan
                s=cpu.readMemoryLong(cpu.addr_regs[1]+2);       // do a qa_dup
                cpu.addr_regs[1]-=4;
                cpu.writeMemoryLong(cpu.addr_regs[1], s);
                s=cpu.readMemoryWord(cpu.addr_regs[1]+4);                  
                cpu.addr_regs[1]-=2;
                cpu.writeMemoryWord(cpu.addr_regs[1], s);
                cpu.pc_reg =this.oldPC+1;                       // start at where we were  
                break;
        }
    }
            
    /**
     * Switches back to SMSQ/E when d1/d2/d3/d4/d7 were saved on the stack.
     * 
     * @param cpu
     * @param sub if A1 was changed, sub this value from it.
     */
    private void resetCpu5Regs(smsqmulator.cpu.MC68000Cpu cpu,int sub)
    {
        cpu.pc_reg =this.oldPC+1;                           // set new PC
        cpu.addr_regs[7] -= 4;                              // undo rts + prepare for D7
                                                            // d1/d2/d3/d4/d7
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[7]);
        cpu.addr_regs[7] -= 4;                              // prepare for D4
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[4]);
        cpu.addr_regs[7] -= 4;                              // prepare for D3
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[3]);
        cpu.addr_regs[7] -= 4;                              // prepare for D2
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[2]);
        cpu.addr_regs[7] -= 4;                              // prepare for D1
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[1]);

        cpu.addr_regs[1]-=sub;
        cpu.data_regs[0]=0;
    }
    
    /**
     * Switches back to SMSQ/E when d1/d2/d3 were saved on the stack.
     * 
     * @param cpu
     * @param sub if A1 was changed, sub this value from it.
     */
    private void resetCpu3Regs(smsqmulator.cpu.MC68000Cpu cpu,int sub)
    {
        cpu.pc_reg =this.oldPC+1;                           // set new PC
        cpu.addr_regs[7] -= 4;                              // undo rts + prepare for D3
                                                            // d1/d2/d3
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[3]);
        cpu.addr_regs[7] -= 4;                              // prepare for D2
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[2]);
        cpu.addr_regs[7] -= 4;                              // prepare for D1
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[1]);

        cpu.addr_regs[1]-=sub;
        cpu.data_regs[0]=0;
    }
    
    /**
     * Switches back to SMSQ/E when d1/d2/d3/a2 were saved on the stack.
     * 
     * @param cpu
     * @param sub if A1 was changed, sub this value from it.
     */
    private void resetCpu4Regs(smsqmulator.cpu.MC68000Cpu cpu)
    {
        cpu.pc_reg =this.oldPC+1;                           // set new PC
        cpu.addr_regs[7] -= 4;                              // undo rts + prepare for a2
    // d1/d2/d3/a2
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.addr_regs[2]);
        cpu.addr_regs[7] -= 4;                              // prepare for D3
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[3]);
        cpu.addr_regs[7] -= 4;                              // prepare for D2
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[2]);
        cpu.addr_regs[7] -= 4;                              // prepare for D1
        cpu.writeMemoryLong(cpu.addr_regs[7], cpu.data_regs[1]);
        cpu.data_regs[0]=0;
    }
     
    
    /*
    private void test(smsqmulator2.cpu.MC68000Cpu cpu)
    {
        double testval=qlFloat2Double (cpu);
        cpu.addr_regs[1]-=6;
        double2QlFloat(testval,cpu);
        cpu.pc_reg=this.oldPC;                          // actually do the jsr
        cpu.addr_regs[7] -= 4;                                  // already do rts
    }
    
    
    
    /*
    /* ******************************* OLD VERSIONS *********************/
    
     /*
     
    private final static double [] mantVals=new double[32];
    
    public Arith()
    {
        double ptr=0.5;
        for (int i=0;i<31;i++)
        {
            mantVals[i]=ptr;
            ptr/=2.0;
        }
    }
    
     private static double qlFloat2Double(int exponent, int mantissa)
    {
        if (mantissa==0)
            return 0;                                           // easy case 
        if (exponent <1027 || exponent>3070) 
            return Double.NaN;                                  // Too big an exponent, I can't handle that      
        exponent-=0x800;                                        // smsq/e bias for FP exponent 
        double result=0;
        int index=0;
        int mask=0x40000000;                                    // bit 30 set (bit 31 = sign)
        boolean neg = mantissa<0;                               // negative?...
        if (neg)
        {
            mantissa=0-mantissa;                                // ...yes, make it so
            mask=0x80000000;
            if ((mantissa & 0x40000000)==0)                     // I'm not really sure of this
                exponent++;
        }
        while (mask!=0)
        {
            if ((mantissa & mask)!=0)
               result+=mantVals[index];
            index++;
            mask>>>=1;
        }
        return (neg)?-result*(Math.pow(2.0, (double)exponent)):result*(Math.pow(2.0, (double)exponent));
    }
       
      */
    
    /**
     * Convert QL float to java (IEEE) double.
     * Based on c68 routine. 
     * 
     *  ***** Not used any  more, doesn't really work *******************
     * 
     * @param exponent  QL exponent
     * @param mantissa  QL mantissa
     * 
     * @return the corresponding IEEE double
     */
    /*
    private static double qlFloat2DoublOld(int exponent, int mantissa)
    {
        if (exponent==0 && mantissa==0)
            return 0;                                           // easy case 
        boolean neg=mantissa<0;                             //080e      100000
        
        if (neg)
        {
            mantissa=0-mantissa;           
            if ((mantissa & 0x40000000)==0)
                exponent++;
        }
        if (exponent <1027 || exponent>3070) 
            return Double.NaN;                                  // I can't handle that
        exponent-=0x402;                                        // -$800 (ql bias) +1023 (IEEE bias) + 1 (IEEE leading bit) = $402
        exponent<<=20;                                          // push into correct bits
        mantissa<<=2;                                           // handle IEEE implied bit & smsqe sign bit
        long lowerMantissa = (mantissa & 0xffff);
        lowerMantissa<<=20;                                     // lower part of the mantissa
        mantissa>>>=12;                                         // and upper bits
        exponent |=mantissa;                                    // npw combines exponent + mantissa upper bits
        long interm1=exponent;                                  // make into long &...
        interm1<<=32;                                           // shift into upper "integer" (long word)
        interm1|=lowerMantissa;                                 // and combine with lower mantissa parts
        if (neg)
        {
            interm1 |=nega;                                     // set negative bit
        }
        return Double.longBitsToDouble(interm1);                // convert long bits into double
    }
    */
   
    /**
     * Tests whether my implementation of the c68 ql to double is correct.
     * @param cpu 
     */
    /*
    private void testCase(smsqmulator2.cpu.MC68000Cpu cpu)
    {
        //d2 = exp,d1 = mant
        long d2 = (int) cpu.data_regs[2];
        d2<<=32;
        long d1=(int)cpu.data_regs[1];
        d2|=d1;
        double voidj=qlFloat2Double(cpu);
        if (d2!=this.test)
            d2 = d2;
    }
    */
}
    
   /*
    * IEEE double precision float = a 64 bits number (java long integer = 2 x mC68 long word)
    * exponent goes from 0 to $7ff (11 bits)
    * in IEEE the actual exponent = exponent -1023
    * mantissa is in bits 0-52 (bit counting starts with bit 0)
    * there is an implied 1.0 added to the mantissa
    * the sign bit is in bit 63 of the long (bit counting starts with bit 0)
    *
    * in ql there is a word for the exponent (nominally 16 bits) and a long word for the mantissa
    * however, values for the exponent only go from 0 to $fff - 12 bits
    * in ql float the actual exponent = exponent -$800
    * the sign bit is in bit 31 of the mantissa
    *
    * mantissas : the first significant bit in the mantissa has a value of .5 (1/2), the second 1/4, the third 1/8 etc.
    * The problem is that the IEEE mantissa is caluclated as 1.0 + (value of mantissa).
    * whereas the SMSQ/E mantissa is calculated as 0.0 + (value of mantissa). 
    * 1.5 * (2^3)=  .75* (2^4)
    */

/*

; routine to convert IEEE double precision (8 byte) floating point
; to a QLFLOAT_t.

    move.l  4(a7),a0        ; pointer to qlfloat
    move.l  8(a7),d0        ; high long of IEEE double

; SNG - avoid loading low part for now so we can treat D1 as temporary

    add.l   d0,d0           ; Put sign bit in carry
    lsr.l   #1,d0           ; put zero where sign was
    bne     notzero         ; not zero
    move.l  12(a7),d1       ; Test low bits too (probably zero!)
    bne     notzero

; here the double was a signed zero - set the QLFLOAT_t and return

    move.w  d1,(a0)+        ; We know that D1 is 0 at this point
    bra     positive

; was not zero - do manipulations

notzero:
    move.l  d0,d1       ; set non-signed high part copy
;                         We are going to lose least significant byte so we
;                         can afford to over-write it.  We can thus take
;                         advantage that the shift size when specified in
;                         a register is modulo 64
    move.b  #20,d0      ; shift amount for exponent
    lsr.l   d0,d0       ; get exponent - tricky but it works!
    add.w   #$402,d0    ; adjust to QLFLOAT_t exponent
    move.w  d0,(a0)+    ; set QLFLOAT_t exponent

; now deal with mantissa
    
    and.l   #$fffff,d1  ; get top 20 mantissa bits
    or.l    #$100000,d1 ; add implied bit
    moveq   #10,d0      ; shift amount ;; save another 2 code bytes
    lsl.l   d0,d1       ; shift top 21 bits into place
;
;   SNG -This test was a nice idea but does not skip enough to be worthwhile
;
;   tst.l   d1          ; worth bothering with low bits ?
;   beq     lowzer      ; no
;   move.w  #22,d0      ; amount to shift down low long

    move.l  12(a7),d0   ; get less significant bits

;                         We are going to lose least significant byte so we
;                         can afford to over-write it.  We can thus take
;                         advantage that the shift size when specified in
;                         a register is modulo 64
    move.b  #22,d0      ; amount to shift down low long: not MOVEQ!
    lsr.l   d0,d0       ; position low 10 bits of mantissa
    or.l    d0,d1       ; D1 now positive mantissa

lowzer:
    tst.b   8(a7)       ; Top byte of IEEE argument
    bpl     positive    ; No need to negate if positive
    neg.l   d1          ; Mantissa in D1 now
positive:
    move.l  d1,(a0)     ; put mantissa in QLFLOAT_t
    subq.l  #2,a0       ; correct for return address
    move.l  a0,d0       ; set return value as original QLFLOAT_t address
    rts

#else

#if IEEE_FLOAT
;------------------------------- IEEE -------------------------------------
;   routine to convert a QL floating point number to a IEEE double.

_qlfp_to_d:
    move.l  4(a7),a0        ; get pointer to qlfp
    movem.l d2-d3,-(a7)     ; save regs
    moveq   #0,d2           ; clear d2 for later use
    move.w  (a0)+,d2        ; qlfp exponent
    move.l  (a0),d0         ; qlfp mantissa
    bne     notzero         ; mantissa not zero
    cmp.w   #0,d2           ; exponent zero ?
    bne     notzero

; if we get here qlfp was zero, just return 0 in d0 and d1

    moveq   #0,d0
    moveq   #0,d1
    bra     finish

; qlfp is not zero
; now check if the qlfp is negative

notzero:
    moveq.l #0,d3           ; set sign bit as clear
    btst.l  #31,d0
    beq     positive

; qlfp number is negative - negate the mantissa
; and set the sign bit in the exponent

negative:
    bset    #31,d3          ; set sign bit
    neg.l   d0
    btst.l  #30,d0
    bne     positive
    addq.w  #1,d2           ; increase exponent value by 1
positive:
    sub.w   #$402,d2        ; set up the exponent
    lsl.w   #4,d2           ; move it up to the top
    swap    d2              ; put the exponent in the top word
    or.l    d3,d2           ; ... and add sign bit

; now shift the mantissa up by 2 - to get rid of the implied bit

    lsl.l   #2,d0

; put the bottom word in storage - we only really need the bottom 12
; bits.
    move.w  d0,d1
    moveq   #12,d3
    lsr.l   d3,d0

; shift up by 4 to get rid of the unwanted top 4 bits
; swap the words in the second part of the mantissa,

    lsl.w   #4,d1
    swap    d1

; or the exponent into d0 - the mantissa

    or.l    d2,d0

; d0 and d1 now contain the double IEEE fp
; - return

*/