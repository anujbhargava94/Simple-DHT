package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;

import java.io.Serializable;
import java.util.Map;

public class MessageDht implements Serializable {


    private Enum<MessageDhtType> msgType;
    private ContentValues contentValues;
    private String selfPort;
    private String prePort;
    private String succPort;
    private String toPort;
    private Map<String, ?> queryContent;
    private String queryKey;

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    public Map<String, ?> getQueryContent() {
        return queryContent;
    }

    public void setQueryContent(Map<String, ?> queryContent) {
        this.queryContent = queryContent;
    }

    public Enum<MessageDhtType> getMsgType() {
        return msgType;
    }

    public void setMsgType(Enum<MessageDhtType> msgType) {
        this.msgType = msgType;
    }

    public String getToPort() {
        return toPort;
    }

    public void setToPort(String toPort) {
        this.toPort = toPort;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }

    public void setContentValues(ContentValues contentValues) {
        this.contentValues = contentValues;
    }

    public String getSelfPort() {
        return selfPort;
    }

    public void setSelfPort(String selfPort) {
        this.selfPort = selfPort;
    }

    public String getPrePort() {
        return prePort;
    }

    public void setPrePort(String prePort) {
        this.prePort = prePort;
    }

    public String getSuccPort() {
        return succPort;
    }

    public void setSuccPort(String succPort) {
        this.succPort = succPort;
    }
}
