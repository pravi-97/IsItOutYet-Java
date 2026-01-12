package com.p2.isitout.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserDto {

    @Id
    private UUID userId;

    private String username;
    private String email;

    private Instant createdAt;
}
