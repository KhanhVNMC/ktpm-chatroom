package dev.ktpm.api.dto;

public record EditMessageRequest(String newContent, boolean keepAttachments) {}
