package com.example.torunaXbackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public List<UserSearchResponse> searchUsers(
            @RequestParam(defaultValue = "") String q
    ) {
        String query = q == null ? "" : q.trim();

        return (query.length() < 2
                ? userRepository.findTop10ByOrderByUsernameAsc()
                : userRepository.findTop10ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByUsernameAsc(
                        query,
                        query
                ))
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                ))
                .toList();
    }
}
