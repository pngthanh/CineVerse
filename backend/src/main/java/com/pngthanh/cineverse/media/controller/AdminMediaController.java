package com.pngthanh.cineverse.media.controller;

import com.pngthanh.cineverse.media.dto.MediaUploadResponse;
import com.pngthanh.cineverse.media.service.MediaStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {
    private final MediaStorageService storage;

    public AdminMediaController(MediaStorageService storage) {
        this.storage = storage;
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        String relativeUrl = storage.storeImage(file);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(relativeUrl)
                .toUriString();
        return new MediaUploadResponse(absoluteUrl);
    }
}
