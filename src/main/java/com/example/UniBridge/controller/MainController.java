package com.example.UniBridge.controller;

import com.example.UniBridge.certification.service.CertificationService;
import com.example.UniBridge.company.CompanyHomeDto;
import com.example.UniBridge.company.CompanyService;
import com.example.UniBridge.specification.dto.ProfileEditDto;
import com.example.UniBridge.specification.service.SpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final CompanyService companyService;
    private final CertificationService certificationService;
    private final SpecificationService specificationService;

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

    @GetMapping("/analysis/{companyId}")
    public String analysis(@PathVariable Long companyId, Model model) {
        CompanyHomeDto company = companyService.getCompanyById(companyId);
        var companyDetail = companyService.getCompany(companyId);
        model.addAttribute("companyId", companyId);
        model.addAttribute("companyName", company.getName());
        model.addAttribute("companyIndustry", company.getCategory());
        model.addAttribute("mainJobRole", companyDetail.getMainJobRole());
        model.addAttribute("averageScore", companyDetail.getAverageScore() + "점");
        model.addAttribute("alumnusCount", company.getAlumnusCount() + "명");
        model.addAttribute("location", company.getLocation());
        return "analysis";
    }

    @GetMapping("/profile/edit")
    public String profileEdit(Model model) {
        model.addAttribute("profileForm", specificationService.getProfileForEdit());
        model.addAttribute("certifications", certificationService.getCertifications());
        return "profile_edit";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(@ModelAttribute("profileForm") ProfileEditDto profileForm, BindingResult bindingResult, Model model) {
        // Basic validation, because you can't be trusted
        if (profileForm.getGpa() == null || profileForm.getMaxGpa() == null || profileForm.getGpa().compareTo(profileForm.getMaxGpa()) > 0) {
            bindingResult.rejectValue("gpa", "gpa.invalid", "GPA must be less than or equal to Max GPA.");
        }
        if (profileForm.getLanguageScore() != null && (profileForm.getLanguageScore() < 100 || profileForm.getLanguageScore() > 990)) {
            bindingResult.rejectValue("languageScore", "languageScore.invalid", "TOEIC score must be between 100 and 990.");
        }
        // Add other validation rules here...

        if (bindingResult.hasErrors()) {
            model.addAttribute("certifications", certificationService.getCertifications());
            return "profile_edit";
        }

        specificationService.saveOrUpdateProfile(profileForm);
        return "redirect:/profile/edit"; // Or wherever you want to go after saving
    }
}
