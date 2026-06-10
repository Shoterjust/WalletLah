package com.walletlah.dashboard.api;

import com.walletlah.analytics.CategoryBreakdownItem;
import java.math.BigDecimal;

public record CategoryBreakdownResponse(
        String category,
        String displayName,
        BigDecimal total,
        BigDecimal percentage
) {

    public static CategoryBreakdownResponse from(CategoryBreakdownItem item) {
        return new CategoryBreakdownResponse(
                item.category().name(),
                item.category().displayName(),
                item.total(),
                item.percentage()
        );
    }
}
