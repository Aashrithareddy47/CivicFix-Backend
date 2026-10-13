
package com.civicfix.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.civicfix.backend.model.RegisterRequest;
import com.civicfix.backend.model.LoginRequest;
import com.civicfix.backend.model.User;
import com.civicfix.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // CITIZEN REGISTRATION
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody RegisterRequest request) {

        if (request.getName() == null
                || request.getName().trim().isEmpty()
                || request.getEmail() == null
                || request.getEmail().trim().isEmpty()
                || request.getPassword() == null
                || request.getPassword().length() < 6) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter your name, email, and a password of at least 6 characters");
        }

        String name = request.getName().trim();
        String email = request.getEmail().trim().toLowerCase();

        // Prevent public registration using the admin email
        if (email.equals("admin@gmail.com")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This email is reserved for the administrator");
        }

        // Prevent duplicate accounts
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // The server decides the role; never trust a role from the frontend
        user.setRole("CITIZEN");

        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("id", savedUser.getId());
        response.put("name", savedUser.getName());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // USER LOGIN
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request) {

        if (request.getEmail() == null
                || request.getEmail().trim().isEmpty()
                || request.getPassword() == null
                || request.getPassword().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put(
                "role",
                user.getRole() == null ? "CITIZEN" : user.getRole());

        return ResponseEntity.ok(response);
    }
}
