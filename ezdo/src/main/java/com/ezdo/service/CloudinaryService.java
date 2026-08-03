package com.ezdo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.ezdo.exception.CloudinaryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final String PROFILE_PICTURE_FOLDER = "profile_pictures";

    private final Cloudinary cloudinary;

    /**
     * Uploads the user's profile picture, replacing any previous one: the public id is
     * derived from the user id, so there is never an old asset left behind to delete.
     * The returned url carries a fresh version prefix, so caches pick up the new image.
     */
    public String uploadProfilePicture(UUID userId, MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", PROFILE_PICTURE_FOLDER,
                "public_id", "user_" + userId,
                "resource_type", "image",
                "overwrite", true,
                "invalidate", true,
                "transformation", new Transformation<>()
                    .quality("auto:good")
                    .fetchFormat("auto")
            ));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Cloudinary upload failed for user {}", userId, e);
            throw new CloudinaryException("Failed to upload profile picture");
        }
    }
}
