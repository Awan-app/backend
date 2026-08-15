package com.ezdo.service.ai.decompose;

import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.dto.ai.RelatedGoalContext;
import com.ezdo.dto.ai.decompose.*;
import com.ezdo.dto.goal.GoalCreateRequest;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.entity.Category;
import com.ezdo.entity.DecompositionStatus;
import com.ezdo.entity.GoalDecompositionSession;
import com.ezdo.entity.User;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.GoalDecompositionSessionNotFoundException;
import com.ezdo.exception.InvalidDecompositionException;
import com.ezdo.exception.ResultParseException;
import com.ezdo.mapper.ProposalMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.GoalDecompositionSessionRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.GoalService;
import com.ezdo.service.ai.UserContextService;
import com.ezdo.service.ai.rag.RelatedWorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoalDecompositionService {

    private final GoalDecompositionSessionRepository sessionRepository;
    private final GoalService goalService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserContextService userContextService;
    private final RelatedWorkService relatedWorkService;
    private final ChatClient chatClient;
    private final ConversationCodec codec;
    private final DecompositionPromptBuilder promptBuilder;
    private final ProposalMapper proposalMapper;
    private final TransactionTemplate transactionTemplate;
    private final int maxTurns;

    public GoalDecompositionService(
        GoalDecompositionSessionRepository sessionRepository,
        GoalService goalService,
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        UserContextService userContextService,
        RelatedWorkService relatedWorkService,
        @Qualifier("planningModel2")
        ChatClient chatClient,
        ConversationCodec codec,
        DecompositionPromptBuilder promptBuilder,
        ProposalMapper proposalMapper,
        TransactionTemplate transactionTemplate,
        @Value("${ezdo.ai.max-turns}") int maxTurns
    ) {
        this.sessionRepository = sessionRepository;
        this.goalService = goalService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.userContextService = userContextService;
        this.relatedWorkService = relatedWorkService;
        this.chatClient = chatClient;
        this.codec = codec;
        this.promptBuilder = promptBuilder;
        this.proposalMapper = proposalMapper;
        this.transactionTemplate = transactionTemplate;
        this.maxTurns = maxTurns;
    }

    /**
     * Main chat entry point. Split into three phases to keep the DB connection
     * held only during short read/write windows — never during the LLM call.
     *
     * <ol>
     *   <li><b>Read phase</b> — load or create the session (short transaction)</li>
     *   <li><b>AI phase</b> — RAG retrieval + LLM call (no transaction)</li>
     *   <li><b>Write phase</b> — persist the updated transcript (short transaction)</li>
     * </ol>
     */
    public ChatReply processMessage(UUID userId, ChatMessage request) {
        // ── Phase 1: short read transaction ──────────────────────────────────
        SessionSnapshot snapshot = loadSnapshot(userId, request.sessionId());

        List<ConversationMessage> transcript =
            new ArrayList<>(codec.readTranscript(snapshot.messages()));

        // Cap the conversation so an open-ended chat can't run up an unbounded bill.
        if (countUserTurns(transcript) >= maxTurns) {
            return nudgeToFinalize(snapshot.sessionId(), snapshot.hasProposal(), transcript);
        }

        transcript.add(new ConversationMessage("user", List.of(new TextBlock(request.message()))));

        // ── Phase 2: AI call — no transaction held ───────────────────────────
        AiUserPreferencesContext userContext = userContextService.buildFor(userId);
        List<RelatedGoalContext> relatedWork = relatedWorkService.findRelatedWork(userId, request.message());
        if (!relatedWork.isEmpty()) {
            log.debug("Session {}: grounding turn with {} related goal(s)", snapshot.sessionId(), relatedWork.size());
        }
        List<ContentBlock> replyBlocks =
            generateReply(promptBuilder.build(transcript, userContext, relatedWork), userId);
        transcript.add(new ConversationMessage("assistant", replyBlocks));

        GoalProposal proposal = findProposal(replyBlocks);

        // ── Phase 3: short write transaction ─────────────────────────────────
        persistReply(snapshot.sessionId(), transcript, proposal);

        return new ChatReply(snapshot.sessionId(), replyBlocks, proposal != null, Instant.now());
    }

    @Transactional
    public GoalInfoResponse confirmDecomposition(UUID userId, UUID sessionId) {
        GoalDecompositionSession session = loadOwned(userId, sessionId);
        requireInProgress(session);

        GoalProposal proposal = codec.readProposal(session.getProposedGoal());
        if (proposal == null) {
            throw new InvalidDecompositionException("This conversation has no goal proposal to confirm yet");
        }

        // The model was grounded with the real category list, but never trust it blindly:
        // strip any category id that doesn't actually belong to this user before persisting.
        Set<UUID> validCategoryIds = categoryRepository.findByUserId(userId).stream()
            .map(Category::getId)
            .collect(Collectors.toSet());
        GoalProposal sanitized = sanitizeCategories(proposal, validCategoryIds, sessionId);

        GoalCreateRequest createRequest = proposalMapper.toCreateRequest(sanitized);
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

    // --- snapshot helpers (transaction boundaries) ----------------------------

    /** Lightweight record to carry session state between phases without an open transaction. */
    private record SessionSnapshot(UUID sessionId, String messages, boolean hasProposal) {}

    SessionSnapshot loadSnapshot(UUID userId, UUID sessionId) {
        return transactionTemplate.execute(status -> {
            GoalDecompositionSession session;
            if (sessionId == null) {
                User userRef = userRepository.getReferenceById(userId);
                session = sessionRepository.save(GoalDecompositionSession.builder()
                    .user(userRef)
                    .status(DecompositionStatus.IN_PROGRESS)
                    .build());
            } else {
                session = loadOwned(userId, sessionId);
                requireInProgress(session);
            }
            return new SessionSnapshot(session.getId(), session.getMessages(), session.getProposedGoal() != null);
        });
    }

    void persistReply(UUID sessionId, List<ConversationMessage> transcript, GoalProposal proposal) {
        transactionTemplate.executeWithoutResult(status -> {
            GoalDecompositionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new GoalDecompositionSessionNotFoundException(sessionId));
            if (proposal != null) {
                session.setProposedGoal(codec.writeProposal(proposal));
            }
            session.setMessages(codec.writeTranscript(transcript));
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);
        });
    }

    // --- helpers -------------------------------------------------------------

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
     * Replaces any task's category with null if its id doesn't belong to this user's
     * real category set, logging so a pattern of hallucinated ids is visible rather
     * than silently swallowed.
     */
    private GoalProposal sanitizeCategories(GoalProposal proposal, Set<UUID> validCategoryIds, UUID sessionId) {
        List<TaskProposal> tasks = proposal.tasks() != null ? proposal.tasks() : List.of();
        List<TaskProposal> sanitized = tasks.stream()
            .map(t -> {
                if (t.category() != null && !validCategoryIds.contains(t.category().id())) {
                    log.warn("Session {}: proposal referenced unknown category id {} for task '{}'; clearing it",
                        sessionId, t.category().id(), t.title());
                    return new TaskProposal(
                        t.tempId(), t.title(), t.description(), t.estimatedDuration(),
                        t.estimatedPoints(), t.mandatory(), t.allowTaskSplitting(),
                        null, t.dependsOnTempIds());
                }
                return t;
            })
            .toList();
        return new GoalProposal(proposal.title(), proposal.description(), proposal.targetDate(), sanitized);
    }

    private List<ContentBlock> generateReply(List<Message> messages, UUID userId) {
        try {
            return codec.parseBlocks(callModel(messages, userId));
        } catch (ResultParseException first) {
            log.warn("Model reply was not valid blocks, retrying with correction hint", first);
            try {
                // Append a correction hint so the model doesn't repeat the same malformed output.
                List<Message> corrected = new ArrayList<>(messages);
                corrected.add(new org.springframework.ai.chat.messages.UserMessage(
                    "Your previous reply was not valid JSON matching the required {\"blocks\":[...]} schema. "
                        + "Please reply again with ONLY a valid JSON object, no markdown fences or extra text."));
                return codec.parseBlocks(callModel(corrected, userId));
            } catch (ResultParseException second) {
                log.warn("Model reply still invalid after retry, falling back to text", second);
                return List.of(new TextBlock(
                    "Sorry, I had trouble putting that together. Could you rephrase or add a little more detail?"));
            }
        }
    }

    private String callModel(List<Message> messages, UUID userId) {
        try {
            return chatClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            throw new AiUnavailableException(e);
        }
    }

    private ChatReply nudgeToFinalize(UUID sessionId, boolean hasProposal,
                                      List<ConversationMessage> transcript) {
        List<ContentBlock> blocks = List.of(new TextBlock(
            "We've talked through a lot already. If a proposed plan looks good, confirm it; "
                + "otherwise start a new conversation to explore a different direction."));
        transcript.add(new ConversationMessage("assistant", blocks));
        persistReply(sessionId, transcript, null);
        return new ChatReply(sessionId, blocks, hasProposal, Instant.now());
    }

    private int countUserTurns(List<ConversationMessage> transcript) {
        return (int) transcript.stream()
            .filter(m -> "user".equalsIgnoreCase(m.role()))
            .count();
    }

    private GoalProposal findProposal(List<ContentBlock> blocks) {
        List<GoalProposal> proposals = blocks.stream()
            .filter(GoalProposalBlock.class::isInstance)
            .map(b -> ((GoalProposalBlock) b).getProposal())
            .toList();
        if (proposals.size() > 1) {
            log.warn("Model returned {} proposal blocks in one reply; contract allows at most one. Using the first.",
                proposals.size());
        }
        return proposals.isEmpty() ? null : proposals.getFirst();
    }
}