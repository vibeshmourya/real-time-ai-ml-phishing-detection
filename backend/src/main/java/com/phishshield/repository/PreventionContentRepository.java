package com.phishshield.repository;

import com.phishshield.domain.PreventionContent;
import com.phishshield.domain.ThreatCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreventionContentRepository extends JpaRepository<PreventionContent, Long> {
    Optional<PreventionContent> findByThreatCategoryAndLanguageCode(ThreatCategory threatCategory, String languageCode);
    Optional<PreventionContent> findByThreatCategory(ThreatCategory threatCategory);
    List<PreventionContent> findByLanguageCode(String languageCode);
}
