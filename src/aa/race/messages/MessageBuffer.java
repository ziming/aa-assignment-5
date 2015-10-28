package aa.race.messages;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

/*
 * Message Buffer class
 * represents the Message Buffer to which the Event Generator will be "writing" to
 */
public class MessageBuffer
{
    private StringBuffer msg; // the actual message being encapsulated
    private int maxMsgSize;   // size of this buffer in number of characters. This size cannot be breached
    private int noOfDroppedCharSoFar;  // a running count of the number of characters which have been discarded because the buffer is full
    private boolean dropNewCharWhenBufferFull; // determines if new characters will push out old characters if an insert is attempted when the buffer is full
    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    // Constructor. initializes instance variables
    public MessageBuffer(int maxMsgSize, boolean dropNewCharWhenBufferFull)
    {
        msg = new StringBuffer("");
        noOfDroppedCharSoFar = 0;
        this.maxMsgSize = maxMsgSize;
        this.dropNewCharWhenBufferFull = dropNewCharWhenBufferFull;
    }

    // Append a String to the back of the encapsulated message
    // Note that if the message buffer size is breached, characters will be dropped (discarded)
    // If dropNewCharWhenBufferFull is true, new characters will be dropped
    // If dropNewCharWhenBufferFull is false, the oldest characters will be dropped & new characters "pushed in"
    public void appendToBack(String newText)
    {
        int maxNoOfNewCharToAppend = maxMsgSize - msg.length();

        // dropNewCharWhenBufferFull is true
        if (dropNewCharWhenBufferFull)
        {
            // buffer is full - whole message dropped
            if (maxNoOfNewCharToAppend <= 0)
            {
                noOfDroppedCharSoFar += newText.length();
                System.out.println("Message Buffer is full - dropping whole message of length: " + newText.length());
                System.out.println("Message Buffer: total number of dropped characters so far: " + noOfDroppedCharSoFar);
                System.out.println("---");
                return;
            }
            // part of new msg dropped
            if (maxNoOfNewCharToAppend < newText.length())
            {
                String charToAppend = newText.substring(0, maxNoOfNewCharToAppend);
                msg.append(charToAppend);
                int noOfCharToDrop = newText.length() - maxNoOfNewCharToAppend;
                noOfDroppedCharSoFar += noOfCharToDrop;
                System.out.println("Message Buffer is full - dropping last " + noOfCharToDrop + " characters in new message");
                System.out.println("Message Buffer: total number of dropped characters so far: " + noOfDroppedCharSoFar);
                System.out.println("---");
                return;
            }
            // whole message is inserted into buffer
            lock.writeLock().lock();
            msg.append(newText);
            lock.writeLock().unlock();

            return;
        }

        // dropNewCharWhenBufferFull is false
        if (!dropNewCharWhenBufferFull)
        {
            msg.append(newText);
            // some characters already in the buffer will be dropped
            if (maxNoOfNewCharToAppend < newText.length())
            {
                int charToCutFrTheFront = msg.length() - maxMsgSize;
                noOfDroppedCharSoFar += charToCutFrTheFront;
                System.out.println("Message Buffer is full - pushing out " + charToCutFrTheFront + " characters already in the buffer.");
                System.out.println("Message Buffer: total number of dropped characters so far: " + noOfDroppedCharSoFar);
                System.out.println("---");

                String newMsg = msg.substring(charToCutFrTheFront, msg.length());
                msg = new StringBuffer(newMsg);
                return;
            }
            // Message buffer size is not breached: whole message is inserted into buffer & life carries on
            lock.writeLock().lock();
            msg.append(newText);
            lock.writeLock().unlock();

        }
    }

    // Erase everything in the buffer
    public void clear()
    {
        if (msg.length() > 0)
            msg.delete(0, msg.length());
    }

    // Return the contents of the buffer as a String or null if there is nothing inside
    public String getWholeMsg()
    {
        return (msg.length() == 0 ? null : msg.toString());
    }

    // Similar to getWholeMsg, except that the buffer is cleared after the message is retrieved
    public String getWholeMsgAndClear() {
        lock.writeLock().lock();
        String temp = msg.toString();
        System.out.println("returning: " + msg + " then clearing");
        //  System.out.println("clearing");
        clear();
        lock.writeLock().unlock();
        return (temp.length() == 0 ? null : temp);
    }

    // Show the contents of the buffer to stdout
    public void print()
    {
        System.out.println("Message Buffer: " + msg);
        System.out.println("Message Buffer contains " + msg.length() + " characters.");
    }

    // Returns true if buffer is empty (i.e. length is zero), returns false otherwise
    public boolean isEmpty()
    {
        return (msg.length() == 0);
    }
}
