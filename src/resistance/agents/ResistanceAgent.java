package resistance.agents;

import resistance.common.Strings;
import jade.core.Agent;

public class ResistanceAgent extends Agent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = -5912654035145807342L;
    
    
    protected void printInfoMessage(String msg){
        System.out.println(Strings.AGENT_INFO+": "+msg);
    }
}
