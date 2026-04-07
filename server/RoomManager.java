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
    // JOIN ROOM
    // ==============================
    public void joinRoom(String room, ClientHandler client) {

        if (client == null) return; // 🔥 defensive

        String newRoom = normalize(room);

        synchronized (client) {

            String oldRoom = clientRooms.get(client);

            if (newRoom.equals(oldRoom)) return;

            if (oldRoom != null) {
                removeFromRoom(oldRoom, client);
            }

            rooms.computeIfAbsent(newRoom, r -> ConcurrentHashMap.newKeySet())
                 .add(client);

            clientRooms.put(client, newRoom);
        }
    }

    // ==============================
    // LEAVE ROOM
    // ==============================
    public void leaveCurrentRoom(ClientHandler client) {

        if (client == null) return;

        synchronized (client) {

            String room = clientRooms.remove(client);

            if (room != null) {
                removeFromRoom(room, client);
            }
        }
    }

    // ==============================
    // INTERNAL REMOVE (FIXED RACE)
    // ==============================
    private void removeFromRoom(String room, ClientHandler client) {

        Set<ClientHandler> members = rooms.get(room);

        if (members != null) {
            members.remove(client);

            // 🔥 safer removal
            if (members.isEmpty() && !room.equals(DEFAULT_ROOM)) {
                rooms.computeIfPresent(room, (r, set) -> set.isEmpty() ? null : set);
            }
        }
    }

    // ==============================
    // SAFE SNAPSHOT
    // ==============================
    public Set<ClientHandler> getRoomMembers(String room) {

        String normalized = normalize(room);
        Set<ClientHandler> members = rooms.get(normalized);

        if (members == null) return Collections.emptySet();

        // 🔥 return snapshot (important)
        return Set.copyOf(members);
    }

    // ==============================
    public String getClientRoom(ClientHandler client) {
        return clientRooms.get(client);
    }

    // ==============================
    public Set<String> getAllRooms() {
        return Set.copyOf(rooms.keySet()); // 🔥 snapshot
    }

    // ==============================
    public void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
    }

    // ==============================
    private String normalize(String room) {
        if (room == null) return DEFAULT_ROOM;
        String trimmed = room.trim().toLowerCase();
        return trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
    }
}