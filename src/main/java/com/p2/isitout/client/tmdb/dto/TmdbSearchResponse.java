package com.p2.isitout.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)


public class TmdbSearchResponse {

    private int page;

    private List<TmdbTvShowDto> results;
}
