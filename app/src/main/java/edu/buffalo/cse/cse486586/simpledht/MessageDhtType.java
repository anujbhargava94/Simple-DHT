package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;

public enum MessageDhtType implements Serializable {
    JOINREQUEST,
    JOINRESPONSE,
    JOINUPDATE,
    INSERT,
    QUERYGLOBAL,
    QUERYSINGLE,
    QUERYSINGLERESPONSE,
    DELETE
}
