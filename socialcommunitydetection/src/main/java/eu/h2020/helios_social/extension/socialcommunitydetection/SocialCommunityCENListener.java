package eu.h2020.helios_social.extension.socialcommunitydetection;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetworkListener;
import eu.h2020.helios_social.core.contextualegonetwork.Edge;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

/**
 * This class acts as a wrapper for the CEN listener, changing its behaviour according to the status of the module (idle when the module is not working, active when the module is working)
 *
 *  @author Andrea Michienzi (andrea.michienzi@di.unipi.it)
 *  @author Barbara Guidi (guidi@di.unipi.it)
 *  @author Laura Ricci (laura.ricci@unipi.it)
 *  @author Fabrizio Baiardi (f.baiardi@unipi.it)
 */
class SocialCommunityCENListener implements ContextualEgoNetworkListener {

    private final ContextualEgoNetworkListener activeCENListener;
    private final ContextualEgoNetworkListener idleCENListener;
    private ContextualEgoNetworkListener listener;

    public SocialCommunityCENListener(SocialCommunityDetection module){
        activeCENListener=new ActiveCENListener(module);
        idleCENListener=new IdleCENListener();
        listener=idleCENListener;
    }

    /**
     * Activate the listener
     */
    void setActiveCENListener(){
        listener=activeCENListener;
    }

    /**
     * Make the listener idle
     */
    void setIdleCENListener(){
        listener=idleCENListener;
    }

    @Override
    public void onCreateNode(Node node) {
        listener.onCreateNode(node);
    }

    @Override
    public void onCreateContext(Context context) {
        listener.onCreateContext(context);
    }

    @Override
    public void onLoadContext(Context context) {
        listener.onLoadContext(context);
    }

    @Override
    public void onSaveContext(Context context) {
        listener.onSaveContext(context);
    }

    @Override
    public void onRemoveContext(Context context) {
        listener.onRemoveContext(context);
    }

    @Override
    public void onAddNode(Context context, Node node) {
        listener.onAddNode(context, node);
    }

    @Override
    public void onRemoveNode(Context context, Node node) {
        listener.onRemoveNode(context, node);
    }

    @Override
    public void onCreateEdge(Edge edge) {
        listener.onCreateEdge(edge);
    }

    @Override
    public void onRemoveEdge(Edge edge) {
        listener.onRemoveEdge(edge);
    }

    @Override
    public void onCreateInteraction(Interaction interaction) {
        listener.onCreateInteraction(interaction);
    }
}
