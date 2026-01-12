package com.p2.isitout.dto;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
public class UserShowIdDto implements Serializable {

    private UUID userId;
    private UUID showId;
}
