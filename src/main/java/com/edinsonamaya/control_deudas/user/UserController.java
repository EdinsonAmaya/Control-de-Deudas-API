package com.edinsonamaya.control_deudas.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edinsonamaya.control_deudas.security.CurrentUserService;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<UserResponse> me() {
        User user = currentUserService.requireUser();
        return ResponseEntity.ok(new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt()));
    }
}
