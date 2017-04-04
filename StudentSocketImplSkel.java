package com.company;

import java.net.*;
import java.io.*;
import java.util.Timer;

class StudentSocketImpl extends BaseSocketImpl {

    // SocketImpl data members:
    //   protected InetAddress address;
    //   protected int port;
    //   protected int localport;

    private Demultiplexer D;
    private Timer tcpTimer;
    private String hostname;
    private boolean server = true;
    private String state = "CLOSED";
    private int seqNum = 1;
    private int ackNum = 0;


    public static final String UDPPORT = "UDPPORT";


    StudentSocketImpl(Demultiplexer D) {  // default constructor
        this.D = D;
        if (System.getProperty("UDPPORT") == null) {
            System.err.println("Must set "+UDPPORT+" to use with "+
                    "-DUDPPORT <num>");
            System.exit(1);
        }

        port = Integer.parseInt(System.getProperty("UDPPORT"));
        if (!server) {
            try {
                this.connect(hostname, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Connects this socket to the specified port number on the specified host.
     *
     * @param      address   the IP address of the remote host.
     * @param      port      the port number.
     * @exception  IOException  if an I/O error occurs when attempting a
     *               connection.
     *
     * goal: call for an active open (client) to connect to address:port
     *
     */
    public synchronized void connect(InetAddress address, int port) throws IOException{
        localport = D.getNextAvailablePort();
        server = false;
        this.port = port;
        this.address = address;
        D.registerConnection(address, localport, this.port, this);
        byte[] packet = new byte[0];
        TCPPacket syn = new TCPPacket(localport, port, seqNum, ackNum, false, true, false, 100, packet);
        TCPWrapper.send(syn, address);
        seqNum++;
        ackNum++;
        state = "SYN_SENT";
    }

    /**
     * Called by Demultiplexer when a packet comes in for this connection
     * @param p The packet that arrived
     *
     * goal: must register with demultiplexer first
     */
    public synchronized void receivePacket(TCPPacket p){

        System.out.println(p.toString());

        // case 1: server connection listening for first SYN from client
        if (server && state.equals("CLOSED") && p.synFlag == true && p.ackFlag == false) {
            seqNum++;

            // unregister listening socket, then register connection
            try {
                D.unregisterListeningSocket(localport, this);
            } catch (Exception e) {

            }
            try {
                D.registerConnection(p.sourceAddr, p.destPort, p.sourcePort, this);
            } catch (Exception e) {
                System.out.println("Failed to register connection after unregistering listening socket");
                System.exit(1);
            }

            // set state to "SYN_RCVD"
            state = "SYN_RCVD";

            // sent SYN+ACK
            byte[] packet = p.getData();
            TCPPacket synack = new TCPPacket(localport, port, seqNum, ackNum, true, true, false, 100, packet);
            TCPWrapper.send(synack, address);

            ackNum++;
        }

        // client receives SYN+ACK
        if (p.ackFlag == true && p.synFlag == true && state.equals("SYN_SENT")) {
            seqNum++;

            // state: Established
            state = "ESTABLISHED";

            // send ACK
            byte[] packet = p.getData();
            TCPPacket ack = new TCPPacket(localport, port, seqNum, ackNum, true, false, false, 100, packet);
            TCPWrapper.send(ack, address);

            ackNum++;
        }

        // client receives FIN
        if (p.finFlag == true && state.equals("ESTABLISHED")) {
            seqNum++;
            // state: CLOSE_WAIT
            state = "CLOSE_WAIT";

            // send: ACK
            byte[] packet = p.getData();
            TCPPacket ack = new TCPPacket(localport, port, seqNum, ackNum, true, false, false, 100, packet);
            TCPWrapper.send(ack, address);

            ackNum++;
        }


    }

    /**
     * Waits for an incoming connection to arrive to connect this socket to
     * Ultimately this is called by the application calling
     * ServerSocket.accept(), but this method belongs to the Socket object
     * that will be returned, not the listening ServerSocket.
     * Note that localport is already set prior to this being called.
     *
     * goal: waits for incoming connection to connect to this socket to. ultimately called by the app calling
     * ServerSocket.accept(), but belongs to socket object that will be returned, not the listening ServerSocket
     */
    public synchronized void acceptConnection() throws IOException {
        D.registerListeningSocket(localport, this);
    }


    /**
     * Returns an input stream for this socket.  Note that this method cannot
     * create a NEW InputStream, but must return a reference to an
     * existing InputStream (that you create elsewhere) because it may be
     * called more than once.
     *
     * @return     a stream for reading from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *               input stream.
     */
    public InputStream getInputStream() throws IOException {
        // project 4 return appIS;
        return null;

    }

    /**
     * Returns an output stream for this socket.  Note that this method cannot
     * create a NEW InputStream, but must return a reference to an
     * existing InputStream (that you create elsewhere) because it may be
     * called more than once.
     *
     * @return     an output stream for writing to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *               output stream.
     */
    public OutputStream getOutputStream() throws IOException {
        // project 4 return appOS;
        return null;
    }


    /**
     * Closes this socket.
     *
     * @exception  IOException  if an I/O error occurs when closing this socket.
     *
     * goal: close the connection. called by the application
     */
    public synchronized void close() throws IOException {
        if (server) {
            // close() called on server side
            // state: FIN_WAIT_1
            state = "FIN_WAIT_1";

            // send FIN
            byte[] packet = new byte[0];
            TCPPacket fin = new TCPPacket(localport, port, seqNum, ackNum, false, false, true, 100, packet);
            TCPWrapper.send(fin, address);
            seqNum++;

            // either receive FIN or receive ACK
            state = "FIN_WAIT_2";
            ackNum++;

            // wait 30 sec
            Object wait = new Object();
            TCPTimerTask timer = createTimerTask(30000, wait);
            timer.run();

            // unregister connection and close
            D.unregisterConnection(address, port, localport, this);

            handleTimer(wait);
            state = "CLOSED";
        }
        else {
            // close() called on client side
            // send FIN
            byte[] packet = new byte[0];
            TCPPacket fin = new TCPPacket(localport, port, seqNum, ackNum, false, false, true, 100, packet);
            TCPWrapper.send(fin, address);
            seqNum++;

            // state = "LAST_ACK"
            state = "LAST_ACK";

            // recv. ACK
            ackNum++;

            // state = "TIME_WAIT"
            state = "TIME_WAIT";

            // wait 30 secs
            Object wait = new Object();
            TCPTimerTask timer = createTimerTask(30000, wait);
            timer.run();

            // unregister connection and close
            D.unregisterConnection(address, port, localport, this);

            handleTimer(wait);
            state = "CLOSED";
        }
    }

    /**
     * create TCPTimerTask instance, handling tcpTimer creation
     * @param delay time in milliseconds before call
     * @param ref generic reference to be returned to handleTimer
     */
    private TCPTimerTask createTimerTask(long delay, Object ref){
        if(tcpTimer == null)
            tcpTimer = new Timer(false);
        return new TCPTimerTask(tcpTimer, delay, this, ref);
    }


    /**
     * handle timer expiration (called by TCPTimerTask)
     * @param ref Generic reference that can be used by the timer to return
     * information.
     *
     * goal: handle timer event, called by TCPTimerTask. ref is a generic pointer that you can use to pass data
     *            back to this routine when the timer expires
     */
    public synchronized void handleTimer(Object ref){

        // this must run only once the last timer (30 second timer) has expired
        tcpTimer.cancel();
        tcpTimer = null;
    }
}
