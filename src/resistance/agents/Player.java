package resistance.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import resistance.common.Strings;

public class Player extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 6978453670733350181L;
    
    private Strings role;
    private String name;
    private List<String> spies;
    private List<String> players;
    
    private Map<String,Float> confidence;
    
    protected void setup(){
        this.init();
        this.printInfoMessage(role+" spawned with name: "+this.getAID().getName());
        this.name = this.getAID().getName().split("@")[0];
        
        this.addBehaviour(new GetPlayersNames());
        
        
        switch(role){
            case REBEL: /*rebel behaviour */
                this.addBehaviour(new InitConfidences());
                break;
            case SPY:   /*spy behaviour */
                this.addBehaviour(new GetSpiesNames());
                this.addBehaviour(new InitConfidences());
                break;
        default: /* do nothing / not implemented */
            break;
        }
    }

    private void init() {
        this.role = (Strings) this.getArguments()[1];
    }
    
    private class GetPlayersNames extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -7037261161648953868L;

        @Override
        public void action() {
            players = new ArrayList<String>();
            
            ACLMessage msg = myAgent.blockingReceive();
            if(msg!=null){
                
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(msg.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.PLAYER_INFORM.toString())){
                        
                        JSONArray JSONPlayers = (JSONArray) info.get("players");
                        if(JSONPlayers != null){
                        
                            for(Object player : JSONPlayers){
                                if(!name.equals(player)){
                                    players.add((String)player);
                                }
                            }
                        }
                        printInfoMessage(name+" Received other players: "+players);
                    }
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                
            }
            else{
                block();
            }
            
        }
        
    }
    
    private class InitConfidences extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -3932447167019689209L;

        @Override
        public void action() {
            /* Initial confidence is equal to all other players.
             * Spies wants other spies to be preferred.
             * SO:
             * - for rebels all other players have the same confidence
             * - for spies, all the spies has the same confidence and rebels have zero confidence.
             */
            confidence = new HashMap<String,Float>();
            ArrayList<String> friends = new ArrayList<String>();
            if(role == Strings.REBEL){
                friends = (ArrayList<String>) players;
            }
            else if(role == Strings.SPY){
                friends = (ArrayList<String>) spies;
            }
            
            float initialConfidence = (float) 1.0/friends.size();
            
            for(String friend : friends){
                confidence.put(friend, initialConfidence);
            }
            
            printInfoMessage(name+" Has confidences: "+confidence);
        }
    
    }
    
    private class GetSpiesNames extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -7073703330422068401L;

        @Override
        public void action() {
            spies = new ArrayList<String>();
            
            ACLMessage msg = myAgent.blockingReceive();
            if(msg != null){
                
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(msg.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.SPY_INFORM.toString())){
                        
                        JSONArray JSONSpies = (JSONArray) info.get("spies");
                        if(JSONSpies!=null){
                        
                            for(Object spy : JSONSpies){
                                if(!name.equals(spy)){
                                    spies.add((String) spy);
                                }
                            }
                        }
                        
                    }
                    printInfoMessage(name+" Received other spies: "+spies);
                  
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else{
                block();
            }
        }
        
    }

}
