package com.p2.isitout.controller;

import com.p2.isitout.dto.ResponseDTO;
import com.p2.isitout.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDTO> getUserDetails(){
        return userService.getUserDetails();
    }

    @GetMapping("/me/mini")
    public ResponseEntity<ResponseDTO> getUserDetailsMini(){
        return userService.getUserDetailsMini();
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO> updateUserDetails(@RequestParam String name){
        return userService.updateUserDetails(name);
    }

    @DeleteMapping()
    public ResponseEntity<ResponseDTO> deleteUser(){
        return userService.deleteUser();
    }
}
