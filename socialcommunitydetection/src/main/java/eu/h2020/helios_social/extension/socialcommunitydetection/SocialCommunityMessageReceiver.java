package eu.h2020.helios_social.extension.socialcommunitydetection;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;

/**
 * Implementation of the {@link HeliosMessagingReceiver} for the Social Community Detection module
 */
class SocialCommunityMessageReceiver implements HeliosMessagingReceiver {

    /**
     * reference to the module
     */
    private final SocialCommunityDetection module;

    /**
     * Constructor to pass references to relevant data structures
     */
    SocialCommunityMessageReceiver(SocialCommunityDetection m){
        module=m;
    }

    @Override
    public void receiveMessage(@NonNull HeliosNetworkAddress heliosNetworkAddress, @NonNull String s, @NonNull FileDescriptor fileDescriptor) {

        try {
            FileInputStream fis = new FileInputStream(fileDescriptor);
            int size = fis.available();
            byte[] bytes = new byte[size];
            if(fis.read(bytes)>0) receiveMessage(heliosNetworkAddress, s, bytes);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void receiveMessage(@NonNull HeliosNetworkAddress heliosNetworkAddress, @NonNull String s, @NonNull byte[] bytes) {

//        rebuild the message object
        InternalMessage received=InternalMessage.parseMessage(bytes);
//        return if the message was not created correctly
        if (received==null) return;
//        search the alter who sent this message
        Node sender=null;
        for (Node alter:module.contextualEgoNetwork.getAlters()) {
            if(alter.getId().compareTo(received.payload)==0){
                sender=alter;
                break;
            }
        }
//        if not found, return
        if (sender==null) return;

//        using a switch-case for simplicity
        switch (received.type){
            case PING:

//                reply with a pong
                byte[] pongMessage=InternalMessage.createMessage(InternalMessage.Type.PONG, module.contextualEgoNetwork.getEgo().getId());
                HeliosNetworkAddress address = new HeliosNetworkAddress();
                address.setNetworkId(sender.getId());
                if (pongMessage!=null)
                    module.messagingModule.sendTo(address, SocialCommunityDetection.PROTOCOL_NAME, pongMessage);

//                here I use no break to avoid repeating the same code. Indeed the difference between a ping and a pong is that a PING must reply with a PONG, but the rest is the same
            case PONG:

//                update status
                module.status.put(sender, Boolean.TRUE);

//                update communities
                boolean added=false;
                for(Context context:module.contextualEgoNetwork.getContexts()){
                    try {
                        for (Community community : Objects.requireNonNull(module.communities.get(context))) {
                            if (community.alterJoin(sender)) {
                                added = true;
                            }
                        }
//                        if the node was not added in any community of that context, try to make a new one
                        if (!added){
                            Community newCommunity=new Community(module.contextualEgoNetwork, context);
                            if(newCommunity.tryFormCommunity(module.status, sender)){
//                                if the community was formed
                                Objects.requireNonNull(module.communities.get(context)).add(newCommunity);
                            }
                        }
//                        reset control variable
                        added=false;
                    } catch (NullPointerException ignore) { }
                }
                break;

            case OFFL:

//                update status
                module.status.put(sender, Boolean.FALSE);

//                update communities
                for(Context context:module.contextualEgoNetwork.getContexts()){
                    try {
                        Set<Set<Node>> shards= new HashSet<>();
                        for (Community community : Objects.requireNonNull(module.communities.get(context))) {
                            community.alterLeave(sender);
                            if (community.wasSplit()){
                                Iterator<Set<Node>> iterator=community.getShards();
                                while(iterator.hasNext()){
                                    shards.add(iterator.next());
                                    iterator.remove();
                                }
                            }
                        }
                        for(Set<Node> shard : shards){
                            Community newcom=new Community(module.contextualEgoNetwork, context, shard);
                            Objects.requireNonNull(module.communities.get(context)).add(newcom);
                        }
                    } catch (NullPointerException ignore) { }
                }

                break;
            default:
//                unknown message type
                break;
        }

    }
}
