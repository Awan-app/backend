package com.ezdo.util;

import com.ezdo.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoalUtil {

    private final GoalRepository goalRepository;
}
