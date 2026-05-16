package com.p2.isitout.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class UserDto {

    private UUID userId;
    private String timeZone;
    private String username;
    private String email;
    private String name;

    private Instant createdAt;
}
