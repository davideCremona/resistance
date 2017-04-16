package resistance.agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import resistance.common.Strings;


public class GameMaster extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 2052656452229351686L;
    
    private String name;
    
    private int NPlayers = 0;
    private int NSpies;
    @SuppressWarnings("unused") // it's kept for convenience for now
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
        this.addBehaviour(new Game());
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
        
        private static final String PLAYER_CLASSNAME = "resistance.agents.Player";
        
        
        
        private int selectNPlayers(){
            Random random = new Random();
            return random.nextInt(PrepareGame.MAX_PLAYERS + 1 - PrepareGame.MIN_PLAYERS) + PrepareGame.MIN_PLAYERS;
        }

        @Override
        public void action() {
            
            /* Select Number of Players (between min and max players) */
            NPlayers = this.selectNPlayers();
            
            /* Spawn Spies floor(NPlayers/3) */
            NSpies = this.mapPlayersSpies.get(NPlayers);
            int currentPlayer = 0;
            for(int i=currentPlayer; i<NSpies; i++){
                
                Object[] spyParameters = {new Integer(NPlayers), Strings.SPY};
                spawnAgent("player"+currentPlayer, PrepareGame.PLAYER_CLASSNAME, spyParameters); 
                spiesNames.add("player"+currentPlayer);
                currentPlayer++;
            }
            
            /* Spawn Rebels (NPlayers-NSpies) */
            NRebels = NPlayers - NSpies;
            for( int i=currentPlayer; i<NPlayers; i++){
                
                Object[] rebelParameters = {new Integer(NPlayers), Strings.REBEL};
                spawnAgent("player"+currentPlayer, PrepareGame.PLAYER_CLASSNAME, rebelParameters);
                rebelsNames.add("player"+currentPlayer);
                currentPlayer++;
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

    private class Game extends Behaviour{

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = -3642853827462485882L;
        
        private final Map<Integer,Integer[]> mapPlayersSpies = new HashMap<Integer,Integer[]>(){

            /**
             * Generated SerialVersionUID
             */
            private static final long serialVersionUID = -2353427350382752483L;
            
            {
                put(5,new Integer[] {2,3,2,3,3});
                put(6,new Integer[] {2,3,4,3,4});
                put(7,new Integer[] {2,3,3,4,4});
                put(8,new Integer[] {3,4,4,5,5});
                put(9,new Integer[] {3,4,4,5,5});
                put(10,new Integer[] {3,4,4,5,5});
            }
            
        };

        @Override
        public void action() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean done() {
            if(rebelsVictories == 3){
                printInfoMessage(name+" Tells that the REBELS are the winners!");
                return true;
            }
            else if(spiesVictories == 3){
                printInfoMessage(name+" Tells that the SPIES are the winners!");
                return true;
            }
            else{
                return false;
            }
        }
        
    }
}
