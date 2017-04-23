package resistance.common;

public enum Strings {
    
    AGENT_INFO,
    PLAYER_INFORM,
    REBEL,
    SPY,
    SPY_INFORM,
    START_ROUND,
    END_ROUND,
    END_GAME,
    MISSION_SUCCESS,
    MISSION_FAIL, 
    PREFERENCE_INFORM, 
    TEAM_FORMATION, 
    MISSION_VOTE, 
    MISSION_OUTCOME, 
    MISSION_FAIL_N, 
    MISSION_SUCCESS_N, 
    PLAYER_NAME, 
    NEW_ROUND;

    public boolean compareTo(String other) {
        return this.toString().equals(other);
    }
}
