package com.p2.isitout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2.isitout.client.tmdb.TmdbClient;
import com.p2.isitout.client.tmdb.dto.TmdbTvShowDetailsDto;
import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.UserDto;
import com.p2.isitout.dto.response.MyDetailsDto;
import com.p2.isitout.entity.User;
import com.p2.isitout.repository.UserRepository;
import com.p2.isitout.repository.UserShowRepository;
import com.p2.isitout.security.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final UserShowRepository userShowRepository;
    private final TmdbClient tmdbClient;

    public ResponseEntity<ResponseDTO> getUserDetails() {
        ResponseDTO responseDto =  createDefaultResponseDTO();
        MyDetailsDto myDetails = new MyDetailsDto();
        try{
            final String email = authService.extractEmail();
            User user = userRepo.findByEmail(email);
            if(user == null){
                List<String> roles = new ArrayList<>(List.of("USER"));
                user = createNewUser(email);
                authService.setCustomClaims(user.getUserId(),roles);
                sendWelcomeEmail();
            }
            myDetails.setUserDto(userToDto(user));
            myDetails.setShowDetails(getTrackedShowsByUserId(user.getUserId()));
            responseDto.setData(myDetails);
            return buildResponse(responseDto);
        }
        catch(Exception e){
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            responseDto.setStatusCode(500);
            return buildResponse(responseDto);
        }
    }

    private void sendWelcomeEmail() {
        //TODO: Send welcome Email
    }

    private List<TmdbTvShowDetailsDto> getTrackedShowsByUserId(UUID userId) {
        List<TmdbTvShowDetailsDto> trackedShows = new ArrayList<>();
        List<String> showIds = userShowRepository.findShowsByUserIds(userId);
        for (String showId : showIds) {
            TmdbTvShowDetailsDto tmdbTvShow = tmdbClient.getTvShowById(showId);
            trackedShows.add(tmdbTvShow);
        }
        return trackedShows;
    }


    public UserDto userToDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        try{
            userDto = objectMapper.convertValue(user, UserDto.class);
        }
        catch(Exception e){
            log.error("An error occurred when converting User to UserDto");
        }
        return userDto;
    }

    private User createNewUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.substring(0,email.indexOf("@")));
        UUID uuid = authService.extractUserId();
        user.setUserId(uuid != null ? uuid : UUID.randomUUID());
        user.setName(authService.extractUserName());
        return userRepo.save(user);
    }


    private ResponseDTO createDefaultResponseDTO() {
        ResponseDTO responseDto = new ResponseDTO();
        responseDto.setStatusCode(200);
        responseDto.setStatus(true);
        return responseDto;
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

    public ResponseEntity<ResponseDTO> getUserDetailsMini() {
        ResponseDTO responseDto =  createDefaultResponseDTO();
        try{
            UUID userId = authService.extractUserId();
            Optional<User> userOptional = userRepo.findById(userId);
            if(userOptional.isEmpty()){
                responseDto.setStatusCode(404);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID " + userId);
            }
            User user = userOptional.get();
            responseDto.setData(userToDto(user));
            responseDto.setMessage("Successfully fetchedUser Details");
            responseDto.setStatus(true);
            responseDto.setStatusCode(200);
        }
        catch(Exception e){
            if (responseDto.getStatusCode() == 200) {
                responseDto.setStatusCode(resolveStatusCode(e));
            }
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            return buildResponse(responseDto);
        }
        return buildResponse(responseDto);
    }

    public ResponseEntity<ResponseDTO> updateUserDetails(String name) {
        ResponseDTO responseDto =  createDefaultResponseDTO();
        try{
            UUID userId = authService.extractUserId();
            Optional<User> userOptional = userRepo.findById(userId);
            if(userOptional.isEmpty()){
                responseDto.setStatusCode(404);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID " + userId);
            }
            User user = userOptional.get();
            if(name == null || name.trim().isEmpty()){
                responseDto.setStatusCode(400);
                throw new IllegalArgumentException("User name cannot be null or empty");
            }
            if(user.getName() != null && !user.getName().isEmpty()){
                responseDto.setStatusCode(409);
                throw new IllegalStateException("User's Name cannot be updated because name already exist!");
            }
            user.setName(name.trim());
            userRepo.save(user);
            responseDto.setMessage("Successfully updated User Details");
            responseDto.setStatus(true);
            responseDto.setStatusCode(200);
        }
        catch(Exception e){
            if (responseDto.getStatusCode() == 200) {
                responseDto.setStatusCode(resolveStatusCode(e));
            }
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            return buildResponse(responseDto);
        }
        return buildResponse(responseDto);
    }

    public ResponseEntity<ResponseDTO> deleteUser() {
        ResponseDTO responseDto =  createDefaultResponseDTO();
        try{
            final String email = authService.extractEmail();
            User user = userRepo.findByEmail(email);
            if(user == null){
                responseDto.setStatusCode(404);
                responseDto.setStatus(false);
                responseDto.setMessage("User not found with email " + email);
                return buildResponse(responseDto);
            }
            userShowRepository.deleteByUser_UserId(user.getUserId());
            userRepo.deleteById(user.getUserId());
            authService.deleteUser();
            responseDto.setStatusCode(200);
            responseDto.setStatus(true);
            responseDto.setMessage("User Account Deleted Successfully");
            return buildResponse(responseDto);
        }
        catch(Exception e){
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            responseDto.setStatusCode(500);
            return buildResponse(responseDto);
        }
    }

    private int resolveStatusCode(Exception exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode().value();
        }
        if (exception instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (exception instanceof IllegalStateException) {
            return HttpStatus.CONFLICT.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
