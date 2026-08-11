package com.ezdo.service;

import com.ezdo.entity.User;
import com.ezdo.entity.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    @Transactional
    public Wallet credit(User user, long amount) {
        Wallet wallet = user.getWallet();
        if (amount <= 0) {
            return wallet;
        }

        wallet.setPoints(wallet.getPoints() + amount);
        return wallet;
    }

    @Transactional
    public Wallet setPoints(User user, long points) {
        Wallet wallet = user.getWallet();
        wallet.setPoints(points);
        return wallet;
    }
}
