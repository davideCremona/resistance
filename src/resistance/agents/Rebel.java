package resistance.agents;

public class Rebel extends ResistanceAgent{

    /**
     * Generated SerialVersionUID
     */
    private static final long serialVersionUID = -4115352088503502124L;
    
    protected void setup(){
        this.printInfoMessage("Rebel "+this.getAID().getName()+" born!");
    }

}
