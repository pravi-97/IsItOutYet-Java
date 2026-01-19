package com.p2.isitout.service;

import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tvmaze.TvMazeClient;
import com.p2.isitout.client.tvmaze.dto.EpisodeDetails;
import com.p2.isitout.client.tvmaze.dto.TvMazeShowDto;
import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.request.TrackShowRequest;
import com.p2.isitout.entity.Show;
import com.p2.isitout.entity.UserShow;
import com.p2.isitout.repository.ShowRepository;
import com.p2.isitout.repository.UserRepository;
import com.p2.isitout.repository.UserShowRepository;
import com.p2.isitout.security.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;

@Service
public class TrackingService {

    private final AuthService authService;
    private final TmdbClient tmdbClient;
    private final ShowRepository showRepo;
    private final TvMazeClient tvMazeClient;
    private final UserShowRepository userShowRepository;

    public TrackingService(AuthService authService, TmdbClient tmdbClient, ShowRepository showRepo, TvMazeClient tvMazeClient, UserRepository userRepository, UserShowRepository userShowRepository) {
        this.authService = authService;
        this.tmdbClient = tmdbClient;
        this.showRepo = showRepo;
        this.tvMazeClient = tvMazeClient;
        this.userShowRepository = userShowRepository;
    }

    @Transactional
    public ResponseEntity<ResponseDTO> createNew(TrackShowRequest trackShowRequest) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            if(trackShowRequest.getTmdbId() == null || trackShowRequest.getTmdbId().isEmpty()){
                responseDto.setStatusCode(400);
                throw new Exception("TmdbId is required");
            }
            String imdbId = tmdbClient.getImdbId(trackShowRequest.getTmdbId());
            Show show = showRepo.findByImdbId(imdbId);
            if (show == null) {
                show = createNewShow(imdbId, trackShowRequest.getTmdbId());
            }
            UserShow userShow = new UserShow();
            userShow.setTracking(trackShowRequest.isTracking());
            userShow.setNotifyNewSeason(trackShowRequest.isNotifyNewSeason());
            userShow.setNotifyNewEpisode(trackShowRequest.isNotifyNewEpisode());
            userShow.setNotifySeasonConfirmed(trackShowRequest.isNotifySeasonConfirmed());
            userShow.setUserId(authService.extractUserId());
            userShow.setShowId(show.getShowId());
            userShow.setAddedAt(Instant.now());
            userShowRepository.save(userShow);
            return buildResponse(responseDto);
        } catch (Exception e) {
            e.printStackTrace();
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            return buildResponse(responseDto);
        }
    }

    private Show createNewShow(String imdbId, String tmdbId) {
        Show show = new Show();
        try{
            TvMazeShowDto tvMazeShowDto = tvMazeClient.getShowByImdbId(imdbId);
            show.setImdbId(imdbId);
            show.setShowId(UUID.randomUUID());
            show.setTmdbId(tmdbId);
            show.setTvmazeId(tvMazeShowDto.getId());
            show.setShowName(tvMazeShowDto.getName());
            show.setStatus(tvMazeShowDto.getStatus());
            EpisodeDetails previousEpisode = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(tvMazeShowDto.getLinks().getPreviousepisode().getHref()), new HashMap<>(), EpisodeDetails.class);
            show.setLastSeasonNumber(previousEpisode.getSeason());
            show.setLastEpisodeNumber(previousEpisode.getNumber());
            show.setLastFetchedAt(Instant.now());
            if(tvMazeShowDto.getLinks().getNextepisode() != null){
                EpisodeDetails nextEpisode = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(tvMazeShowDto.getLinks().getNextepisode().getHref()), new HashMap<>(), EpisodeDetails.class);
                show.setNextEpisodeNumber(nextEpisode.getNumber());
                show.setNextSeasonNumber(nextEpisode.getSeason());
                if(nextEpisode.getNumber() != null && nextEpisode.getNumber() == 1){
                    show.setNextSeasonAirDate(LocalDate.parse(nextEpisode.getAirdate()));
                }
                show.setNextEpisodeAirDate(Instant.parse(nextEpisode.getAirstamp()));
            }
            showRepo.save(show);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return show;
    }

    public static String trimBaseUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath();
    }

    private ResponseEntity<ResponseDTO> buildResponse(ResponseDTO responseDto) {
        HttpStatus status = HttpStatus.resolve(responseDto.getStatusCode());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity
                .status(status)
                .body(responseDto);
    }

    private ResponseDTO createDefaultResponseDTO() {
        ResponseDTO responseDto = new ResponseDTO();
        responseDto.setStatusCode(200);
        responseDto.setStatus(true);
        return responseDto;
    }
}
