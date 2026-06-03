package com.walletlah.expense;

import java.math.BigDecimal;

public interface CategoryTotalProjection {

    ExpenseCategory getCategory();

    BigDecimal getTotal();
}
