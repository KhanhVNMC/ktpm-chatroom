package dev.ktpm.model;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public record ChatMessage(
	UUID chatMessageUUID, 
	long sentTimestamp ,
	UUID chatRoomUUID,
	UUID sender, 
	String content, 
	List<URI> attachments
) {}
