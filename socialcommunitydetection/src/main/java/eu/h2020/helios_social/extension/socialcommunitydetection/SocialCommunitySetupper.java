package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

public class SocialCommunitySetupper extends Thread{

    /**
     * A reference to the module
     */
    SocialCommunityDetection module=null;


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

//        TODO: set up (other) data structures
//      set up data structures
        module.communities=new HashMap<>();
        for(Context context : module.contextualEgoNetwork.getContexts()) {
            module.communities.put(context, new LinkedList<>());
        }
//        TODO: set up messaging callbacks

//      create the cen listener and attach it to the cen if necessary, then activate it
        if(module.cenListener==null){
            module.cenListener=new SocialCommunityCENListener();
            module.contextualEgoNetwork.addListener(module.cenListener);
        }
        module.cenListener.setActiveCENListener();


//        TODO: send messages

    }


}
