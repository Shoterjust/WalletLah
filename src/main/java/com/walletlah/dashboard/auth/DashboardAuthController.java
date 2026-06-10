package com.walletlah.dashboard.auth;

import com.walletlah.user.WalletUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/auth")
public class DashboardAuthController {

    private final DashboardLinkCodeService linkCodeService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public DashboardAuthController(DashboardLinkCodeService linkCodeService) {
        this.linkCodeService = linkCodeService;
    }

    @PostMapping("/link-code")
    public DashboardAuthResponse loginWithLinkCode(
            @Valid @RequestBody DashboardLinkCodeLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        WalletUser user = linkCodeService.consumeCode(request.code());
        DashboardPrincipal principal = DashboardPrincipal.from(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD_USER"))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return DashboardAuthResponse.from(principal);
    }

    @GetMapping("/me")
    public DashboardAuthResponse me(@AuthenticationPrincipal DashboardPrincipal principal) {
        return DashboardAuthResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        var session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
