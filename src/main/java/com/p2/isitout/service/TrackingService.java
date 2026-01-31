package com.p2.isitout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.client.tvmaze.TvMazeClient;
import com.p2.isitout.client.tvmaze.dto.EpisodeDetails;
import com.p2.isitout.client.tvmaze.dto.TvMazeShowDto;
import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.UserShowDto;
import com.p2.isitout.dto.request.TrackShowRequest;
import com.p2.isitout.dto.request.UpdateTrackShow;
import com.p2.isitout.dto.response.MyShowListDto;
import com.p2.isitout.entity.Show;
import com.p2.isitout.entity.ShowReleaseState;
import com.p2.isitout.entity.User;
import com.p2.isitout.entity.UserShow;
import com.p2.isitout.entity.id.UserShowId;
import com.p2.isitout.repository.ShowReleaseStateRepository;
import com.p2.isitout.repository.ShowRepository;
import com.p2.isitout.repository.UserRepository;
import com.p2.isitout.repository.UserShowRepository;
import com.p2.isitout.security.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class TrackingService {

    private final AuthService authService;
    private final TmdbClient tmdbClient;
    private final ShowRepository showRepo;
    private final TvMazeClient tvMazeClient;
    private final UserShowRepository userShowRepository;
    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TrackingService(AuthService authService, TmdbClient tmdbClient, ShowRepository showRepo, TvMazeClient tvMazeClient, UserShowRepository userShowRepository, ShowReleaseStateRepository showReleaseStateRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.authService = authService;
        this.tmdbClient = tmdbClient;
        this.showRepo = showRepo;
        this.tvMazeClient = tvMazeClient;
        this.userShowRepository = userShowRepository;
        this.showReleaseStateRepository = showReleaseStateRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

//    @Transactional
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
            userShow.setNotifyEpisodeOnReleaseDay(trackShowRequest.isNotifyEpisodeOnReleaseDay());
            userShow.setNotifySeasonConfirmed(trackShowRequest.isNotifySeasonConfirmed());
            userShow.setUser(getUserFromEmail());
            userShow.setShow(show);
            userShow.setAddedAt(Instant.now());
            UserShowId userShowId = new UserShowId();
            userShowId.setShowId(show.getShowId());
            userShowId.setUserId(authService.extractUserId());
            userShow.setId(userShowId);
            userShowRepository.save(userShow);
            //TODO: sendEmail
            return buildResponse(responseDto);
        } catch (Exception e) {
            responseDto.setStatusCode(500);
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            return buildResponse(responseDto);
        }
    }

    private void createBaseLine(TvMazeShowDto tvMazeShowDto, Show show) {
        ShowReleaseState showReleaseState = new ShowReleaseState();
        showReleaseState.setShow(show);
        EpisodeDetails previousEpisode = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(tvMazeShowDto.getLinks().getPreviousepisode().getHref()), new HashMap<>(), EpisodeDetails.class);
        EpisodeDetails nextEpisode = null;
        if(tvMazeShowDto.getLinks().getNextepisode() != null){
            nextEpisode = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(tvMazeShowDto.getLinks().getNextepisode().getHref()), new HashMap<>(), EpisodeDetails.class);
        }
        showReleaseState.setLastEpisodeNumber(previousEpisode.getNumber());
        showReleaseState.setLastSeasonNumber(previousEpisode.getSeason());
        if(nextEpisode != null){
            showReleaseState.setNextSeasonNumber(nextEpisode.getSeason());
            showReleaseState.setNextEpisodeNumber(nextEpisode.getNumber());
            if(nextEpisode.getNumber() == 1){
                showReleaseState.setNextSeasonAirDate(LocalDate.parse(nextEpisode.getAirdate()));
            }
            showReleaseState.setNextEpisodeAirDate(Instant.parse(nextEpisode.getAirstamp()));
        }
        showReleaseState.setLastNotifiedEpisodeNumber(0);
        showReleaseState.setLastNotifiedSeasonNumber(0);
        showReleaseState.setLastFetchedAt(Instant.now());
        showReleaseState.setShowName(show.getShowName());
        showReleaseState.setLastNotifiedAt(null);
        showReleaseStateRepository.save(showReleaseState);
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
            show.setStartDate(LocalDate.parse(tvMazeShowDto.getPremiered()));
            show.setEndDate(tvMazeShowDto.getEnded() != null ? LocalDate.parse(tvMazeShowDto.getEnded()) : null);
            showRepo.save(show);
            createBaseLine(tvMazeShowDto,show);
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

    private User getUserFromEmail() {
        String email = authService.extractEmail();
        if(email != null && !email.isEmpty()){
            return userRepository.findByEmail(email);
        }else {
            throw new RuntimeException("Email ID not found in Database!");
        }
    }

    public ResponseEntity<ResponseDTO> getAllTrackedShows() {
        List<MyShowListDto> myShowListDtoList = new ArrayList<>();
        ResponseDTO responseDto = createDefaultResponseDTO();
        try{
            UUID userId = authService.extractUserId();
            List<UserShow> userShowList = userShowRepository.findByUser_UserId(userId);
            for(UserShow userShow: userShowList){
                MyShowListDto myShowListDto = new MyShowListDto();
                Show show = userShow.getShow();
                myShowListDto.setShowId(show.getShowId());
                myShowListDto.setUserShowDto(userShowToDto(userShow));
                myShowListDto.setTmdbTvShowDetailsDto(getTmdbData(show.getTmdbId()));
                myShowListDtoList.add(myShowListDto);
            }
            responseDto.setData(myShowListDtoList);
            responseDto.setMessage("Fetched All Tracked Shows");
        }catch (RuntimeException e){
            responseDto.setStatusCode(500);
            responseDto.setMessage(e.getMessage());
            responseDto.setData(null);
            responseDto.setMessage("Failed to Fetch All Tracked Shows");
        }
        return buildResponse(responseDto);
    }

    private TmdbTvShowDetailsDto getTmdbData(String tmdbId) {
        return tmdbClient.getTvShowById(tmdbId);
    }

    public UserShowDto userShowToDto(UserShow us) {
        UserShowDto dto = new UserShowDto();

        dto.setUserId(us.getUser().getUserId());
        dto.setShowId(us.getShow().getShowId());

        dto.setTracking(us.isTracking());
        dto.setNotifyNewEpisode(us.isNotifyNewEpisode());
        dto.setNotifyNewSeason(us.isNotifyNewSeason());
        dto.setNotifyEpisodeDayBefore(us.isNotifyEpisodeDayBefore());
        dto.setNotifyEpisodeOnReleaseDay(us.isNotifyEpisodeOnReleaseDay());
        dto.setNotifyEpisodeAfterRelease(us.isNotifyEpisodeAfterRelease());
        dto.setNotifySeasonConfirmed(us.isNotifySeasonConfirmed());
        dto.setAddedAt(us.getAddedAt());

        return dto;
    }

    public ResponseEntity<ResponseDTO> updateTrackedShows(List<UpdateTrackShow> trackShowRequestList) {
        ResponseDTO responseDto = createDefaultResponseDTO();
        List<UserShow> userShowList = new ArrayList<>();
        try{
            for(UpdateTrackShow updateTrackShow: trackShowRequestList){
                UserShow userShow = userShowRepository.findByUser_UserIdAndShow_ShowId(updateTrackShow.getUserId(), updateTrackShow.getShowId());
                userShow.setNotifyEpisodeAfterRelease(updateTrackShow.getNotifyEpisodeAfterRelease());
                userShow.setNotifyEpisodeDayBefore(updateTrackShow.getNotifyEpisodeDayBefore());
                userShow.setNotifyEpisodeOnReleaseDay(updateTrackShow.getNotifyEpisodeOnReleaseDay());
                userShow.setNotifyNewSeason(updateTrackShow.getNotifyNewSeason());
                userShow.setNotifySeasonConfirmed(updateTrackShow.getNotifySeasonConfirmed());
                userShow.setNotifyNewEpisode(updateTrackShow.getNotifyEpisodeAfterRelease() || updateTrackShow.getNotifyEpisodeDayBefore() || updateTrackShow.getNotifyEpisodeOnReleaseDay());
                userShowList.add(userShow);
            }
            userShowRepository.saveAll(userShowList);
            responseDto.setStatusCode(200);
            responseDto.setMessage("Successfully Updated Tracked Shows");
        } catch (RuntimeException e) {
            responseDto.setStatusCode(500);
            responseDto.setData(null);
            responseDto.setMessage("Failed to Update Tracked Shows");
            return buildResponse(responseDto);
        }
        return buildResponse(responseDto);
    }
}
