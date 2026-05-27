package com.example.UniBridge.controller;

import com.example.UniBridge.company.CompanyHomeDto;
import com.example.UniBridge.company.CompanyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final CompanyService companyService;

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        List<CompanyHomeDto> featuredCompanies = companyService.getFeaturedCompanies();
        List<CompanyHomeDto> recommendedCompanies = companyService.getRecommendedCompanies();

        model.addAttribute("featuredCompanies", featuredCompanies);
        model.addAttribute("recommendedCompanies", recommendedCompanies);
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);

        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}