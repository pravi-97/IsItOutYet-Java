package com.p2.isitout.entity.id;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
@Data
public class UserShowId implements Serializable {
    private UUID userId;
    private UUID showId;
}
