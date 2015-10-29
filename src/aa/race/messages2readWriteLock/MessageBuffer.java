package aa.race.messages2readWriteLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    // Self added
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();

    // Constructor. initializes instance variables
    public MessageBuffer(int maxMsgSize, boolean dropNewCharWhenBufferFull)
    {
        msg = new StringBuffer("");
        noOfDroppedCharSoFar = 0;
        this.maxMsgSize = maxMsgSize;
        this.dropNewCharWhenBufferFull = dropNewCharWhenBufferFull;

    }

    // Our addition for problem 2, if only want the latest just override it!
    public void setMessageBufferText(String newText) {

        writeLock.lock();
        try {
            // We can do msg = new StringBuffer(newText) too but I decided not to do that
            // as logic regarding discard may not be applied in the event if newText contains
            // more characters than the buffer size limit.
            msg.setLength(0);
            appendToBack(newText);
        } finally {
            writeLock.unlock();
        }

    }

    // Append a String to the back of the encapsulated message
    // Note that if the message buffer size is breached, characters will be dropped (discarded)
    // If dropNewCharWhenBufferFull is true, new characters will be dropped
    // If dropNewCharWhenBufferFull is false, the oldest characters will be dropped & new characters "pushed in"
    public void appendToBack(String newText)
    {
        writeLock.lock();
        try {
            // we need to lock even before msg.append(..) because what if 'msg' was cleared right after maxNoOfNewCharToAppend
            // is assigned a value below? That would make things inaccurate and we discard unnecessarily.
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
                // newText is something like 1446024217384~************************************************************~
                msg.append(newText);
                return;
            }

            // dropNewCharWhenBufferFull is false
            // We are going to ignore this since the default is true and I'm not changing it to false.
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
                msg.append(newText);
            }
        } finally {
            writeLock.unlock();
        }

    }

    // Erase everything in the buffer
    public void clear()
    {
        writeLock.lock();
        try {
            msg.setLength(0);
        } finally {
            writeLock.unlock();
        }

    }

    // Return the contents of the buffer as a String or null if there is nothing inside
    public String getWholeMsg()
    {
        readLock.lock();
        try {
            return (msg.length() == 0 ? null : msg.toString());
        } finally {
            readLock.unlock();
        }

    }

    // Similar to getWholeMsg, except that the buffer is cleared after the message is retrieved
    public String getWholeMsgAndClear()
    {
        // this method is not used in problem 2. Ignore it!
        String temp;
        writeLock.lock();
        try {
            temp = msg.toString();

            if (temp.length() == 0) {
                return null;
            }

            System.out.println("returning: " + temp + " then clearing");
            clear(); // write lock inside clear
            return temp;

        } finally {
            writeLock.unlock();
        }
    }

    // Show the contents of the buffer to stdout
    public void print()
    {
        // this method is not used.
        System.out.println("Message Buffer: " + msg);
        System.out.println("Message Buffer contains " + msg.length() + " characters.");
    }

    // Returns true if buffer is empty (i.e. length is zero), returns false otherwise
    public boolean isEmpty()
    {
        // this method is not used.
        return (msg.length() == 0);
    }
}
