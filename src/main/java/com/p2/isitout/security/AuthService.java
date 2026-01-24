package com.p2.isitout.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    private FirebaseToken getTokenFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FirebaseToken token) {
            return token;
        }
        throw new IllegalStateException("No Firebase token found in Security Context");
    }

    public UUID extractUserId() {
        // Firebase claims are stored in a Map<String, Object>
        String userId = (String) getTokenFromContext().getClaims().get("internal_user_id");
        return userId != null ? UUID.fromString(userId) : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles() {
        return (List<String>) getTokenFromContext()
                .getClaims()
                .getOrDefault("roles", List.of());
    }

    public String extractEmail() {
        return getTokenFromContext().getEmail();
    }
    public String extractFirebaseUid() {
        return getTokenFromContext().getUid();
    }

    public void setCustomClaims(@NonNull UUID userId, List<String> roles) throws FirebaseAuthException {
        Map<String, Object> claims = new HashMap<>();
        claims.put("internal_user_id", userId.toString());
        claims.put("roles", roles);

        FirebaseAuth.getInstance().setCustomUserClaims(extractFirebaseUid(), claims);
    }
}