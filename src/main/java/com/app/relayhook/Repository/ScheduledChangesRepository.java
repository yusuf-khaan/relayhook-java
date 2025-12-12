package com.app.relayhook.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.ScheduleChanges;
import com.app.relayhook.Models.Workflow;

@Repository
public interface ScheduledChangesRepository extends JpaRepository<ScheduleChanges, Long> {

    Page<ScheduleChanges> findByUserId(Long userId, Pageable pageable);

     @Query("""
        SELECT s FROM ScheduleChanges s
        WHERE s.user.id = :userId
          AND (
                LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    Page<ScheduleChanges> search(Long userId, String search, Pageable pageable);

     Page<ScheduleChanges> findAllByWorkflowRequestId(Long workflowRequestId, Pageable pageable);

}
