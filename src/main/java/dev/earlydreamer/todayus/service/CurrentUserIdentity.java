package dev.earlydreamer.todayus.service;

public record CurrentUserIdentity(String authUserId, String displayName, String authProvider) {
}
