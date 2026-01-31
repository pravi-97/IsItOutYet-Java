package com.p2.isitout.dto.response;

import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.dto.UserShowDto;
import lombok.Data;

import java.util.UUID;

@Data
public class MyShowListDto {
    private UUID showId;
    private UserShowDto userShowDto;
    private TmdbTvShowDetailsDto tmdbTvShowDetailsDto;
}
