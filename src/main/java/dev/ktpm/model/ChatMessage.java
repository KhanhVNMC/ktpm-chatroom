package dev.ktpm.model;

import java.net.URL;
import java.util.List;
import java.util.UUID;

public record ChatMessage(
	UUID chatMessageUUID, 
	UUID sender, 
	String content, 
	List<URL> attachments,
	long unixTimestamp
) {}
