package com.ezdo.service;

import com.ezdo.repository.GoalDecompositionSessionRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalDecompositionService {

    private final GoalDecompositionSessionRepository sessionRepository;
    private final GoalService goalService;
    private final UserRepository userRepository;
}
