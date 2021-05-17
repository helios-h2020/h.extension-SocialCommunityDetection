package eu.h2020.helios_social.extension.socialcommunitydetection;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.messaging.ReliableHeliosMessagingNodejsLibp2pImpl;

public class SocialCommunityDetection {

    public static final String PROTOCOL_NAME = "/protocol/social_community_detection";
    /**
     * controls if the module is considered running
     */
    private boolean running=false;

    /**
     * A representation of the community structure
     */
    Map<Context, List<HashSet<Node>>> communities=null;

    /**
     * A reference to the CENListener
     */
    SocialCommunityCENListener cenListener=null;

    /**
     * A reference to the CEN
     */
    ContextualEgoNetwork contextualEgoNetwork=null;

    /**
     * A reference to the messagingModule
     */
    ReliableHeliosMessagingNodejsLibp2pImpl messagingModule=null;

    /**
     * Call this method to start the module. It is suggested not to call this method from a UI thread.
     * @param cen The {@link ContextualEgoNetwork} on which the module should compute the community structure
     * @param reliableHeliosMessagingNodejsLibp2p An already correctly initialized {@link ReliableHeliosMessagingNodejsLibp2pImpl}. The id of the nodes in the {@link ContextualEgoNetwork} should be the same addesses used in {@link ReliableHeliosMessagingNodejsLibp2pImpl}.
     * @return true if the module started correctly
     */
    public synchronized boolean startModule(ContextualEgoNetwork cen, ReliableHeliosMessagingNodejsLibp2pImpl reliableHeliosMessagingNodejsLibp2p){
//      if already running, return false
        if (running) return false;
//      if parameters are null, return false
        if (cen==null) return false;
        contextualEgoNetwork=cen;
        if(reliableHeliosMessagingNodejsLibp2p==null) return false;
        messagingModule=reliableHeliosMessagingNodejsLibp2p;

//      setup and start the started thread
        SocialCommunitySetupper setupper=new SocialCommunitySetupper(this);
        setupper.start();

        running=true;
        return true;
    }

    /**
     * Checks if this instance of the module has already been already started
     * @return true if the module has been started and is now running
     */
    public synchronized boolean isStarted() {
        return running;
    }

    /**
     * Stops this instance of the module if it was running. Stopping the module will dispose all internal data structures: do not attempt to call getCommunities() after stopping the module.
     * @return true if the module was successfully stopped
     */
    public synchronized boolean stopModule(){
        running=false;

//      callback detachment
        cenListener.setIdleCENListener(); // cannot detach =(, so make it idle!
        messagingModule.removeReceiver(PROTOCOL_NAME);

//      data structures cleanup
        communities.clear();
        contextualEgoNetwork=null;
        return true;
    }

    /**
     * Call this method to return the current community structure in the requested context.
     * Only works if the module is running.
     * @return the community structure. In case there is no community structure or the module is not running, returns null
     */
    public synchronized List<HashSet<Node>> getCommunities(@NonNull Context context){
        if (!running) return null;
        try {
            if (Objects.requireNonNull(communities.get(context)).size()==0) return null;
            return Collections.unmodifiableList(Objects.requireNonNull(communities.get(context)));
        } catch (NullPointerException e){
            return null;
        }
    }
}