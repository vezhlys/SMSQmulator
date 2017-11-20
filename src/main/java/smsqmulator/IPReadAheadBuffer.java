package smsqmulator;

/**
 * A (very primitive) "peekable" byte (not char!) buffer for an InputStream (supposedly from a socket). 
 * Contrary to BufferedReader, this operates on bytes, not on characters.
 * It allows "peeks", i.e. reading bytes into an array without removing them from the buffer.
 * This is so that the "C" recv function can actually peek at the content of the InputStream.
 * This is not a real buffer in the sense that it would read more bytes than requested in one go to make reading more
 * efficient.
 * 
 * Implementation note : the buffer is discarded by setting the empty flag to true.
 * Note for future versions : could reuse existing "empty" buffer if possible, use pointers to mark start/stop
 * 
 * @author Wolfgang Lenerz copyright (c) 2016
 * @version 1.00
 */
public class IPReadAheadBuffer
{
    private boolean empty;                                      // if this is true, the buffer is "empty" : it is considered not to exist
    public final java.io.InputStream rawIn;                     // the stream to get the bytes from
    private byte[]buffer;                                       // possible buffer
    private int mySize;                                         // size of existing buffer (even if it is empty)
    
    /**
     * Create a buffer for an InputStream.
     * 
     * @param raw_In the InputStream
     */
    public IPReadAheadBuffer(java.io.InputStream raw_In)
    {
        this.rawIn=raw_In;                                      
        this.empty=true;
        this.buffer = new byte[2];
        this.mySize=2;
    }
    
    /**
     * Read bytes from the stream and/or buffer.
     * 
     * @param result destination array : where to put the bytes.
     * @param keep  true if the buffer should be created/kept, else it will be discarded at the end.
     * If the buffer is kept, the bytes are copied to the destination but still remain in the buffer : PEEK
     * If the buffer is not "empty", the size of the buffer is always adjusted such that the buffer is always of the
     * size of valid bytes got from the stream.
     * 
     * @return number of bytes read. (If an exception occurs, return the exception to the caller).
     * 
     * @throws Exception any exception from operations on the underlying stream.
     */
    public int read(byte[]result,boolean keep) throws Exception
    {
        if (this.empty)                                         // I have no buffer
        {
            if (!keep)                                          // and I don't need to create one, straight through operation
              return this.rawIn.read(result);
            else                                                // I must create and keep the buffer
            {
                int gotten =this.rawIn.read(result);            // read bytes into destination, if exception return to caller
                if (gotten>0)
                {
                    if (gotten!=this.mySize)                    // I got less bytes that expected, make my buffer msaller
                    {
                        this.mySize=gotten;
                        this.buffer=new byte[gotten]; 
                    }
                    System.arraycopy(result, 0,  this.buffer,0,gotten ); // copy bytes into buffer
                    this.empty=false;                               // signal that a buffer exists.
                }
                return gotten;                                      // if no byte got, there will still be no buffer!
            }
        }
        
        /*
         * Here I have an existing buffer, I must either keep it or discard it.
         */
        else                                                    // I have an existing buffer
        {
            int resSize=result.length;                          // nbr of bytes to get
            if (keep)                                           // I must read from my buffer AND keep it
            {
                if (resSize<=mySize)                            // both arrays are of the same size, or I'm bigger; I keep my buffer;
                {
                    System.arraycopy( this.buffer, 0, result, 0, resSize );// just bytes copy accross from my exisitng buffer
                    return resSize;                             // and return nbr of bytes got
                }
                
                else                                            // my buffer is smaller than the requested buffer, and I'm
                                                                // supposer to keep my buffer.
                {
                    System.arraycopy(this.buffer, 0, result, 0, this.mySize);// copy my existing bytes from my current buffer
                    int gotten;
                    try                                         // try to get more bytes, no prob if I can't
                    {
                        gotten=this.rawIn.read(result,this.mySize,resSize-this.mySize);
                    }
                    catch (Exception e)                         // if any error occurs, just return my already copied bytes.
                    {
                        gotten=-1;
                    }
                    if (gotten>0)
                    {
                        this.mySize+=gotten;                    // new size of my buffer
                        this.buffer =new byte[this.mySize];     // make new buffer of new size
                        System.arraycopy(result, 0, this.buffer, 0, this.mySize);// fill my buffer from result
                    }
                    return this.mySize;
                }
            }
            else                                                // do operation & discard my buffer
            {
                if (resSize==mySize)                            // both arrays are of the same size and I discard my buffer
                {
                    System.arraycopy( this.buffer, 0, result, 0, this.mySize );
                    this.empty=true;                            // pretend my buffer is now empty
                    return this.mySize;                         // nbr of bytes got
                } 
                
                if (resSize<this.mySize)                        // my buffer is bigger than the requested buffer and I'm to
                                                                // discard what I have read : make my buffer smaller and 
                                                                // keep the remainder
                {
                    System.arraycopy(this.buffer, 0, result, 0, resSize );// copy requested bytes only
                    this.mySize-=resSize;                       // size of new buffer for remaining bytes
                    byte[]remainder=new byte[this.mySize];      // buffer for remaining bytes
                    System.arraycopy(this.buffer, resSize, remainder, 0, this.mySize);
                    this.buffer=remainder;                      // remainder is new buffer
                    return resSize;                             // DO NOT DISCARD REMAINING BUFFER!
                }
                else                                            // my buffer is smaller than the requested buffer, and I'm
                                                                // supposed to discard my buffer.
                {
                    System.arraycopy( this.buffer, 0, result, 0, this.mySize);// copy existing bytes into result
                    int gotten;
                    try                                         // try to get more bytes
                    {
                        gotten=this.rawIn.read(result,this.mySize,resSize-this.mySize); //read remainong bytes from stream
                        if (gotten<0)
                            gotten=0;                           // but there were no more
                    }
                    catch (Exception e)                         // If I culdn't get more bytes, just use the ones from the buffer
                    {
                        gotten=0;
                    }
                    this.empty=true;                            // I don't have a buffer any more
                    return this.mySize+gotten;                  // return nbr of bytes read
                }   
            }
        }
    }
    /*
    public boolean available()
    {
        if (!this.empty)
            return true;
        else
        {
            try
            {
                return (this.rawIn.available()!=0);
            }
            catch (Exception e)
            {
                return false;
            }
        }     
    }
    */
}
