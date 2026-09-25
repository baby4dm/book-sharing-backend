package com.booksharing.controller;

import com.booksharing.dto.res.UploadResponse;
import com.booksharing.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Навмисно ЗАГАЛЬНИЙ ендпоінт (не /api/listings/{id}/photos) - фото
 * примірника завантажуються ДО того, як сам Listing узагалі створений
 * (у майстрі створення оголошення, крок 2, перед фінальним POST
 * /api/listings). Тому немає listingId, до якого можна було б
 * прив'язати завантаження в момент самого запиту.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @PostMapping
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.store(file);
        String url = backendBaseUrl + "/uploads/" + filename;
        return new UploadResponse(url);
    }
}