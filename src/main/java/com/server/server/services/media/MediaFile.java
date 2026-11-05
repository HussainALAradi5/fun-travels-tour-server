package com.server.server.services.media;

/** A stored file's bytes with the name and type to send it under (internal to the media download, not an API DTO). */
public record MediaFile(String fileName, String contentType, byte[] content) {
}
