package resistance.agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.jws.soap.SOAPBinding.ParameterStyle;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import resistance.common.GMPhases;
import resistance.common.PlayerMission;
import resistance.common.Preference;
import resistance.common.PreferenceProfile;
import resistance.common.Strings;


public class GameMaster extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 2052656452229351686L;
    
    private String name;
    
    private int NPlayers = 0;
    private int NSpies;
    // it's kept for convenience for now
    private int NRebels;
    
    public int spiesVictories = 0;
    public int rebelsVictories = 0;
    
    private List<String> spiesNames = new ArrayList<String>();
    private List<String> rebelsNames = new ArrayList<String>();

    
    
    protected void setup(){
        this.printInfoMessage("GameMaster "+this.getAID().getName()+" born!");
        
        this.name = this.getAID().getName().split("@")[0];
        
        this.addBehaviour(new PrepareGame());
        this.addBehaviour(new AssignRoles());
        this.addBehaviour(new ManageGame());
    }
    
    private class PrepareGame extends OneShotBehaviour{
        
        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = 2001907503291208460L;
        
        private static final int MIN_PLAYERS = 5;
        private static final int MAX_PLAYERS = 10;
        private final Map<Integer,Integer> mapPlayersSpies = new HashMap<Integer,Integer>(){

            /**
             * Generated SerialVersionUID
             */
            private static final long serialVersionUID = -3964541290027669503L;
            
            {
                put(5,2);
                put(6,2);
                put(7,3);
                put(8,3);
                put(9,3);
                put(10,4);
            }
            
        };
        
 
        private int selectNPlayers(){
            Random random = new Random();
            return random.nextInt(PrepareGame.MAX_PLAYERS + 1 - PrepareGame.MIN_PLAYERS) + PrepareGame.MIN_PLAYERS;
        }

        @Override
        public void action() {
            
            /* Select Number of Players (between min and max players) */
            NPlayers = this.selectNPlayers();// TODO: change to random for final release

            NSpies = this.mapPlayersSpies.get(NPlayers);
            NRebels = NPlayers - NSpies;
            List<Object[]> rolesDeck = new ArrayList<Object[]>();
            Object[] spyParameters = {new Integer(NPlayers), Strings.SPY};
            Object[] rebelParameters = {new Integer(NPlayers), Strings.REBEL};
            for(int i=0; i<NRebels; i++){
                rolesDeck.add(rebelParameters);
            }
            for(int i=0; i<NSpies; i++){
                rolesDeck.add(spyParameters);
            }
            
            long seed = System.nanoTime();
            long seedone = System.nanoTime();
            Collections.shuffle(rolesDeck, new Random(seed));
            Collections.shuffle(rolesDeck, new Random(seedone));
            
            for(int i=0; i<NPlayers; i++){
                Object[] parameters = rolesDeck.get(i);
                Strings role = (Strings) parameters[1];
                String classpath = PlayersFactory.getCharacter(role);
                spawnAgent("player"+i, classpath, parameters);
                if(role == Strings.SPY)
                    spiesNames.add("player"+i);
                else if(role == Strings.REBEL)
                    rebelsNames.add("player"+i);
            }
        }

        private void spawnAgent(String name, String classname, Object[] parameters) {
            try {

                ContainerController cc = this.getAgent().getContainerController();
                AgentController ac = cc.createNewAgent(name, classname, parameters);
                ac.start();
                
            } catch (StaleProxyException e) {
                e.printStackTrace();
            } 
        }
    }
    
    private class AssignRoles extends OneShotBehaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -4089206863887956629L;

        @SuppressWarnings("unchecked")
        @Override
        public void action() {
            /* Inform all players */
            ACLMessage allMsg = new ACLMessage(ACLMessage.INFORM);
            JSONObject allInfo = new JSONObject();
            JSONArray players = new JSONArray();
            for(String player : rebelsNames){
                allMsg.addReceiver(new AID(player, AID.ISLOCALNAME));
                players.add(player);
            }
            for(String player : spiesNames){
                allMsg.addReceiver(new AID(player, AID.ISLOCALNAME));
                players.add(player);
            }
            allInfo.put("msgType", Strings.PLAYER_INFORM.toString());
            allInfo.put("players", players);
            allMsg.setContent(allInfo.toJSONString());
            myAgent.send(allMsg);
            
            /* Inform spies */
            ACLMessage spyMsg = new ACLMessage(ACLMessage.INFORM);
            JSONObject spyInfo = new JSONObject();
            JSONArray spies = new JSONArray();
            for(String spy : spiesNames){
                spyMsg.addReceiver(new AID(spy, AID.ISLOCALNAME));
                spies.add(spy);
            }
            spyInfo.put("msgType", Strings.SPY_INFORM.toString());
            spyInfo.put("spies", spies);
            spyMsg.setContent(spyInfo.toJSONString());
            myAgent.send(spyMsg);
        }
        
    }

    private class ManageGame extends Behaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -3642853827462485882L;
        
        private GMPhases gameMasterPhase = GMPhases.INITIAL_STUFF;
        
        private Map<Integer, PlayerMission[]> missions = new HashMap<Integer, PlayerMission[]>(){

            /**
             * Generated SerialVersionUID
             */
            private static final long serialVersionUID = 4079226855435235770L;
            
            {
                put(5, new PlayerMission[] {new PlayerMission(2), new PlayerMission(3), new PlayerMission(2), new PlayerMission(3), new PlayerMission(3)});
                put(6, new PlayerMission[] {new PlayerMission(2), new PlayerMission(3), new PlayerMission(4), new PlayerMission(3), new PlayerMission(4)});
                put(7, new PlayerMission[] {new PlayerMission(2), new PlayerMission(3), new PlayerMission(3), new PlayerMission(4,2), new PlayerMission(4)});
                put(8, new PlayerMission[] {new PlayerMission(3), new PlayerMission(4), new PlayerMission(4), new PlayerMission(5,2), new PlayerMission(5)});
                put(9, new PlayerMission[] {new PlayerMission(3), new PlayerMission(4), new PlayerMission(4), new PlayerMission(5,2), new PlayerMission(5)});
                put(10, new PlayerMission[] {new PlayerMission(3), new PlayerMission(4), new PlayerMission(4), new PlayerMission(5,2), new PlayerMission(5)});
                
            }
            
        };
        
        private Integer currentMissionIndex;
        private PlayerMission[] instanceMissions;
        private PlayerMission currentMission;
        
        private Integer currentRound;
        private PreferenceProfile currentPreferenceProfile;
        private Integer playersPreferencesRemaining;
        private Integer teamVotesRemaining;

        private ArrayList<String> currentTeam;

        private int currentMissionFails;

        private int currentMissionSuccess;
        

        @Override
        public void action() {
            
            switch(gameMasterPhase){
            
            case INITIAL_STUFF:
                initGameVariables();
                this.gameMasterPhase = GMPhases.GM_START_ROUND;
                break;
            
            case GM_START_ROUND:
                broadcastStartRound();
                updateMission();
                this.gameMasterPhase = GMPhases.GM_COLLECT_PREFERENCES;
                this.playersPreferencesRemaining = NPlayers;
                break;
                
            case GM_COLLECT_PREFERENCES:
                collectPreferences();
                this.gameMasterPhase = (this.playersPreferencesRemaining == 0) ? 
                        GMPhases.GM_TEAM_SELECTION : GMPhases.GM_COLLECT_PREFERENCES;
                break;
                
            case GM_TEAM_SELECTION:
                selectTeam();
                informTeam();
                this.teamVotesRemaining = this.currentTeam.size();
                this.currentMissionFails = 0;
                this.currentMissionSuccess = 0;
                this.gameMasterPhase = GMPhases.GM_COLLECT_MISSION_OUTCOME;
                break;
            
            /*
            case GM_CAST_VOTE:
                castMissionVote();
                this.gameMasterPhase = GMPhases.GM_COLLECT_MISSION_OUTCOME;
                break;
            */
                
            case GM_COLLECT_MISSION_OUTCOME:
                collectMissionOutcome();
                this.gameMasterPhase = (this.teamVotesRemaining == 0) ?
                        GMPhases.GM_ASSIGN_MISSION_POINTS : GMPhases.GM_COLLECT_MISSION_OUTCOME;
                break;
                
            case GM_ASSIGN_MISSION_POINTS:
                assignMissionPoints();
                this.gameMasterPhase = GMPhases.GM_CHECK_GAME_END;
                break;
                
            case GM_CHECK_GAME_END:
                if (checkGameEnd())
                    this.gameMasterPhase = GMPhases.GM_GAME_ENDING;
                else
                    this.gameMasterPhase = GMPhases.GM_END_ROUND;
                break;
                
            case GM_GAME_ENDING:
                onGameEnding();
                this.gameMasterPhase = GMPhases.GM_GAME_ENDED;
                break;
            
            case GM_END_ROUND:
                broadcastEndRound();
                this.currentRound += 1;
                this.currentMissionIndex += 1;
                if(this.currentMissionIndex >= this.instanceMissions.length)
                    this.gameMasterPhase = GMPhases.GM_GAME_ENDING;
                else
                    this.gameMasterPhase = GMPhases.GM_START_ROUND;
                break;

            default:
                break;
            
            }
        }
        
        private void updateMission() {
            this.currentMission = this.instanceMissions[this.currentMissionIndex];
        }

        private void initGameVariables() {
            this.currentRound = 0;
            this.currentMissionIndex = 0;
            this.currentMissionSuccess = 0;
            this.currentMissionFails = 0;
            this.instanceMissions = this.missions.get(NPlayers);
            this.currentMission = this.instanceMissions[this.currentMissionIndex];
            this.currentTeam = new ArrayList<String>();
            this.currentPreferenceProfile = new PreferenceProfile();
        }

        @SuppressWarnings("unchecked")
        private void onGameEnding() {
            
            printInfoMessage(name+ " EXECUTING ON GAME ENDING OPERATIONS");
            
            ACLMessage endGame = new ACLMessage(ACLMessage.INFORM);
            JSONObject gameInform = new JSONObject();
            for(String player : rebelsNames){
                endGame.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            for(String player : spiesNames){
                endGame.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            gameInform.put("msgType", Strings.END_GAME.toString());
            endGame.setContent(gameInform.toJSONString());
            myAgent.send(endGame);
            
            String endMessage = (rebelsVictories == 3) ? "REBELS WINS" : "SPIES WINS";
            printInfoMessage(name +" "+ endMessage);
        }

        private boolean checkGameEnd() {
            //printInfoMessage(name+ " CHECKING GAME END");
            return (rebelsVictories == 3 || spiesVictories == 3) ? true : false;
        }

        @SuppressWarnings("unchecked")
        private void broadcastEndRound() {
            printInfoMessage(name+ " BROADCASTING END OF ROUND #"+this.currentRound);
            ACLMessage endRound = new ACLMessage(ACLMessage.INFORM);
            JSONObject gameInform = new JSONObject();
            for(String player : rebelsNames){
                endRound.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            for(String player : spiesNames){
                endRound.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            gameInform.put("msgType", Strings.END_ROUND.toString());
            gameInform.put(Strings.NEW_ROUND.toString(), this.currentRound);
            endRound.setContent(gameInform.toJSONString());
            myAgent.send(endRound);
        }

        @SuppressWarnings("unchecked")
        private void assignMissionPoints() {
            //printInfoMessage(name+ " ASSIGNING MISSION POINTS TO TEAM");
            
            /*
             * Deve:
             * - valutare il mission vote e modificare l'outcome di conseguenza, 
             * - avvisare i player dell'outcome
             * - incrementare spiesVictories oppure rebelsVictories.
             * 
             * 
             * Inviare un messaggio con:
             * "msgType" = Strings.MISSION_OUTCOME.toString()
             * Strings.MISSION_OUTCOME.toString() = computed_outcome
             * Strings.MISSION_FAIL_N.toString()  = computed_n_fails
             * Strings.MISSION_SUCCESS_N.toString() = computed_n_success
             */
            
            Strings outcome = (this.currentMissionFails >= this.currentMission.getDifficulty()) ?
                    Strings.MISSION_FAIL : Strings.MISSION_SUCCESS;
            
            if(outcome == Strings.MISSION_FAIL)
                spiesVictories++;
            else if(outcome == Strings.MISSION_SUCCESS)
                rebelsVictories++;
            
            printInfoMessage(name + " mission outcome: "+ outcome + "; current points: ["+spiesVictories+" spies § "+rebelsVictories+" rebels]");
            
            ACLMessage missionOutcomeMessage = new ACLMessage(ACLMessage.INFORM);
            JSONObject jsonMissionOutcome = new JSONObject();
            for(String player : rebelsNames){
                missionOutcomeMessage.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            for(String player : spiesNames){
                missionOutcomeMessage.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            jsonMissionOutcome.put("msgType", Strings.MISSION_OUTCOME.toString());
            jsonMissionOutcome.put(Strings.MISSION_OUTCOME.toString(), outcome.toString());
            jsonMissionOutcome.put(Strings.MISSION_FAIL_N.toString(), String.valueOf(this.currentMissionFails));
            jsonMissionOutcome.put(Strings.MISSION_SUCCESS_N.toString(), String.valueOf(this.currentMissionSuccess));
            missionOutcomeMessage.setContent(jsonMissionOutcome.toJSONString());
            myAgent.send(missionOutcomeMessage);
        }

        private void collectMissionOutcome() {
            //printInfoMessage(name+ " COLLECTING MISSION #"+(this.currentMissionIndex+1)+" OUTCOMES ("+this.teamVotesRemaining+" remaining)" );
            
            /*
             * Per ogni player nel team,
             * Deve restare in attesa di un messaggio:
             * "msgType" = Strings.MISSION_VOTE.toString()
             * Strings.MISSION_VOTE.toString()missionVote().toString()
             * Strings.PLAYER_NAME.toString(), getPlayerName()
             * 
             * salvare currentMissionFails e currentMissionSuccess
             */
            
            ACLMessage missionVoteMessage = myAgent.blockingReceive();
            if(missionVoteMessage != null){
                
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(missionVoteMessage.getContent());
                    String msgType = (String) info.get("msgType");
                    
                    if(msgType != null && msgType.equals(Strings.MISSION_VOTE.toString())){
                        
                        String outcome = (String) info.get(Strings.MISSION_VOTE.toString());
                        String playerName = (String) info.get(Strings.PLAYER_NAME.toString());
                        
                        if(Strings.MISSION_FAIL.compareTo(outcome))
                            this.currentMissionFails += 1;
                        else if(Strings.MISSION_SUCCESS.compareTo(outcome))
                            this.currentMissionSuccess += 1;
                        
                        printInfoMessage(name+" Player: "+playerName+" voted: "+outcome);
                        
                        this.teamVotesRemaining--;
                        
                    }
                    else{ block(); }
                    
                } catch (ParseException e) { e.printStackTrace(); }
                
            }
            else{ block(); }
            
        }

        /* -> commentato perchè non richiede più il voto ma lo riceve subito dopo aver comunicato il team
         * Se non dovesse funzionare, bisogna modificare Player per attendere il vote casting prima di passare
         * alla fase di missionVote.
        private void castMissionVote() {
            
            printInfoMessage(name+ " ASKING FOR MISSION #"+(this.currentMissionIndex+1)+" OUTCOMES");
        }
        */

        private void selectTeam() {
            //printInfoMessage(name+ " SELECTING TEAM MEMBERS");
            
            this.currentTeam = new ArrayList<String>();
            
            /*
             * Deve applicare Borda's Rule:
             * - per ogni Preference i in PreferenceProfile
             *  - per ogni playerName in i,
             *      - assegnare playerNamePoints += (size - posizione) - 1
             * - ordinare
             * - prendere i primi N player e metterli nel currentTeam.
             */
            
            // GET FOR EACH PLAYER THEIR POINTS (ACCORDING TO BORDA's RULE)
            Map<String, Integer> playerPoints = new HashMap<String, Integer>();
            for(Entry<String, Preference> playerPref : this.currentPreferenceProfile.getAll()){
                Preference p = playerPref.getValue();
                for(String candidate : p.getPreference()){
                    if(!playerPoints.containsKey(candidate))
                        playerPoints.put(candidate, 0);
                    Integer currentPoints = playerPoints.get(candidate);
                    currentPoints += p.getPreference().size() - p.getPreference().indexOf(candidate);
                    playerPoints.put(candidate, currentPoints);
                }
            }
            // SORTING BY VALUE
            List<Entry<String, Integer>> leaderboard = new ArrayList<Entry<String, Integer>>(playerPoints.entrySet());
            Collections.sort(leaderboard, new Comparator<Entry<String, Integer>>(){
                @Override
                public int compare(Entry<String, Integer> x, Entry<String, Integer> y){
                    if(x.getValue() == y.getValue())
                        return 0;
                    else
                        return (x.getValue() < y.getValue()) ? 1 : -1;
                }
            });
            
            // GETTING THE FIRST N PLAYERS TO FORM THE TEAM
            for(int i=0; i<this.currentMission.getPlayersRequired(); i++){
                this.currentTeam.add(leaderboard.get(i).getKey());
            }
            
            printInfoMessage(name+" Team Selected: "+this.currentTeam);
        }

        @SuppressWarnings("unchecked")
        private void informTeam() {
            
            //printInfoMessage(name+ " INFORMING TEAM MEMBERS FOR MISSION #"+(this.currentMissionIndex+1)+" (need "+this.currentMission.getDifficulty()+" fails to fail)");
           
            /*
             * Deve mandare un messaggio a tutti i player con 
             * "msgType" = Strings.TEAM_FORMATION
             * "TEAM_FORMATION" = JSONArray con i componenti del team.
             */
            
            ACLMessage teamMessage = new ACLMessage(ACLMessage.INFORM);
            JSONObject jsonTeamMessage = new JSONObject();
            JSONArray jsonTeam = new JSONArray();
            for(String player : rebelsNames){
                teamMessage.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            for(String player : spiesNames){
                teamMessage.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            jsonTeamMessage.put("msgType", Strings.TEAM_FORMATION.toString());
            for(String player : this.currentTeam){
                jsonTeam.add(player);
            }
            jsonTeamMessage.put(Strings.TEAM_FORMATION.toString(), jsonTeam);
            teamMessage.setContent(jsonTeamMessage.toJSONString());
            myAgent.send(teamMessage);
            
        }

        private void collectPreferences() {
            //printInfoMessage(name+ " COLLECTING PLAYERS PREFERENCES");
            
            ACLMessage preferenceMessage = myAgent.blockingReceive();
            if(preferenceMessage != null){
                
                try {
                    
                    JSONParser jsp = new JSONParser();
                    JSONObject info = (JSONObject) jsp.parse(preferenceMessage.getContent());
                    String msgType = (String) info.get("msgType");
                    if(msgType != null && msgType.equals(Strings.PREFERENCE_INFORM.toString())){
                        
                        Preference playerPreference = new Preference();
                        playerPreference.parseJSON((JSONArray) info.get(Strings.PREFERENCE_INFORM.toString()));
                        currentPreferenceProfile.addPreferencesOf((String) info.get(Strings.PLAYER_NAME.toString()), playerPreference);
                        
                        
                        printInfoMessage(name
                                +" received prefernce: "+ playerPreference.toJSONArray() 
                                + " FROM: "+ (String) info.get(Strings.PLAYER_NAME.toString()));
                        
                        this.playersPreferencesRemaining--;
                    }
                    else{ block(); }
                    
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else{ block(); }
        }

        @SuppressWarnings("unchecked")
        private void broadcastStartRound(){
            printInfoMessage(name+"------------------------------\n"+name+ " BROADCASTING START_ROUND #"+this.currentRound);
            ACLMessage startRound = new ACLMessage(ACLMessage.INFORM);
            JSONObject gameInform = new JSONObject();
            for(String player : rebelsNames){
                startRound.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            for(String player : spiesNames){
                startRound.addReceiver(new AID(player, AID.ISLOCALNAME));
            }
            gameInform.put("msgType", Strings.AGENT_INFO.toString());
            gameInform.put("info", Strings.START_ROUND.toString());
            gameInform.put("data", this.currentRound.toString());
            startRound.setContent(gameInform.toJSONString());
            myAgent.send(startRound);
        }

        @Override
        public boolean done() {
            return this.gameMasterPhase == GMPhases.GM_GAME_ENDED;  
        }
    }
}
