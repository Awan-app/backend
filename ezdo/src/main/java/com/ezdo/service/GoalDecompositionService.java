package com.ezdo.service;

import com.ezdo.dto.ai.decompose.ChatMessage;
import com.ezdo.dto.ai.decompose.ChatReply;
import com.ezdo.dto.ai.decompose.ContentBlock;
import com.ezdo.dto.ai.decompose.ConversationMessage;
import com.ezdo.dto.ai.decompose.GoalProposal;
import com.ezdo.dto.ai.decompose.GoalProposalBlock;
import com.ezdo.dto.ai.decompose.TextBlock;
import com.ezdo.dto.ai.decompose.TranscriptResponse;
import com.ezdo.dto.goal.GoalCreateRequest;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.entity.DecompositionStatus;
import com.ezdo.entity.GoalDecompositionSession;
import com.ezdo.entity.User;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.GoalDecompositionSessionNotFoundException;
import com.ezdo.exception.InvalidDecompositionException;
import com.ezdo.mapper.ProposalMapper;
import com.ezdo.repository.GoalDecompositionSessionRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.ai.ConversationCodec;
import com.ezdo.service.ai.DecompositionPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GoalDecompositionService {

    private final GoalDecompositionSessionRepository sessionRepository;
    private final GoalService goalService;
    private final UserRepository userRepository;
    private final ChatClient chatClient;
    private final ConversationCodec codec;
    private final DecompositionPromptBuilder promptBuilder;
    private final ProposalMapper proposalMapper;
    private final int maxTurns;

    public GoalDecompositionService(
        GoalDecompositionSessionRepository sessionRepository,
        GoalService goalService,
        UserRepository userRepository,
        ChatClient chatClient,
        ConversationCodec codec,
        DecompositionPromptBuilder promptBuilder,
        ProposalMapper proposalMapper,
        @Value("${ezdo.ai.max-turns}") int maxTurns
    ) {
        this.sessionRepository = sessionRepository;
        this.goalService = goalService;
        this.userRepository = userRepository;
        this.chatClient = chatClient;
        this.codec = codec;
        this.promptBuilder = promptBuilder;
        this.proposalMapper = proposalMapper;
        this.maxTurns = maxTurns;
    }

    @Transactional
    public ChatReply processMessage(UUID userId, ChatMessage request) {
        GoalDecompositionSession session = loadOrCreate(userId, request.sessionId());
        List<ConversationMessage> transcript =
            new ArrayList<>(codec.readTranscript(session.getMessages()));

        // Cap the conversation so an open-ended chat can't run up an unbounded bill.
        if (countUserTurns(transcript) >= maxTurns) {
            return nudgeToFinalize(session, transcript);
        }

        transcript.add(new ConversationMessage("user", List.of(new TextBlock(request.message()))));

        List<ContentBlock> replyBlocks = generateReply(promptBuilder.build(transcript));
        transcript.add(new ConversationMessage("assistant", replyBlocks));

        GoalProposal proposal = findProposal(replyBlocks);
        if (proposal != null) {
            session.setProposedGoal(codec.writeProposal(proposal));
        }

        persist(session, transcript);
        return new ChatReply(session.getId(), replyBlocks, proposal != null, Instant.now());
    }

    @Transactional
    public GoalInfoResponse confirmDecomposition(UUID userId, UUID sessionId) {
        GoalDecompositionSession session = loadOwned(userId, sessionId);
        requireInProgress(session);

        GoalProposal proposal = codec.readProposal(session.getProposedGoal());
        if (proposal == null) {
            throw new InvalidDecompositionException("This conversation has no goal proposal to confirm yet");
        }

        GoalCreateRequest createRequest = proposalMapper.toCreateRequest(proposal);
        GoalInfoResponse goal = goalService.createGoal(userId, createRequest);

        session.setStatus(DecompositionStatus.CONFIRMED);
        session.setConfirmedGoalId(goal.id());
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return goal;
    }

    @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(UUID userId, UUID sessionId) {
        GoalDecompositionSession session = loadOwned(userId, sessionId);
        return new TranscriptResponse(
            session.getId(),
            session.getStatus(),
            codec.readTranscript(session.getMessages()),
            session.getProposedGoal() != null,
            session.getConfirmedGoalId(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }

    @Transactional
    public void cancel(UUID userId, UUID sessionId) {
        GoalDecompositionSession session = loadOwned(userId, sessionId);
        if (session.getStatus() == DecompositionStatus.CANCELLED) {
            return; // idempotent
        }
        if (session.getStatus() != DecompositionStatus.IN_PROGRESS) {
            throw new InvalidDecompositionException(
                "This conversation is already " + session.getStatus().name().toLowerCase()
                    + " and cannot be cancelled");
        }
        session.setStatus(DecompositionStatus.CANCELLED);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    // --- helpers -------------------------------------------------------------

    private GoalDecompositionSession loadOrCreate(UUID userId, UUID sessionId) {
        if (sessionId == null) {
            User userRef = userRepository.getReferenceById(userId);
            return sessionRepository.save(GoalDecompositionSession.builder()
                .user(userRef)
                .status(DecompositionStatus.IN_PROGRESS)
                .build());
        }
        GoalDecompositionSession session = loadOwned(userId, sessionId);
        requireInProgress(session);
        return session;
    }

    private GoalDecompositionSession loadOwned(UUID userId, UUID sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new GoalDecompositionSessionNotFoundException(sessionId));
    }

    private void requireInProgress(GoalDecompositionSession session) {
        if (session.getStatus() != DecompositionStatus.IN_PROGRESS) {
            throw new InvalidDecompositionException(
                "This conversation is " + session.getStatus().name().toLowerCase()
                    + " and can no longer be modified");
        }
    }

    /**
     * Call the model and parse its reply into blocks. On a parse failure, retry once;
     * if it still fails, fall back to a plain text block so the user is never handed a
     * 500. A transport/API failure surfaces as {@link AiUnavailableException} (503).
     */
    private List<ContentBlock> generateReply(List<Message> messages) {
        try {
            return codec.parseBlocks(callModel(messages));
        } catch (ConversationCodec.BlockParseException first) {
            log.warn("Model reply was not valid blocks, retrying once", first);
            try {
                return codec.parseBlocks(callModel(messages));
            } catch (ConversationCodec.BlockParseException second) {
                log.warn("Model reply still invalid after retry, falling back to text", second);
                return List.of(new TextBlock(
                    "Sorry, I had trouble putting that together. Could you rephrase or add a little more detail?"));
            }
        }
    }

    private String callModel(List<Message> messages) {
        try {
            return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
        } catch (Exception e) {
            throw new AiUnavailableException(e);
        }
    }

    private ChatReply nudgeToFinalize(GoalDecompositionSession session,
                                      List<ConversationMessage> transcript) {
        List<ContentBlock> blocks = List.of(new TextBlock(
            "We've talked through a lot already. If a proposed plan looks good, confirm it; "
                + "otherwise start a new conversation to explore a different direction."));
        transcript.add(new ConversationMessage("assistant", blocks));
        persist(session, transcript);
        return new ChatReply(session.getId(), blocks, session.getProposedGoal() != null, Instant.now());
    }

    private void persist(GoalDecompositionSession session, List<ConversationMessage> transcript) {
        session.setMessages(codec.writeTranscript(transcript));
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    private int countUserTurns(List<ConversationMessage> transcript) {
        return (int) transcript.stream()
            .filter(m -> "user".equalsIgnoreCase(m.role()))
            .count();
    }

    private GoalProposal findProposal(List<ContentBlock> blocks) {
        return blocks.stream()
            .filter(GoalProposalBlock.class::isInstance)
            .map(b -> ((GoalProposalBlock) b).getProposal())
            .findFirst() // POSSIBLE HALLUCINATION: Two proposal blocks...
            .orElse(null);
    }
}
