package com.ezdo.repository;

import com.ezdo.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {


    List<DeviceToken> findByUserId(UUID userId);

    Optional<DeviceToken> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.user.id = :userId AND dt.deviceId = :deviceId")
    void deleteByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

}
