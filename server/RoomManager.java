package server;

import java.util.Collections;
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
    // JOIN ROOM (STRONGER CONSISTENCY)
    // ==============================
    public void joinRoom(String room, ClientHandler client) {

        String newRoom = normalize(room);

        synchronized (client) { // 🔥 per-client lock (clean & effective)

            String oldRoom = clientRooms.get(client);

            if (newRoom.equals(oldRoom)) return;

            // Remove from old
            if (oldRoom != null) {
                removeFromRoom(oldRoom, client);
            }

            // Add to new
            rooms.computeIfAbsent(newRoom, r -> ConcurrentHashMap.newKeySet())
                 .add(client);

            clientRooms.put(client, newRoom);
        }
    }

    // ==============================
    // LEAVE CURRENT ROOM
    // ==============================
    public void leaveCurrentRoom(ClientHandler client) {

        synchronized (client) {

            String room = clientRooms.remove(client);

            if (room != null) {
                removeFromRoom(room, client);
            }
        }
    }

    // ==============================
    // INTERNAL REMOVE
    // ==============================
    private void removeFromRoom(String room, ClientHandler client) {

        Set<ClientHandler> members = rooms.get(room);

        if (members != null) {
            members.remove(client);

            // safer cleanup
            if (members.isEmpty() && !room.equals(DEFAULT_ROOM)) {
                rooms.remove(room, members);
            }
        }
    }

    // ==============================
    // GET ROOM MEMBERS
    // ==============================
    public Set<ClientHandler> getRoomMembers(String room) {

        String normalized = normalize(room);
        Set<ClientHandler> members = rooms.get(normalized);

        if (members == null) return Collections.emptySet();

        // 🔥 return safe view
        return Collections.unmodifiableSet(members);
    }

    // ==============================
    // GET CLIENT ROOM
    // ==============================
    public String getClientRoom(ClientHandler client) {
        return clientRooms.get(client);
    }

    // ==============================
    // LIST ROOMS
    // ==============================
    public Set<String> getAllRooms() {
        return Collections.unmodifiableSet(rooms.keySet());
    }

    // ==============================
    // REMOVE CLIENT
    // ==============================
    public void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
    }

    // ==============================
    // NORMALIZE
    // ==============================
    private String normalize(String room) {
        if (room == null) return DEFAULT_ROOM;
        String trimmed = room.trim().toLowerCase();
        return trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
    }
}