package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.util.LinkedList;
import java.util.Objects;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetworkListener;
import eu.h2020.helios_social.core.contextualegonetwork.Edge;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;

class ActiveCENListener implements ContextualEgoNetworkListener {

    SocialCommunityDetection module;

    ActiveCENListener(SocialCommunityDetection m){
        module=m;
    }

//    ignore (for now), adding anode to a context is managed in onAddNode
    @Override
    public void onCreateNode(Node node) { }

//    add a context to the set (map) of the communities
    @Override
    public void onCreateContext(Context context) {
        module.communities.putIfAbsent(context, new LinkedList<>());
    }

//    ignored for now, but will consider if we should limit the community computation only to active contexts
    @Override
    public void onLoadContext(Context context) {}
    @Override
    public void onSaveContext(Context context) {}

//    remove the context from the communities structure
    @Override
    public void onRemoveContext(Context context) {
        module.communities.remove(context);
    }

    @Override
    public void onAddNode(Context context, Node node) {
//        send a ping message
        byte[] pingMessage=InternalMessage.createMessage(InternalMessage.Type.PING, module.contextualEgoNetwork.getEgo().getId());
//        stop if the message was not created
        if (pingMessage==null) return;
        HeliosNetworkAddress address = new HeliosNetworkAddress();
        address.setNetworkId(node.getId());
        module.messagingModule.sendTo(address, SocialCommunityDetection.PROTOCOL_NAME, pingMessage);
    }

//    try to remove the node also from the communities
    @Override
    public void onRemoveNode(Context context, Node node) {
        if (!module.communities.containsKey(context)) return;
        try {
            for (Community community : Objects.requireNonNull(module.communities.get(context))) {
                community.alterLeave(node);
            }
        } catch (NullPointerException e){
//            ignore exception and exit
        }
    }

//    start the joining procedure with the ping messages
    @Override
    public void onCreateEdge(Edge edge) {

        byte[] pingMessage=InternalMessage.createMessage(InternalMessage.Type.PING, module.contextualEgoNetwork.getEgo().getId());
//        stop if the message was not created
        if (pingMessage==null) return;
//        to source
        if (edge.getSrc()!=edge.getEgo()) {
            HeliosNetworkAddress address = new HeliosNetworkAddress();
            address.setNetworkId(edge.getSrc().getId());
            module.messagingModule.sendTo(address, SocialCommunityDetection.PROTOCOL_NAME, pingMessage);
        }
//        and destination
        if (edge.getDst()!=edge.getEgo()) {
            HeliosNetworkAddress address = new HeliosNetworkAddress();
            address.setNetworkId(edge.getDst().getId());
            module.messagingModule.sendTo(address, SocialCommunityDetection.PROTOCOL_NAME, pingMessage);
        }
    }

//    try to remove both ends of the edge from the communities of the context the edge was included
    @Override
    public void onRemoveEdge(Edge edge) {
        Context context=edge.getContext();
        if (edge.getSrc()!=edge.getEgo()) {
            try {
                for (Community community : Objects.requireNonNull(module.communities.get(context))) {
                    community.alterLeave(edge.getSrc());
                }
            } catch (NullPointerException ignored){}
//            ignore the exception and do nothing
        }
        if (edge.getDst()!=edge.getEgo()){
            try{
                for (Community community : Objects.requireNonNull(module.communities.get(context))){
                    community.alterLeave(edge.getDst());
                }
            } catch (NullPointerException ignored){}
//            ignore the exception and do nothing
        }
    }

//    ignore (for now)
    @Override
    public void onCreateInteraction(Interaction interaction) {}


}
