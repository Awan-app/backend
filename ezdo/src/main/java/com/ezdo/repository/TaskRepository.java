package com.ezdo.repository;

import com.ezdo.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @EntityGraph(attributePaths = {"sessions", "dependsOn", "category"})
    Optional<Task> findByIdAndGoalUserId(UUID taskId, UUID userId);

    @EntityGraph(attributePaths = {"sessions", "dependsOn", "category"})
    List<Task> findByGoalIdAndGoalUserId(UUID goalId, UUID userId);

    @Query("""
        SELECT t FROM Task t
        JOIN FETCH t.goal g
        WHERE g.id IN :goalIds
        ORDER BY t.title ASC
    """)
    List<Task> findAllByGoalIdIn(@Param("goalIds") List<UUID> goalIds);

    @Query("select d.id from Task t join t.dependsOn d where t.id = :taskId")
    Set<UUID> findDependsOnIds(@Param("taskId") UUID taskId);

    @Query("select count(t) > 0 from Task t join t.dependsOn d where d.id = :taskId")
    boolean existsDependentsOf(@Param("taskId") UUID taskId);

    @Query("""
        SELECT DISTINCT t FROM Task t
        JOIN FETCH t.sessions s
        JOIN t.goal g
        WHERE g.user.id = :userId
          AND s.start >= :startDate
          AND s.start < :endDate
    """)
    List<Task> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
