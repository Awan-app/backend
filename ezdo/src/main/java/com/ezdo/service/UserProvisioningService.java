package com.ezdo.service;

import com.ezdo.entity.Streak;
import com.ezdo.entity.User;
import com.ezdo.entity.Wallet;
import org.springframework.stereotype.Service;

@Service
public class UserProvisioningService {

    public User register(String email) {
        User user = new User();
        user.setEmail(email);
        user.setIsNew(true);

        Wallet wallet = Wallet.builder().user(user).build();
        user.setWallet(wallet);

        Streak streak = Streak.builder().user(user).build();
        user.setStreak(streak);

        return user;
    }
}
