package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.io.FileDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;

class SocialCommunitySetupper extends Thread{

    /**
     * A reference to the module
     */
    SocialCommunityDetection module;


    /**
     * Build the setupper thread
     */
    SocialCommunitySetupper(SocialCommunityDetection scd){
        module=scd;
    }

    /**
     * Setup data structures and callbacks
     */
    @Override
    public void run() {

//      set up data structures
        module.communities=new HashMap<>();
        for(Context context : module.contextualEgoNetwork.getContexts()) {
            module.communities.put(context, new LinkedList<>());
        }

//      set up messaging callbacks
        HeliosMessagingReceiver receiver=new SocialCommunityMessageReceiver();
        module.messagingModule.addReceiver(SocialCommunityDetection.PROTOCOL_NAME, receiver);

//      create the cen listener and attach it to the cen if necessary, then activate it
        if(module.cenListener==null){
            module.cenListener=new SocialCommunityCENListener();
            module.contextualEgoNetwork.addListener(module.cenListener);
        }
        module.cenListener.setActiveCENListener();


//      send ping messages
        byte[] pingMessage=InternalMessage.createMessage(InternalMessage.Type.PING, module.contextualEgoNetwork.getEgo().getId());
        for(Node alter:module.contextualEgoNetwork.getAlters()) {
            HeliosNetworkAddress address = new HeliosNetworkAddress();
            address.setNetworkId(alter.getId());
            module.messagingModule.sendTo(address, SocialCommunityDetection.PROTOCOL_NAME, pingMessage);
        }


    }


}
