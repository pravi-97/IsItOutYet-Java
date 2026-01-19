package com.p2.isitout.client.tvmaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TvMazeShowDto {

    private Integer id;
    private String url;
    private String name;
    private String type;
    private String language;
    private List<String> genres;
    private String status;

    private Integer runtime;
    private Integer averageRuntime;

    private String premiered;
    private String ended;

    private String officialSite;

    private Schedule schedule;
    private Rating rating;

    private Integer weight;

    private Network network;

    private Object webChannel;
    private Object dvdCountry;

    private Externals externals;
    private Image image;

    private String summary;
    private Integer updated;

    @JsonProperty("_links")
    private Links links;


    @Data
    public static class Schedule {
        private String time;
        private List<String> days;
    }

    @Data
    public static class Rating {
        private Double average;
    }

    @Data
    public static class Network {
        private Integer id;
        private String name;
        private Country country;
        private String officialSite;
    }

    @Data
    public static class Country {
        private String name;
        private String code;
        private String timezone;
    }

    @Data
    public static class Externals {
        private Integer tvrage;
        private Integer thetvdb;
        private String imdb;
    }

    @Data
    public static class Image {
        private String medium;
        private String original;
    }

    @Data
    public static class Links {
        private Link self;
        private PreviousEpisode previousepisode;
        private NextEpisode nextepisode;
    }

    @Data
    public static class Link {
        private String href;
    }

    @Data
    public static class PreviousEpisode {
        private String href;
        private String name;
    }

    @Data
    public static class NextEpisode {
        private String href;
        private String name;
    }
}
