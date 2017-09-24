
package smsqmulator;

//import sun.nio.ch.DirectBuffer;

/**
 * This isn't really a class per se - it just contains various utility routines (<code>public static final </code> ones).
 * @author and copyright (C) Wolfgang Lenerz 2010-2014.
 * 
 * @version 
 * 1.03 converttoSMQQE (char c) introduced.
 * 1.02 writeSMSQEString no longer used (is in cpu anyway)
 * 1.01 ConvertStringToInt don't skip last char anymore.
 */
public class Helper 
{
    private final static String javaAcc="@[]£{|}~äéöüçœàñáâëèêïíîóôùúûßÄÉÖÜÇ"; // these java accented chars will be converted to ...
    private final static int []smsqeBytes={64,91,93,96,123,124,125,126,128,131,// ...these SMSQE chars and vice-versa.
                       132,135,136,139,141,137,140,142,143,144,145,146,147,149,
                       150,152,154,153,155,156,160,163,164,167,168};
    
    /**
     *This shows an error window with a title and a warning string.
     *
     * @param title a string, the title of the error window.
     * @param text a sgtring, the error message.
     * @param frame a JFrame for the JOptionPane.
     */
    public static final void reportError(String title,String text,javax.swing.JFrame frame)
    {
        java.awt.Toolkit.getDefaultToolkit().beep();
        javax.swing.JOptionPane.showMessageDialog(frame,text,title,javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    /**
     * This shows an error window with a title, a warning string and the Java error string.
     *
     * @param title a string, the title of the error window.
     * @param text a string, the error message.
     * @param frame a JFrame for the JOptionPane.
     * @param e the error to report.
     */
    public static final void reportError(String title,String text,javax.swing.JFrame frame,Exception e)
    {
        reportError(title,text + " : "+e.toString(),frame);
    }

  
    /**
     * Converts from java string to smsqe char/byte.
     * 
     * @param c the char to convert
     * 
     * @return the converted char
     */
    public static final byte convertToSMSQE(String c)
    {
     //   assert Helper.javaAcc.length()==smsqeBytes.length;
        if (c==null || c.isEmpty())
        {
            return 0;
        }
        if (c.length()!=1)
            c=c.substring(0,1);
        int p= Helper.javaAcc.indexOf(c);
        if (p!=-1)
            return (byte)(smsqeBytes[p]&0xff);
        else 
            return (byte) c.charAt(0);
    }
    
    /**
     * Converts from java char to smsqe char/byte.
     * 
     * @param c the char to convert
     * 
     * @return the converted char
     */
    public static final byte convertToSMSQE(char c)
    {
     //   assert Helper.javaAcc.length()==smsqeBytes.length;
        int p= Helper.javaAcc.indexOf(c);
        if (p!=-1)
            return (byte)(smsqeBytes[p]&0xff);
        else 
            return (byte) c;
    }
    
    /**
     * Converts from SMSQE byte to Java string.
     * 
     * @param c the byte ti convert
     * 
     * @return the byte as a java unicode string.
     */
    public static final String convertToJava(byte c)
    {
    //    assert Helper.javaAcc.length()==Helper.smsqeBytes.length;
        int in=(int)c&0xff;
        for (int i=0;i<Helper.smsqeBytes.length;i++)
        {
            if (Helper.smsqeBytes[i]==in)
                return Helper.javaAcc.substring(i,i+1);
        }
        return Character.toString((char)c);
    }
    
     /**
     * Converts a three letter name into an "upper cased" int and adds '0' at end.
     * 
     * @param s the string to convert, eg 'nfa'.
     * 
     * @return the converted int, eg the hex representation of 'NFA0', or 0 if error.
     */
    public static int convertUsageName(String s)
    {
        int temp=0;
        if (s != null && !s.isEmpty() && s.length()==3)
        {
            s=s.toUpperCase();
            for (int i=0;i<3;i++)
            {
                temp+=(byte)(s.charAt(i)&0xff);
                temp<<=8;
            }
            if (temp!=0)
                temp+=0x30;
        }
        return temp;
    }
    /**
     * Converts an M68K register list (for MOVEM) to a disassembly string.
     * this is copied from Tony Headford's code.
     * 
     * @param reglist the register list.
     * @param reversed true id list is reversed.
     * @return the register list as a normal disassembly string.
     */
    public final static String regListToString(int reglist, boolean reversed)
    {
        StringBuilder sb = new StringBuilder();
        int first = -1;
        int count = 0;

        if(!reversed)
        {
            //normal mode lsb = d0
            char prefix = 'd';
            int mask = 1;

            for(int i = 0; i < 2; i++)
            {
                    for(int n = 0; n < 8; n++, mask <<= 1)
                    {
                            if((reglist & mask) != 0)
                            {
                                    if(first != -1)
                                    {
                                            count++;
                                    }
                                    else
                                    {
                                            first = n;
                                    }
                            }
                            else
                            {
                                if(first != -1)
                                {
                                    if(sb.length() > 0)
                                            sb.append('/');

                                    sb.append(prefix);
                                    sb.append(first);
                                    if(count == 1)
                                    {
                                            sb.append('/');
                                            sb.append(prefix);
                                            sb.append(n - 1);
                                    }
                                    else if(count > 1)
                                    {
                                            sb.append('-');
                                            sb.append(prefix);
                                            sb.append(n - 1);
                                    }

                                    count = 0;
                                    first = -1;
                                }
                            }
                    }

                    if(first != -1)
                    {
                        if(sb.length() > 0)
                                sb.append('/');

                        sb.append(prefix);
                        sb.append(first);
                        if(count == 1)
                        {
                                sb.append('/');
                                sb.append(prefix);
                                sb.append(7);
                        }
                        else if(count > 1)
                        {
                                sb.append('-');
                                sb.append(prefix);
                                sb.append(7);
                        }

                        count = 0;
                        first = -1;
                    }

                    prefix = 'a';
            }
        }
        else
        {
            //reverse mode for -(an) lsb = a7
            char prefix = 'd';
            int mask = 0x8000;

            for(int i = 0; i < 2; i++)
            {
                for(int n = 0; n < 8; n++, mask >>= 1)
                {
                    if((reglist & mask) != 0)
                    {
                        if(first != -1)
                        {
                                count++;
                        }
                        else
                        {
                                first = n;
                        }
                    }
                    else
                    {
                        if(first != -1)
                        {
                            if(sb.length() > 0)
                                sb.append('/');

                            sb.append(prefix);
                            sb.append(first);
                            if(count == 1)
                            {
                                sb.append('/');
                                sb.append(prefix);
                                sb.append(n - 1);
                            }
                            else if(count > 1)
                            {
                                sb.append('-');
                                sb.append(prefix);
                                sb.append(n - 1);
                            }

                            count = 0;
                            first = -1;
                        }
                    }
                }

                if(first != -1)
                {
                    if(sb.length() > 0)
                            sb.append('/');

                    sb.append(prefix);
                    sb.append(first);
                    if(count == 1)
                    {
                            sb.append('/');
                            sb.append(prefix);
                            sb.append(7);
                    }
                    else if(count > 1)
                    {
                            sb.append('-');
                            sb.append(prefix);
                            sb.append(7);
                    }

                    count = 0;
                    first = -1;
                }

                prefix = 'a';
            }
        }
        return sb.toString();
    }
    
    /**
      * Converts a string of max 4 chars to an integer, each byte is the hex of the char.
      * The int it right filled with 0 if the length of the string is smaller than 4.
      * If the string is longer than 4 chars long, anything after the fourth char is ignored.
      * 
      * @param toConvert the string to convert into an int.
      * 
      * @return the int
      */
    public static final int convertStringToInt(String toConvert)
    {
        int result=0;
        if (toConvert.isEmpty())
            return result;
        byte[] b=toConvert.getBytes();
        int l=b.length;
        if (l>4)
            l=4;
        int leftShift=24;
        for (int i=0;i<l;i++)
        {
            int p=b[i]<<leftShift;
            result|=p;
            leftShift-=8;
        }
        return result;
    }
   
    /**
     * The removes the memory for a direct bytebuffer, w/o waiting for garbage collection.
     * 
     * @param bb 
     *
    
    
    public static void deAllocate(java.nio.ByteBuffer bb) 
    {
        if(bb == null || !bb.isDirect()) 
            return;
        sun.misc.Cleaner cleaner = ((sun.nio.ch.DirectBuffer) bb).cleaner();
        if (cleaner != null)
            cleaner.clean();
    }
    
    */
}

