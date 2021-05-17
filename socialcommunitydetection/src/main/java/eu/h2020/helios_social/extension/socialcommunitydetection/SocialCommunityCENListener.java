package eu.h2020.helios_social.extension.socialcommunitydetection;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetworkListener;
import eu.h2020.helios_social.core.contextualegonetwork.Edge;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

public class SocialCommunityCENListener implements ContextualEgoNetworkListener {

    private final ContextualEgoNetworkListener activeCENListener=new ActiveCENListener();
    private final ContextualEgoNetworkListener idleCENListener=new IdleCENListener();
    private ContextualEgoNetworkListener listener=idleCENListener;


    public SocialCommunityCENListener(){ }

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
