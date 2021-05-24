package message;

import java.util.HashMap;
import java.util.Map;

public class Message {
	
	private String type;
	private Map<String, Object> properties;
	
	public Message() {
		this.properties = new HashMap<String, Object>();
	}
	public Message(String type, Map<String, Object> properties) {
		
		this.type = type;
		this.properties = properties;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Map<String, Object> getProperties() {
		return (HashMap<String, Object>) this.properties;
	}
		
	
	public void addProperty(String name, Object value) {
		this.properties.put(name, value);
	}
	

}
