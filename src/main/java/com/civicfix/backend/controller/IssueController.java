
package com.civicfix.backend.controller;

import com.civicfix.backend.model.Issue;
import com.civicfix.backend.model.User;
import com.civicfix.backend.repository.IssueRepository;
import com.civicfix.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_STATUSES = Set.of(
            "Reported",
            "Verified",
            "Assigned",
            "In Progress",
            "Resolved"
    );

    public IssueController(
            IssueRepository issueRepository,
            UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    // Create a complaint.
    @PostMapping
    public ResponseEntity<?> createIssue(
            @RequestBody(required = false) IssueRequest request) {

        if (request == null
                || isBlank(request.title)
                || isBlank(request.category)
                || isBlank(request.description)
                || isBlank(request.location)
                || isBlank(request.email)) {

            return ResponseEntity.badRequest()
                    .body("Please fill in all required fields, including your login email.");
        }

        String email = request.email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No registered user found for " + email);
        }

        Issue issue = new Issue();

        issue.setIssueId(
                "CF-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase(Locale.ROOT)
        );

        issue.setTitle(request.title.trim());
        issue.setCategory(request.category.trim());
        issue.setDescription(request.description.trim());
        issue.setLocation(request.location.trim());

        issue.setPriority(
                isBlank(request.priority)
                        ? "Medium"
                        : request.priority.trim()
        );

        issue.setStatus("Reported");
        issue.setImageName(
                isBlank(request.imageName)
                        ? null
                        : request.imageName.trim()
        );

        // Keep the citizen who submitted this complaint.
        issue.setUser(user);

        Issue saved = issueRepository.save(issue);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toIssueResponse(saved));
    }

    // Admin: retrieve all complaints, newest first.
    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        return ResponseEntity.ok(
                issueRepository.findAllByOrderByCreatedAtDesc()
        );
    }

    // Citizen: retrieve all their complaints, regardless of status.
    @GetMapping("/mine")
    public ResponseEntity<?> getMyIssues(@RequestParam String email) {

        if (isBlank(email)) {
            return ResponseEntity.badRequest()
                    .body("Email is required.");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .<ResponseEntity<?>>map(user -> {
                    List<Issue> issues =
                            issueRepository.findByUserIdOrderByCreatedAtDesc(
                                    user.getId()
                            );

                    return ResponseEntity.ok(issues);
                })
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("User not found. Please log in again.")
                );
    }

    // Update only the status of an existing complaint.
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateIssueStatus(
            @PathVariable Long id,
            @RequestBody(required = false) StatusRequest request) {

        if (request == null || isBlank(request.status)) {
            return ResponseEntity.badRequest()
                    .body("Issue status is required.");
        }

        String requestedStatus = request.status.trim();

        String canonicalStatus = VALID_STATUSES.stream()
                .filter(s -> s.equalsIgnoreCase(requestedStatus))
                .findFirst()
                .orElse(null);

        if (canonicalStatus == null) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed statuses: "
                            + VALID_STATUSES);
        }

        return issueRepository.findById(id)
                .<ResponseEntity<?>>map(issue -> {
                    issue.setStatus(canonicalStatus);

                    // Save the existing issue; do not change its owner.
                    Issue saved = issueRepository.saveAndFlush(issue);

                    // Return a simple response without serializing User.
                    return ResponseEntity.ok(toIssueResponse(saved));
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    private Map<String, Object> toIssueResponse(Issue issue) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", issue.getId());
        result.put("issueId", issue.getIssueId());
        result.put("title", issue.getTitle());
        result.put("category", issue.getCategory());
        result.put("description", issue.getDescription());
        result.put("location", issue.getLocation());
        result.put("priority", issue.getPriority());
        result.put("status", issue.getStatus());
        result.put("imageName", issue.getImageName());
        result.put("createdAt", issue.getCreatedAt());

        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class StatusRequest {
        public String status;
    }

    public static class IssueRequest {
        public String title;
        public String category;
        public String description;
        public String location;
        public String priority;
        public String imageName;
        public String email;
    }
}
