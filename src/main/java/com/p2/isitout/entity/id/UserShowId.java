package com.p2.isitout.entity.id;

import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class UserShowId implements Serializable {

    private UUID userId;
    private UUID showId;
}
