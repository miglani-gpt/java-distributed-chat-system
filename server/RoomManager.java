package server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final ConcurrentHashMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ClientHandler, String> clientRooms = new ConcurrentHashMap<>();

    private static final String DEFAULT_ROOM = "global";

    public RoomManager() {
        rooms.put(DEFAULT_ROOM, ConcurrentHashMap.newKeySet());
    }

    // ==============================
    // Join Room (NON-BLOCKING)
    // ==============================
    public void joinRoom(String room, ClientHandler client) {

        String normalizedRoom = normalize(room);

        leaveCurrentRoom(client);

        rooms.putIfAbsent(normalizedRoom, ConcurrentHashMap.newKeySet());
        rooms.get(normalizedRoom).add(client);

        clientRooms.put(client, normalizedRoom);
    }

    // ==============================
    // Leave Current Room
    // ==============================
    public void leaveCurrentRoom(ClientHandler client) {

        String currentRoom = clientRooms.get(client);

        if (currentRoom != null) {
            Set<ClientHandler> clients = rooms.get(currentRoom);

            if (clients != null) {
                clients.remove(client);

                // 🔥 cleanup empty rooms (except default)
                if (clients.isEmpty() && !currentRoom.equals(DEFAULT_ROOM)) {
                    rooms.remove(currentRoom);
                }
            }
        }

        clientRooms.remove(client);
    }

    // ==============================
    // Get Room Members (SAFE)
    // ==============================
    public Set<ClientHandler> getRoomMembers(String room) {
        String normalizedRoom = normalize(room);
        Set<ClientHandler> members = rooms.get(normalizedRoom);
        return members != null ? members : ConcurrentHashMap.newKeySet();
    }

    // ==============================
    // Get Client Room
    // ==============================
    public String getClientRoom(ClientHandler client) {
        return clientRooms.getOrDefault(client, DEFAULT_ROOM);
    }

    // ==============================
    // List Rooms
    // ==============================
    public Set<String> getAllRooms() {
        return rooms.keySet();
    }

    // ==============================
    // Cleanup
    // ==============================
    public void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
    }

    // ==============================
    // 🔥 Normalization (NEW)
    // ==============================
    private String normalize(String room) {
        if (room == null) return DEFAULT_ROOM;
        return room.trim().toLowerCase();
    }
}