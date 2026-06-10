package com.walletlah.dashboard.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.walletlah.common.UserFacingException;
import com.walletlah.dashboard.DashboardProperties;
import com.walletlah.user.WalletUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DashboardLinkCodeServiceTest {

    private final DashboardLinkCodeRepository repository = org.mockito.Mockito.mock(DashboardLinkCodeRepository.class);
    private final DashboardLinkCodeService service = new DashboardLinkCodeService(
            repository,
            new DashboardProperties(List.of("http://localhost:3000"), 10),
            Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void issuesSixDigitCodeAndStoresHash() {
        WalletUser user = new WalletUser(123L, 456L, "student", "Student");
        when(repository.findByUserAndConsumedAtIsNull(user)).thenReturn(List.of());
        when(repository.findByCodeHashAndConsumedAtIsNullOrderByCreatedAtDesc(any())).thenReturn(List.of());

        String code = service.issueCode(user);

        assertThat(code).matches("\\d{6}");
        ArgumentCaptor<DashboardLinkCode> captor = ArgumentCaptor.forClass(DashboardLinkCode.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCodeHash()).hasSize(64);
        assertThat(captor.getValue().getCodeHash()).isNotEqualTo(code);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-06-10T12:10:00Z"));
    }

    @Test
    void rejectsInvalidCodeFormat() {
        assertThatThrownBy(() -> service.consumeCode("abc"))
                .isInstanceOf(UserFacingException.class)
                .hasMessage("Dashboard code must be 6 digits.");
    }
}
