package resistance.agents;

import java.util.HashMap;
import java.util.Map;

import resistance.common.Strings;

public class PlayersFactory {
    
    private static final String SPY_CLASSPATH = "resistance.agents.Spy";
    private static final String REBEL_CLASSPATH = "resistance.agents.Rebel";
    
    private static final Map<Strings, String> roles = new HashMap<Strings, String>(){

        /**
         * Generated SerialVersionUID
         */
        private static final long serialVersionUID = 3214900519336614457L;
        
        {
            put(Strings.REBEL, PlayersFactory.REBEL_CLASSPATH);
            put(Strings.SPY, PlayersFactory.SPY_CLASSPATH);
        }
        
    };
    
    public static String getCharacter(Strings role){
        return PlayersFactory.roles.get(role);
    }

}
