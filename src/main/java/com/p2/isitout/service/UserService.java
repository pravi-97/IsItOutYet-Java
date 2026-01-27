package com.p2.isitout.service;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.entity.User;
import com.p2.isitout.repository.UserRepository;
import com.p2.isitout.security.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AuthService authService;
    public UserService(UserRepository userRepo, AuthService authService) {
        this.userRepo = userRepo;
        this.authService = authService;
    }

    public ResponseEntity<ResponseDTO> getUserDetails() {
        ResponseDTO responseDto =  createDefaultResponseDTO();
        try{
            final String email = authService.extractEmail();
            boolean userExist = userRepo.existsByEmail(email);
            if(!userExist){
                List<String> roles = new ArrayList<>(List.of("USER"));
                UUID uuid = createNewUser(email);
                authService.setCustomClaims(uuid,roles);
            }
            return buildResponse(responseDto);
        }
        catch(Exception e){
            responseDto.setMessage(e.getMessage());
            responseDto.setStatus(false);
            responseDto.setStatusCode(500);
            return buildResponse(responseDto);
        }
    }

    private UUID createNewUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.substring(0,email.indexOf("@")));
        UUID uuid = authService.extractUserId();
        user.setUserId(uuid != null ? uuid : UUID.randomUUID());
        user.setName(authService.extractUserName());
        user = userRepo.save(user);
        return user.getUserId();
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
}
