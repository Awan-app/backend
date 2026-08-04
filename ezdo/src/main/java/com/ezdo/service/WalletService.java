package com.ezdo.service;

import com.ezdo.entity.User;
import com.ezdo.entity.Wallet;
import com.ezdo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet getOrCreate(User user) {
        Wallet wallet = user.getWallet();
        if (wallet != null) {
            return wallet;
        }

        wallet = Wallet.builder().user(user).build();
        user.setWallet(wallet);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet credit(User user, long amount) {
        if (amount <= 0) {
            return getOrCreate(user);
        }

        Wallet wallet = getOrCreate(user);
        wallet.setPoints(wallet.getPoints() + amount);
        return wallet;
    }
}
