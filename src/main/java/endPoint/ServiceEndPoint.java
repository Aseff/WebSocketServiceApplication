package endPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import bean.LockResult;
import bean.Seat;
import bean.SeatStatus;
import message.Message;
import message.MessageDecoder;
import message.MessageEncoder;

@ServerEndpoint(value = "/cinema", decoders = { MessageDecoder.class }, encoders = { MessageEncoder.class })
public class ServiceEndPoint {

	private static boolean initialized;

	private static Set<Session> connections;
	private static MessageEncoder messageEncoder;

	private static List<Seat> seats;
	private static List<LockResult> locks;

	private static int rows;
	private static int columns;

	public ServiceEndPoint() {

		messageEncoder = new MessageEncoder();
		seats = new ArrayList<Seat>();
		locks = new ArrayList<LockResult>();
		rows = 0;
		columns = 0;

		if (connections == null) {
			connections = Collections.synchronizedSet(new HashSet<Session>());
		}

	}

	@OnOpen
	public void open(Session session) {
		System.out.println("WebSocket opened: " + session.getId());
		connections.add(session);
	}

	@OnClose
	public void close(Session session) {
		System.out.println("WebSocket closed: " + session.getId());
		connections.remove(session);
	}

	@OnError
	public void error(Throwable throwable) {
		System.out.println("WebSocket error: " + throwable.getMessage());
	}

	@OnMessage
	public void message(Message message, Session session) throws IOException, EncodeException {
		System.out.println("WebSocket message: " + message);

		switch (message.getType()) {
		case "initRoom":
			this.initializeRoom(message, session);
			break;

		case "getRoomSize":
			this.sendRoomSize(session);
			break;

		case "updateSeats":
			this.updateSeats(session);
			break;

		case "lockSeat":
			this.lockSeat(message, session);
			break;

		case "unlockSeat":
			this.unlockSeat(message, session, SeatStatus.FREE);
			break;

		case "reserveSeat":
			this.reserveSeat(message, session, SeatStatus.RESERVED);
			break;

		default:
			System.out.println("Message type cannot be handle with: " + message.getType());
			break;
		}
	}

	private void updateSeats(Session session) throws IOException, EncodeException {
		for (Seat seat : seats) {
			Message seatStatusMessage = new Message();

			seatStatusMessage.setType("seatStatus");
			seatStatusMessage.addProperty("row", seat.getRow());
			seatStatusMessage.addProperty("column", seat.getColumn());
			seatStatusMessage.addProperty("status", seat.getStatus().toString().toLowerCase());

			this.sendMessage(seatStatusMessage, session);
		}
	}

	private void initializeRoom(Message message, Session session) throws IOException, EncodeException {
		Map<String, Object> properties = message.getProperties();
		if (properties.containsKey("rows") && properties.containsKey("columns")) {
			int newRows = new Integer(properties.get("rows").toString());
			int newColumns = new Integer(properties.get("columns").toString());

			if (newRows <= 0 || newColumns <= 0) {
				String errorMessage = "Number of rows and columns must be greater than 0.";
				this.sendErrorMessage(errorMessage, session);
				return;
			}

			rows = newRows;
			columns = newColumns;
			seats = new ArrayList<Seat>();
			locks = new ArrayList<LockResult>();
			initialized = true;

			for (int i = 1; i < rows + 1; i++) {
				for (int j = 1; j < columns + 1; j++) {
					seats.add(new Seat(i, j));
				}
			}

			Message roomSizeMessage = new Message();

			roomSizeMessage.setType("roomSize");
			roomSizeMessage.addProperty("rows", rows);
			roomSizeMessage.addProperty("columns", columns);

			this.broadcastMessage(roomSizeMessage);
		}
	}

	private void sendRoomSize(Session session) throws IOException, EncodeException {
		Message roomSizeMsg = new Message();

		roomSizeMsg.setType("roomSize");
		roomSizeMsg.addProperty("rows", rows);
		roomSizeMsg.addProperty("columns", columns);

		this.sendMessage(roomSizeMsg, session);

	}

