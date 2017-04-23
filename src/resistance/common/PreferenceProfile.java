package resistance.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONObject;

public class PreferenceProfile {
    
    private Map<String, Preference> preferenceProfile;
    
    public PreferenceProfile(){
        this.preferenceProfile = new HashMap<String, Preference>();
    }
    
    public Preference getPreferencesOf(String player){
        return this.preferenceProfile.get(player);
    }
    
    public Set<Entry<String, Preference>> getAll(){
        return this.preferenceProfile.entrySet();
    }
    
    public void addPreferencesOf(String player, Preference preferences){
        this.preferenceProfile.put(player, preferences);
    }
    
    @SuppressWarnings("unchecked")
    public JSONObject toJSONObject(){
        JSONObject jsonRepresentation = new JSONObject();
        for(Entry<String, Preference> e : preferenceProfile.entrySet()){
            jsonRepresentation.put(e.getKey(), e.getValue().toJSONArray());
        }
        return jsonRepresentation;
    }
    
    public String toJSONString(){
        return this.toJSONObject().toJSONString();
    }
}
