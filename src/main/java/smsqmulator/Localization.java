package smsqmulator;

/**
 * This is the localization class, which contains strings in different languages.
 * @author and copyright (C) Wolfgang Lenerz 2008-2017.
 * 
 * @version
 * 1.04 getQLVersion added
 * 1.03 version made into variable, getVersion added.
 * 1.02 MB changed into MiB, many spelling errors corrected throughout,  better spanish translation.
 * 1.01 added italian.
 * 1.00 initial version
 */
public class Localization 
{
    /**
    * The language strings
    */
    public static final String []Texts = new String [150] ; // the language strings
    private static final int [] language=new int[1];                                   // what language we're using
                                                            // 1 = german   Deutsch
                                                            // 2 = english  English
                                                            // 3 = spanish  Espanol
                                                            // 4 = french   Français
    // also change ql version !!!!!!                        // 5 = italian  Italiano
    private static final String version="2.24";             // ß = altgr s
    private final static int QLversion=0x322e3234;          // 0x322e3231 ß = 0x329c3231
    
    
    /**
     * Creates the object and tries to set the language to the current locale.
     */
    public Localization()
    {
       this (0);
    }
    
    /**
     * Creates the object and tries to set the language to the chosen language.
     * 
     * @param langage The chosen language to set :
     * <ul>
     *  <li>1 --De  </li>
     *  <li>2 --US/UK  </li>
     *  <li>3 --ES </li>
     *  <li>4 --FR </li>
     *  <li>5 --IT </li>
     *  <li>any other choice: this attempts to set the language to the current locale - if that fails: english </li>
     * </ul>
     */
    public Localization(int langage)
    {
        if (langage<1 || langage> 5)                            // no valid language to set, try to set to current user language
        { 
            String pattern = "deenesfrit";                        // these are the language I know about
            String intern = System.getProperty("user.language").toLowerCase();// get current localization
            langage = (pattern.indexOf(intern)) / 2 + 1;        // index
            if (langage<1 || langage> 4)
                langage=2;                                      // if not found, preset english
        }
        Localization.language[0]=langage;                         
        setLanguage (Localization.language[0]);
    }
    
    /**
     * This sets the language and the language dependent texts.
     * 
     * @param mlanguage a string containing an int with the language :
     * <ul>
     *  <li>1 --De  
     *  <li>2 --US/UK  
     *  <li>3 --ES 
     *  <li>4 --FR   
     *  <li>5 --IT </li>
     * </ul>
     */
    public final void setLanguage(String mlanguage)
    {
        try
        {
            setLanguage(Integer.parseInt(mlanguage));
        }
        catch (Exception e)
        {/*nop*/}
    }
    
