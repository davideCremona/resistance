package resistance.agents;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import resistance.stdlibs.StdRandom;
import resistance.common.Strings;

public class Spy extends Player {

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 2850103151945065824L;
    
    private List<String> otherSpies;
    
    private double stupidity;
    
    @Override
    protected void setup(){
        this.init();
        this.stupidity = StdRandom.uniform(0.0, 0.27);
        this.salute();
        this.addBehaviour(new GetPlayersNames());
        this.addBehaviour(new GetSpiesNames());
        this.addBehaviour(new InitConfidences());
        this.addBehaviour(new Play());
    }
    
    @Override
    protected void updatePlayerConfidences(Strings missionOutcome, int nSuccess, int nFails){
        
        // per Spy:
        // nel caso di successo (la spia ha perso) devo diminuire la confidenza. di #success/|team| dei rebel nel team
        //     e aumentare di #success/|team| la confidenza nelle spie presenti nel team.
        //         
        // 
        // nel caso di fail (la spia ha vinto) devo aumentare la confidenza solo delle spie nel team di #fail/|team|

        for(String teamMember : this.getCurrentTeam()){
            
            if(!teamMember.equals(getPlayerName())){
                Float confidence = this.getConfidenceOf(teamMember);
                if(missionOutcome == Strings.MISSION_FAIL){
                    
                    confidence = (this.otherSpies.contains(teamMember)) ? 
                            confidence + (float) nSuccess/this.getTeamSize() 
                            : confidence - (float) nSuccess/this.getTeamSize();       
                }
                else if(missionOutcome == Strings.MISSION_SUCCESS){
                    
                    confidence = (this.otherSpies.contains(teamMember)) ?
                            confidence + (float) nFails/this.getTeamSize() :
                            confidence + (float) 0;
                }
                
                this.addConfidencesEntry(teamMember, confidence);
            }
        }
        //printInfoMessage(getPlayerName() + " computed NEW CONFIDENCES: " + getConfidences());
    }
    
    @Override
    protected Strings missionVote(){
        
        int nSpiesInTeam = (this.getCurrentTeam().contains(this.getPlayerName())) ? 1 : 0;
        for(String spy : this.otherSpies){
            if(this.getCurrentTeam().contains(spy))
                nSpiesInTeam++;
        }
        
        int strategy = 0;
        if(this.getnRebelVictories() == 1 && this.getnSpyVictories() == 1){
            strategy = 1;
        }
        else{
            strategy = this.getStrategy(this.getnSpyVictories(), this.getnRebelVictories());
        }
        
        double[] probModifiers = {0.4, 0.25, stupidity};        
        
        // single-intelligence
        double voteFailProbability = 1.0 - probModifiers[strategy-1];
        // with knowledge of others
        //double voteFailProbability = 1.0 - map(nSpiesInTeam, 1, this.getTeamSize(), 0, probModifiers[strategy-1]);
        
        printInfoMessage(this.getPlayerName() + " will vote MISSION_FAIL with probability: " + voteFailProbability);
        
        
        if(StdRandom.bernoulli(voteFailProbability))
            return Strings.MISSION_FAIL;
        else
            return Strings.MISSION_SUCCESS;
    }
    
    
    // from Arduino with Love <3
    private double map(double x, double in_min, double in_max, double out_min, double out_max){
      return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
    
    private int getStrategy(int nRebelVictories, int nSpyVictories){
        double c = 1.333;
        double a = 0.5;
        double b = 0.5;
        
        double rawStrategy = a*nRebelVictories + b*nSpyVictories + c;
        
        if(rawStrategy > 2)
            return 3;
        else
            return (int) Math.round(rawStrategy);
    }
    
    /* SPY RELATED BEHAVIOURS */
    
    private class GetSpiesNames extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -7073703330422068401L;

        @Override
        public void action() {
            otherSpies = new ArrayList<String>();
            
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
                                if(!getPlayerName().equals(spy)){
                                    otherSpies.add((String) spy);
                                }
                            }
                        }
                        
                    }
                    //printInfoMessage(getPlayerName()+" Received other spies: "+otherSpies);
                  
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
            ArrayList<String> friends = new ArrayList<String>();
            friends = (ArrayList<String>) otherSpies;
            ArrayList<String> players = new ArrayList<String>();
            players = (ArrayList<String>) getOtherPlayers();
            
            float initialConfidence = (float) 1.0/friends.size();
            
            for(String player : players){
                if(friends.contains(player)){
                    addConfidencesEntry(player, initialConfidence);
                }
                else{
                    addConfidencesEntry(player, (float) 0);
                }
            }
            sortConfidences();
            printInfoMessage(getPlayerName()+" Has confidences: "+getConfidences());
        }
    
    }

}
