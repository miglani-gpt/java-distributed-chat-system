package server;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final ConcurrentHashMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ClientHandler, String> clientRooms = new ConcurrentHashMap<>();

    // 🔥 NEW: MESSAGE HISTORY
    private final ConcurrentHashMap<String, java.util.List<String>> roomHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 50;

    private static final String DEFAULT_ROOM = "global";

    public RoomManager() {
        rooms.put(DEFAULT_ROOM, ConcurrentHashMap.newKeySet());
    }

    // ==============================
    // JOIN ROOM
    // ==============================
    public void joinRoom(String room, ClientHandler client) {

        if (client == null)
            return;

        String newRoom = normalize(room);

        synchronized (client) {

            String oldRoom = clientRooms.get(client);

            if (newRoom.equals(oldRoom))
                return;

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

        if (client == null)
            return;

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

        if (members == null)
            return Collections.emptySet();

        return Set.copyOf(members);
    }

    // ==============================
    public String getClientRoom(ClientHandler client) {
        return clientRooms.get(client);
    }

    // ==============================
    public Set<String> getAllRooms() {
        return Set.copyOf(rooms.keySet());
    }

    // ==============================
    public void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
    }

    // ==============================
    // 🔥 NEW: ADD MESSAGE TO HISTORY
    // ==============================
    public void addMessage(String room, String msg) {

        room = normalize(room);

        roomHistory.putIfAbsent(room,
                Collections.synchronizedList(new java.util.ArrayList<>()));

        var list = roomHistory.get(room);

        synchronized (list) {
            list.add(msg);

            if (list.size() > MAX_HISTORY) {
                list.remove(0);
            }
        }
    }

    // ==============================
    // 🔥 NEW: GET RECENT MESSAGES
    // ==============================
    public java.util.List<String> getRecentMessages(String room, int n) {

        room = normalize(room);

        var list = roomHistory.getOrDefault(room, new java.util.ArrayList<>());

        synchronized (list) {
            int size = list.size();
            int start = Math.max(0, size - n);

            return new java.util.ArrayList<>(list.subList(start, size));
        }
    }

    // ==============================
    private String normalize(String room) {
        if (room == null)
            return DEFAULT_ROOM;
        String trimmed = room.trim().toLowerCase();
        return trimmed.isEmpty() ? DEFAULT_ROOM : trimmed;
    }
}