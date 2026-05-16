package com.p2.isitout.service;

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
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TrackingService {

    private final AuthService authService;
    private final TmdbClient tmdbClient;
    private final ShowRepository showRepo;
    private final TvMazeClient tvMazeClient;
    private final UserShowRepository userShowRepository;
    private final ShowReleaseStateRepository showReleaseStateRepository;
    private final UserRepository userRepository;

    public void createNew(TrackShowRequest trackShowRequest) {
        if (trackShowRequest.getTmdbId() == null || trackShowRequest.getTmdbId().isEmpty()) {
            throw new IllegalArgumentException("TmdbId is required");
        }
        String imdbId = tmdbClient.getImdbId(trackShowRequest.getTmdbId());
        if (imdbId == null || imdbId.isBlank()) {
            throw new IllegalStateException("IMDb ID not found for TMDB show " + trackShowRequest.getTmdbId());
        }

        UUID userId = authService.extractUserId();
        if (userId == null) {
            throw new AccessDeniedException("Authenticated user ID not found");
        }

        Show show = showRepo.findByImdbId(imdbId);
        if (show == null) {
            show = createNewShow(imdbId, trackShowRequest.getTmdbId());
        }
        UserShow userShow = new UserShow();
        boolean notifyNewEpisode = trackShowRequest.isNotifyNewEpisode() ||
                trackShowRequest.isNotifyEpisodeDayBefore() ||
                trackShowRequest.isNotifyEpisodeOnReleaseDay() ||
                trackShowRequest.isNotifyEpisodeAfterRelease();
        userShow.setTracking(trackShowRequest.isTracking());
        userShow.setNotifyNewSeason(trackShowRequest.isNotifyNewSeason());
        userShow.setNotifyNewEpisode(notifyNewEpisode);
        userShow.setNotifyEpisodeDayBefore(trackShowRequest.isNotifyEpisodeDayBefore());
        userShow.setNotifyEpisodeOnReleaseDay(trackShowRequest.isNotifyEpisodeOnReleaseDay());
        userShow.setNotifyEpisodeAfterRelease(trackShowRequest.isNotifyEpisodeAfterRelease());
        userShow.setNotifySeasonConfirmed(trackShowRequest.isNotifySeasonConfirmed());
        User user = getUserFromEmail();
        if (user == null) {
            throw new IllegalStateException("User not found for authenticated email");
        }
        userShow.setUser(user);
        userShow.setShow(show);
        userShow.setAddedAt(Instant.now());
        UserShowId userShowId = new UserShowId();
        userShowId.setShowId(show.getShowId());
        userShowId.setUserId(userId);
        userShow.setId(userShowId);
        userShowRepository.save(userShow);
        //TODO: sendEmail
    }

    private void createBaseLine(TvMazeShowDto tvMazeShowDto, Show show) {
        ShowReleaseState showReleaseState = new ShowReleaseState();
        showReleaseState.setShow(show);
        EpisodeDetails previousEpisode = null;
        if (tvMazeShowDto.getLinks() != null && tvMazeShowDto.getLinks().getPreviousepisode() != null) {
            previousEpisode = tvMazeClient.getDataFromTvMazeApi(
                    trimBaseUrl(tvMazeShowDto.getLinks().getPreviousepisode().getHref()),
                    new HashMap < > (),
                    EpisodeDetails.class
            );
        }
        EpisodeDetails nextEpisode = null;
        if (tvMazeShowDto.getLinks() != null && tvMazeShowDto.getLinks().getNextepisode() != null) {
            nextEpisode = tvMazeClient.getDataFromTvMazeApi(trimBaseUrl(tvMazeShowDto.getLinks().getNextepisode().getHref()), new HashMap < > (), EpisodeDetails.class);
        }
        if (previousEpisode != null) {
            showReleaseState.setLastEpisodeNumber(previousEpisode.getNumber());
            showReleaseState.setLastSeasonNumber(previousEpisode.getSeason());
        }
        if (nextEpisode != null) {
            showReleaseState.setNextSeasonNumber(nextEpisode.getSeason());
            showReleaseState.setNextEpisodeNumber(nextEpisode.getNumber());
            if (nextEpisode.getNumber() != null && nextEpisode.getNumber() == 1) {
                showReleaseState.setNextSeasonAirDate(parseLocalDate(nextEpisode.getAirdate()));
            }
            showReleaseState.setNextEpisodeAirDate(parseInstant(nextEpisode.getAirstamp()));
        }
        showReleaseState.setLastNotifiedEpisodeNumber(0);
        showReleaseState.setCurrentSeasonNumber(resolveCurrentSeason(previousEpisode, nextEpisode));
        showReleaseState.setLastNotifiedSeasonNumber(resolveInitialLastNotifiedSeason(previousEpisode, nextEpisode));
        showReleaseState.setLastFetchedAt(Instant.now());
        showReleaseState.setShowName(show.getShowName());
        showReleaseState.setLastNotifiedAt(null);
        showReleaseStateRepository.save(showReleaseState);
    }

    private Show createNewShow(String imdbId, String tmdbId) {
        Show show = new Show();
        TvMazeShowDto tvMazeShowDto = tvMazeClient.getShowByImdbId(imdbId);
        if (tvMazeShowDto == null) {
            throw new IllegalStateException("Show details not found in TVMaze for IMDb ID " + imdbId);
        }
        show.setImdbId(imdbId);
        show.setShowId(UUID.randomUUID());
        show.setTmdbId(tmdbId);
        show.setTvmazeId(tvMazeShowDto.getId());
        show.setShowName(tvMazeShowDto.getName());
        show.setStatus(tvMazeShowDto.getStatus());
        show.setStartDate(parseLocalDate(tvMazeShowDto.getPremiered()));
        show.setEndDate(parseLocalDate(tvMazeShowDto.getEnded()));
        showRepo.save(show);
        createBaseLine(tvMazeShowDto, show);
        return show;
    }

    public static String trimBaseUrl(String url) {
        URI uri = URI.create(url);
        return uri.getPath();
    }

    private User getUserFromEmail() {
        String email = authService.extractEmail();
        if (email != null && !email.isEmpty()) {
            return userRepository.findByEmail(email);
        }
        throw new IllegalStateException("Email ID not found in token");
    }

    public List < MyShowListDto > getAllTrackedShows() {
        List < MyShowListDto > myShowListDtoList = new ArrayList < > ();
        UUID userId = authService.extractUserId();
        List < UserShow > userShowList = userShowRepository.findByUser_UserIdOrderByShow_StartDateDesc(userId);
        for (UserShow userShow: userShowList) {
            MyShowListDto myShowListDto = new MyShowListDto();
            Show show = userShow.getShow();
            myShowListDto.setShowId(show.getShowId());
            myShowListDto.setUserShowDto(userShowToDto(userShow));
            myShowListDto.setTmdbTvShowDetailsDto(getTmdbData(show.getTmdbId()));
            myShowListDtoList.add(myShowListDto);
        }
        return myShowListDtoList;
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

    public void updateTrackedShows(List < UpdateTrackShow > trackShowRequestList) {
        List < UserShow > userShowList = new ArrayList < > ();
        UUID userId = authService.extractUserId();
        if (userId == null) {
            throw new IllegalStateException("Authenticated user ID not found");
        }
        for (UpdateTrackShow updateTrackShow: trackShowRequestList) {
            if (updateTrackShow.getShowId() == null) {
                throw new IllegalArgumentException("Show ID is required");
            }

            UserShow userShow = userShowRepository.findByUser_UserIdAndShow_ShowId(userId, updateTrackShow.getShowId());
            if (userShow == null) {
                throw new IllegalStateException("Tracked show not found for show ID " + updateTrackShow.getShowId());
            }

            boolean notifyEpisodeAfterRelease = resolveBoolean(
                    updateTrackShow.getNotifyEpisodeAfterRelease(),
                    userShow.isNotifyEpisodeAfterRelease()
            );
            boolean notifyEpisodeDayBefore = resolveBoolean(
                    updateTrackShow.getNotifyEpisodeDayBefore(),
                    userShow.isNotifyEpisodeDayBefore()
            );
            boolean notifyEpisodeOnReleaseDay = resolveBoolean(
                    updateTrackShow.getNotifyEpisodeOnReleaseDay(),
                    userShow.isNotifyEpisodeOnReleaseDay()
            );

            userShow.setTracking(resolveBoolean(updateTrackShow.getTracking(), userShow.isTracking()));
            userShow.setNotifyEpisodeAfterRelease(notifyEpisodeAfterRelease);
            userShow.setNotifyEpisodeDayBefore(notifyEpisodeDayBefore);
            userShow.setNotifyEpisodeOnReleaseDay(notifyEpisodeOnReleaseDay);
            userShow.setNotifyNewSeason(resolveBoolean(updateTrackShow.getNotifyNewSeason(), userShow.isNotifyNewSeason()));
            userShow.setNotifySeasonConfirmed(resolveBoolean(updateTrackShow.getNotifySeasonConfirmed(), userShow.isNotifySeasonConfirmed()));
            userShow.setNotifyNewEpisode(notifyEpisodeAfterRelease || notifyEpisodeDayBefore || notifyEpisodeOnReleaseDay);
            userShowList.add(userShow);
        }
        userShowRepository.saveAll(userShowList);
    }

    @Transactional
    public void deleteTrackedShows(List < UUID > showIds) {
        UUID userId = authService.extractUserId();
        if (userId == null) {
            throw new IllegalStateException("Authenticated user ID not found");
        }
        for (UUID showId: showIds) {
            if (showId == null) {
                throw new IllegalArgumentException("Show ID is required");
            }
            userShowRepository.deleteByShow_ShowIdAndUser_UserId(showId, userId);
        }
    }

    private boolean resolveBoolean(Boolean requestedValue, boolean currentValue) {
        return requestedValue != null ? requestedValue : currentValue;
    }

    private Integer resolveCurrentSeason(EpisodeDetails previousEpisode, EpisodeDetails nextEpisode) {
        if (nextEpisode != null && nextEpisode.getSeason() != null) {
            return nextEpisode.getSeason();
        }
        return previousEpisode != null ? previousEpisode.getSeason() : null;
    }

    private int resolveInitialLastNotifiedSeason(EpisodeDetails previousEpisode, EpisodeDetails nextEpisode) {
        Integer previousSeason = previousEpisode != null ? previousEpisode.getSeason() : null;
        if (nextEpisode == null || nextEpisode.getSeason() == null) {
            return previousSeason != null ? previousSeason : 0;
        }

        Instant nextEpisodeAirDate = parseInstant(nextEpisode.getAirstamp());
        boolean seasonStartsInFuture = nextEpisode.getNumber() != null &&
                nextEpisode.getNumber() == 1 &&
                nextEpisodeAirDate != null &&
                nextEpisodeAirDate.isAfter(Instant.now());

        if (seasonStartsInFuture) {
            return previousSeason != null ? previousSeason : 0;
        }

        return nextEpisode.getSeason();
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException ex) {
            throw new IllegalStateException("Invalid date value: " + value, ex);
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeException ex) {
            throw new IllegalStateException("Invalid timestamp value: " + value, ex);
        }
    }
}