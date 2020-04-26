package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class SimpleDhtProvider extends ContentProvider {

    static final int SERVER_PORT = 10000;
    static final int JOIN_REQUEST_SERVER = 11108;
    private static final String SERVER_TAG = "Server Log";
    private static final String CLIENT_TAG = "Client Log";
    static String prePort;
    static String succPort;
    static String myPort;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        if(values ==null || values.size()<1){
            return null;
        }
        String key = values.getAsString(KEY_FIELD);
        String value = values.getAsString(VALUE_FIELD);
//        try {
//            if (genHash(key).compareTo(genHashForPort(prePort)))
//
//
//                SharedPreferences sharedPref = getContext().getSharedPreferences(key, Context.MODE_PRIVATE);
//
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString(key, value);
//            editor.commit();
//        }
        Log.v("insert", values.toString());



        return null;
    }

    @Override
    public boolean onCreate() {
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        serverSocketInit();
        sendJoinRequest("11108", myPort);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHashForPort(String input) throws NoSuchAlgorithmException {
        return genHash(String.valueOf((Integer.parseInt(input) / 2)));
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();

                    String msgReceived = null;
                    try {
                        msgReceived = readMessage(socket.getInputStream());
                        String[] msgTokenized = msgReceived.split(":");
                        if (msgTokenized[0].equals("joinRequest")) {
                            String portReq = msgTokenized[1];
                            String selfPortHash = genHashForPort(myPort);
                            String reqPortHash = genHashForPort(portReq);
                            String succPortHash = genHashForPort(succPort);
                            if ((reqPortHash.compareTo(selfPortHash) > 0 && reqPortHash.compareTo(succPortHash) < 0)
                                    || (reqPortHash.compareTo(selfPortHash) > 0 && reqPortHash.compareTo(succPortHash) > 0 && selfPortHash.compareTo(succPortHash) > 0)
                                    || (reqPortHash.compareTo(selfPortHash) < 0 && reqPortHash.compareTo(succPortHash) < 0 && selfPortHash.compareTo(succPortHash) > 0)) {
                                String succ = succPort;
                                succPort = portReq;
                                sendJoinResponse(portReq, myPort, succ);
                                sendJoinPreUpdate(succPort, portReq);

                            } else {
                                sendJoinRequest(succPort, portReq);
                            }
                        } else if (msgTokenized[0].equals("joinResponse")) {
                            prePort = msgTokenized[1];
                            succPort = msgTokenized[2];
                        } else if (msgTokenized[0].equals("joinPreUpdate")) {
                            prePort = msgTokenized[1];
                        }
                    } catch (Exception e) {
                        Log.d(SERVER_TAG, "socket timeout for server while reading");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void sendJoinRequest(String toPort, String fromPort) {
        if (myPort.equals("11108")) {
            succPort = myPort;
            prePort = myPort;
        } else {
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "joinRequest", toPort, fromPort);
        }

    }

    private void sendJoinResponse(String toPort, String pre, String succ) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "joinResponse", toPort, pre, succ);
    }

    private void sendJoinPreUpdate(String toPort, String pre) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "joinPreUpdate", toPort, pre);
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket;
                if (msgs[0].equals("joinResponse")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[1]));
                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2] + ":" + msgs[3]));
                } else if (msgs[0].equals("joinRequest")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[1]));
                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2]));
                } else if (msgs[0].equals("joinPreUpdate")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[1]));
                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void serverSocketInit() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(SERVER_TAG, "Can't create a ServerSocket");
            return;
        } catch (Exception e) {
            Log.e(SERVER_TAG, "ServerTask UnknownException");
        }
    }

    private void sendMessage(OutputStream out, String msgToSend) throws IOException, SocketTimeoutException {
        DataOutputStream dOut = new DataOutputStream(out);
        dOut.writeUTF(msgToSend);
        dOut.flush();
    }

    private String readMessage(InputStream in) throws IOException, SocketTimeoutException {
        DataInputStream dIn = new DataInputStream(in);
        String msgFromServer = dIn.readUTF();
        return msgFromServer;
    }


}
