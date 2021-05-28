package eu.h2020.helios_social.extension.socialcommunitydetection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.h2020.helios_social.core.contextualegonetwork.Context;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;

public class Community {

    // reference to the cen
    private final ContextualEgoNetwork cen;
    // reference to the context
    private final Context context;
    //	nodes inside the community
    private Set<Node> communityCore;

    // threshold for a node to join the community (used only in the cut strats). La soglia deve essere SUPERATA (>, senza uguale) per essere dentro!
    private static final int THRESHOLD=2;

    Community(ContextualEgoNetwork contextualEgoNetwork, Context c){
        cen=contextualEgoNetwork;
        context=c;
        communityCore=new HashSet<>();
    }

    public int getCommunityId(){
        return hashCode();
    }
    public Set<Node> getCore(){
        return Collections.unmodifiableSet(communityCore);
    }
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

    private boolean checkJoinConditions(Node alterNode) {
//      if the alter does not even belong to the context, return
        if(!context.getNodes().contains(alterNode)) return  false;
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

    public boolean alterJoin(Node alterNode, double loadFactor){
        if(checkJoinConditions(alterNode)) {
            communityCore.add(alterNode);
            return true;
        }
        return false;
    }

    /**
     * alternatives:
     * 1) insistere sui triangoli
     * 2) componente connessa = comunità
     * 3) nodi con almeno 2 vicini dentro la comunità
     *
     * allò. siccome devo rivisitare comunque tutta la comunità, rivaluto il core scoprendo i triangoli adiacenti
     */

    public boolean alterLeave(String alterSocialId){

        boolean destroy=false;
        //	check that there is still a structure
//			potrebbe valer la pena lasciare leggera questa funzione ed eseguire una "pulizia" alla fine di ogni timeslot?
//			qualcosa tipo: elimino il nodo e se mi pare che qualche suo vicino non faccia più parte della comunità, lo segnalo
//			così che possa cercarsi una nuova comunità. al timeslot successivo lo elimino davvero
        //if the alter is not part of this ego network, do nothing and return
        if(!graph.get(egoSocialId).contains(alterSocialId)) return false;
        // return if the core did not contain the alter
        if(!communityCore.remove(alterSocialId)) return false;
        communityMembersLoadingFactors.remove(alterSocialId);
        // delete this community without further action if it is too small
        if(communityCore.size()<3){
            CommunitiesObserver.deaths++;
            return true;
        }


//		NEW NEW STRATS: segna chi sono i vicini e per ognuno prova a toglierlo e rimetterlo
        Set<String> neighbours = new HashSet<String> (graph.get(alterSocialId));
        //	remove the ones outside the community
        Iterator<String> iterator=neighbours.iterator();
        while(iterator.hasNext()) {
            String neighbour=iterator.next();
            if(!communityCore.contains(neighbour)) {
                iterator.remove();
            }
        }
        //	try to readd them until nothing changes
        boolean changed=true;
        while(changed) {
            changed=false;
            iterator=neighbours.iterator();
//			for each neigh
            while(iterator.hasNext()) {
                String neighbour=iterator.next();
//				check if the join conditions still hold
                if(checkJoinConditions(neighbour)) {
//					if this works, it is still part of the community
                    iterator.remove();
                    changed=true;
                }
            }
        }
//		definitely expel nodes still outside the community
        iterator=neighbours.iterator();
        while(iterator.hasNext()) {
            String neighbour=iterator.next();
            if(neighbour.compareTo(moderator)==0) continue;
            TilesMessage unmembership=new TilesMessage(Type.END_COM, moderator);
            GenericTilesBody body=unmembership.new GenericTilesBody(egoSocialId, getCommunityId(), null);
            unmembership.body=body;
            unmembership.recipient=neighbour;
            DisTilesProtocol.sendTilesMessage(unmembership);
            communityCore.remove(neighbour);
        }

        //	the resulting connected components are the resulting communities
        List<Set<String>> components=new LinkedList<>();
        while(!communityCore.isEmpty()) {
            //	find a component and add it to the list of components
            //	init
            String seed=communityCore.iterator().next();
            components.add(connectedComponent(seed));//, new HashSet<>(), new LinkedList<>(), new HashSet<>()));
        }

        //count splits and deaths
        if(components.size()==0) CommunitiesObserver.deaths++;
        else if(components.size()>1) CommunitiesObserver.splits++;
        else if(components.size()==1) CommunitiesObserver.survives++;

//		destroy small communities right away
        Iterator<Set<String>> setiterator=components.iterator();
        while(setiterator.hasNext()) {
            Set<String> shard=setiterator.next();
            if(shard.size()>2) continue;
            for(String member : shard) {
                //send unmemberships
                if(member.compareTo(moderator)==0) continue;
                TilesMessage unmembership=new TilesMessage(Type.END_COM, moderator);
                GenericTilesBody body=unmembership.new GenericTilesBody(egoSocialId, getCommunityId(), null);
                unmembership.body=body;
                unmembership.recipient=member;
                DisTilesProtocol.sendTilesMessage(unmembership);
            }
            setiterator.remove();
        }

        //	if there is just one component and it contains the old moderator, everything is fine
        if(components.size()==1 && components.get(0).contains(moderator)) {
            communityCore=components.get(0);
            if(!communityCore.contains(secondaryModerator))	{
                updateSecondaryModerator();
            }
            loadfactorCleanup();
            return false;
        }

        //	find a community with this moderator
        destroy=true;
        setiterator=components.iterator();
        while(iterator.hasNext()) {
            Set<String> shard=setiterator.next();
            if(shard.size()<3) continue;
            if(shard.contains(egoSocialId)) {
                setiterator.remove();
                communityCore=shard;

                if(!communityCore.contains(secondaryModerator)) {
                    updateSecondaryModerator();
                }

                destroy=false;
                break;
            }
        }
        //	send unmembership to all other nodes
        setiterator=components.iterator();
        while(setiterator.hasNext()) {
            Set<String> shard=setiterator.next();
            for(String member : shard) {
                //send unmemberships
                TilesMessage unmembership=new TilesMessage(Type.END_COM, moderator);
                GenericTilesBody body=unmembership.new GenericTilesBody(egoSocialId, getCommunityId(), null);
                unmembership.body=body;
                unmembership.recipient=member;
                DisTilesProtocol.sendTilesMessage(unmembership);
            }
            //	don't forget to eject the community, but only if they have reasonable size!
            if(shard.size()<3) continue;
            //elect a moderator
            //String elected=shard.iterator().next(); //ELEZIONE MODERATORE
            String elected = selectPrimaryModerator(shard);
            //create the message
            Tiles t = new Tiles(elected, egoSocialId, shard, prevCore, previousId);
            t.communityMembersLoadingFactors=new HashMap<String, Double>(this.communityMembersLoadingFactors);
            TilesMessage kapew=new TilesMessage(Type.GNT_PRI_MOD, moderator, elected);
            GenericTilesBody body=kapew.new GenericTilesBody(egoSocialId, getCommunityId(), t);
            kapew.body=body;
            //FIRE ZA KAPEW
            DisTilesProtocol.sendTilesMessage(kapew);
        }

        loadfactorCleanup();
        return destroy;

    }

    private Set<String> connectedComponent(String seed){
//		setup
        Set<String> visited=new HashSet<String>();
        Queue<String> queue=new LinkedList<String>();
        queue.add(seed);
        visited.add(seed);
        communityCore.remove(seed);

//		iterative visit
        while(!queue.isEmpty()) {
            String node=queue.poll();
//			"node" was already reached, i need to check only the neighbours
            Set<String> neighbours=graph.get(node);
            for(String neighbour : neighbours) {
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

    @SuppressWarnings("unused")
    private Set<String> connectedComponent(String seed, Set<String> component, Queue<String> queue, Set<String> visited) {

        //	if this node belongs to the community core, schedule a visit of its neighbours
        if(communityCore.contains(seed)) {
            component.add(seed);
            communityCore.remove(seed);
            //	but only if they have still to be visited
            graph.get(seed).forEach(new Consumer<String>() {
                @Override
                public void accept(String t) {
                    if(!visited.contains(t)) {
                        queue.add(t);
                        visited.add(t);
                    }
                }
            });
        }
        // at the end return the component
        if(queue.isEmpty()) return component;
        // or the recursive call to visit the next node
        return connectedComponent(queue.poll(), component, queue, visited);
    }


    private boolean closeCoreTriangle(String node){
        for(String first : communityCore){
            for(String second : communityCore){
                if(graph.get(first).contains(second)){
                    if(graph.get(second).contains(node)){
                        if(graph.get(node).contains(first)){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Try to see if there is a triangle of online users in this egonetwork
     * @return true if the community is formed, the owner of the object becomes a moderator and needs to do all the stuff related to the fact that he is now a moderator
     */
    public boolean tryFormCommunity(Map<String, Double> status){//ELEZIONE MODERATORE TODO: status contiene solo gli utenti online, quindi dovrebbe essere migliore da scorrere
        for(String f1 : graph.get(moderator)){
            if(f1.compareTo(egoSocialId)==0) continue;
            if(!status.keySet().contains(f1)) continue;
            if(!graph.get(egoSocialId).contains(f1)) return false;
            for(String f2 : graph.get(moderator)){
                if(f2.compareTo(egoSocialId)==0) continue;
                if(!status.keySet().contains(f2)) continue;
                //and they are both in the egonetwork of the ego
                if(!graph.get(egoSocialId).contains(f2)) return false;
                //if we take a couple of friends of the moderator
                if(graph.get(f1).contains(f2)){
                    //which close a triangle
                    //and they are both online
                    // a new community is formed
                    // add the triangle to the core
                    communityCore.add(moderator);
                    communityCore.add(f1);
                    communityCore.add(f2);
                    // complete the core by trying to close triangles
                    for(String s : status.keySet()){
                        if(s.compareTo(egoSocialId)==0) continue;
                        //also skip it if he does not belong to this egonetwork
                        if(!graph.get(egoSocialId).contains(s)){
                            continue;
                        }
                        //try to close a triangle
                        if(closeCoreTriangle(s)){
                            communityCore.add(s);
                        }
                    }
                    communityMembersLoadingFactors=status;
                    // elect a secondary moderator
                    secondaryModerator=selectSecondaryModerator();
                    CommunitiesObserver.births++;
                    return true;
                }
            }
        }
        return false;
    }

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


    /**
     * Emergency eject, sent this Tiles object to the secondary moderator. He has to update the community too!
     */
    public void emergencyEject() { //ELEZIONE MODERATORE
//		TODO:se la comunità è piccola forse va cancellata subito
        // Rimuovo il i dati relativi al fattore di carico del moderatore che sta abbandonando.
        communityMembersLoadingFactors.remove(moderator);

//		to elect a new primary moderator, temporarily remove the old one from the community core
        communityCore.remove(moderator);
//		select the new primary moderator
        String newPrimaryModerator = selectPrimaryModerator(communityCore);
        communityCore.add(moderator);

        TilesMessage kapew=new TilesMessage(Type.GNT_PRI_MOD, moderator, newPrimaryModerator);
        GenericTilesBody body=kapew.new GenericTilesBody(egoSocialId, getCommunityId(), this);
        kapew.body=body;
        DisTilesProtocol.sendTilesMessage(kapew);
    }

    public void nextSlot(){
        prevCore.clear();
        prevCore.addAll(communityCore);
        previousId=getCommunityId();
    }

}
