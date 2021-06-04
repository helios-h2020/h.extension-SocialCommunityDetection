package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Edge;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

public class Community {

    // reference to the cen
    private final ContextualEgoNetwork cen;
    // reference to the context
    private final Context context;
    //	nodes inside the community
    private Set<Node> communityCore;
//    shards created from an alter leave
    private List<Set<Node>> shards;
    // threshold for a node to join the community (used only in the cut strats).
    private static final int THRESHOLD=2;

    /**
     * Default constructor
     * @param contextualEgoNetwork a reference to the contextual ego network
     * @param c the context on which this community is defined
     */
    Community(ContextualEgoNetwork contextualEgoNetwork, Context c){
        cen=contextualEgoNetwork;
        context=c;
        communityCore=new HashSet<>();
    }

    /**
     * Constructor to use in case of a community split
     * @param contextualEgoNetwork a reference to the contextual ego network
     * @param c the context on which this community is defined
     * @param initialCore the initial community structure
     */
    Community(ContextualEgoNetwork contextualEgoNetwork, Context c, Set<Node> initialCore){
        cen=contextualEgoNetwork;
        context=c;
        communityCore=initialCore;
    }

    /**
     * method to get a unique id for the community
     * @return the result of the call {@link Object#hashCode()}
     */
    public int getCommunityId(){
        return hashCode();
    }

    /**
     * returns an unmodifiable view of the nodes in the community. Do not attempt to modify the returned data structure because it will raise exceptions
     * @return a set containing the nodes in the community
     */
    public Set<Node> getCore(){
        return Collections.unmodifiableSet(communityCore);
    }

