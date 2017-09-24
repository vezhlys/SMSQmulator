
package smsqmulator;

/**
 * A fixed size true circular LIFO buffer. Elements are added to the tail of the buffer.
 * If the buffer is full, and a new element is added, the head element is deleted (without generating any error!!!!),
 * all other elements are "shifted up" by one and the new element is added at the new tail.
 * <p>
 * Thus, elements are never removed from the buffer, unless by an add operation.
 * <P>
 * Null elements are never added to the buffer, but trying to do so will NOT generate any kind of error - they are just silently ignored.<p>
 * The buffer is backed by an array of element T. A buffer must have at least a capacity of 1 element.
 * 
 * @author and copyright (c) 2013-2015 Wolfgang Lenerz.
 * @version 
 * 1.01 number of elements in buffer never exceeds capacity.
 * @param <T> the objects in the buffer.
 */
public class CircularBuffer <T> 
{
    private final T[] buffer;                                   // the array backing this object
    private int tail;                                           // points to the last element inserted   
    private final int capacity;                                 // capacity of this buffer
    private int elements;                                       // number of elements in the buffer
    
    /**
     * Creates the buffer with a capacity of 10 elements.
     */
    public CircularBuffer()
    {
        this (10);
    }

    /**
     * Creates the buffer with a capacity of n elements.
     * 
     * @param n the number of elements the buffer is to contain.
     * 
     * @throws IllegalArgumentException if the the number of elements the buffer is to have is smaller than 1.
     */
    @SuppressWarnings("unchecked")
    public CircularBuffer(int n) throws IllegalArgumentException
    {
        if (n<1)
            throw new IllegalArgumentException();
        this.buffer = (T[]) new Object[n];
        this.capacity = n;
        this.tail = 0;
        this.elements = 0;
    }

    /** 
     * Adds an element (to the end of) the buffer. If buffer would be full, the first element is discarded.
     * 
     * <code>NULL</code> elements are NOT added to the buffer.
     * 
     * @param newElement the element to add. NULL elements are NOT added to the buffer.
     */
    public void add(T newElement) 
    {
        if (newElement==null)
            return;
        this.tail++;                                            // where element is to be inserted
        if (this.tail==this.capacity)                           // if we reach the end switch back to start of buffer
            this.tail=0;
        else
            this.elements++;                                    // keep track of how many elements there are in the buffer
        if (this.elements>this.capacity)
            this.elements=this.capacity;
        this.buffer[this.tail] = newElement;                    // insert element
    }
    
    /** 
     * Adds an element to the end of the buffer, if this is NOT equal to the same element already at the end of the buffer.
     * What constitutes an equal element depends of the type of object in this circular buffer.<p>
     * If buffer would be full, the first element is discarded.
     * 
     * @param newElement the element to add. NULL elements are NOT added to the buffer.
     */
    public void addNoDoubles(T newElement) 
    {
        if (newElement==null)
            return;
        if (this.buffer[this.tail]!=null && newElement.equals(this.buffer[this.tail]))
            return;
        add (newElement);
    }
    
    /**
     * Gets the last element of this buffer. This may be null if and only if the buffer is empty.
     * 
     * @return the last element of this buffer. This will be null if and only if the buffer is empty.
     */
    public T getlast() 
    {
        return this.buffer[this.tail];
    }
    
    /**
     * Gets the nth element of this buffer, counting from the TAIL. 
     * 
     * This may be null if and only if the element at his place in buffer doesn't exist (which will be the case if the buffer is empty).
     * @param elementNumber the number of the element to get. This is counted from the tail upwards, i.e. 0 = tail element, 1 = element before that etc.
     * 
     * @return the element required. This will be null if this element doesn't existing (also if buffer is empty).
     */
    public T get(int elementNumber) 
    {
        if (elementNumber>this.elements)
            return null;                                        // element doesn't exist.
        int counter=this.tail-elementNumber;
        if (counter<0)
            counter=this.capacity - counter;
        return this.buffer[counter];
    }
    
    @Override
    public String toString() 
    {
        return "CircularBuffer (size= " + this.capacity + "Number of elements already in buffer= "+this.elements+")";
    }   
}
