package resistance.agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import resistance.common.PlayerPhases;
import resistance.common.Preference;
import resistance.common.Strings;

public class Player extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 6978453670733350181L;
    
    private Strings playerRole;
    private String playerName;
    private List<String> otherPlayers;
    
    private Map<String,Float> confidences;
    
    private Preference currentPreference;
    private List<String> currentTeam;
    
    private AID gameMasterName;
    
    private int nRebelVictories;
    private int nSpyVictories;
    
    protected void init(){
        this.playerRole = (Strings) this.getArguments()[1];
        this.playerName = this.getAID().getName().split("@")[0];
        this.otherPlayers = new ArrayList<String>();
        this.setConfidences(new HashMap<String,Float>());
        this.setCurrentPreference(new Preference());
        this.gameMasterName = new AID();
        this.currentTeam = new ArrayList<String>();
        this.nRebelVictories = 0;
        this.nSpyVictories = 0;
    }
    
    protected void salute(){
        printInfoMessage(this.playerName+" spawned with name: "+this.getAID().getName()+" and role: "+this.getPlayerRole());
    }
    
    protected void setPlayerName(String name){
        this.playerName = name;
    }
    
    protected String getPlayerName(){
        return this.playerName;
    }
    
    protected void setPlayerRole(Strings playerRole){
        this.playerRole = playerRole;
    }
    
    protected Strings getPlayerRole(){
        return this.playerRole;
    }
    
    /**
     * @return the confidences of this player to other players
     */
    protected Map<String,Float> getConfidences() {
        return confidences;
    }

    /**
     * @param confidences the confidences of this player to other players to set
     */
    protected void setConfidences(Map<String,Float> confidences) {
        this.confidences = confidences;
    }
    
    protected void addConfidencesEntry(String player, Float confidence){
        this.confidences.put(player, confidence);
    }
    
    protected Float getConfidenceOf(String player){
        return this.confidences.get(player);
    }
    
    protected void sortConfidences(){
        confidences = confidences.entrySet().stream()
                .sorted(Entry.comparingByValue())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, 
                        (e1, e2) -> e1, LinkedHashMap::new));
    }
    
    /**
     * @return the otherPlayers
     */
    protected List<String> getOtherPlayers() {
        return otherPlayers;
    }

    /**
     * @param otherPlayers the otherPlayers to set
     */
    protected void setOtherPlayers(List<String> otherPlayers) {
        this.otherPlayers = otherPlayers;
    }
    
    protected Strings missionVote(){
        return Strings.MISSION_SUCCESS;
    }
    
    /**
     * @return the currentPreference
     */
    protected Preference getCurrentPreference() {
        return currentPreference;
    }

    /**
     * @param currentPreference the currentPreference to set
     */
    protected void setCurrentPreference(Preference currentPreference) {
        this.currentPreference = currentPreference;
    }
    
    protected int getTeamSize(){
        return this.currentTeam.size();
    }
    
    protected List<String> getCurrentTeam(){
        return this.currentTeam;
    }
    
    
    protected void updatePlayerConfidences(Strings missionOutcome, int nSuccess, int nFails){
        /* Not Implemented here */
        /* it must be implemented in the specification of this class */
    }
    
    
    /**
     * @return the nSpyVictories
     */
    protected int getnSpyVictories() {
        return nSpyVictories;
    }

    /**
     * @param nSpyVictories the nSpyVictories to set
     */
    protected void setnSpyVictories(int nSpyVictories) {
        this.nSpyVictories = nSpyVictories;
    }



    /**
     * @return the nRebelVictories
     */
    protected int getnRebelVictories() {
        return nRebelVictories;
    }

    /**
     * @param nRebelVictories the nRebelVictories to set
     */
    protected void setnRebelVictories(int nRebelVictories) {
        this.nRebelVictories = nRebelVictories;
    }
    
    
    /* BEHAVIOURS OF EVERY PLAYER */

    protected class Play extends Behaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -526554039059282509L;
        
        private PlayerPhases playerPhase = PlayerPhases.WAIT_NEW_ROUND;
        
        private String currentOutcome;
        private int currentNFails;
        private int currentNSuccess;
        
        
        @Override
        public void action() {
            switch(playerPhase){
            
            case WAIT_NEW_ROUND:
                waitStartRound();
                break;
                
            case COMPUTE_PREFERENCES:
                computePreferences();
                this.playerPhase = PlayerPhases.BROADCAST_PREFERENCES;
                break;
                
            case BROADCAST_PREFERENCES:
                broadcastPreferences();
                this.playerPhase = PlayerPhases.RECEIVE_TEAM_FORMATION;
                break;
            
            case RECEIVE_TEAM_FORMATION:
                /* if I'm in the team, switch to VOTE_MISSION_OUTCOME */
                /* otherwise switch to UPDATE POINTS */
                this.playerPhase = receiveTeamFormation() ? 
                        PlayerPhases.VOTE_MISSION_OUTCOME : PlayerPhases.COLLECT_MISSION_OUTCOME;        
                break;
                
            case VOTE_MISSION_OUTCOME:
                voteMissionOutcome();
                this.playerPhase = PlayerPhases.COLLECT_MISSION_OUTCOME;
                break;
                
            case COLLECT_MISSION_OUTCOME:
                /* 
                 * increment missionSuccess or missionFail counters
                 * to the current team players.
                 */
                collectMissionOutcome();
                this.playerPhase = PlayerPhases.UPDATE_CONFIDENCES;
                break;
                
            case UPDATE_CONFIDENCES:
                updateConfidences();
                this.playerPhase = PlayerPhases.WAIT_END_ROUND;
                break;

            case WAIT_END_ROUND:
                /* here I wait the end of the round, but if I receive end_game then I will end the game. */
                waitEndRound();
                break;
            
            default: /* do nothing */
                break;
            
            }
        }

        private void waitStartRound() {
            
            ACLMessage startRound = myAgent.blockingReceive();
            if(startRound != null){
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(startRound.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.AGENT_INFO.toString())){
                        
                        gameMasterName = startRound.getSender();
                        String content = (String) info.get("info");
                        if(content != null && content.equals(Strings.START_ROUND.toString())){
                            String roundNumber = (String) info.get("data");
                            printInfoMessage(getPlayerName()+" Starts new round #"+roundNumber);
                            this.playerPhase = PlayerPhases.COMPUTE_PREFERENCES;
                        }
                        else{block();}

                    }
                    else{block();}
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else{block();}
        }

        /**
         * Compute preferences.
         * Computes the player preference by ordering current confidence map <Player, Confidence> wrt the confidence.
         */
        private void computePreferences() {

            currentPreference = new Preference();
            List<Map.Entry<String, Float>> confList = new ArrayList<Entry<String, Float>>(getConfidences().entrySet());
            Collections.sort(confList, new Comparator<Entry<String, Float>>(){
                @Override
                public int compare(Entry<String, Float> x, Entry<String, Float> y){
                    if(x.getValue() == y.getValue())
                        return 0;
                    else
                        return (x.getValue() < y.getValue()) ? -1 : 1;
                }
            });
            for(Entry<String, Float> entry : confList){
                currentPreference.insert(entry.getKey());
            }
            //printInfoMessage(getPlayerName() + " has Preferences: " + currentPreference.toJSONString());
        }

        /**
         * Broadcast preferences.
         * Tells to the GameMaster what is the preference of this player for the new team
         * by sending an ACLMessage containing the JSON representation of the currentPreference.
         */
        @SuppressWarnings("unchecked")
        private void broadcastPreferences() {
            ACLMessage preferenceMessage = new ACLMessage(ACLMessage.INFORM);
            JSONObject jsonPreference = new JSONObject();
            jsonPreference.put("msgType", Strings.PREFERENCE_INFORM.toString());
            jsonPreference.put(Strings.PLAYER_NAME.toString(), getPlayerName());
            jsonPreference.put(Strings.PREFERENCE_INFORM.toString(), currentPreference.toJSONArray());
            preferenceMessage.addReceiver(gameMasterName);
            preferenceMessage.setContent(jsonPreference.toJSONString());
            myAgent.send(preferenceMessage);
        }

        /**
         * Receive team formation.
         * It block-receive the agent for a message that tells what is the new team formation.
         * It updates the list of players in the current team.
         * @return true, if this player is in the team formation.
         * @return false, if this player is not in the team formation.
         */
        private boolean receiveTeamFormation() {
            boolean in = false;
            ACLMessage teamMessage = myAgent.blockingReceive();
            currentTeam = new ArrayList<String>();
            if(teamMessage != null){

                try {

                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(teamMessage.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.TEAM_FORMATION.toString())){

                        JSONArray jsonTeam = (JSONArray) info.get(Strings.TEAM_FORMATION.toString());
                        for(Object jsonPlayer : jsonTeam){
                            currentTeam.add((String) jsonPlayer);
                            if(currentTeam.contains(getPlayerName())){
                                in = true;
                            }
                        }
                    }
                    else{ block(); }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
            else{ block(); } 
            return in;
        }

        /**
         * Vote mission outcome.
         * This method tells to the GameMaster the preference (Success/Fail)
         * of this player by sending an ACLMessage.
         */
        @SuppressWarnings("unchecked")
        private void voteMissionOutcome() {
            
            ACLMessage voteMessage = new ACLMessage(ACLMessage.INFORM);
            JSONObject jsonVote = new JSONObject();
            jsonVote.put("msgType", Strings.MISSION_VOTE.toString());
            jsonVote.put(Strings.PLAYER_NAME.toString(), getPlayerName());
            jsonVote.put(Strings.MISSION_VOTE.toString(), missionVote().toString());
            voteMessage.addReceiver(gameMasterName);
            voteMessage.setContent(jsonVote.toJSONString());
            myAgent.send(voteMessage);
        }

        private void collectMissionOutcome() {
            ACLMessage missionOutcomeMessage = myAgent.blockingReceive();
            if(missionOutcomeMessage != null){
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(missionOutcomeMessage.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.MISSION_OUTCOME.toString())){
                        
                        String outcome = (String) info.get(Strings.MISSION_OUTCOME.toString());
                        Integer nFails = Integer.parseInt((String) info.get(Strings.MISSION_FAIL_N.toString()));
                        Integer nSuccess = Integer.parseInt((String) info.get(Strings.MISSION_SUCCESS_N.toString()));
                        
                        this.currentOutcome = outcome;
                        this.currentNSuccess = nSuccess;
                        this.currentNFails = nFails;
                        
                        // save number of victories for each team.
                        if(this.currentOutcome.equals(Strings.MISSION_FAIL.toString())){
                            setnSpyVictories(getnSpyVictories()+1);
                        }
                        else if(this.currentOutcome.equals(Strings.MISSION_SUCCESS.toString())){
                            setnRebelVictories(getnRebelVictories()+1);
                        }

                    }
                    else{ block(); }
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else{ block(); }
        }

        private void updateConfidences() {
           
            // delegate to child classes Rebel and Spy the method.
            updatePlayerConfidences(Strings.valueOf(this.currentOutcome), this.currentNSuccess, this.currentNFails);
//            printInfoMessage("--------");
//            printInfoMessage(getPlayerName() + " has NEW confidences: " + getConfidences());
//            printInfoMessage("--------");
        }

        private void waitEndRound() {
            
            // Devo aspettare un messaggio di END_ROUND o END_GAME.
            /*
             * se "msgType" == Strings.END_ROUND.toString()
             *      Allora END_ROUND, aggiorno round number, restituisco true.
             * se "msgType", Strings.END_GAME.toString()
             *      Allora END_GAME, restituisco false.
             */
            
            ACLMessage endRoundMessage = myAgent.blockingReceive();
            if(endRoundMessage != null){
              
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(endRoundMessage.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.END_ROUND.toString())){
                        this.playerPhase = PlayerPhases.WAIT_NEW_ROUND;
                    }
                    else if(msgType != null && msgType.equals(Strings.END_GAME.toString())){
                        this.playerPhase = PlayerPhases.GAME_ENDED;
                        printInfoMessage(getPlayerName()+" game ended.");
                    }
                    else{
                        block();
                    }
                    
                } catch (ParseException e) { e.printStackTrace(); }
            }
            else{ block(); }
            
        }

        @Override
        public boolean done() {
            
            return this.playerPhase == PlayerPhases.GAME_ENDED;
        }
        
    }

    protected class GetPlayersNames extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -7037261161648953868L;

        @Override
        public void action() {
            
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
                                if(!playerName.equals(player)){
                                    otherPlayers.add((String)player);
                                }
                            }
                        }
                        //printInfoMessage(playerName+" Received other players: "+otherPlayers);
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
}
