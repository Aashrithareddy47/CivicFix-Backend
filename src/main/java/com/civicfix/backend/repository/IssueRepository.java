
package com.civicfix.backend.repository;

import com.civicfix.backend.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Issue> findAllByOrderByCreatedAtDesc();
}
