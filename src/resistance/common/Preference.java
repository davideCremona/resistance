package resistance.common;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONArray;

public class Preference {
    
    private List<String> preferenceOrder;
    private int current;
    
    public Preference(){
        this.preferenceOrder = new ArrayList<String>();
        this.current = 0;
    }
    
    /**
     * Insert a new element in the preference order.
     * The new element is preferred to all the other elements in the preference order.
     * 
     * (if the previous order was [a,b], the new order will be [element,a,b]
     * corresponding to: element > a > b)
     *
     * @param element the element
     */
    public void insert(String element){
        this.preferenceOrder.add(0,element);
    }
    
    public String getNext(){
        String element = this.preferenceOrder.get(current);
        current++;
        return element;
    }
    
    public List<String> getPreference(){
        return this.preferenceOrder;
    }
    
    public Entry <String, Integer> getNextWithPoints(){
        String element = this.preferenceOrder.get(current);
        Integer points = (this.preferenceOrder.size()-1) - current;
        current++;
        return new AbstractMap.SimpleEntry<String, Integer>(element, points);
    }
    
    @SuppressWarnings("unchecked")
    public JSONArray toJSONArray(){
        JSONArray jsonRepresentation = new JSONArray();
        jsonRepresentation.addAll(preferenceOrder);
        return jsonRepresentation;
    }
    
    @SuppressWarnings("unchecked")
    public JSONArray reversedJSONArray(){
        JSONArray jsonRepresentation = new JSONArray();
        List<String> copy = new ArrayList<String>();
        copy = this.preferenceOrder;
        Collections.reverse(copy);
        jsonRepresentation.addAll(copy);
        return jsonRepresentation;
    }
    
    public String toJSONString(){
        return this.toJSONArray().toJSONString();
    }
    
    public void parseJSON(JSONArray jsonPreference){
        for(Object element : jsonPreference)
            this.preferenceOrder.add((String) element);
    }

}
