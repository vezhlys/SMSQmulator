/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smsqmulator;


/**
 * This just contains static data types.
 * v. 1.01 removed linkage_keyrow
 * v. 1.02 communication with java is now based on A line (instructions $a000  +) instead of eb00+.
 * 
 * @author and copyright (C) Wolfgang Lenerz 2012-2016
 */
public class Types 
{
    // return types from SMSQE
    public static final int RETURN_BASE=0xa000;
    public static final int TRAP0 = RETURN_BASE;
    public static final int TRAP1 = TRAP0+1;                // return from traps
    public static final int TRAP2 = TRAP1+1;
    public static final int TRAP3 = TRAP2+1;
    public static final int TRAP4 = TRAP3+1;
    public static final int TRAP5 = TRAP4+1;
    public static final int TRAP6 = TRAP5+1;
    public static final int TRAP7 = TRAP6+1;
    public static final int TRAP8 = TRAP7+1;
    public static final int TRAP9 = TRAP8+1;
    public static final int TRAPA = TRAP9+1;
    public static final int TRAPB = TRAPA+1;
    public static final int TRAPC = TRAPB+1;
    // note $ab00 + are for ieee  FP ops.
    
    
    // error returns to smsqe
    public static final int ERR_NC   = -1;  // OPERATION NOT COMPLETE;
    public static final int ERR_IJOB = -2;  // INVALID JOB ID;
    public static final int ERR_IMEM = -3;  // INSUFFICIENT MEMORY;
    public static final int ERR_ORNG = -4;  // PARAMETER OUTSIDE PERMITTED RANGE (C_F_ ERR_IPAR;
    public static final int ERR_BFFL = -5;  // BUFFER FULL;
    public static final int ERR_ICHN = -6;  // INVALID CHANNEL ID;
    public static final int ERR_FDNF = -7;  // FILE OR DEVICE NOT FOUND;
    public static final int ERR_ITNF = -7;  // ITEM NOT FOUND;
    public static final int ERR_FEX  = -8;  // FILE ALREADY EXISTS;
    public static final int ERR_FDIU = -9;  // FILE OR DEVICE IS IN USE;
    public static final int ERR_EOF  = -10;  // END OF FILE;
    public static final int ERR_DRFL = -11;  // DRIVE FULL;
    public static final int ERR_INAM = -12;  // INVALID FILE, DEVICE OR THING NAME;
    public static final int ERR_TRNS = -13;  // TRANSMISSION ERROR;
    public static final int ERR_PRTY = -13;  // PARITY ERROR;
    public static final int ERR_FMTF = -14;  // FORMAT DRIVE FAILED;
    public static final int ERR_IPAR = -15;  // INVALID PARAMETER (C_F_ ERR_ORNG);
    public static final int ERR_MCHK = -16;  // FILE SYSTEM MEDIUM CHECK FAILED;
    public static final int ERR_IEXP = -17;  // INVALID EXPRESSION;
    public static final int ERR_OVFL = -18;  // ARITHMETIC OVERFLOW;
    public static final int ERR_NIMP = -19;  // OPERATION NOT IMPLEMENTED;
    public static final int ERR_RDO  = -20;  // READ ONLY PERMITTED;
    public static final int ERR_ISYN = -21;  // INVALID SYNTAX;
    public static final int ERR_NOMS = -22;  // NO ERROR MESSAGE;
    public static final int ERR_ACCD = -23;  // ACCESS DENIED

    public static final int SMSQEHeaderLength=64;// SMSQE header length of one file.
    public static final int SFAHeaderLength=SMSQEHeaderLength+4;//length of header for SFA files ($40 bytes normal SMSQE files, 4 bytes "SFA0" flag.
    
    public static final int NFADriver=0x4e464130;           //'NFA0' = deviceid of nfa driver
    public static final int SFADriver=0x53464130;           //'SFA0';
    public static final int FLPDriver=0x464c5030;           //'FLP0';
    public static final int WINDriver=0x57494e30;           //'WIN0'- don't become one!;
    public static final int SDCDriver=0x53444330;           //'SDC0'
    public static final int MEMDriver=0x4d454d30;           //'MEM0';
    public static final int SWINDriver=0x57484e30;          //'WHN0'
    
