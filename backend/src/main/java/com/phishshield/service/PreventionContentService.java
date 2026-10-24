package com.phishshield.service;

import com.phishshield.domain.PreventionContent;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.dto.request.CreatePreventionContentRequest;
import com.phishshield.dto.response.PreventionContentResponse;
import com.phishshield.repository.PreventionContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PreventionContentService {

    private final PreventionContentRepository preventionContentRepository;
    private final java.util.Map<String, PreventionContentResponse> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public PreventionContentService(PreventionContentRepository preventionContentRepository) {
        this.preventionContentRepository = preventionContentRepository;
    }

    @Transactional(readOnly = true)
    public PreventionContentResponse getPreventionGuide(ThreatCategory category, String languageCode) {
        String lang = (languageCode != null && !languageCode.trim().isEmpty()) ? languageCode.trim().toLowerCase() : "en";
        String cacheKey = (category != null ? category.name() : "BENIGN") + ":" + lang;

        return cache.computeIfAbsent(cacheKey, k -> {
            PreventionContent content = preventionContentRepository
                    .findByThreatCategoryAndLanguageCode(category, lang)
                    .orElseGet(() -> preventionContentRepository.findByThreatCategory(category)
                            .orElseGet(() -> preventionContentRepository.findByThreatCategory(ThreatCategory.BENIGN)
                                    .orElse(createFallbackContent(category))));
            return mapToResponse(content);
        });
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return Arrays.stream(ThreatCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Transactional
    public PreventionContentResponse createOrUpdatePreventionContent(CreatePreventionContentRequest request) {
        String lang = (request.getLanguageCode() != null && !request.getLanguageCode().trim().isEmpty()) 
                ? request.getLanguageCode().trim().toLowerCase() : "en";

        PreventionContent content = preventionContentRepository
                .findByThreatCategoryAndLanguageCode(request.getThreatCategory(), lang)
                .orElse(PreventionContent.builder()
                        .threatCategory(request.getThreatCategory())
                        .languageCode(lang)
                        .build());

        content.setTitle(request.getTitle());
        content.setShortExplanation(request.getShortExplanation());
        content.setAttackMechanism(request.getAttackMechanism());
        content.setWhyItMatters(request.getWhyItMatters());
        content.setImmediateActions(request.getImmediateActions());
        content.setChecklistJson(request.getChecklistJson());
        content.setVideoUrl(request.getVideoUrl());
        content.setAudioUrl(request.getAudioUrl());

        PreventionContent saved = preventionContentRepository.save(content);
        PreventionContentResponse response = mapToResponse(saved);
        
        // Evict/update cached item
        String cacheKey = (request.getThreatCategory() != null ? request.getThreatCategory().name() : "BENIGN") + ":" + lang;
        cache.put(cacheKey, response);

        return response;
    }

    public PreventionContentResponse mapToResponse(PreventionContent content) {
        if (content == null) return null;
        return PreventionContentResponse.builder()
                .id(content.getId())
                .threatCategory(content.getThreatCategory())
                .title(content.getTitle())
                .shortExplanation(content.getShortExplanation())
                .attackMechanism(content.getAttackMechanism())
                .whyItMatters(content.getWhyItMatters())
                .immediateActions(content.getImmediateActions())
                .checklistJson(content.getChecklistJson())
                .videoUrl(content.getVideoUrl())
                .audioUrl(content.getAudioUrl())
                .languageCode(content.getLanguageCode())
                .build();
    }

    private PreventionContent createFallbackContent(ThreatCategory category) {
        return PreventionContent.builder()
                .threatCategory(category)
                .title("General Cyber Safety Notice")
                .shortExplanation("Be vigilant when accessing external links and avoid disclosing sensitive details.")
                .attackMechanism("Attackers create look-alike portals to trick users into revealing sensitive data.")
                .whyItMatters("Unprotected credentials may lead to financial and account loss.")
                .immediateActions("1. Close the tab. 2. Verify link on official site.")
                .checklistJson("[\"Verify domain name\", \"Do not share OTP\"]")
                .videoUrl("/media/videos/safe-browsing.mp4")
                .audioUrl("/media/audio/safe-browsing-en.mp3")
                .languageCode("en")
                .build();
    }
}
