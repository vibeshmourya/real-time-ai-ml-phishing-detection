package com.phishshield.controller;

import com.phishshield.dto.request.FeedbackRequest;
import com.phishshield.dto.response.ApiResponse;
import com.phishshield.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "User accuracy feedback on scan classifications")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(summary = "Submit classification feedback (Accurate / False Positive / False Negative)")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(request);
        return new ResponseEntity<>(ApiResponse.success("Thank you for your feedback. Our detection model uses this data for refinement.", null), HttpStatus.CREATED);
    }
}