    public static final int DATE_OFFSET=284000399-3600;     // ** magic offset pc <-->  SMSQE date for UTC time (I hope)
    
    // offsets in the pc <-->  SMSQE linkage block
    
    // DO NOT CHANGE THIS WITHOUT ALSO CHANGING IN SMSQ/KEYS JAVA AND RECOMPILING
    public static final int LINKAGE_KBD=0;                  // keyboard data put here
    public static final int LINKAGE_RTC=LINKAGE_KBD+4;      // the PC clock put here (already in SMSQE fomrat)
    public static final int LINKAGE_MOUSEPOS=LINKAGE_RTC+4; // the current mouse position (absolute in wdw)
    public static final int LINKAGE_MOUSEREL=LINKAGE_MOUSEPOS+4;// the relative mouse mvmt (since last movement)
    public static final int LINKAGE_MOUSEBTN=LINKAGE_MOUSEREL+4;// mouse buttons pressed
    public static final int LINKAGE_SCREENBASE=LINKAGE_MOUSEBTN+4;// screen base                                20
    public static final int LINKAGE_SCREENSIZE=LINKAGE_SCREENBASE+4;// size of screen in bytes
    public static final int LINKAGE_SCREEN_LINE_SIZE=LINKAGE_SCREENSIZE+4;// size of one line in bytes
    public static final int LINKAGE_SCREEN_XSIZE=LINKAGE_SCREEN_LINE_SIZE+2;// screen xsize in pixels(2)
    public static final int LINKAGE_SCREEN_YSIZE=LINKAGE_SCREEN_XSIZE+2;// screen xsize in pixels(2)
    public static final int LINKAGE_BOOT_DEVICE= LINKAGE_SCREEN_YSIZE+2;// Boot device (4) (NIY)
   // public static final int LINKAGE_KEYROW= LINKAGE_BOOT_DEVICE+4;// keyrow (8 bytes)
    //public static final int LINKAGE_NFA_USE= LINKAGE_KEYROW+8;// name for nfa device 4 bytes
    public static final int LINKAGE_NFA_USE= LINKAGE_BOOT_DEVICE+4;// name for nfa device 4 bytes
    public static final int LINKAGE_SFA_USE= LINKAGE_NFA_USE+4;// name for sfa device 4 bytes
    public static final int LINKAGE_WIN_USE= LINKAGE_SFA_USE+4;// name for WIN device 4 bytes
    public static final int LINKAGE_FLP_USE= LINKAGE_WIN_USE+4;// name for flp device 4 bytes
    public static final int LINKAGE_MEM_USE= LINKAGE_FLP_USE+4;// name for flp device 4 bytes
    public static final int LINKAGE_RANDOM=LINKAGE_MEM_USE+4;   // space for random number, 2 bytes
    public static final int LINKAGE_LENGTH=LINKAGE_RANDOM+12;//  some slack space et end
    
    public static final short HWinitStart1=0x4a61;        //start of String "Java Emul"
    public static final short HWinitStart2=0x7661;
    public static final short HWinitStart3=0x2045;
    public static final short HWinitStart4=0x6d75;
    
    public static final int QXL_DRIVE_HEADER=0x40;
    public final static byte[] QEMU={0x5D,0x21,0x51,0x44,0x4f,0x53,0x20,0x46,0x69,0x6c,
                                    0x65,0x20,0x48,0x65,0x61,0x64,0x65,0x72};//  "]!QDOS File Header" from Qemulator + 2 spare bytes
    
    public final static java.nio.ByteBuffer QEMUBUF= java.nio.ByteBuffer.wrap(QEMU);
    public static final int QEMUHeader=0x5d215144;
    public static byte UNDERSCORE=95;
    
    public static short [] SMSQMULATOR_CONFIG_FLAG={0x3c3c,0x534d,0x5351,0x4d55,0x4c41,0x544f,0x5258,0x3e3e};//"<<SMSQMULATORX>>"
    public static final int SRMask=0xa71f;
    public final static String MINIMUM_VERSION_NEEDED ="3.31";  // also modify this in smsq_java_hwinit_asm
    public final static String MINIMUM_MINOR_VERSION_NEEDED ="0000";  // also modify this in smsq_java_hwinit_asm
}
