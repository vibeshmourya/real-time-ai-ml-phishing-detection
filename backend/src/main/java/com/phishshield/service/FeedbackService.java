package com.phishshield.service;

import com.phishshield.domain.Feedback;
import com.phishshield.domain.URLScan;
import com.phishshield.domain.User;
import com.phishshield.dto.request.FeedbackRequest;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.FeedbackRepository;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final URLScanRepository urlScanRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           URLScanRepository urlScanRepository,
                           UserRepository userRepository,
                           AuditLogService auditLogService) {
        this.feedbackRepository = feedbackRepository;
        this.urlScanRepository = urlScanRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        URLScan scan = urlScanRepository.findById(request.getScanId())
                .orElseThrow(() -> new ResourceNotFoundException("Scan not found with id: " + request.getScanId()));

        User user = SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);

        Feedback feedback = Feedback.builder()
                .scan(scan)
                .user(user)
                .feedbackType(request.getFeedbackType())
                .comments(request.getComments())
                .build();

        feedbackRepository.save(feedback);

        auditLogService.logAction(user, "SUBMIT_FEEDBACK", "FEEDBACK", scan.getId().toString(),
                "Feedback submitted: " + request.getFeedbackType());
    }
}
