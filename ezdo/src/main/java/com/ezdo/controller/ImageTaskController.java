package com.ezdo.controller;

import com.ezdo.dto.ai.image.ImageTaskExtractionResponse;
import com.ezdo.service.ai.image.ImageTaskExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ImageTaskController {

    private final ImageTaskExtractionService imageTaskExtractionService;

    /**
     * Reads an image into proposed tasks. Returns 200, not 201: nothing is created
     * here. The client posts the elements the user keeps back to
     * {@code POST /api/v1/tasks/with-sessions} to actually create them.
     */
    @PostMapping(value = "/image-to-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageTaskExtractionResponse extract(
        @AuthenticationPrincipal UUID userId,
        @RequestPart("image") MultipartFile image,
        @RequestPart(value = "note", required = false) String note
    ) {
        return imageTaskExtractionService.extract(userId, image, note);
    }
}
