/**
 * The purpose of this project is to have an SMSQ/E emulator "machine" in java.
 * <p>To this end, an MC 68000 processor is emulated.
 * <p>
 * Some implementation notes.
 * <p>
 * As a general rule, most of the objects here are NOT thread safe. Concurrency was NOT
 * an issue when writing this - it is considered that there is ONE CPU that is emulated,
 * which can only access ONE instruction at a time.
 * <p>
 * Likewise, all I/O operations on emulated devices (WIN, FLP, NFA, SFA, MEM) are always atomic to SMSQE: the corresponding trap
 * is entered, the device driver is called by it and the CPU emulation is totally suspended whilst the I/O operation is handled 
 * by the device driver(the emulation thread is handling the I/O call, too). Hence, the device drivers are not thread safe, either. 
* 
 * 
 * <p>
 *  ----------------- SMSQmulator versions -------------------------<p>
 * <b>v.2.25</b>
 * <p> Improvements: SoundDevice can do resampling, different (hopefully better) way of handling mouse scroll wheel. JVA_SYNCSCRAP, "/" may be replaced by 
 * <p> Bugfixes (for windrives, win_drive$, setting drives for devices with different usage names, IEEE floats...)
 * <ul>
 *    <li> SMSQ/E
 *      <ul> 
 *          <li> JVA_SYNCSCRAP added.</li>
 *          <li> driver_snd_ssss_asm modified totally.</li>
 *      </ul>
 *    </li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li>SoundDevice v.1.02 add resampling for 22.5 Khz dataline.</li>
 *          <li>SMSQmulator v.1.23 set new parameter for SoundDevice ; config item MOUSEWHEEL-ACCEL ; new way of handling finding of ini file ; 
 *              ./ expansion added ; may use command line parameter = entire ini file path.</li>
 *          <li>MC68000Cpu  v.2.13 writeSMSQEString : if string is empty but not null, write 0 word ; set and removeKeyrow : do not presume sysvars at $28000.</li>
 *          <li>XfaDriver v.1.06 openFile as directory, use root dir if no part of the file is a dir ; use correct subdir found, if any.</li>
 *          <li>Monitor v.1.18 inputMouseWheel created, when setting device names handle "./".</li>
 *          <li>Screen v.1.21 different way of handling mousewheel ; setMousewheelAccel created.</li>
 *          <li>TrapDispatcher v.1.22 don't add fileseparator at end of name if it is for win or mem drive ; get/setNamesForDives: if 
 *              device not found in map (different usage name) use getDeviceFromMapValues ; expand scrap operations to include
 *              starting/stopping of clipboard monitor thread, all scrap ops now in TRAP D ; setDirForDrive : passing a single space is the 
 *              same as no name at all ; trap c modified for JVA_IS_QLSCREMU%</li>
 *          <li>WinFile v.1.07 when getting the length of the root dir, get the length set in root sector minus the fileheader length..</li>
 *          <li>Helper v.1.04 handles conversion for øå¿æÑÆŒ€.</li>
 *          <li>NfaFileheader v.1.08 setAttrs implemented, try to set some native file attributes in header..</li>
 *          <li>XfaFile v.1.11  use NfaFileheader.setAttrs in makeDirBuffer to set some file attributes in SMSQE file header.</li>
 *          <li>Arith v.1.09 sometimes we need to round up the result (in double2QlFloat).</li>
 *          <li>IPHandler v.1.02 handleTrap added case 7.</li>
 *          <li>MonitorGui v.1.33 when setting device names, show to what "./" will be expanded.</li> 
 *          <li>DriveAssignmentsDialog v.1.05 show to what directory "./" will be expanded.</li>
 *          <li>Screen32 v.1.14 getColoursFromStipple correctly for horizontal stripes.</li>
 *          <li>SampledSound v. 1.05 interface SMSQE  to this object totally revamped, all the buffering is now done in java</li>
 *          <li>Sounddevice v. 1.03 revamped, uses SampledSound to actually play the sound.</li>
 *          <li>.</li>
 *          <li>.</li>
 *      </ul>
 *    </li>
 * </ul>
 *
 *  
 * <b>v.2.24</b>
 * <p> Bugfixes : devices submenu pointer position corrected, win_use$/win_drive$ gets name even if device hase different usage name.
 * <p> Feature : CTRL + mousewheel produces left/right scroll keycodes
 * <ul>
 *    <li> SMSQ/E
 *    </li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li>MonitorGui v.1.32 setDeviceNames : when setting the ptr. pos. for opening the dad wdw, use a new point if ptr is outsie the wdw.</li>
 *          <li>WinDir v.1.05 deleteFile: if file to be deleted is a subdir of mine, and if it is deleted, rebuild subdir list.</li>
 *          <li>Screen v.1.20  CTRL + mousewheel produces left/right scroll keycodes.</li>
 *          <li>TrapDispatcher v.1.21 get drivename (trap5,15) gets name even if device hae different usage name ; 
 *                  trap 5; cases 28,29,30,36,37 : interface change, needs smsqe 3.31.</li>
 *          <li>WinDrive v.1.09 readFat : if non standard drive, the warning wdw referred to the wrong drivename.</li>
 *          <li>MOVEAnWxx (all) byte size was indicated wrongly as word size when disassembling.</li>
 *          <li>IPHandler v.1.01 interface change for get_netname (case 6), needs at least 3.30.0002.</li>
 *          <li>MC6800Cpu v.2.12 RESET instruction is actually linked in.</li>
 *      </ul>
 *    </li>
 * </ul>
 * 
 *  
 * <b>v.2.23</b>
 * <p> Bugfix release : better handling of nfa/sfa drives, keyboard handling.
 * <ul>
 *    <li> SMSQ/E
 *    </li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li>XfaDrive v.1.05 openFile doesn't crash if directory not found.</li> 
 *          <li>XfaFile v.1.10 don't show files whose filenames are too long.</li>
 *          <li>Screen v.1.19 CTRl-Shift +a..z produces keys 160+ ; mac : ctrl shift c is not the same as ctrl c.</li>
 *          <li>TrapDispatcher v.1.20 resetDrives, use map.values directly.</li>
 *      </ul>
 *    </li>
 * </ul>
 *  
 * <b>v.2.22</b>
 * <p> Bugfix release.
 * <ul>
 *    <li> SMSQ/E
 *    </li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li>QemuFileheader v. 1.03, show correct file size in directory, setFileDates : setting a file date to a date before 01.01.1970 will set date to 01.01.1970. </li>
 *          <li>NfaFileheader v. 1.07 setFileDates : setting a file date to a date before 01.01.1970 will set date to 01.01.1970. </li>
 *          <li>XfaFile v.1.09  if file is a dir on sfa and file has qemuheader: set correct filelength in dirBuffer ; getExtendedInfo is for all files, not ony dirs..
 *          <li>XfaDrive v.1.04 openFile better handling of names in chan defn block when opening a directory file.</li> 
 *          <li>Screen16 v.1.10 xorBlock correct handling when trailing or leading ; writeByte doesn't exceed max buffer size at bottom right.</li>
 *          <li>Screen v.1.18 totsize variable introduced.</li>
 *          <li>WinDrive v.1.08 openfile : if dir open for an inexisting dir, copy filename of underlying dir correctly.</li>
 *      </ul>
 *    </li>
 * </ul>
 * 
 * <b>v.2.21</b> JVA_VER$, JVA_WINDOWTITLE, JVA_NETNAME$ implemented. TCP and SCK drivers should work with most common calls.
 * Unlockable qxl.win files may be made read only. SampledSound will upsample to 22.05 Khz. Various bug fixes. Better QL Screen
 * emulation. Window may popup or icon flash if popup event.
 * <br>
 * <ul> 
 *    <li> SMSQ/E 
 *      <ul>
 *          <li>smsq_java_driver_scrap_asm - implement jva_ver$, jva_windowtitle, jva_netname$, jva_popup.</li>
 *          <li>smsq_java_driver_ip_xxxx many changes.</li>
 *          <li>smsq_java_copyscr_asm revamped for jva_qlscremu.</li>
 *          <li>iod_con2_java_xmode_asm catch mode change for automatic mode setting if scr emu.</li>
 *      </ul>
 *    </li>
 *    <li> SMSQmulator      
 *      <ul>
 *          <li>Localization v.1.03 version made into variable, getVersion and getQLVersion added.</li>
 *          <li>TrapDispatcher v.1.19 trap5,36 for SMSQmulator version implemented, also 23-35 for fillblock/xorblock 
 *              and 37 for windowtitle ;  drive query return empty string if no drive defined ; 
 *              setDirForDrive also stores setting in inifile (calls MonitorGui) and shows them in Gui, 
 *              Trap5, D0=5 : set emulated screen mode implemented; setDirForDrive and setNames use forceRemoval 
 *              parameter ; reset force resets the drives, resetDrives() implemented, Trap $b for popup
 *          </li>
 *          <li>Screen16 v.1.09 ,Screen32 v.1.14 fillBlock and xorBlock totally rewritten, they are called from TrapDispatcher, not the cpu.</li>
 *          <li>Screen0 v.1.11 and all other Screen objects: readXXXXFromScreen and alphaBlock removed, copyScreen moved to Screen object.</li>
 *          <li>IPError 0.00, IPTypes 0.00 added.</li>
 *          <li>IPHandler v.1.00 implements SCK and TCP, trap 3: 1-7,50,51,53,58,59,5b,5e,62,7b,7c. No UDP.</li>
 *          <li>IPsocket v.1.00, handles common ops for TCP and SCK.</li>
 *          <li>Arith v.1.08  better handling of negative mantissa, -32768 is no longer a special case.</li>
 *          <li>IPReadAheadBuffer v.1.00 to allow "peeking" the socket input stream.</li> 
 *          <li>Screen v.1.17, readXXXXFromScreen, alphaBlock and getCopyMode removed, copyScreen implemented here, 
 *              accept "automatic" qL screen emulation  (field QLmode) ; µ,£ and ¤ keys corrected.</li>
 *          <li>MC68000Cpu v.2.11 fillBlock, xorBlock deleted ; setEmuScreenMode fallthrough method implemented.</li>
 *          <li>Monitor v.1.17 setCopyScreen amended to suit jva_qlscremu ; setNamesForDrives; forceRemoval parameter 
 *              to force "unmount" of existing drives and remount.</li>
 *          <li>SMSQmulator v.1.22 new config option to make unlockable qxl.win files read only ; correct window mode settings (when did they get changed back?).</li>
 *          <li>WinDrive v.1.07 readFat : unlockable qxl.win files may be made read only.</li>
 *          <li>DeviceDriver, WinDriver 1.04, XfaDriver 1.03,MemDiver 1.02  setNames takes additional parameter to force removal of existing drive before resetting it.</li>
 *          <li>SampledSound  v.1.04 resampling if 22.05 Khz is chosen, thanks to Marcel Kilgus for the algorithm.</li>
 *          <li>SoundDevice v. 1.01 implemented iob.sbyt, posre, flush and added resampling..</li>
 *          <li>MonitorGui v.1.31 1.31 device names set from within SMSQ/E are stored in the inifile ; new config item locked 
 *              qxl.win files may be read only ; SUSPEND-WHEN-ICONIFIED state is actually loaded at startup ;
 *              new config item action upon jva_popup ; java version item added.</li>
 *          <li>WinFile 1.06 readbytes, getLine, sendMultipleBytes: return bytes read / sent in D1.L, not only D1.W, trap#3,D0=6 implemented.</li>
 *          <li>XfaFile 1.08  getLine,saveFile: return bytes read / sent in D1.L, not only D1.W, trap#3,D0=6 implemented , setdate modified ;
 *              call setFileDates when closing file ; max. file name length is 36 chars not 34.</li>
 *          <li>XfaDriver 1.03 trying to open a non-existing file returns err.fdnf and not medium is full.</li>
 *          <li>XfaFileHeader setFilesDates added.</li>
 *          <li>NfaFileheader v.1.06, SfaFileheader v.1.04 and QemuFileheader v.1.02: setFileDates implemented</li>
 *          <li>TBflasher v.1.00 A class to flash the taskbar icon, if it is not focused.</li>
 *          <li>.</li>
 *      </ul> 
 *    </li> 
 * </ul>
 * 
 * <b>v.2.20</b>
 * <p> This contains several bugfixes (stipples in mode 32, Beep, CMP instructions, file open on xFA devices),
 *     configuration of SSSS sample frequency and the possibility to copy the original QL screen to the extended display.
 * <ul>
 *    <li> SMSQ/E
 *      <ul>
 *          <li>java_copyscr_asm implemented.</li>  
 *          <li>iod_con2_java16_block_asm - call java for all operations.</li>
 *          <li>smsq_java_driver_snd_ssss_asm - better way to kill sound.</li>
 *      </ul>
 *    </li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li> Various files throughout: ByteBuffer allocations are no longer made with direct allocations (allocateDirect) but with normal ones.</li>
 *          <li> CMP_AnContent, CMP_AnPlus, MOVEAnT1...T8 : Correct size indication when disassembling .byte.</li>
 *          <li> BEEP v. 1.05 improved parameter handling, should produce sounds closer to the original.</li>
 *          <li> XFADriver v.1.02 if file open is exclusive, file is locked in MY fileOpen method.</li>
 *          <li> XFAFile v.1.02 attempt to lock the file is made in XfaDriver.</li>
 *          <li> Screen v.1.16, Screen0 v.1.10 ,Screen16 v.1.08, Screen32 v.1.13 copyScreen implemented, setDisplayRegion.</li>
 *          <li> MC68000Cpu v. 2.10 setCopyScreen implemented.</li>
 *          <li> CPUforScreenEmulation 1.00 new: alternate CPU for screen emulation.</li>
 *          <li> Monitor v.1.16 setCopyScreen implemented, changeMemSize: provide for alternate CPU, reset cpu for floppy if cpu changed </li>
 *          <li> TrapDispatcher v.1.18 implement trapC for original QL screen copying ; Trap5, cases 32-35 for better mode 32 screen fill and xor.</li>
 *          <li> Types adjusted to suit, needs SMSQE v. 3.27.0002.</li>
 *          <li> SMSQmulator v.1.21 may use CPUforScreenEmulation, new config item.</li>
 *          <li> MonitorGui v.1.30 allowScreenEmulation config item added.</li>
 *          <li> Localization adjusted to suit new config item.</li>
 *          <li> FloppyDriver v.1.07 setCpu added.</li>
 *          <li> Screen32 v.1.13 copyScreen implemented, setDisplayRegion - new,, fillBlock and xorBlock totally revamped.</li>
 *          <li> SampledSound v.1.02 better handling of killsound, faster killsound by limiting the sample size in getFromQueue, frequency in object creation.</li>
 *          <li> </li>
 *      </ul>
 *    </li>
 * </ul>
 * 
 * <b>v.2.19</b>
 * <br>
 * <ul>
 *    <li> SMSQ/E</li>
 *      <li>iod_con2_java8_spcch_asm : do not clr.l odd addresses.</li>
 *      <li></li>
 *    <li> SMSQmulator
 *      <ul>
 *          <li>Size of rom file set to 350000 throughout.</li>
 *          <li>Screen16, screen32 provide for alpha block., NIY</li>
 *          <li>Screen0 v.1.09 set stopAddress correctly, don't write beyond screen.</li>
 *          <li>TrapDispatcher v.1.17 fixed reset (in some cases!!!=.</li>
 *          <li>MC68000Cpu v. 2.09 moveBlock : better handling of odd cases for mode 16 (aurora 8 bit colour).</li> 
 *          <li>DBcc slightly optimized.</li>
 *          <li>MOVEAnD1 slightly optimized for word sized op.</li>
 *          <li>SMSQmulator v.1.20 correct window mode settings.</li>
 *          <li>MonitorPanel v.1.01 also implements down keystroke in buffer.</li>
 *          <li>Arith  v.1.07 -32768 is treated as NAN and handed back to smsq/e. (sometimes it's 80f , 00000000) </li>
 *     </ul> 
 *  </li> 
 * </ul>
 * 
 * 
 * <b>v.2.18</b>
 * <p>
 * Arith bug fix, Full size window implemented, exit item in "files" menu.
 * 
 * <ul>
 *    <li> SMSQmulator
 *    <ul>
 *          <li>Screen v.1.15 isFullSize field, getPreferredSize returns screen size if isFullSize..</li>
 *          <li>Screen0 v. 1.09 set stopAddress correctly, don't write beyond screen.</li>
 *          <li>MonitorGui  v.1.29 creation requires new parameter (window mode); new config item for window mode and display
 *              full screen on which monitor?, and corresponding action routines ; if full screen window mode, window is 
 *              undecorated; setupScreen calls Screen.setFullSize ; exit item in files menu and acction routine.</li>
 *          <li>Localization amended to suit.</li>
 *          <li>SMSQmulator v.1.19  new config option "WINDOW-MODE" ;  ; set Screen x, y sizes according to window mode.</li>
 *          <li>Scc,DBcc and Bcc : corrected case 13 (already in second version of 2.17)</li>
 *          <li>TrapDispatcher v.1.17 fixed reset.</li>
 *          <li>MultiMonitorDalog - new.</li>
 *          <li>Arith v.1.06 0x7b,0x86a80000 is a special value = 360. I'm sure this is a bug in SMSQ/e somehow (sbext_ext_turtle_asm)</li>
 *    </ul> </li> 
 * </ul>
 * 
 * <b>v.2.17</b>
 * <p>
 * Floppy driver bugfix, RORimm bugfix. Small optimizations for speed. When changing screen mode, this is acted upon 
 * immediately (immediate reset).
 * 
 * <ul>
 *    <li> SMSQmulator
 *    <ul>
 *          <li>Bcc, DBcc, Scc : yet more different flag handling for some tests.</li>
 *          <li>MC68000Cpu v.2.08 traceFlag set at various states, avoids ORing reg_sr, newInterruptGeneated no longer volatile, 
                use variable for RTE opcode in executeContinuous, readMemoryLongPCInc use ++ twice, testTrace() introduced.</li>
 *          <li>Localization added forgotten italian translations</li>
 *          <li>Arith v.1.05 cosmetic changes</li>
 *          <li>Screen v.1.14 setVramBase only takes one parameter, getScreenSizeInBytes made abstract, abstract isQLScreen() introduced.</li>
 *          <li>Screen0 v.1.08 implement getScreenSizeInBytes, setVibrantColours takes vram parameter, isQLScreen() introduced.</li>
 *          <li>Screen16 v.1.05 , Screen 32 v.1.11 The vrambuffer is now within the main memory, no longer a separate buffer 
 *          created in the objects, all operations involving writing to the screeen memory adjusted accordingly ; implement 
 *          getScreenSizeInBytes, setVibrantColours takes vram parameter. moveBlock streamlined, fillBlock and xorBlock never 
 *          return error ; isQLScreen() introduced.</li>
 *	    <li>RORimm if word sized op, test with $8000 and not $80</li>
 *          <li>MOVEAxx streamlined for word sized move.
 *          <li>FloppyDiver v.1.06 correctly get floppy number parameter for setDrive.</li>
 *          <li>Beep v.1.04 kill sound before playing new one.</li>
 *          <li>Monitor v.1.15 changeMemSize also called when new screen made, reset no longer changes screen.</li>
 *          <li>MonitorGui v.1.28 Monitor.changeMemSize amended parameter used when changing mem size, screenSizeMenuItemActionPerformed makes new screen,
 *              screenMode variable introduced, monitor.reset() no longEr takes params, when screen mode/colours are changed,
 *              this takes effect immediately (setScreenModeForNextRestart), Throttle removed.</li>
 *          <li>screen v.1.14 setVramBase only takes one parameter, getScreenSizeInBytes made abstract, abstract isQLScreen() introduced.</li>
 *    </ul> </li> 
 * </ul>
 * <br>
 *
 * <b>v.2.16</b>
 * <p>
 * Small optimizations for speed, screen update interval is selectable.
 * 
 * <ul>
 *    <li> SMSQmulator
 *    <ul>
 *          <li>All instructions : removed class variable cpu, execute is called with cpu parameter, seems to give slight speed boost.</li>
 *          <li>Bcc, DBcc, Scc : flags moved into the instructions instead of the general class code.</li>
 *          <li>QL50HzInterrupt v.1.06. Update interval is selectable</li>
 *          <li>MonitorGui v.1.27 getMonitor() implemented, screen update rate, devices sub-menu added.</li> 
 *          <li>Monitor v.1.14 setScreenUpdateInterval implemented.</li>
 *          <li>TrapDispatcher v.1.16 implements sending screen update value to monitor (trap 5, case 29).</li>
 *          <li>Screen16 v.1.04 moveBlock implemented for all sources and destinations.</li>
 *          <li>MC68000Cpu v.2.01 when writing mem, no need to OR the value with $ffff when cast to short ;
 *              moveBlock does the RTS directly, copyMem handles all cases.</li>
 *          <li></li>
 *    </ul> </li>
 * </ul>
 * <br>
 * 
 * <b>v. 0.00</b> --
 * <p> -------------------- To do list ---------------------------
 * <p><b>Known bugs/Still to do are (at least):</b>
 * <p> (v 2.16)
 * <b> Bugs or necessary things to do:</b> 
 * <p>
 * None.
 * <p>
 * <B>Desirable enhancements :</b>
 * <ul>
 * <li> PAR, SER drivers (probably never gonna happen)</li>
 * </ul>
 * 
 * @author and copyright (C) Wolfgang Lenerz 2012-16.
 */

package smsqmulator;
