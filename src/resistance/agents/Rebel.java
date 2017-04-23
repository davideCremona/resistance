package resistance.agents;

import jade.core.behaviours.OneShotBehaviour;

import java.util.ArrayList;

import resistance.common.Strings;
import resistance.stdlibs.StdRandom;

public class Rebel extends Player {

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = -4115352088503502124L;
    
    private double trustFactor;
    private double suspectFactor;
    
    @Override
    protected void setup(){
        this.init();
        this.trustFactor = StdRandom.uniform();
        this.suspectFactor = StdRandom.uniform(0.0, 0.2);
        this.salute();
        this.addBehaviour(new GetPlayersNames());
        this.addBehaviour(new InitConfidences());
        this.addBehaviour(new Play());
    }
    
    @Override
    protected Strings missionVote(){
        return Strings.MISSION_SUCCESS;
    }
    
    @Override
    protected void updatePlayerConfidences(Strings missionOutcome, int nSuccess, int nFails){
        
        // per Rebel:
        // devo aumentare la confidenza nei player che hanno vinto tante partite.
        // potrebbe essere #success/|team|
        // così se ci sono tanti successi la confidenza aumenta tanto (utile per quando servono
        // due fail per fallire, significa che c'è solo una spia e quindi il team è degno di fiducia)
        //
        // devo anche diminuire la confidenza nei player, se l'uoutcome è negativo.
        // potrebbe essere #fail/|team|
        // così se ci sono tanti fail significa che ci sono tante spie ed il team è poco degno di fiducia.
        
        for(String teamMember : this.getCurrentTeam()){
            
            if(!teamMember.equals(getPlayerName())){
                Float confidence = this.getConfidenceOf(teamMember);

                if(missionOutcome == Strings.MISSION_FAIL){
                    if(nFails == this.getTeamSize()){
                        confidence = (float) (confidence - 1000);
                    }
                    else{
                        confidence = (float) (confidence - (float) nFails/this.getTeamSize() - suspectFactor);
                    }
                }
                else if(missionOutcome == Strings.MISSION_SUCCESS){
                    
                    if(nSuccess == this.getTeamSize()){
                        confidence = (float) confidence + ((float)nSuccess/this.getTeamSize()) + (float)trustFactor;
                    }
                    else{
                        confidence = confidence + (float) nSuccess/this.getTeamSize();
                    }
                }
                this.addConfidencesEntry(teamMember, confidence);
            }
        }
        //printInfoMessage(getPlayerName() + " computed NEW CONFIDENCES: " + getConfidences());
    }

    
    /* REBEL RELATED BEHAVIOURS */
    
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
            friends = (ArrayList<String>) getOtherPlayers();
            
            float initialConfidence = (float) 1.0/friends.size();
            
            for(String friend : friends){
                addConfidencesEntry(friend, initialConfidence);
            }
            
            printInfoMessage(getPlayerName()+" Has confidences: "+getConfidences());
        }
    }
}
