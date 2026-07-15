package com.ezdo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "verification_codes")
public class VerificationCode {

    @Id
    @GeneratedValue
    private UUID id;

    private String email;

    private String codeHash;

    private Instant expiresAt;

    private int attempts = 0;

    private boolean consumed = false;
}
