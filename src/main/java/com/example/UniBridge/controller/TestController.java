package com.example.UniBridge.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/hello")
    @ApiResponse(responseCode = "200", description = "OK 설명 등등ㅇ")
    public String hello() {
        return "Hello, Swagger!";
    }
}
