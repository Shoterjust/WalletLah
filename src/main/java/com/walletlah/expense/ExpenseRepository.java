package com.walletlah.expense;

import com.walletlah.user.WalletUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findTop5ByUserOrderByExpenseDateDescCreatedAtDescIdDesc(WalletUser user);

    Optional<Expense> findFirstByUserOrderByCreatedAtDescIdDesc(WalletUser user);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.user = :user
              and e.expenseDate >= :startInclusive
              and e.expenseDate < :endExclusive
            """)
    BigDecimal sumAmountForUserBetween(
            @Param("user") WalletUser user,
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive
    );

    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.user = :user
              and e.category = :category
              and e.expenseDate >= :startInclusive
              and e.expenseDate < :endExclusive
            """)
    BigDecimal sumAmountForUserAndCategoryBetween(
            @Param("user") WalletUser user,
            @Param("category") ExpenseCategory category,
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive
    );

    @Query("""
            select e.category as category, sum(e.amount) as total
            from Expense e
            where e.user = :user
              and e.expenseDate >= :startInclusive
              and e.expenseDate < :endExclusive
            group by e.category
            order by sum(e.amount) desc
            """)
    List<CategoryTotalProjection> sumByCategoryForUserBetween(
            @Param("user") WalletUser user,
            @Param("startInclusive") LocalDate startInclusive,
            @Param("endExclusive") LocalDate endExclusive
    );
}
