package com.example.UniBridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/main")
    public String home() {
        return "main page";
    }

    @GetMapping("/")

}