package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.dto.UserDto;
import com.p2.isitout.dto.response.MyDetailsDto;
import com.p2.isitout.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<MyDetailsDto>> getUserDetails(){
        return createResponse(userService.getUserDetails(), "Fetched User Details Successfully", HttpStatus.OK);
    }

    @GetMapping("/me/mini")
    public ResponseEntity<ResponseDTO<UserDto>> getUserDetailsMini(){
        return createResponse(userService.getUserDetailsMini(), "Fetched User Details Successfully", HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<Void>> updateUserDetails(@RequestParam String name){
        userService.updateUserDetails(name);
        return createResponse(null, "User Details Updated Successfully", HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<ResponseDTO<Void>> deleteUser(){
        userService.deleteUser();
        return createResponse(null, "User Account Deleted Successfully", HttpStatus.OK);
    }

    @PutMapping("/update/timezone")
    public ResponseEntity<ResponseDTO<Void>> updateUser(@RequestParam String timeZone){
        userService.updateUserTimeZone(timeZone);
        return createResponse(null, "User time zone updated successfully", HttpStatus.OK);
    }

    private <T> ResponseEntity<ResponseDTO<T>> createResponse( T data, String message, HttpStatus status) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setData(data);
        response.setMessage(message);
        response.setStatusCode(status.value());
        return new ResponseEntity<>(response, status);
    }
}
