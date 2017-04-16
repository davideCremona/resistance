package resistance.agents;


public class GameMaster extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = 2052656452229351686L;
    
    protected void setup(){
        this.printInfoMessage("GameMaster "+this.getAID().getName()+" born!");
    }

}
