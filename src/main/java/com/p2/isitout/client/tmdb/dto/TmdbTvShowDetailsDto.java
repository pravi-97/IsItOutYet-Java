package com.p2.isitout.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TmdbTvShowDetailsDto {
    private boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("created_by")
    private List < CreatedBy > createdBy;

    @JsonProperty("episode_run_time")
    private List < Integer > episodeRunTime;

    @JsonProperty("first_air_date")
    private String firstAirDate;
    private List < Genres > genres;
    private String homepage;
    private Integer id;
    private boolean inProduction;
    private List < String > languages;

    @JsonProperty("last_air_date")
    private String lastAirDate;

    @JsonProperty("last_episode_to_air")
    private LastEpisodeToAir lastEpisodeToAir;
    private String name;

    @JsonProperty("next_episode_to_air")
    private LastEpisodeToAir nextEpisodeToAir;
    private List < Networks > networks;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;

    @JsonProperty("origin_country")
    private List < String > originCountry;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_name")
    private String originalName;
    private String overview;
    private double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("production_companies")
    private List < ProductionCompanies > productionCompanies;

    @JsonProperty("production_countries")
    private List < ProductionCountries > productionCountries;
    private List < Seasons > seasons;

    @JsonProperty("spoken_languages")
    private List < SpokenLanguages > spokenLanguages;
    private String status;

    private String tagline;
    private String type;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @Data
    public static class CreatedBy {
        private Integer id;

        @JsonProperty("credit_id")
        private String creditId;
        private String name;

        @JsonProperty("original_name")
        private String originalName;
        private Integer gender;

        @JsonProperty("profile_path")
        private Object profilePath;

    }

    @Data
    public static class Genres {
        private Integer id;
        private String name;

    }

    @Data
    public static class LastEpisodeToAir {
        private Integer id;
        private String name;
        private String overview;

        @JsonProperty("vote_average")
        private double voteAverage;

        @JsonProperty("vote_count")
        private Integer voteCount;

        @JsonProperty("air_date")
        private String airDate;

        @JsonProperty("episode_number")
        private Integer episodeNumber;

        @JsonProperty("episode_type")
        private String episodeType;

        @JsonProperty("production_code")
        private String productionCode;
        private Integer runtime;

        @JsonProperty("season_number")
        private Integer seasonNumber;

        @JsonProperty("show_id")
        private Integer showId;

        @JsonProperty("still_path")
        private String stillPath;

    }

    @Data
    public static class Networks {
        private Integer id;

        @JsonProperty("logo_path")
        private String logoPath;
        private String name;

        @JsonProperty("origin_country")
        private String originCountry;

    }

    @Data
    public static class ProductionCompanies {
        private Integer id;

        @JsonProperty("logo_path")
        private String logoPath;
        private String name;

        @JsonProperty("origin_country")
        private String originCountry;

    }

    @Data
    public static class ProductionCountries {

        @JsonProperty("iso_3166_1")
        private String iso31661;
        private String name;

    }

    @Data
    public static class Seasons {

        @JsonProperty("air_date")
        private String airDate;

        @JsonProperty("episode_count")
        private Integer episodeCount;
        private Integer id;
        private String name;
        private String overview;

        @JsonProperty("poster_path")
        private String posterPath;

        @JsonProperty("season_number")
        private Integer seasonNumber;

        @JsonProperty("vote_average")
        private double voteAverage;

    }

    @Data
    public static class SpokenLanguages {
        @JsonProperty("english_name")
        private String englishName;

        @JsonProperty("iso_639_1")
        private String iso6391;
        private String name;

    }

}