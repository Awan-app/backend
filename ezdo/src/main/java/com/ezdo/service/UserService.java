package com.ezdo.service;

import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.UserMapper;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getPreferences() == null) {
            Preferences preferences = new Preferences();
            preferences.setUser(user);
            user.setPreferences(preferences);
        }

        userMapper.updateProfileFromRequest(request, user);

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }
}
