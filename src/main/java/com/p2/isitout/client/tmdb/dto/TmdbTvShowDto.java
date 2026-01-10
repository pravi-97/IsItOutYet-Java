package com.p2.isitout.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbTvShowDto {

    private Long id;
    private String name;

    @JsonProperty("original_name")
    private String originalName;

    private String overview;

    @JsonProperty("first_air_date")
    private LocalDate firstAirDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private Double popularity;

    @JsonProperty("origin_country")
    private List<String> originCountry;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;
}
