package dev.ktpm.impl.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.ktpm.api.dto.RoomOverviewResponse;
import dev.ktpm.model.ChatRoom;
import dev.ktpm.model.RoomMembership;
import dev.ktpm.repository.IChatRoomRepository;
import dev.ktpm.repository.IRoomMembershipRepository;
import dev.ktpm.repository.IUserRepository;
import dev.ktpm.service.IChatRoomService;
import dev.ktpm.consts.RoomPermissions;

public class ChatRoomService implements IChatRoomService {
    private final IChatRoomRepository chatRoomRepo;
    private final IRoomMembershipRepository roomMembershipRepo;
    private final IUserRepository userRepo;

    public ChatRoomService(
            IChatRoomRepository chatRoomRepo,
            IRoomMembershipRepository roomMembershipRepo,
            IUserRepository userRepo
    ) {
        this.chatRoomRepo = chatRoomRepo;
        this.roomMembershipRepo = roomMembershipRepo;
        this.userRepo = userRepo;
    }

    @Override
    public List<ChatRoom> listPublicRooms(UUID requester, int limit) {
        if (requester != null && userRepo.get(requester).isEmpty()) {
            throw new IllegalArgumentException("Requester not found");
        }

        int normalizedLimit = limit <= 0 ? 50 : Math.min(limit, 100);
        return chatRoomRepo.listPublicRooms(normalizedLimit);
    }

    @Override
    public void joinRoom(
            UUID userUUID,
            UUID roomUUID,
            Optional<String> inviteCode
    ) {
        // neu ton tai user và room
        if (userUUID == null || roomUUID == null) {
            throw new IllegalArgumentException(
                    "User UUID and roomUUID are null"
            );
        }

        if (userRepo.get(userUUID).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        ChatRoom room = chatRoomRepo.get(roomUUID)
                .orElseThrow(() ->
                        new IllegalArgumentException("Room not found")
                );

        if (roomMembershipRepo.exists(roomUUID, userUUID)) {
            throw new IllegalStateException(
                    "User is already inside the room"
            );
        }

        /*
         * Chưa có trường invite code trong ChatRoom hoặc database,
         * nên hiện tại chỉ cho phép tham gia phòng public.
         */
        if (!room.isPublic()) {
            throw new IllegalStateException(
                    "This is a private room, you have to be invited to join"
            );
        }

        RoomMembership membership = new RoomMembership(
                roomUUID,
                userUUID,
                System.currentTimeMillis(),
                RoomPermissions.SEND_MESSAGES,
                0
        );

        roomMembershipRepo.create(membership);
    }

    @Override
    public void leaveRoom(
            UUID userUUID,
            UUID roomUUID,
            boolean silentLeave,
            Optional<String> reason
    ) {
        if (userUUID == null || roomUUID == null) {
            throw new IllegalArgumentException(
                    "User UUID and roomUUID are null"
            );
        }

        if (userRepo.get(userUUID).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        ChatRoom room = chatRoomRepo.get(roomUUID)
                .orElseThrow(() ->
                        new IllegalArgumentException("Room not found")
                );

        if (!roomMembershipRepo.exists(roomUUID, userUUID)) {
            throw new IllegalStateException(
                    "User is not a member of this room"
            );
        }

        if (room.owner().equals(userUUID)) {
            throw new IllegalStateException(
                    "Room owner cannot leave the room"
            );
        }


        roomMembershipRepo.delete(roomUUID, userUUID);

        /*
         * silentLeave và reason chưa có luồng logic trong persistence layer,
         * nên chưa thêm vô.
         */
    }

    @Override
    public void deleteRoom(
            UUID requesterUUID,
            UUID roomUUID
    ) {
        if (requesterUUID == null || roomUUID == null) {
            throw new IllegalArgumentException(
                    "Requester UUID and room UUID must not be null"
            );
        }

        if (userRepo.get(requesterUUID).isEmpty()) {
            throw new IllegalArgumentException(
                    "Requester not found"
            );
        }

        ChatRoom room = chatRoomRepo.get(roomUUID)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room not found"
                        )
                );

        if (!room.owner().equals(requesterUUID)) {
            throw new IllegalStateException(
                    "Only the room owner can delete the room"
            );
        }

        /*
         * Xóa tất cả membership trước khi xóa room.
         */
        List<UUID> memberUUIDs =
                roomMembershipRepo.getMemberUUIDs(roomUUID);

        for (UUID memberUUID : memberUUIDs) {
            roomMembershipRepo.delete(roomUUID, memberUUID);
        }

        chatRoomRepo.delete(roomUUID);
    }

    @Override
    public RoomOverviewResponse getOverview(
            UUID requester,
            UUID roomUUID
    ) {
        // hàm RoomOverviewResponse trả về 3 giá trị, content lời mời,
        // owner name, và danh sách member names.

        if (requester == null || roomUUID == null) {
            throw new IllegalArgumentException(
                    "Requester UUID and room UUID must not be null"
            );
        }

        if (userRepo.get(requester).isEmpty()) {
            throw new IllegalArgumentException("Requester not found");
        }

        ChatRoom room = chatRoomRepo.get(roomUUID)
                .orElseThrow(() ->
                        new IllegalArgumentException("Room not found")
                );

        boolean isMember =
                roomMembershipRepo.exists(roomUUID, requester);

        if (!room.isPublic() && !isMember) {
            throw new IllegalStateException(
                    "You are not a member of this private room"
            );
        }
        // owner name
        String ownerName = userRepo.get(room.owner())
                .map(user -> user.displayName() != null
                        ? user.displayName()
                        : user.username())
                .orElseThrow(() ->
                        new IllegalStateException("Room owner not found")
                );

        // trả về member names
        List<String> memberNames = roomMembershipRepo
                .getMemberUUIDs(roomUUID)
                .stream()
                .limit(50) // chỉ xem tối đa 50 member names
                .map(memberUUID -> userRepo.get(memberUUID)
                        .map(user -> user.displayName() != null
                                ? user.displayName()
                                : user.username())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Room member not found: "
                                                + memberUUID
                                )))
                .toList();

        return new RoomOverviewResponse(
                room.motd(),
                ownerName,
                memberNames
        );
    }
}