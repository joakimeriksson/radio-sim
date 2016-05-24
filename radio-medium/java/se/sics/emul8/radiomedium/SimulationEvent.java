package se.sics.emul8.radiomedium;

import java.util.HashMap;

/*
 * Simulaton event that contains information about packets, configuration and log information for
 * all event listenrs - NOTE: this is for monitoring, visualization, etc. Not actual packets sent
 * between nodes.
 */
public class SimulationEvent {
    public enum EventType {
        RADIO_PACKET,
        RADIO_CONFIG,
        LOG_MESSAGE
    };

    private long time;
    private Node source;
    private EventType type;
    private HashMap<String,Object> data = new HashMap<String, Object>();

    public SimulationEvent(EventType type, long time, Node source) {
        this.time = time;
        this.type = type;
        this.source = source;
    }
    
    public long getTime() {
        return time;
    }
    
    public Node getSource() {
        return source;
    }
    
    public EventType getType() {
        return type;
    }

    public void setData(String name, Object value) {
        data.put(name, value);
    }
    
    public Object getData(String name) {
        if (data == null) {
            return null;
        }
        return data.get(name);
    }
    
}
