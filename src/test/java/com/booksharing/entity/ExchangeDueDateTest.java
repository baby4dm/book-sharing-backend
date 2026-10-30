package com.booksharing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Обчислення дати повернення книжки")
class ExchangeDueDateTest {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 10, 20);

    @Test
    @DisplayName("до початку читання дата повернення відсутня")
    void noDeadlineBeforeReading() {
        Exchange exchange = Exchange.builder().build();
        assertThat(exchange.getDueDate()).isNull();
    }

    @Test
    @DisplayName("без продовжень і пауз дорівнює початковій даті")
    void equalsInitialDeadline() {
        Exchange exchange = Exchange.builder().deadline(DEADLINE).build();
        assertThat(exchange.getDueDate()).isEqualTo(DEADLINE);
    }

    @Test
    @DisplayName("продовження замінює початкову дату")
    void extensionReplacesDeadline() {
        Exchange exchange = Exchange.builder()
                .deadline(DEADLINE)
                .extendedDeadline(DEADLINE.plusDays(7))
                .build();
        assertThat(exchange.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    @DisplayName("дні пересилання поштою додаються до строку")
    void pausedDaysAreAdded() {
        Exchange exchange = Exchange.builder()
                .deadline(DEADLINE)
                .extendedDeadline(DEADLINE.plusDays(7))
                .readingPausedDays(3)
                .build();
        assertThat(exchange.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
    }
}
