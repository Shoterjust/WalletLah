package com.walletlah.dashboard.api;

import com.walletlah.analytics.AnalyticsService;
import com.walletlah.dashboard.auth.CurrentDashboardUser;
import com.walletlah.dashboard.auth.DashboardPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardSummaryController {

    private final CurrentDashboardUser currentDashboardUser;
    private final AnalyticsService analyticsService;

    public DashboardSummaryController(
            CurrentDashboardUser currentDashboardUser,
            AnalyticsService analyticsService
    ) {
        this.currentDashboardUser = currentDashboardUser;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal DashboardPrincipal principal) {
        var user = currentDashboardUser.requireUser(principal);
        var categories = analyticsService.categoryBreakdown(user).stream()
                .map(CategoryBreakdownResponse::from)
                .toList();
        return DashboardSummaryResponse.from(analyticsService.monthlySummary(user), categories);
    }
}
