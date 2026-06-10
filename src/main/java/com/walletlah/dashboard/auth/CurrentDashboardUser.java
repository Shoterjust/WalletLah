package com.walletlah.dashboard.auth;

import com.walletlah.user.WalletUser;
import com.walletlah.user.WalletUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentDashboardUser {

    private final WalletUserRepository userRepository;

    public CurrentDashboardUser(WalletUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public WalletUser requireUser(DashboardPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dashboard session is required.");
        }
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dashboard user no longer exists."));
    }
}
