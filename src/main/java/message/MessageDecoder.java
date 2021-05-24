package message;

import java.io.StringReader;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class MessageDecoder implements Decoder.Text<Message> {
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(EndpointConfig config) {
		// TODO Auto-generated method stub
	}

	@Override
	public Message decode(String message) throws DecodeException {
		StringReader strReader = new StringReader(message);
		JsonReader jsonReader = Json.createReader(strReader);
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		
		Message resultMessage = new Message();
		resultMessage.setType(jsonObject.getString("type"));
		
		for (Entry<String, JsonValue> property: jsonObject.entrySet()) {
			if (!property.getKey().equals("type")) {
				resultMessage.addProperty(property.getKey(), property.getValue());
			}
		}
		
		return resultMessage;
	}

	@Override
	public boolean willDecode(String message) {
		return true;
	}

	
}
