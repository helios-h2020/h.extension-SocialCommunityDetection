package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

public class SocialCommunityDetection {

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
     * Call this method to start the module. It is suggested not to call this method from a UI thread.
     * @return true if the module started correctly
     */
    public synchronized boolean startModule(ContextualEgoNetwork cen){
//      if already running, return false
        if (running) return false;
//      if parameters are null, return false
//      TODO: add the messaging module reference when doubts are clarified
        if (cen==null) return false;
        contextualEgoNetwork=cen;

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
//      TODO: dispose threads and stuff
//      callback detachment
        cenListener.setIdleCENListener(); // cannot detach =(, so make it idle!
//        TODO: detach messagin callbacks
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
    public synchronized List<HashSet<Node>> getCommunities(Context context){
        if (!running) return null;
        if (context==null) return null;
        if (communities.get(context).size()==0) return null;
        return Collections.unmodifiableList(communities.get(context));
    }
}
