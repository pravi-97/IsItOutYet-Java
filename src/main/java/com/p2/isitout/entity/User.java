package com.p2.isitout.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    private UUID userId;

    private String username;
    private String email;

    private Instant createdAt;
}
