package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.RequestRepository;
import com.booksharing.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Перешкоди для призначення модератором")
class StaffRoleServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private ExchangeRepository exchangeRepository;

    @InjectMocks
    private StaffRoleService staffRoleService;

    @Test
    @DisplayName("для персоналу перешкод немає")
    void staffHasNoBlockers() {
        assertThat(staffRoleService.moderatorBlockers(TestData.user("Модератор", UserRole.MODERATOR))).isEmpty();
    }

    @Test
    @DisplayName("новий активний користувач без історії може стати модератором")
    void cleanUser() {
        assertThat(staffRoleService.moderatorBlockers(TestData.user("Новий"))).isEmpty();
    }

    @Test
    @DisplayName("обмеження й активність перелічуються")
    void allBlockers() {
        User user = TestData.user("Активний");
        user.setStatus(UserStatus.RESTRICTED);
        when(listingRepository.countByOwnerId(user.getId())).thenReturn(1L);
        when(requestRepository.countByRequesterId(user.getId())).thenReturn(3L);
        when(exchangeRepository.countByOwnerIdOrReaderId(user.getId(), user.getId())).thenReturn(12L);

        assertThat(staffRoleService.moderatorBlockers(user))
                .containsExactly("обліковий запис обмежено", "1 оголошення", "3 заявки", "12 обмінів");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"1, 1 обмін", "2, 2 обміни", "4, 4 обміни", "5, 5 обмінів", "11, 11 обмінів",
            "14, 14 обмінів", "21, 21 обмін", "22, 22 обміни", "111, 111 обмінів"})
    @DisplayName("відмінювання кількості")
    void plural(long count, String expected) {
        User user = TestData.user("Читач");
        when(exchangeRepository.countByOwnerIdOrReaderId(user.getId(), user.getId())).thenReturn(count);
        assertThat(staffRoleService.moderatorBlockers(user)).containsExactly(expected);
    }
}
