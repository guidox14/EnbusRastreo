package com.movitrak.enbusrastreo;

import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by Adrian on 23/01/2018.
 */

public class NetClient {
    /**
     * Maximum size of buffer
     */
    public static final int BUFFER_SIZE = 1024;
    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private String host = null;
    private int port = 4000;

    /**
     * Constructor with Host, Port and MAC Address
     * @param host
     * @param port
     */
    public NetClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connectWithServer() {
        try {
            socket = null;
            socket = new Socket();
            SocketAddress sockaddr = new InetSocketAddress(this.host, this.port);
            socket.connect(sockaddr, 10000);
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
            return false;
        }
    }

    public boolean chkWithServer() {
        String ack = "ACK:"+System.currentTimeMillis();
        String chk = null;
        sendDataWithString(ack);
        chk = receiveDataFromServer();
        if(chk == null) {
            chk = "";
        }
        if (chk.equals(ack)) {
            return true;
        }
        return false;
    }

    // Trasformato in visible ....tolto private
    public void disConnectWithServer() {
        if (socket != null) {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            } catch (IOException e) {
                Log.e("Error", e.getMessage());
            }
        }
    }

    public void sendDataWithString(String message) {
        if (message != null) {
            out.write(message);
            out.flush();
        }
    }

    public String receiveDataFromServer() {
        String message = "";
        try {
            message = in.readLine();
            return message;
        } catch (IOException e) {
            Log.e("Error", e.getMessage());
            return message;
        }
    }

}
