package aa.race.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

// This class is only used in PULL mode. It represents a pull server which listens at a port for a pull client
// When the client sends a request, messages from the message buffer are retrieved & sent back to the client.
public class PullServer extends Thread {

    private long exptStartTime;

    // private attributes will be set in the constructor
    private MessageBuffer msgBuffer;
    private int portOfServer;
    private int period;

    // private static Lock reentrantLock = new ReentrantLock();

    // Constructor
    public PullServer(MessageBuffer msgBuffer, int portOfServer, int period) {
        this.msgBuffer = msgBuffer;
        this.portOfServer = portOfServer;
        this.period = period;
    }

    // returns true when its time to stop this whole thing
    private boolean exptTimeUp() {
        return (new Date().getTime() - exptStartTime >= (period * 1000));
    }

    // Run method
    public void run() {
        exptStartTime = (new Date()).getTime();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portOfServer);
            System.out.println("Pull Server started...Listening at port " + portOfServer);
            Socket clientSocket = null;
            clientSocket = serverSocket.accept(); // accept the first pull client that attempt to etablish a connection

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String outputLine;

            // while time is not up and pull client got send something which is "request" see pull client source code.
            while (!exptTimeUp() & in.readLine() != null) {
                // exit if period for experiment is up
                if (exptTimeUp())
                    break;

                //reentrantLock.lock();
                outputLine = msgBuffer.getWholeMsgAndClear(); // everyone take from the same message buffer.
                //reentrantLock.unlock();

                // send empty string if outputLine is null
                out.println(outputLine == null ? "" : outputLine);
            }
            // Clean up
            out.close();
            in.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("*** FATAL ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}