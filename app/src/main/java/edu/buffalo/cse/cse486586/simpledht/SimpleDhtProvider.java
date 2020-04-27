package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

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
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    private static boolean queryFlag = false;
    Map<String, ?> messageGlobal;
    String singleMessageGlobal;


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

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

        if (values == null || values.size() < 1) {
            return null;
        }
        String key = values.getAsString(KEY_FIELD);
        String value = values.getAsString(VALUE_FIELD);

        try {
            String keyHash = genHash(key);
            String selfPortHash = genHashForPort(myPort);
            String succPortHash = genHashForPort(succPort);

            if (keyHash.compareTo(selfPortHash) > 0
                    && keyHash.compareTo(succPortHash) > 0
                    && succPortHash.compareTo(selfPortHash) > 0) {
                sendInsertRequestToSucc(values, succPort);
            } else {
                SharedPreferences sharedPref = getContext().getSharedPreferences(key, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(key, value);
                editor.commit();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
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

        MatrixCursor cursor = null;
        String[] columnNames = new String[]{KEY_FIELD, VALUE_FIELD};
        SharedPreferences sharedPref = getContext().getSharedPreferences(selection, Context.MODE_PRIVATE);

        if (selection.equals("@")) {
            messageGlobal = sharedPref.getAll();
            //Log.d("query debug", selection + ":" + message);
        } else if (selection.equals("*")) {
            messageGlobal = sharedPref.getAll();
            MessageDht messageDht = new MessageDht();
            messageDht.setSelfPort(myPort);
            messageDht.setToPort(succPort);
            messageDht.setQueryContent(messageGlobal);
            messageDht.setMsgType(MessageDhtType.QUERYGLOBAL);
            sendGlobalMessageQuery(messageDht);
            queryFlag = false;
            while (queryFlag) {

            }

        } else {

            String message = sharedPref.getString(selection, "0");
            if (message != null) {
                Log.d("query debug", selection + ":" + message);

                String[] columnValues = new String[]{selection, message};
                cursor = new MatrixCursor(columnNames, 1);
                try {
                    if (getContext() == null) return cursor;
                    cursor.addRow(columnValues);
                    cursor.moveToFirst();
                } catch (Exception e) {
                    cursor.close();
                    throw new RuntimeException(e);
                }
                Log.v("query", selection);
                queryFlag = false;
                return cursor;
            } else {
                MessageDht msg = new MessageDht();
                msg.setQueryKey(selection);
                msg.setToPort(succPort);
                msg.setMsgType(MessageDhtType.QUERYSINGLE);
                sendSingleMessageQuery(msg);
                queryFlag = false;
                while (queryFlag) {
                }
            }

            String[] columnValues = new String[]{selection, singleMessageGlobal};
            cursor = new MatrixCursor(columnNames, 1);
            try {
                if (getContext() == null) return cursor;
                cursor.addRow(columnValues);
                cursor.moveToFirst();
            } catch (Exception e) {
                cursor.close();
                throw new RuntimeException(e);
            }
            Log.v("query", selection);
            queryFlag = false;
            return cursor;
        }
        if (messageGlobal != null) {
            String[] columnValues = new String[2];
            cursor = new MatrixCursor(columnNames, messageGlobal.size());
            if (getContext() == null) return cursor;
            for (String key : messageGlobal.keySet()) {
                columnValues[0] = key;
                columnValues[1] = (String) messageGlobal.get(key);
                cursor.addRow(columnValues);
            }
            try {
                cursor.moveToFirst();
            } catch (Exception e) {
                cursor.close();
                throw new RuntimeException(e);
            }
        }
        Log.v("query", selection);
        queryFlag = false;
        return cursor;
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

                    MessageDht msgReceived = null;
                    try {
                        msgReceived = readMessage(socket.getInputStream());
                        if (msgReceived.getMsgType() == MessageDhtType.JOINREQUEST) {
                            String selfPortHash = genHashForPort(myPort);
                            String reqPortHash = genHashForPort(msgReceived.getSelfPort());
                            String succPortHash = genHashForPort(succPort);
                            if ((reqPortHash.compareTo(selfPortHash) > 0 && reqPortHash.compareTo(succPortHash) < 0)
                                    || (reqPortHash.compareTo(selfPortHash) > 0 && reqPortHash.compareTo(succPortHash) > 0 && selfPortHash.compareTo(succPortHash) > 0)
                                    || (reqPortHash.compareTo(selfPortHash) < 0 && reqPortHash.compareTo(succPortHash) < 0 && selfPortHash.compareTo(succPortHash) > 0)) {
                                String succ = succPort;
                                succPort = msgReceived.getSelfPort();
                                sendJoinResponse(msgReceived.getSelfPort(), myPort, succ);
                                sendJoinPreUpdate(succPort, msgReceived.getSelfPort());

                            } else if (selfPortHash.equals(succPortHash)) {
                                succPort = msgReceived.getSelfPort();
                                prePort = msgReceived.getSelfPort();
                                sendJoinResponse(msgReceived.getSelfPort(), myPort, myPort);
                            } else {
                                sendJoinRequest(succPort, msgReceived.getSelfPort());
                            }
                        } else if (msgReceived.getMsgType() == MessageDhtType.JOINRESPONSE) {
                            prePort = msgReceived.getPrePort();
                            succPort = msgReceived.getSuccPort();
                        } else if (msgReceived.getMsgType() == MessageDhtType.JOINUPDATE) {
                            prePort = msgReceived.getPrePort();
                        } else if (msgReceived.getMsgType() == MessageDhtType.INSERT) {
                            try {
                                ContentValues values = msgReceived.getContentValues();
                                insert(mUri, values);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else if(msgReceived.getMsgType() == MessageDhtType.QUERYGLOBAL){
                            

                        }else if(msgReceived.getMsgType() == MessageDhtType.QUERYSINGLE){

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

    private void sendGlobalMessageQuery(MessageDht msg) {
       // new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);

    }

    private void sendSingleMessageQuery(MessageDht msg) {
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);

    }

    private void sendJoinRequest(String toPort, String fromPort) {
        if (myPort.equals("11108")) {
            succPort = myPort;
            prePort = myPort;
        } else {
            MessageDht msg = new MessageDht();
            msg.setMsgType(MessageDhtType.JOINREQUEST);
            msg.setToPort(toPort);
            msg.setSelfPort(fromPort);
            //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            clientSocket(msg);
        }

    }

    private void sendJoinResponse(String toPort, String pre, String succ) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.JOINRESPONSE);
        msg.setToPort(toPort);
        msg.setPrePort(pre);
        msg.setSuccPort(succ);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);
    }

    private void sendJoinPreUpdate(String toPort, String pre) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.JOINUPDATE);
        msg.setToPort(toPort);
        msg.setPrePort(pre);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);
    }

    private void sendInsertRequestToSucc(ContentValues values, String succPort) {
        MessageDht msg = new MessageDht();
        msg.setMsgType(MessageDhtType.INSERT);
        msg.setToPort(succPort);
        //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
        clientSocket(msg);
    }


    private void clientSocket(MessageDht msg){
        try {
        Socket socket;
        //if (msgs[0].getMsgType() == MessageDhtType.JOINRESPONSE) {
        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                Integer.parseInt(msg.getToPort()));
        sendMessage(socket.getOutputStream(), msg);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private class ClientTask extends AsyncTask<MessageDht, Void, Void> {

        @Override
        protected Void doInBackground(MessageDht... msgs) {
            try {
                Socket socket;
                //if (msgs[0].getMsgType() == MessageDhtType.JOINRESPONSE) {
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[0].getToPort()));
                sendMessage(socket.getOutputStream(), msgs[0]);
//                } else if (msgs[0].getMsgType() == MessageDhtType.JOINREQUEST) {
//                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                            Integer.parseInt(msgs[0].getToPort()));
//                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2]));
//                } else if (msgs[0].getMsgType() == MessageDhtType.JOINUPDATE) {
//                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                            Integer.parseInt(msgs[0].getToPort()));
//                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2]));
//                } else if (msgs[0].getMsgType() == MessageDhtType.INSERT) {
//                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//                            Integer.parseInt(msgs[0].getToPort()));
//                    sendMessage(socket.getOutputStream(), msgs[0].concat(":" + msgs[2]));
//                }
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

    private void sendMessage(OutputStream out, MessageDht msgToSend) throws Exception {
        ObjectOutputStream dOut = new ObjectOutputStream(out);
        dOut.writeObject(msgToSend);
        dOut.flush();
    }

    private MessageDht readMessage(InputStream in) throws Exception {
        ObjectInputStream dIn = new ObjectInputStream(in);
        MessageDht msgFromServer = (MessageDht) dIn.readObject();
        return msgFromServer;
    }


}
