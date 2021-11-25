package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class model a message used by the protocol
 *
 *  @author Andrea Michienzi (andrea.michienzi@di.unipi.it)
 *  @author Barbara Guidi (guidi@di.unipi.it)
 *  @author Laura Ricci (laura.ricci@unipi.it)
 *  @author Fabrizio Baiardi (f.baiardi@unipi.it)
 */
class InternalMessage {

    /**
     * type of this instance of the message
     */
    Type type;

    /**
     * payload of this instance of the message
     */
    String payload;

    /**
     * Type of the messages sent by the protocol. The type must be always 4 bytes (the parser method parseMessage() relies on this).
     */
    enum Type{

        PING,	// used to notify online transition
        PONG,	// used to reply to a PING message
        OFFL,   // used to notify an offline transition

    }

    /**
     * Private constructor
     */
    private InternalMessage(){}

    /**
     * Factory method to create messages
     * @param t The type of the message
     * @param p The payload to be added to the message
     * @return a serialized message ready to be sent, returns null if the message was not created
     */
    static byte[] createMessage(Type t, String p){

        ByteArrayOutputStream outstream=new ByteArrayOutputStream();
        try {
            outstream.write(t.toString().getBytes(StandardCharsets.UTF_8));
            outstream.write(p.getBytes(StandardCharsets.UTF_8));
            byte[] message=outstream.toByteArray();
            outstream.close();
            return message;
        } catch (IOException e){
            return null;
        }
    }

    /**
     *
     * @param serializedMessage the byte array returned by the messaging module
     * @return an object with type and payload created from the serialized message passed as input. Returns null if the passed parameter is badly formatted
     */
    static InternalMessage parseMessage(byte[] serializedMessage){

        InternalMessage message=new InternalMessage();
        ByteArrayInputStream instream=new ByteArrayInputStream(serializedMessage);

//      read msg type
        byte[] msgtype=new byte[4];
        int b=instream.read(msgtype, 0, 4);
        if(b!=4) return null;
        String t=new String(msgtype, StandardCharsets.UTF_8);
        try{
            message.type=InternalMessage.Type.valueOf(t);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            return null;
        }
//      read msg payload
        byte[] msgpay=new byte[instream.available()];
        b=instream.read(msgpay, 0, instream.available());
        if (b>0) {
            message.payload = new String(msgpay, StandardCharsets.UTF_8);
        } else{
            message.payload=null;
        }

        return message;
    }
}