    /**
     * method to create a human-readable representation of the nodes in the community, based on the values returned by {@link Node#getId()}
     * @return the string to be printed
     */
    public String prettyPrint(){
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("Community id: ");
        stringBuilder.append(this.getCommunityId());
        stringBuilder.append(" Number of nodes: ");
        stringBuilder.append(communityCore.size());
        stringBuilder.append("\n");
        for(Node n : communityCore){
            stringBuilder.append(n.getId());
            stringBuilder.append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        return stringBuilder.toString();
    }

    /**
     * checks if the given node can be added to the community
     * @param alterNode the node to be checked
     * @return true if the node
     */
    private boolean checkJoinConditions(Node alterNode) {
//      if the alter does not even belong to the context, return
        if(!context.getNodes().contains(alterNode)) return false;
//      cut strategy
//      count the amount of edges going inside the community this node has
        int count=0;
        for(Node coreNodeId : communityCore){
            if(context.getEdge(alterNode, coreNodeId)!=null){
                count++;
                if(count>THRESHOLD) {
//				add the new node to the community core
                    return true;
                }
            }
        }

//      triangle strategy
        for(Node firstCore : communityCore){
//          check if they are friends
            if(context.getEdge(firstCore, alterNode)==null) continue;
            for(Node secondCore : communityCore){
//              check if also second node is friend
                if(context.getEdge(secondCore, alterNode)==null) continue;
//              check if first and second core are the same node
                if(firstCore == secondCore) continue;
//              lastly check if first and second core are friends
                if(context.getEdge(firstCore, secondCore)!=null){
//					add the new node to the community core
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * tries to add the alter to this community
     * @param alterNode the alter node to be added
     * @return true if the node is passing the join conditions (it may be the case that the node was already in)
     */
    boolean alterJoin(Node alterNode){
        if(checkJoinConditions(alterNode)) {
            communityCore.add(alterNode);
            return true;
        }
        return false;
    }

    /**
     * Internal routine for the management of an alter leave
     * @param alterNode the {@link Node} object to be removed from the community
     * @return true if the community must be destroyed, false otherwise
     */
    boolean alterLeave(Node alterNode){

//        check that there is still a structure
//        if the alter is not part of this ego network, do nothing and return
        if(!context.getNodes().contains(alterNode)) return false;
//        return if the core did not contain the alter
        if(!communityCore.remove(alterNode)) return false;
//        delete this community without further action if it is too small
        if(communityCore.size()<3){
            return true;
        }

//		  local strategy: remove and readd each neighbour of the leaving node
        Set<Node> neighbours = new HashSet<> ();
        Iterator<Edge> inedges=context.getInEdges(alterNode).iterator();
        while(inedges.hasNext()){
            neighbours.add(inedges.next().getSrc());
        }
        Iterator<Edge> outedges=context.getOutEdges(alterNode).iterator();
        while(outedges.hasNext()){
            neighbours.add(outedges.next().getDst());
        }

        //	remove the ones outside the community
        Iterator<Node> iterator=neighbours.iterator();
        while(iterator.hasNext()) {
            Node neighbour=iterator.next();
            if(!communityCore.contains(neighbour)) {
                iterator.remove();
            }
        }
        //	try to readd them until nothing changes
        boolean changed=true;
        while(changed) {
            changed=false;
            iterator=neighbours.iterator();
//			for each neighbour (inside the community, but unchecked)
            while(iterator.hasNext()) {
                Node neighbour=iterator.next();
//				check if the join conditions still hold
                if(checkJoinConditions(neighbour)) {
//					if this works, it is still part of the community
                    iterator.remove();
                    changed=true;
                }
            }
        }
//		definitely expel nodes still outside the community
        for(Node neighbour:neighbours) {
            communityCore.remove(neighbour);
        }

        //	the resulting connected components are the resulting communities
        List<Set<Node>> components=new LinkedList<>();
        while(!communityCore.isEmpty()) {
            //	find a component and add it to the list of components
            //	init
            Node seed=communityCore.iterator().next();
            components.add(connectedComponent(seed));
        }

//		destroy small communities right away
        Iterator<Set<Node>> setiterator=components.iterator();
        while(setiterator.hasNext()) {
            Set<Node> shard=setiterator.next();
            if(shard.size()<3) setiterator.remove();
        }

//        if there are no resulting components, destroy the community
        if (components.size()==0) return true;
        else if (components.size()==1) {
//            if there is only one shard, assign it to this object
            communityCore=components.get(0);
        }
        else {
//            otherwise assign the first shard here
            communityCore=components.remove(0);
            shards=components;
        }
        return false;
    }

    /**
     * check if there are shards resulted from a leave to be managed somehow
     * @return true if there are shards to be managed, false otherwise
     */
    boolean wasSplit(){
        return shards!=null;
    }

    /**
     * get an iterator of the shards that have to be managed
     * @return the iterator
     */
    Iterator<Set<Node>> getShards(){
        return shards.iterator();
    }

    /**
     * simple algorithm for cpnnected components
     * @param seed the seed node from which the search must start
     * @return a set of nodes that can be reached from the seed node
     */
    private Set<Node> connectedComponent(Node seed){
//		setup
        Set<Node> visited=new HashSet<>();
        Queue<Node> queue=new LinkedList<>();
        queue.add(seed);
        visited.add(seed);
        communityCore.remove(seed);

//		iterative visit
        while(!queue.isEmpty()) {
            Node node=queue.poll();
            if(node==null) continue;
//			"node" was already reached, i need to check only the neighbours
            Set<Node> neighbours=new HashSet<>();
            context.getInEdges(node).forEach(edge -> neighbours.add(edge.getSrc()));
            context.getOutEdges(node).forEach(edge -> neighbours.add(edge.getDst()));

            for(Node neighbour : neighbours) {
//				if the neighbour is in the core but not in the set of visited
                if(communityCore.contains(neighbour) && !visited.contains(neighbour)) {
                    visited.add(neighbour);
                    queue.add(neighbour);
                    communityCore.remove(neighbour);
                }
            }

        }
        return visited;
    }

    /**
     * Try to form a new community
     * @param status a map with the known status of the users (online vs offline)
     * @param seed the node around which the community should be created, it is a community-less alter of the ego
     * @return true if the community is formed, false otherwise
     */
    boolean tryFormCommunity(Map<Node, Boolean> status, Node seed){

//        find the online neighbours of the seed
        Set<Node> seedNeighbours = new HashSet<> ();
        Iterator<Edge> inedges=context.getInEdges(seed).iterator();
        while(inedges.hasNext()){
            Node neighbour=inedges.next().getSrc();
            if(status.getOrDefault(neighbour, Boolean.FALSE)) seedNeighbours.add(neighbour);
        }
        Iterator<Edge> outedges=context.getOutEdges(seed).iterator();
        while(outedges.hasNext()){
            Node neighbour=outedges.next().getDst();
            if(status.getOrDefault(neighbour, Boolean.FALSE)) seedNeighbours.add(neighbour);
        }

//        scan the neighbours of the online neighbours and find one that closes a triangle
        for(Node neighbour:seedNeighbours){
            Iterator<Edge> closingEdgeIterator=context.getOutEdges(neighbour).iterator();
            while(closingEdgeIterator.hasNext()){
                Node closingNode=closingEdgeIterator.next().getDst();
                if(seedNeighbours.contains(closingNode)){
//                    found
                    communityCore.add(seed);
                    communityCore.add(neighbour);
                    communityCore.add(closingNode);
//                    try to add other nodes to the community until can't add any more
                    boolean changed=true;
                    while(changed) {
                        changed=false;
//                        for each node in the same context
                        for(Node candidate:context.getNodes()){
//                            check online status and if node is already inside
                            if(communityCore.contains(candidate)) continue;
                            if(status.get(candidate)) continue;
//                            check if the join conditions hold
                            if(checkJoinConditions(candidate)) {
                                communityCore.add(candidate);
                                changed=true;
                            }
                        }
                    }
//                    exit the community creation process
                    return true;
                }
            }
        }
        return false;
    }

    /*
    public void sendUnmemberships(){
        //send unmemberships
        TilesMessage unmembership=new TilesMessage(Type.END_COM, moderator);
        GenericTilesBody body=unmembership.new GenericTilesBody(egoSocialId, getCommunityId(), null);
        for(String node : communityCore){
            if(node.compareTo(moderator)==0) continue;
            unmembership=new TilesMessage(Type.END_COM, moderator);
            unmembership.body=body;
            unmembership.recipient=node;
            DisTilesProtocol.sendTilesMessage(unmembership);
        }
    }
    */

}
