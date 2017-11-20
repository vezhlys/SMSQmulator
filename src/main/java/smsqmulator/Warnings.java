
package smsqmulator;

/**
 * This is just a data structure containing what type of warnings should be suppressed or shown.
 * 
 * @version
 * v. 1.03  added warnIfFLPDriveIsReadOnly and warnIfNonexistingFLPDrive
 * v. 1.02  added warnIfSoundProblem
 * v. 1.01  added warnIfQXLDriveIsReadOnly
 * v. 1.00  use the correct tokens after change from QXL to WIN.
 * @author and copyright (C) wolfgang lenerz 2012
 */
public class Warnings 
{
    public boolean warnIfQXLDriveNotCompliant;
    public boolean warnIfNonexistingQXLDrive;
    public boolean warnIfNonexistingFLPDrive;
    public boolean warnIfQXLDriveFull;
    public boolean warnIfQXLDriveIsReadOnly;
    public boolean warnIfFLPDriveIsReadOnly;
    public boolean warnIfSoundProblem;
    
    /**
     * Sets the warning statusses from an inifile object.
     * 
     * @param inifile the <code>IniFile</code> object to get the warning statusses from.
     */
    public void setWarnings(inifile.IniFile inifile)
    {
        this.warnIfQXLDriveNotCompliant=inifile.getTrueOrFalse("WARN-ON-NONSTANDARD-WINDRIVE");
        this.warnIfNonexistingQXLDrive=inifile.getTrueOrFalse("WARN-ON-NONEXISTING-WINDRIVE");
        this.warnIfNonexistingFLPDrive=inifile.getTrueOrFalse("WARN-ON-NONEXISTING-FLPDRIVE");
        this.warnIfQXLDriveFull=inifile.getTrueOrFalse("WARN-ON-WINDRIVE-FULL");
        this.warnIfQXLDriveIsReadOnly=inifile.getTrueOrFalse("WARN-ON-WINDRIVE-READ-ONLY");
        this.warnIfFLPDriveIsReadOnly=inifile.getTrueOrFalse("WARN-ON-FLPDRIVE-READ-ONLY");
        this.warnIfSoundProblem=inifile.getTrueOrFalse("WARN-ON-SOUND-PROBLEM");
    }
}
