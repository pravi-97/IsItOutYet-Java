package com.p2.isitout.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    public void setCustomClaims(String firebaseUid, @NonNull UUID userId, List<String> roles) throws FirebaseAuthException {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("roles", roles);

        FirebaseAuth.getInstance().setCustomUserClaims(firebaseUid, claims);
    }

    public UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getClaim("userId"));
    }

    public List<String> extractRoles(Jwt jwt) {
        return jwt.getClaimAsStringList("roles");
    }
}
