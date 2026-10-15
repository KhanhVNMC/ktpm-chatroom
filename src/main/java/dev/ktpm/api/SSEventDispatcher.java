package dev.ktpm.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ktpm.consts.RealtimeEventType;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IRoomMembershipRepository;
import io.javalin.http.sse.SseClient;

public class SSEventDispatcher {
	private record Connection(String sessionID, SseClient client) {}
	
	private static final ObjectMapper JSON = new ObjectMapper();
	
	private final IRoomMembershipRepository memberships;
	private final Map<UUID, List<Connection>> connections = new ConcurrentHashMap<>();
	
	public SSEventDispatcher(IRoomMembershipRepository memberships) {
		this.memberships = memberships;
		// keep-alive comment so proxies/browsers dont kill idle connections, also cleans up dead clients
		var heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "máy hút bụi");
			thread.setDaemon(true);
			return thread;
		});
		heartbeat.scheduleAtFixedRate(this::heartbeat, 25, 25, TimeUnit.SECONDS);
	}
	
	public void connect(UUID user, String sessionID, SseClient client) {
		var conn = new Connection(sessionID, client);
		connections.computeIfAbsent(user, k -> new CopyOnWriteArrayList<>()).add(conn);
		client.onClose(() -> connections.computeIfPresent(user, (k, list) -> {
			list.remove(conn);
			return list.isEmpty() ? null : list;
		}));
		client.keepAlive();
		send(conn, RealtimeEventType.CONNECTED, Sputnik.obj("user_uuid", user));
	}
	
	// logout closes every stream of that session too
	public void disconnectSession(String sessionID) {
		for (var list : connections.values()) {
			for (Connection conn : list) {
				if (conn.sessionID().equals(sessionID)) {
					conn.client().close();
				}
			}
		}
	}
	
	public void sendToUsers(Collection<UUID> users, RealtimeEventType type, Object payload) {
		String data = serialize(type, payload);
		for (UUID user : new HashSet<>(users)) {
			for (Connection conn : connections.getOrDefault(user, List.of())) {
				push(conn, data);
			}
		}
	}
	
	// everyone currently in the room + extra (this could be the niga who just got beaned)
	public void sendToRoom(UUID roomUUID, RealtimeEventType type, Object payload, UUID... extra) {
		Set<UUID> recipients = new HashSet<>(List.of(extra));
		try {
			for (RoomMembership m : memberships.getMemberships(roomUUID)) {
				if (m.isInRoom()) {
					recipients.add(m.userUUID());
				}
			}
		} catch (RuntimeException e) {}
		sendToUsers(recipients, type, payload);
	}

	// everyone sharing a room with the user (incl. the user), for presence/profile changes
	public void sendToPeers(UUID user, RealtimeEventType type, Object payload) {
		Set<UUID> recipients = new HashSet<>();
		recipients.add(user);
		try {
			recipients.addAll(memberships.getPeerUUIDs(user));
		} catch (RuntimeException e) {
			// same as above
		}
		sendToUsers(recipients, type, payload);
	}

	private void send(Connection conn, RealtimeEventType type, Object payload) {
		push(conn, serialize(type, payload));
	}

	// SseClient is not threadsafe, so use a mutx
	private void push(Connection conn, String data) {
		synchronized (conn.client()) {
			try {
				conn.client().sendData(data);
			} catch (RuntimeException e) {
				conn.client().close();
			}
		}
	}
	
	private void heartbeat() {
		for (var list : connections.values()) {
			for (Connection conn : list) {
				synchronized (conn.client()) {
					try {
						conn.client().sendComment("keep-alive");
					} catch (RuntimeException e) {
						conn.client().close();
					}
				}
			}
		}
	}
	
	// payload is a json STRING (can nest anything), not a nested object
	private static String serialize(RealtimeEventType type, Object payload) {
		try {
			return JSON.writeValueAsString(Sputnik.obj("type", type, "payload", JSON.writeValueAsString(payload)));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Cannot serialize event", e);
		}
	}
}