	private void lockSeat(Message message, Session session) throws IOException, EncodeException {
		Map<String, Object> properties = message.getProperties();
		if (properties.containsKey("row") && properties.containsKey("column")) {

			int row = new Integer(properties.get("row").toString());
			int column = new Integer(properties.get("column").toString());
			Seat seatToLock = getSeatAt(row, column);

			if (seatToLock.getStatus() != SeatStatus.FREE) {
				String errorMessage = "Seat is not free!";
				this.sendErrorMessage(errorMessage, session);
				return;
			}
			
			if (!setSeatStatus(seatToLock, SeatStatus.LOCKED)) {
				String errorMessage = "Failed to change seat status at row: " + seatToLock.getRow() + ", column: " + seatToLock.getColumn() + ".";
				this.sendErrorMessage(errorMessage, session);
				return;
			}
			
			LockResult lockResult = new LockResult(row, column);
			locks.add(lockResult);
			
			Message lockResultMessage = new Message();
			
			lockResultMessage.setType("lockResult");
			lockResultMessage.addProperty("lockId", lockResult.getId());
			
			this.sendMessage(lockResultMessage, session);
			this.broadcastSeatStatus(seatToLock);
		}
	}

	private void unlockSeat(Message message, Session session, SeatStatus status) throws IOException, EncodeException {
		Map<String, Object> properties = message.getProperties();
		if (properties.containsKey("lockId")) {
			String lockId = properties.get("lockId").toString();
			lockId = lockId.substring(1, lockId.length() - 1);
			LockResult lock = getLock(lockId);
			
			if (lock == null) {
				String errorMessage = lockId + " lock does not exist.";
				this.sendErrorMessage(errorMessage, session);
				return;
			}
			
			Seat seat = getSeatAt(lock.getRow(), lock.getColumn());
			if (!setSeatStatus(seat, status)) {
				String errorMessage = "Failed to change  seat status at row: " + seat.getRow() + ", column: " + seat.getColumn() + ".";
				this.sendErrorMessage(errorMessage, session);
				return;
			}
			
			locks.remove(lock);
			this.broadcastSeatStatus(seat);

		}

	}

	private void reserveSeat(Message message, Session session, SeatStatus status) throws IOException, EncodeException {
		this.unlockSeat(message, session, status);
	}
	private boolean setSeatStatus(Seat seatToLock, SeatStatus status) {
		for (Seat seat: seats) {
			if (seat.equals(seatToLock)) {
				seatToLock.setStatus(status);
				
				int i = seats.indexOf(seat);
				seats.remove(i);
				seats.add(i, seatToLock);
				
				return true;
			}
		}
		
		return false;
	}
	
	private static Seat getSeatAt(int row, int column) {
		for (Seat seat: seats) {
			if (seat.getRow() == row && seat.getColumn() == column) {
				return seat;
			}
		}
		return null;
	}
	
	private static LockResult getLock(String lockId) {
		for (LockResult lock: locks) {
			if (lock.getId().equals(lockId)) {
				return lock;
			}
		}
		return null;
	}

	private void broadcastSeatStatus(Seat seat) throws IOException, EncodeException {
		Message seatStatusMessage = new Message();

		seatStatusMessage.setType("seatStatus");
		seatStatusMessage.addProperty("row", seat.getRow());
		seatStatusMessage.addProperty("column", seat.getColumn());
		seatStatusMessage.addProperty("status", seat.getStatus().toString().toLowerCase());

		broadcastMessage(seatStatusMessage);
	}

	private void broadcastMessage(Message roomSizeMessage) throws IOException, EncodeException {
		synchronized (connections) {
			for (Session client : connections) {
				sendMessage(roomSizeMessage, client);
			}
		}
	}

	private void sendMessage(Message message, Session session) throws IOException, EncodeException {
		session.getBasicRemote().sendText(messageEncoder.encode(message));

	}

	private void sendErrorMessage(String str, Session session) throws IOException, EncodeException {
		Message errorMessage = new Message();

		errorMessage.setType("error");
		errorMessage.addProperty("message", str);

		sendMessage(errorMessage, session);

	}

}
