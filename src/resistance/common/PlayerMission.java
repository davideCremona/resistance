package resistance.common;

public class PlayerMission {
    
    private int playersRequired;
    private int difficulty;
    
    public PlayerMission(){
        this.difficulty = 1;
    }
    
    public PlayerMission(int playersRequired){
        this();
        this.playersRequired = playersRequired;
    }
    
    public PlayerMission(int playersRequired, int difficulty){
        this.playersRequired = playersRequired;
        this.difficulty = difficulty;
    }
    
    public int getDifficulty(){
        return this.difficulty;
    }
    
    public int getPlayersRequired(){
        return this.playersRequired;
    }

}