    /**
     * Gets the current version of SMSQmulator.
     * 
     * @return the version as a 4 letter string "x.yy"
     */
    public final static int getQLVersion()
    {
        return QLversion;
    }
    /**
     * Gets the current version of SMSQmulator.
     * 
     * @return the version as a 4 letter string "x.yy"
     */
    public final static String getVersion()
    {
        return version;
    }
    /**
     * This sets the language and the language dependent texts.
     * 
     * @param mlanguage an int with the language :
     * <ul>
     *  <li>1 --De  
     *  <li>2 --EN US/UK  
     *  <li>3 --ES 
     *  <li>4 --FR  
     *  <li>5 --IT     
     * </ul>
     */
    public static final void setLanguage(int mlanguage)
    {
        if ((mlanguage>5)|| (mlanguage<1))
            return;                                         // I only know about these languages
        Localization.language[0]=mlanguage;                 // this is the language to be used as of here
        Texts[5]="SMSQmulator\n"+"\n"+
                              "Copyright © W. Lenerz 2012 - 2017\n"+"\n v."+
                              version;                      // whatever the language, this stays the same;;
       
        switch (mlanguage)
        {
            case 1:                                         // german
                Texts[1]="Datei";
                Texts[2]="Bearbeiten";
                Texts[3]="Optionen";
                Texts[4]="Über SMSQmulator";
                Texts[6]="SMSQ/E Datei laden…";  
                Texts[7]="Neustart";
                Texts[8]="Ordner für NFA Laufwerke setzen…";
                Texts[9]="NFA Dateinamenschreibweise";
                Texts[10]="NFA Gebrauchsname (NFA_USE)…";
                Texts[11]="Ordner für SFA Laufwerke setzen…";
                Texts[12]="SFA Dateinamenschreibweise";
                Texts[13]="SFA Gebrauchsname (SFA_USE)…";
                Texts[14]="Dateien für WIN Laufwerke setzen…";
                Texts[15]="WIN Gebrauchsname (WIN_USE)…";
                Texts[16]="Bildschirmgrösse…";
                Texts[17]="Bildschirmfarben/modus";
                Texts[18]="Speicherplatz setzen…";
                Texts[19]="Monitor anzeigen";
                Texts[20]="Schneller Modus";
                Texts[21]="Dateinamen so lassen wie sie sind";
                Texts[22]="Alle Dateinamen in Grossbuchstaben";
                Texts[23]="Alle Dateinamen in Kleinbuchstaben";
                Texts[24]="16-bit Farben";
                Texts[25]="QL Farben";
                Texts[26]="Speicherplatz in MiB festlegen.";
                Texts[27]="Aktueller Speicherplatz ist : ";
                Texts[28]="Speicherplatz bestimmen";
                Texts[29]="Vorsicht : danach wird SMSQ/E neu gestartet!";
                Texts[30]="Falsch";
                Texts[31]=" ist kein gültiger Speicherplatz.";
                Texts[32]="Es konnte keine gültige SMSQ/E-datei gefunden oder gelesen werden"; 
                Texts[33]="Die SMSQ/E-datei kann nicht gefunden werden denn gibt noch keinen Namen für sie.";
                Texts[34]="Benutzen Sie das Dateimenü um eine SMSQ/E-datei zu laden.";
                Texts[35]="Laufwerke/Dateien einrichten";
                Texts[36]="Setzen sie bitte den Gebrauchsnamen für das ";
                Texts[37]=" Laufwerk (immer 3 Buchstaben, z.B. 'win'.)";
                Texts[38]="Gebrauchsname für ";
                Texts[39]="Der Gebrauchsname muss ein Wort aus 3 Buchstaben sein, und nicht ";
                Texts[40]="Geben Sie die erwünschte Auflösung als x_Grösse x Grösse_y an.";
                Texts[41]="Zum Beispiel: ";
                Texts[42]="oder";
                Texts[43]="Die aktuelle Auflösung ist ";
                Texts[44]="Bildschirmauflösung";
                Texts[45]="Fehler";
                Texts[46]="Abbrechen";
                Texts[47]="Interner Fehler 01 : Schreiben soll nach dem Ende des Dateipuffers beginnen";
                Texts[48]="Interner Fehler 02 : Dateischliessfehler : konnte Cluster nicht schreiben";
                Texts[49]="Fehler beim Aufsetzen eines QXL.WIN Laufwerks: ";
                Texts[50]="Kann die SMSQ/E-datei nicht lesen!";
                Texts[51]="Der Go-thread lebt noch.";
                Texts[52]="Der Emulationsthread lässt sich nicht beenden\n (wurde vielleicht im 'schnellen Modus' gestartet?)\n";
                Texts[53]="Starten sie bitte das ganze Programm erneut.";
                Texts[54]="Unbekannter TRAP-Aufruf";
                Texts[55]="Abgefangen : Trap #2 mit Parameter ";
                Texts[56]="Das Hauptverzeichnis des Laufwerks konnte nicht gelesen werden!";
                Texts[57]="Interner Fehler 04 : ";
                Texts[58]=" Kanäle sind noch offen für die Datei : ";
                Texts[59]="Die Datei wurde NICHT gelöscht";
                Texts[60]="Interner Fehler 05 : Zu wenige Bytes gelesen";
                Texts[61]="Interner Fehler 06 : Der Puffer war nicht gross genug";
                Texts[62]="Interner Fehler 07 : konnte Cluster nicht schreiben";
                Texts[63]="Interner Fehler 08 : konnte FAT nicht schreiben";
                Texts[64]=" konnte nicht gefunden werden";
                Texts[65]="Die FAT konnte nicht richtig gelesen werden";
                Texts[66]="konnte nicht geöffnet werden : ist schon von einem anderen Programm geöffnet,";
                Texts[67]="Interner Fehler 09 : Startadresse ist ungerade. Datei kann nicht gespeichert werden";
                Texts[68]="Interner Fehler 10 : Datei als Verzeichnis geöffnet, aber es ist kein Verzeichnis";
                Texts[69]="Interner Fehler 11 : Lesezugriff nach dem Ende der Datei";
                Texts[70]="Interner Fehler 12 : Schreibversuch nach dem Ende der Datei - es konnte kein neuer Cluster gefunden werden";
                Texts[71]="Menüleiste unsichtbar machen";
                Texts[72]="Die Tonausgabe (SSSS) kann nicht initiiert werden.";
                Texts[73]="Vorsicht";
                Texts[74]="Die Datei ";
                Texts[75]=" wurde NICHT nach dem gültigen Standard erstellt.";
                Texts[76]="Die darauf gespeicherten Daten sind NICHT sicher.";
                Texts[77]="Wollen Sie trotzdem mit dieser Datei als Laufwerk ";
                Texts[78]="fortfahren?";
                Texts[79]="Dateien für FLP Laufwerke setzen…";
                Texts[80]="FLP Gebrauchsname (FLP_USE)…";
                Texts[81]="Fehler beim Aufsetzen eines FLP Laufwerks: ";
                Texts[82]="Warnungen";
                Texts[83]="Warnen wenn eine WIN Datei nicht nach dem gültigen Standard erstellt wurde";
                Texts[84]="Warnen wenn eine WIN Datei nicht existiert";
                Texts[85]="Warnen wenn ein WIN Laufwerk voll ist";
                Texts[86]="ist kein gültiges QL Diskettenabbild";
                Texts[87]="Cursorblinkenpausenwert…";
                Texts[88]="Geben sie die Anzahl Millisekunden ein, die SMSQmulator wartet wenn der Cursor blinken soll.";
                Texts[89]="0 heisst, dass SMSQmulator keine Pause macht. 100 scheint ein guter Wert zu sein.";
                Texts[90]=" ist kein gültiger wert. Bitte nur ganze Zahlen eingeben.";
                Texts[91]=" konnte nicht erstellt werden.";
                Texts[92]="Sind Sie sicher, dass es ein gültiger Dateiname in einem gültigen (Unter-) Verzeichnis ist?";
                Texts[93]="Uhrzeitkorrektur…";
                Texts[94]="Geben sie die Zeit (in Sekunden) an, um die die Uhrzeit korrigiert werden muss (+ oder -, 3600 sec = 1 Stunde).";
                Texts[95]="Die X Auflösung muss ein Vielfaches von 8 sein.";
                Texts[96]="Bildschirmgrösse verdoppeln";
                Texts[97]="Bildschirmgrösse halbieren";
                Texts[98]="Die Minimalauflösung ist 512 x 256.";
                Texts[99]="WIN Treiber nicht einbinden";
                Texts[100]="NFA Treiber nicht einbinden";
                Texts[101]="SFA Treiber nicht einbinden";
                Texts[102]="FLP Treiber nicht einbinden";
                Texts[103]="Ihre SMSQ/E Datei ist zu alt, bitte nehmen Sie die Neuere";
                Texts[104]="Warnen wenn auf ein WIN Laufwerk nur lesend zugegriffen werden kann";
                Texts[105]="Auf das WIN Laufwerk kann nur lesend zugegriffen werden";
                Texts[106]=" (für Java ";
                Texts[107]="8-bit Farben (Aurora-kompatibel)";
                Texts[108]="Oje, ich habe nicht mehr genug Speicher! Versuchen Sie bitte den Speicher zu reduzieren (im 'Optionen' Menü).";
                Texts[109]="Leicht lebhaftere Farben";
                Texts[110]="Suspendieren wenn Fenster minimiert wird";
                Texts[111]=" ist keine gültige qxl.win Containerdatei.";
                Texts[112]="Lautstärke setzen, von 0 (kein Ton) bis 100 (ganz laut).";
                Texts[113]="Lautstärke…";
                Texts[114]="Warnen wenn Tonausgabe nicht richtig funktionieren wird";
                Texts[115]="Die Datei ist zu gross";
                Texts[116]="Bytes)";
                Texts[117]="Die Datei konnte nicht richtig gelesen werden";
                Texts[118]="Dateien für MEM Laufwerke setzen…";
                Texts[119]="MEM Gebrauchsname (MEM_USE)…";
                Texts[120]="MEM Treiber nicht einbinden";
                Texts[121]="Weniger CPU-Zeit benutzen wenn nichts zu tun ist";
                Texts[122]="Warnen wenn auf ein FLP Laufwerk nur lesend zugegriffen werden kann";
                Texts[123]="Auf das FLP Laufwerk kann nur lesend zugegriffen werden";
                Texts[124]="Warnen wenn eine FLP Datei nicht geöffnet werden kann";
                Texts[125]="Qxl.win Dateisperrefehler ignorieren";
                Texts[126]="Qxl.win Datei konnte nicht gesperrt werden";
                Texts[127]="Pause nach Mausklick… ";
                Texts[128]="Zeit in 1/1000 Sekunden angeben";
                Texts[129]="Bildschirmaktualisierungsintervall";
                Texts[130]="Geben Sie ein, nach welcher Zeitspanne der Bildschirm aktualisiert wird, in Millisekunden (von 0 bis 65535).";
                Texts[131]="Der aktuelle Wert ist: ";
                Texts[132]="Laufwerke";
                Texts[133]=" ist kein gültiger Intervalwert.";
                Texts[134]="Fenster modus";
                Texts[135]="Fenster";
                Texts[136]="Vollbild";
                Texts[137]="Vollbild - spezial";
                Texts[138]="Bildschirm für Vollbild…";
                Texts[139]="Geben Sie bitte ein auf welchem Bildschirm das Vollbild gezeigt werden soll";
                Texts[140]=" Bei ";
                Texts[141]="Verlassen";
                Texts[142]="QL Bildschirm Emulation erlauben?";
                Texts[143]="Frequenz für SSSS";
                Texts[144]="Auf nicht gesperrte Datei nur lesend zugreifen";
                Texts[145]="Aktion nach JVA_POPUP";
                Texts[146]="Fenster wieder öffnen";
                Texts[147]="Taskleisteneintrag blinken";
                break;
                
            case 2:                                         // english   
                Texts[1]="File";
                Texts[2]="Edit";
                Texts[3]="Config";
                Texts[4]="About SMSQmulator";
                Texts[6]="Load SMSQ/E file…";   
                Texts[7]="Reset";
                Texts[8]="Set dirs for NFA drives…";
                Texts[9]="NFA file name changes";
                Texts[10]="NFA USE name…";
                Texts[11]="Set dirs for SFA drives…";
                Texts[12]="SFA file name changes";
                Texts[13]="SFA USE name…";
                Texts[14]="Set files for WIN drives…";
                Texts[15]="WIN USE name…";
                Texts[16]="Set screen size…";
                Texts[17]="Set screen colours/mode";
                Texts[18]="Set memory size…";
                Texts[19]="Monitor visible";
                Texts[20]="Fast mode";
                Texts[21]="Leave as they are";
                Texts[22]="Set all file names to upper case";
                Texts[23]="Set all file names to lower case";
                Texts[24]="16 bit colours";
                Texts[25]="QL colours";
                Texts[26]="Set the memory size in MiB.";
                Texts[27]="Current size is : ";
                Texts[28]="Set memory size…";
                Texts[29]="Attention : SMSQ/E will be restarted after this!";
                Texts[30]="Wrong choice !";
                Texts[31]=" isn't a valid memory choice!";
                Texts[32]="No valid SMSQ/E file could be found or read"; 
                Texts[33]="Can't find the SMSQ/E file as no name has been given for the SMSQ/E file yet";
                Texts[34]="Please use the 'File' menu to load an SMSQ/E file";
                Texts[35]="Drive/File Assignments";
                Texts[36]="Please set the usage name for the ";
                Texts[37]=" device as a three letter word (eg 'win'.)";
                Texts[38]="Usage name for ";
                Texts[39]="The Usage name must be a three letter word and not ";
                Texts[40]="Give the desired resolution as xsize x ysize.";
                Texts[41]="Such as: ";
                Texts[42]="or";
                Texts[43]="Current size is ";
                Texts[44]="Screen resolution";
                Texts[45]="Error";
                Texts[46]="Cancel";
                Texts[47]="Internal error 01 : write is to start after end of filebuffer.";
                Texts[48]="Internal error 02 : Error closing file : write cluster didn't work";
                Texts[49]="Error setting QXL.WIN drive(s) :";
                Texts[50]="Can't read SMSQ/E file!";
                Texts[51]="Go thread isn't dead";                
                Texts[52]="The emulation thread refuses to die\n (possibly started in 'Fast mode'?)\n";
                Texts[53]="Please close the window and restart the emulation entirely";
                Texts[54]="Unknown Trap call";
                Texts[55]="I caught an unknown Trap #2 call with parameter ";
                Texts[56]="Couldn't read main directory of drive!";
                Texts[57]="Internal error 04 : ";
                Texts[58]=" Channels are open for file : ";
                Texts[59]="The file was NOT deleted";
                Texts[60]="Internal error 05 : Not enough bytes were read";
                Texts[61]="Internal error 06 : Buffer too small";
                Texts[62]="Internal error 07 : write cluster didn't work";
                Texts[63]="Internal error 08 : couldn't write the FAT";
                Texts[64]=" cannot be found";
                Texts[65]="Couldn't read the FAT";
                Texts[66]=" cannot be opened : already opened by another program.";
                Texts[67]="Internal error 09 : can't save file with an odd start address";
                Texts[68]="Internal error 10 : file opened as directory - but it isn't";
                Texts[69]="Internal error 11 : Trying to read beyond the end of the file";
                Texts[70]="Internal error 12 : Trying to write beyond the end of the file - no new cluster could be found";
                Texts[71]="Hide menu bar";
                Texts[72]="Cannot initialise the SMSQ/E sampled sound system.";
                Texts[73]="Attention";
                Texts[74]="The file ";
                Texts[75]=" was NOT created according to the standard.";
                Texts[76]="Data saved to this drive may NOT be safe.";
                Texts[77]="Do you still want to use this file as drive ";
                Texts[78]="?";
                Texts[79]="Set files for FLP drives…";
                Texts[80]="FLP USE name…";
                Texts[81]="Error setting FLP drive(s) :";
                Texts[82]="Warnings";
                Texts[83]="Warn if a WIN drive doesn't comply with the standard";
                Texts[84]="Warn if a WIN drive can't be opened because the native file doesn't exist";
                Texts[85]="Warn when a WIN drive is full";
                Texts[86]="is not a valid QL floppy image file";
                Texts[87]="Blinking cursor pause… ";
                Texts[88]="Enter the number of miliseconds SMSQmulator suspends itself for when the cursor is about to blink.";
                Texts[89]="If this is 0, SMSQmulator doesn't stop. 100 seems a good value";
                Texts[90]=" is not a valid value. Please only enter integer numbers";
                Texts[91]=" could not be created.";
                Texts[92]="Are you sure this is a valid file name in a valid (sub) directory?";
                Texts[93]="Time correction…";
                Texts[94]="Enter the number of seconds (+ or -, 1 hour = 3600 secs) by which the time should be adjusted.";
                Texts[95]="The X size must be a multiple of 8.";
                Texts[96]="Double the window size";
                Texts[97]="Halve the window size";
                Texts[98]="Minimum size is 512 x 256";
                Texts[99]="Disable WIN device";
                Texts[100]="Disable NFA device";
                Texts[101]="Disable SFA device";
                Texts[102]="Disable FLP device";
                Texts[103]="Your SMSQE file is too old, please upgrade to the one that came with this version of SMSQmulator";
                Texts[104]="Warn if a WIN drive is read only";
                Texts[105]="WIN drive is read only";
                Texts[106]=" (for Java ";
                Texts[107]="8 bit colours (Aurora compatible)";
                Texts[108]="Oh dear, I'm out of memory! Please try to reduce the memory size (in the 'Config' menu).";
                Texts[109]="Slightly brighter colours";
                Texts[110]="Suspend execution when window is minimized";
                Texts[111]=" is not a valid qxl.win container file";
                Texts[112]="Set the sound volume from 0 (no sound) to 100 (loudest).";
                Texts[113]="Sound volume…";
                Texts[114]="Warn if sound won't work correctly";
                Texts[115]="The file is too big";
                Texts[116]="bytes)";
                Texts[117]="The file could not be read correctly";
                Texts[118]="Set files for MEM drives…";
                Texts[119]="MEM USE name…";
                Texts[120]="Disable MEM device";
                Texts[121]="Use less CPU time when idle";
                Texts[122]="Warn if an FLP drive is read only";
                Texts[123]="FLP drive is read only";
                Texts[124]="Warn if an FLP image file can't be opened";
                Texts[125]="Ignore qxl.win file lock error";
                Texts[126]="File could not be locked.";
                Texts[127]="Pause after mouse click : ";
                Texts[128]="Enter the duration in 1/1000th of a second";
                Texts[129]="Screen update rate";
                Texts[130]="Set the rate at which the screen is updated, in milliseconds (from 0 to 65535).";
                Texts[131]="The current rate is: ";
                Texts[132]="Devices";
                Texts[133]="is not a valid value for the interval ";
                Texts[134]="Window mode";
                Texts[135]="Window";
                Texts[136]="Full size";
                Texts[137]="Special full size";
                Texts[138]="Display to show full screen on…";
                Texts[139]="Please select which display is to be used for full screen mode";
                Texts[140]=" At ";
                Texts[141]="Quit";
                Texts[142]="Allow QL screen emulation?";
                Texts[143]="SSSS frequency";
                Texts[144]="Make unlockable files read only";
                Texts[145]="Action for JVA_POPUP event";
                Texts[146]="Re-open window";
                Texts[147]="Blink taskbar entry";
                break;
                
            case 3:                                         // spanish  úéñ¡¿óááÁéÉíÍóÓúÚñÑ¡¿
                Texts[1]="Archivo";
                Texts[2]="Editar";
                Texts[3]="Opciones";
                Texts[4]="Acerca de SMSQmulator";
                Texts[6]="Cargar archivo SMSQ/E…";
                Texts[7]="Reiniciar";
                Texts[8]="Directorios para unidades NFA…";
                Texts[9]="Cambiar mayúsculas o minúsculas en nombres de archivos en unidades NFA";
                Texts[10]="Nombre de uso del dispositivo NFA…";
                Texts[11]="Directorios para unidades SFA…";
                Texts[12]="Cambiar mayúsculas o minúsculas en nombres de archivos en unidades SFA";
                Texts[13]="Nombre de uso del dispositivo SFA…";
                Texts[14]="Archivos para unidades WIN…";
                Texts[15]="Nombre de uso del dispositivo WIN…";
                Texts[16]="Tamaño de la pantalla…";
                Texts[17]="Modo y resolución de color de la pantalla";
                Texts[18]="Tamaño de la memoria…";
                Texts[19]="Monitor visible";
                Texts[20]="Modo rápido";
                Texts[21]="Dejar los nombres de archivo tal y como están";
                Texts[22]="Poner los nombres de archivo en mayúsculas";
                Texts[23]="Poner los nombres de archivo en minúsculas";
                Texts[24]="Colores de 16 bitios";
                Texts[25]="Colores de QL";
                Texts[26]="Escriba el tamaño de la memoria en MiB.";
                Texts[27]="Tamaño actual: ";
                Texts[28]="Tamaño de la memoria";
                Texts[29]="Atención: SMSQ/E se reiniciará a continuación.";
                Texts[30]="Opción incorrecta";
                Texts[31]=" no es un tamaño de memoria correcto.";
                Texts[32]="No se puede encontrar o leer el archivo SMSQ/E";
                Texts[33]="El archivo SMSQ/E aún no tiene nombre";
                Texts[34]="Utilice el menú Archivo para cargarlo.";
                Texts[35]="Asociar archivos o directorios";
                Texts[36]="Escriba el nombre del dispositivo ";
                Texts[37]=" con tres letras (por ejemplo 'win'):";
                Texts[38]="Nombre del dispositivo ";
                Texts[39]="El nombre del dispositivo debe tener tres letras y no ";
                Texts[40]="Escriba el tamaño (resolución) de la pantalla como ancho x alto.";
                Texts[41]="Por ejemplo: ";
                Texts[42]="o";
                Texts[43]="La resolución actual es de ";
                Texts[44]="Resolución de la pantalla";
                Texts[45]="Error";
                Texts[46]="Cancelar";
                Texts[47]="Error interno 01: Escritura después del final del búfer";
                Texts[48]="Error interno 02: Error cerrando el archivo: imposible escribir el clúster";
                Texts[49]="Error asignando un archivo WIN: ";
                Texts[50]="No se puede leer el archivo SMSQ/E.";
                Texts[51]="El proceso Go aún está ejecutándose";
                Texts[52]="El proceso de emulación se niega a parar\n (¿quizás fue iniciado en modo rápido?).\n";
                Texts[53]="Por favor, cierre la ventana y arranque de nuevo el emulador.";
                Texts[54]="Llamada a un Trap desconocido";
                Texts[55]="Se ha llamado a un Trap #2 desconocido, con el parámetro ";
                Texts[56]="No es posible leer el directorio principal del disco.";
                Texts[57]="Error interno 04: ";
                Texts[58]=" canales aún están abiertos para el archivo: ";
                Texts[59]="El archivo NO fue suprimido.";
                Texts[60]="Error interno 05: No se han leído bastantes octetos.";
                Texts[61]="Error interno 06: El búfer es demasiado pequeño.";
                Texts[62]="Error interno 07: Imposible escribir el clúster.";
                Texts[63]="Error interno 08: Imposible escribir la tabla de asignación de archivos.";
                Texts[64]=" no se puede encontrar.";
                Texts[65]="Imposible leer la tabla de asignación de archivos.";
                Texts[66]=" no se puede abrir: ya está abierto por otro programa.";
                Texts[67]="Error interno 09: Imposible guardar un archivo desde una dirección impar.";
                Texts[68]="Error interno 10: archivo abierto como directorio sin ser un directorio.";
                Texts[69]="Error interno 11: Intento de leer después del final del archivo.";
                Texts[70]="Error interno 12: Escribiendo más allá del fin del archivo; no hay un nuevo clúster";
                Texts[71]="Ocultar la barra de herramientas";
                Texts[72]="Ha sido imposible iniciar el sonido SSSS.";
                Texts[73]="Cuidado";
                Texts[74]="El archivo ";
                Texts[75]=" NO se ha creado conforme al estándar.";
                Texts[76]="Los datas guardados podrían NO estar seguros.";
                Texts[77]="¿Quiere continuar usando este archivo como disco ";
                Texts[78]="?";
                Texts[79]="Archivos para unidades FLP…";
                Texts[80]="Nombre de uso del dispositivo FLP…";
                Texts[81]="Error iniciando una unidad FLP: ";
                Texts[82]="Avisos";
                Texts[83]="Avisar si un archivo WIN no es conforme al estándar";
                Texts[84]="Avisar si una unidad WIN no se puede abrir porque el archivo nativo no existe";
                Texts[85]="Avisar si una unidad WIN está llena";
                Texts[86]="no es una imagen válida de un disquete de QL";
                Texts[87]="Pausa cuando el cursor parpadea…";
                Texts[88]="Escriba el número de milisegundos que SMSQmulator se detiene cuando el cursor va a parpadear.";
                Texts[89]="Si es 0, SMSQmulator no se detiene. 100 es un valor adecuado.";
                Texts[90]=" no es un número válido. Por favor escriba un número entero.";
                Texts[91]=" no se pude crear.";
                Texts[92]="¿Está seguro de que es un nombre de archivo válido en un directorio válido?";
                Texts[93]="Ajuste del reloj…";
                Texts[94]="Escriba el número de segundos con que ajustar el reloj (positivos o negativos; 1 hora = 3600 segundos).";
                Texts[95]="El ancho debe ser un múltiplo de 8.";
                Texts[96]="Duplicar el tamaño de la pantalla";
                Texts[97]="Reducir el tamaño de la pantalla a la mitad";
                Texts[98]="El tamaño mínimo es de 512 x 256.";
                Texts[99]="Desactivar el dispositivo WIN";
                Texts[100]="Desactivar el dispositivo NFA";
                Texts[101]="Desactivar el dispositivo SFA";
                Texts[102]="Desactivar el dispositivo FLP";
                Texts[103]="El archivo SMSQ/E es demasiado antiguo, por favor sustitúyalo por el que viene con esta versión de SMSQmulator";
                Texts[104]="Avisar si una unidad WIN es de solo lectura";
                Texts[105]="La unidad WIN es de solo lectura";
                Texts[106]=" (para Java ";
                Texts[107]="Colores de 8 bitios (compatibles con Aurora)";
                Texts[108]="¡Caramba, me falta memoria! Por favor, reduzca el tamaño de la memoria (en el menú 'Opciones').";
                Texts[109]="Colores un poco más luminosos";
                Texts[110]="Suspender la ejecución si la ventana está minimizada";
                Texts[111]=" no es un archivo WIN válido.";
                Texts[112]="Elija el volumen del sonido entre 0 (silencio) y 100 (máximo)";
                Texts[113]="Volumen del sonido…";
                Texts[114]="Avisar si el sonido no funcionará bien";
                Texts[115]="El archivo es demasiado grande";
                Texts[116]="octetos)";
                Texts[117]="El archivo no se puede leer correctamente.";
                Texts[118]="Archivos para unidades MEM…";
                Texts[119]="Nombre de uso del dispositivo MEM…";
                Texts[120]="Desactivar el dispositivo MEM";
                Texts[121]="Utilizar menos CPU cuando el emulador esté desocupado";
                Texts[122]="Avisar si una unidad FLP es de solo lectura";
                Texts[123]="La unidad FLP es de solo lectura";
                Texts[124]="Avisar si un archivo FLP no se puede abrir";
                Texts[125]="Ignorar errores de bloqueo de archivos WIN";
                Texts[126]="No se pudo bloquear el archivo.";
                Texts[127]="Pausa después de un clic de ratón…";
                Texts[128]="Escriba la duración en milésimas de segundo.";
                Texts[129]="Periodo de actualización de la pantalla";
                Texts[130]="Escriba el periodo de actualización de la pantalla en milisegundos (de 0 a 65535).";
                Texts[131]="Periodo actual: ";
                Texts[132]="Unidades";
                Texts[133]=" no es un periodo correcto.";
                Texts[134]="Modo de la ventana";
                Texts[135]="Ventana"; 
                Texts[136]="Pantalla entera";
                Texts[137]="Pantalla entera (especial)";
                Texts[138]="Monitor para modo pantalla entera…";
                Texts[139]="Elija el monitor para el modo de pantalla entera";
                Texts[140]=" A ";
                Texts[141]="Salir";
                Texts[142]="¿Permitir emulación pantalla QL?";
                Texts[143]="Frecuencia SSSS ";
                Texts[144]="Archivos WIN non bloqueados solo se pueden leer";
                Texts[145]="¿Qué hacer tras un JVA_POPUP?";
                Texts[146]="Reabrir la ventana";
                Texts[147]="Parpadear la entrada en la barra de tareas";
                break;
                 
            case 4:                                         // french   
                Texts[1]="Fichier";
                Texts[2]="Edition";
                Texts[3]="Options";
                Texts[4]="A propos de SMSQmulator";
                Texts[6]="Charger un fichier SMSQ/E…";  
                Texts[7]="Redémarrer";
                Texts[8]="Choisir répertoires pour lecteurs NFA…";
                Texts[9]="Choisir changement nom de fichiers NFA";
                Texts[10]="Nom d'Usage NFA…";
                Texts[11]="Choisir répertoires pour lecteurs SFA…";
                Texts[12]="Choisir changement nom de fichiers SFA";
                Texts[13]="Nom d'Usage SFA…";
                Texts[14]="Choisir fichiers pour lecteurs WIN…";
                Texts[15]="Nom d'Usage WIN…";
                Texts[16]="Taille d'écran…";
                Texts[17]="Nombre couleurs/mode de l'écran";
                Texts[18]="Taille mémoire…";
                Texts[19]="Moniteur visible";
                Texts[20]="Mode rapide";
                Texts[21]="Laisser les noms de fichier tels quels";
                Texts[22]="Changer tous les noms de fichier en majuscules";
                Texts[23]="Changer tous les noms de fichier en minuscules";
                Texts[24]="Couleurs 16 bits";
                Texts[25]="Couleurs QL";
                Texts[26]="Donnez la taille de la mémoire en MiB.";
                Texts[27]="Taille actuelle ! : ";
                Texts[28]="Taille de la mémoire";
                Texts[29]="Attention, SMSQ/E sera redémarré après ce changement!";
                Texts[30]="Mauvais choix !";
                Texts[31]=" n'est pas une taille mémoire valable!";
                Texts[32]="Fichier SMSQ/E non trouvé ou impossible de le lire"; 
                Texts[33]="Le fichier SMSQ/E n'a pas encore de nom il n'est donc pas trouvable";
                Texts[34]="Utilisez le menu 'Fichier' pour le charger.";
                Texts[35]="Choix des disques/répertoires";
                Texts[36]="Donner le nom d'usage du lecteur ";
                Texts[37]=" en trois lettres (p.ex. 'win'.)";
                Texts[38]="Nom d'isage pour ";
                Texts[39]="Le nom d'usage doit faire trois lettres et non pas ";
                Texts[40]="Donner la taille (résolution) de l'écran par  x_taille x taille_y:";
                Texts[41]="Tels que : ";
                Texts[42]="ou";
                Texts[43]="La résolution actuelle est de ";
                Texts[44]="Résolution de l'écran";
                Texts[45]="Erreur";
                Texts[46]="Annuler";
                Texts[47]="Erreur interne 01 : Ecriture devant commencer après la fin du tampon de fichier.";
                Texts[48]="Erreur interne 02 : Erreur de fermeture de fichier : impossible d'écrire le cluster";
                Texts[49]="Erreur dans la mise en place d'un lecteur QXL.WIN :";
                Texts[50]="Impossible de lire le fichier SMSQ/E";
                Texts[51]="Le Go thread n'est pas arrêté";
                Texts[52]="Le thread d'émulation refuse de s'arrêter\n (démarré peut-être en 'mode rapide'?)\n";
                Texts[53]="Merci de fermer la fenêtre et de redémarrer l'émulation entière.";
                Texts[54]="Appel à un TRAP inconnu";
                Texts[55]="J'ai empêché un appel à un Trap #2 avec le paramètre ";
                Texts[56]="Impossible de lire le répertoire principal du lecteur!";
                Texts[57]="Erreur interne 04 : ";
                Texts[58]=" canaux sont encore ouverts pour le fichier : ";
                Texts[59]="Ce fichier n'a PAS été effacé.";
                Texts[60]="Erreur interne 05 : pas assez d'octets ont été lus";
                Texts[61]="Erreur interne 06 : le tampon de lecture est trop petit";
                Texts[62]="Erreur interne 07 : impossible d'écrire le cluster";
                Texts[63]="Erreur interne 08 : impossible d'écrire la T.A.F.";
                Texts[64]=" ne peut être trouvé";
                Texts[65]="Impossible de lire la T.A.F.";
                Texts[66]="ne peut être ouvert : est déjà ouvert par un autre programme.";
                Texts[67]="Erreur interne 09 : impossible d'écrire le fichier à partir d'une adresse impaire.";
                Texts[68]="Erreur interne 10 : fichier ouvert comme répertoire, mais ce n'est en pas un.";
                Texts[69]="Erreur interne 11 : lecture après la fin du fichier.";
                Texts[70]="Erreur interne 12 : écriture après la fin du fichier - mais il n'y a pas de nouveau cluster.";
                Texts[71]="Cacher la barre de menu";
                Texts[72]="Le son SSSS ne peut être initialisé.";
                Texts[73]="Attention";
                Texts[74]="Le fichier ";
                Texts[75]=" n'a pas été créé conformément au standard.";
                Texts[76]="Les données pourraient ne pas être sûres.";
                Texts[77]="Voulez-vous quand-même continuer avec de fichier comme disque  ";
                Texts[78]="?";
                Texts[79]="Choisir fichiers pour lecteurs FLP…";
                Texts[80]="Nom d'Usage FLP…";
                Texts[81]="Erreur dans la mise en place d'un lecteur FLP :";
                Texts[82]="Avertissements";
                Texts[83]="Avertir si un disque WIN est d'un format non standard";
                Texts[84]="Avertir si un disque WIN ne peut être ouvert parce que le fichier natif n'existe pas";
                Texts[85]="Avertir si un disque WIN est plein";
                Texts[86]="n'est pas une image valable d'une disquette QL";
                Texts[87]="Pause quand le curseur clignote…";
                Texts[88]="Donner le nombre de millisecondes pendant lesquelles SMSQmulator se suspend quand le curseur doit clignoter.";
                Texts[89]="Si c'est '0' SMSQmulator ne s'arrête pas; 100 semble être une bonne valeur.";
                Texts[90]=" n'est pas un nombre valable. Utilisez uniquement des nombres entiers.";
                Texts[91]=" n'a pas pu être créé.";
                Texts[92]="Ëtes-vous sûr qu'il s'agit d'un nom de fichier valable dans un (sous-) répertoire valable?";
                Texts[93]="Correction d'heure…";
                Texts[94]="Entrer le nombre de secondes (+ or -, 1 heure = 3600 sec) par lequel il faut corriger l'heure.";
                Texts[95]="La taille X doit être un multiple de 8.";
                Texts[96]="Doubler la taille de la fenêtre";
                Texts[97]="Diviser par deux la taille de la fenêtre";
                Texts[98]="La taille minimale est 512 x 256.";
                Texts[99]="Désactiver les lecteurs WIN";
                Texts[100]="Désactiver les lecteurs NFA";
                Texts[101]="Désactiver les lecteurs SFA";
                Texts[102]="Désactiver les lecteurs FLP";
                Texts[103]="Votre fichier SMSQ/E est trop ancien, merci de le mettre à jour en prenant celui qui vient avec cette version de SMSQmulator";
                Texts[104]="Avertir si un disque WIN est en lecture seule";
                Texts[105]="Le disque WIN est en lecture seule";
                Texts[106]=" (pour Java ";
                Texts[107]="Couleurs 8 bits (compatibles Aurora)";
                Texts[108]="Mince alors, je n'ai plus assez de mémoire! Merci de la réduire (dans le menu 'Options')";
                Texts[109]="Couleurs légèrement plus vives";
                Texts[110]="Suspendre exécution si la fenêtre est minimisée";
                Texts[111]=" nest pas un fichier qxl.win valable!";
                Texts[112]="Donner le volume du son, de 0 (pas de son) à 100 (le plus fort).";
                Texts[113]="Volume du son…";
                Texts[114]="Avertir si le son ne fonctionnera pas correctement";
                Texts[115]="Le fichier est trop grand";
                Texts[116]="octets)";
                Texts[117]="Le fichier ne peut être lu correctement";
                Texts[118]="Choisir fichiers pour lecteurs MEM…";
                Texts[119]="Nom d'Usage MEM…";
                Texts[120]="Désactiver les lecteurs MEM";
                Texts[121]="Tourner au ralenti si possible";
                Texts[122]="Avertir si un disque FLP est en lecture seule";
                Texts[123]="Le disque FLP est en lecture seule";
                Texts[124]="Avertir si un fichier image FLP ne peut être ouvert";
                Texts[125]="Ignorer erreurs de verrouillage des fichiers qxl.win";
                Texts[126]="Verrouillage du fichier impossible";
                Texts[127]="Pause après clic de souris…";
                Texts[128]="Donnez le temps de pause en 1/1000ème de seconde";
                Texts[129]="Fréquence de rafraîchissement de l'écran.";
                Texts[130]="Entrez l'intervalle entre chaque actualisation de l'écran, en millisecondes (de 0 à 65535).";
                Texts[131]="Valeur actuelle : ";
                Texts[132]="Unités/Lecteurs";
                Texts[133]=" ,'est pas un intervalle correcte.";
                Texts[134]="Mode de la fenêtre";
                Texts[135]="Fenêtre";
                Texts[136]="Plein écran";
                Texts[137]="Plein écran - spécial";
                Texts[138]="Moniteur pour plein écran…";
                Texts[139]="Veuillez choisir quel moniteur affiche le plein écran";
                Texts[140]=" A ";
                Texts[141]="Quitter";
                Texts[142]="Permettre émulation écran QL?";
                Texts[143]="Fréquence SSSS";
                Texts[144]="Fichier qxl.win non verrouibables sont en lecture seule";
                Texts[145]="Action après un JVA_POPUP?";
                Texts[146]="Rouvrir la fenêtre";
                Texts[147]="Faire clignoter l'entrée dans la barre de tâches";
                break; 
                
              case 5:                                         // italian   
                Texts[1]="File";
                Texts[2]="Edita";
                Texts[3]="Configura";
                Texts[4]="Informazioni su SMSQmulator";
                Texts[6]="Carica un file SMSQ/E…";   
                Texts[7]="Reset";
                Texts[8]="Imposta cartelle per un drive NFA…";
                Texts[9]="Modifica nome file NFA";
                Texts[10]="Nome NFA USE…";
                Texts[11]="Imposta cartelle per drive SFA…";
                Texts[12]="Modifica nome file SFA";
                Texts[13]="Nome SFA USE…";
                Texts[14]="Imposta files per un drive WIN…";
                Texts[15]="Nome WIN USE…";
                Texts[16]="Imposta la dimensione dello schermo…";
                Texts[17]="Imposta colori/modalità dello schermo";
                Texts[18]="Imposta la dimensione della memoria…";
                Texts[19]="Monitor visibile";
                Texts[20]="Modalità veloce";
                Texts[21]="Lasciali come sono";
                Texts[22]="Imposta tutti i nomi di file in maiuscolo";
                Texts[23]="Imposta tutti i nomi di file in minuscolo";
                Texts[24]="Colori 16 bit";
                Texts[25]="Colori base QL";
                Texts[26]="Imposta la dimensione delle memoria in MiB.";
                Texts[27]="La dimensione corrente è : ";
                Texts[28]="Imposta la dimensione della memoria";
                Texts[29]="Attenzione : SMSQ/E verrà fatto ripartire subito dopo";
                Texts[30]="Scelta sbagliata !";
                Texts[31]=" non è una scelta valida per la memoria.";
                Texts[32]="Impossibile trovare o leggere un file SMSQ/E valido"; 
                Texts[33]="Non puoi trovare il file SMSQ/E in quanto non hai ancora impostato il nome";
                Texts[34]="Per favore utilizza il menù 'File' per caricare un file SMSQ/E";
                Texts[35]="Assegnazioni Drive/File";
                Texts[36]="Per favore imposta il nome d'uso per ";
                Texts[37]=" device con una parola di tre lettere (ad esempio 'win'.)";
                Texts[38]="Nome d'uso per ";
                Texts[39]="Il nome d'uso deve essere una parola di tre lettere e non ";
                Texts[40]="Imposta la risoluzione desiderata come dimensione_x x dimensione_y.";
                Texts[41]="Ad esempio: ";
                Texts[42]="oppure";
                Texts[43]="La dimensione corrente è ";
                Texts[44]="Risoluzione dello schermo";
                Texts[45]="Errore";
                Texts[46]="Annulla";
                Texts[47]="Errore interno 01 : la scrittura è iniziata dopo la fine del buffer.";
                Texts[48]="Errore interno 02 : Errore chiudendo il file : la scrittura del cluster non è andata a buon fine";
                Texts[49]="Errore configurando il drive QXL.WIN :";
                Texts[50]="Non riesco a leggere il file SMSQ/E!";
                Texts[51]="Il Go-thread è ancora in esecuzione";                
                Texts[52]="Impossibile interrompere l'emulazione\n (possibile partenza in 'Fast mode'?)\n";
                Texts[53]="Per favore chiudi la finestra e fai ripartire l'emulatore";
                Texts[54]="Chiamata Trap sconosciuta";
                Texts[55]="Ho ricevuto una chiamata Trap #2 sconosciuta con il parametro ";
                Texts[56]="Non riesco a leggere la directory principale del drive!";
                Texts[57]="Errore interno 04 : ";
                Texts[58]=" I canali sono aperti per il file : ";
                Texts[59]="Il file NON è stato cancellato";
                Texts[60]="Errore interno 05 : Non sono stati letti abbastanza bytes";
                Texts[61]="Errore interno 06 : Buffer troppo piccolo";
                Texts[62]="Errore interno 07 : Il cluster di scrittura non funziona";
                Texts[63]="Errore interno 08 : Non riesco a scrivere nella FAT";
                Texts[64]=" non può essere trovato";
                Texts[65]="Non riesco a leggere la FAT";
                Texts[66]=" impossibile aprire : già aperto da un altro programma.";
                Texts[67]="Errore interno 09 : Non posso salvare file con un indirizzo di partenza dispari";
                Texts[68]="Errore interno 10 : file aperto come directory - ma non lo è";
                Texts[69]="Errore interno 11 : Tentativo di leggere oltre la fine del file";
                Texts[70]="Errore interno 12 : Tentativo di leggere oltre la fine del file - nessun nuovo cluster trovato";
                Texts[71]="Nascondi la barra del menù";
                Texts[72]="Non riesco ad inizializzare l'SMSQ/E sampled sound system.";
                Texts[73]="Attenzione";
                Texts[74]="Il file ";
                Texts[75]=" NON è stato creato secondo gli standard.";
                Texts[76]="I dati salvati su questo drive possono NON essere sicuri.";
                Texts[77]="Vuoi ancora usare questo file come drive ";
                Texts[78]="?";
                Texts[79]="Imposta i files per i drive FLP…";
                Texts[80]="Nome per FLP USE…";
                Texts[81]="Errore impostando il drive FLP :";
                Texts[82]="Segnalazioni";
                Texts[83]="Segnala se un drive WIN non soddisfa gli standard";
                Texts[84]="Segnala se un drive WIN non può essere aperto perché il file nativo non esiste";
                Texts[85]="Segnala se un drive WIN è pieno";
                Texts[86]=" non è un file immagine di un floppy QL";
                Texts[87]="Pausa durante il lampeggiamento del cursore…";
                Texts[88]="Inserisci il numero di millisecondi in cui SMSQmulator sospende se stesso in attesa del lampeggiamento del cursore.";
                Texts[89]="Se questo è 0, SMSQmulator non si fermerà. 100 sembra essere un buon valore";
                Texts[90]=" non è un valore valido. Inserisci solo numeri interi";
                Texts[91]=" potrebbe essere creato.";
                Texts[92]="Sei sicuro che sia un nome valido in una directory valida?";
                Texts[93]="Correzione dell'ora…";
                Texts[94]="Inserisci il numero di secondi (+ o -, 1 ora = 3600 secondi) necessari per correggere l'ora.";
                Texts[95]="La dimensione X deve essere un multiplo di 8.";
                Texts[96]="Raddoppia le dimensioni della finestra";
                Texts[97]="Dimezza le dimensioni della finestra";
                Texts[98]="La dimensione minima è 512 x 256";
                Texts[99]="Disabilita le device WIN";
                Texts[100]="Disabilita le device NFA";
                Texts[101]="Disabilita le device SFA";
                Texts[102]="Disabilita le device FLP";
                Texts[103]="Il tuo file SMSQE è troppo vecchio, si prega di effettuare l'aggiornamento con quello fornito con la presente versione di SMSQmulator";
                Texts[104]="Segnala se un disco WIN è in sola lettura";
                Texts[105]="Il disco WIN è in sola lettura";
                Texts[106]=" (per Java ";
                Texts[107]="Colori a 8 bit (compatibile con Aurora)";
                Texts[108]="Poffarbacco :-) ho finito la memoria! Per favore prova a ridurre la dimensione della memoria (nel menu 'Configura').";
                Texts[109]="Colori leggermente più brillanti";
                Texts[110]="Sospendi l'esecuzione quando la finestra è ridotta ad icona";
                Texts[111]=" non è un file qxl.win valido";
                Texts[112]="Imposta il volume dell'audio da 0 (nessun audio) a 100 (massimo).";
                Texts[113]="Volume audio…";
                Texts[114]="Segnala se l'audio non funziona correttamente";
                Texts[115]="Il file è troppo grande";
                Texts[116]="bytes)";
                Texts[117]="Il file potrebbe essere stato letto non correttamente";
                Texts[118]="Imposta il file per il disco MEM…";
                Texts[119]="Nome per MEM USE…";
                Texts[120]="Disabilita il device MEM";
                Texts[121]="Utilizza meno tempo di CPU quando inattivo";
                Texts[122]="Avverti quando un drive FLP è in sola lettura";
                Texts[123]="Drive FLP è in sola lettura";
                Texts[124]="Avverti se un file immagine FLP non può essere aperta";
                Texts[125]="Ignora il 'file lock error' del file qxl.win";
                Texts[126]="Il file potrebbe non essere bloccato.";
                Texts[127]="Pausa dopo il click del mouse…";
                Texts[128]="Inserisci la durata in millesimi di secondo";
                Texts[129]="Frequenza di aggiornamento dello schermo";
                Texts[130]="Imposta la frequenza di aggiornamento dello schermo, in millesimi di secondo (da 0 a 65535)";
                Texts[131]="La frequenza corrente è : ";
                Texts[132]="Drives";
                Texts[133]=" non è un tasso valido.";
                Texts[134]="Modalità della finestra";
                Texts[135]="Finestra";
                Texts[136]="Schermo intero";
                Texts[137]="Schermo intero - speciale";
                Texts[138]="Monitor per schermo intero…";
                Texts[139]="Seleziona quale monitor utilizzare per la modalità 'schermo intero'";
                Texts[140]=" A ";
                Texts[141]="Uscire";
                Texts[142]="Lasciare emulazione schermo QL?";
                Texts[143]="Frequenza SSSS";
                Texts[144]="Un file qxl.win non bloccato diventa un file di sola lettura";
                Texts[145]="Azione dopo un JVA_POPUP";
                Texts[146]="Riaprire la finestra";
                Texts[147]="Lampeggiare la voce nelle barra delle applicazioni";
                
                break;       
        }
    } 
    
    /**
     * Returns the current language used.
     * 
     * @return One of these five values:
     * <ul>
     *  <li>1 --De  
     *  <li>2 --US/UK  
     *  <li>3 --ES 
     *  <li>4 --FR 
     *  <li>5 --IT   
     * </ul>
     */
    public static final int getLanguage()
    {
        return language[0];
    }
}
