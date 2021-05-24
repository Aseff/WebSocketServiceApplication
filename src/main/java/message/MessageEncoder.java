package message;

import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class MessageEncoder  implements Encoder.Text<Message>{

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(EndpointConfig arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String encode(Message message) throws EncodeException {
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("type", message.getType());
		
		for (Entry<String, Object> property: message.getProperties().entrySet()) {
			if(property.getValue() instanceof String) {
				String value = (String) property.getValue();
				jsonBuilder.add(property.getKey(), value);
			} else if (property.getValue() instanceof Integer) {
				Integer value = (Integer) property.getValue();
				jsonBuilder.add(property.getKey(), value);
			} 
			
		}
		
		return jsonBuilder.build().toString();
	}

}
