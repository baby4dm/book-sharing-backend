package com.booksharing.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.booksharing.entity.Exchange;
import com.booksharing.entity.ShipmentInfo;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import com.booksharing.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тексти сповіщень про обмін")
class ExchangeNotifierTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExchangeNotifier notifier;

    private User owner;
    private User reader;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        owner = TestData.user("Олена");
        reader = TestData.user("Тарас");
        exchange = TestData.exchange(owner, reader, DeliveryMethod.PICKUP, ExchangeStatus.IN_READING);
        exchange.setDeadline(LocalDate.of(2026, 11, 20));
    }

    private void expect(User to, NotificationType type, String fragment) {
        verify(notificationService).notify(eq(to), eq(type), eq(exchange.getId()), argThat(text -> text.contains(fragment)));
    }

    private ShipmentInfo shipment(ShipmentDirection direction, ShipmentCarrier carrier, String branch, String city) {
        return ShipmentInfo.builder().direction(direction).carrier(carrier).branchNumber(branch).city(city).build();
    }

    @Test
    @DisplayName("передача при зустрічі пропонує домовитися в чаті")
    void handoverPickup() {
        notifier.handoverReady(exchange);
        expect(reader, NotificationType.HANDOVER_READY, "Домовтеся про зустріч");
    }

    @Test
    @DisplayName("передача поштою просить вказати відділення")
    void handoverMail() {
        exchange.setDeliveryMethod(DeliveryMethod.MAIL);
        notifier.handoverReady(exchange);
        expect(reader, NotificationType.HANDOVER_READY, "Вкажіть відділення");
    }

    @Test
    @DisplayName("адреса до читача повідомляється власнику")
    void addressToReader() {
        notifier.shipmentAddressAdded(exchange, ShipmentDirection.TO_READER);
        expect(owner, NotificationType.SHIPMENT_ADDRESS_ADDED, "Тарас вказав(-ла) відділення");
    }

    @Test
    @DisplayName("адреса для повернення повідомляється читачу")
    void addressToOwner() {
        notifier.shipmentAddressAdded(exchange, ShipmentDirection.TO_OWNER);
        expect(reader, NotificationType.SHIPMENT_ADDRESS_ADDED, "для повернення");
    }

    @Test
    @DisplayName("відправлення Новою Поштою з відділенням і містом")
    void shippedNovaPoshta() {
        notifier.bookShipped(exchange, shipment(ShipmentDirection.TO_READER, ShipmentCarrier.NOVA_POSHTA, " 12 ", " Рівне "));
        expect(reader, NotificationType.BOOK_SHIPPED, "«Лісова пісня»: Нова Пошта №12, Рівне");
    }

    @Test
    @DisplayName("повернення Укрпоштою")
    void shippedBackUkrposhta() {
        notifier.bookShipped(exchange, shipment(ShipmentDirection.TO_OWNER, ShipmentCarrier.UKRPOSHTA, null, "Київ"));
        expect(owner, NotificationType.BOOK_SHIPPED, "назад: Укрпошта, Київ");
    }

    @Test
    @DisplayName("інший перевізник: лише номер відділення")
    void shippedOtherCarrier() {
        notifier.bookShipped(exchange, shipment(ShipmentDirection.TO_READER, ShipmentCarrier.OTHER, "7", " "));
        expect(reader, NotificationType.BOOK_SHIPPED, ": відділення №7");
    }

    @Test
    @DisplayName("без даних про відправлення текст без деталей")
    void shippedWithoutDetails() {
        notifier.bookShipped(exchange, shipment(ShipmentDirection.TO_READER, ShipmentCarrier.OTHER, null, null));
        verify(notificationService).notify(eq(reader), eq(NotificationType.BOOK_SHIPPED), eq(exchange.getId()),
                argThat(text -> text.endsWith("«Лісова пісня»")));
    }

    @Test
    @DisplayName("отримання книги з датою повернення")
    void received() {
        notifier.bookReceived(exchange);
        expect(owner, NotificationType.BOOK_RECEIVED, "до 20.11.2026");
    }

    @Test
    @DisplayName("отримання книги без дати")
    void receivedNoDate() {
        exchange.setDeadline(null);
        notifier.bookReceived(exchange);
        verify(notificationService).notify(eq(owner), eq(NotificationType.BOOK_RECEIVED), eq(exchange.getId()),
                argThat(text -> !text.contains("Термін")));
    }

    @Test
    @DisplayName("повернення при зустрічі та поштою")
    void returnStarted() {
        notifier.returnStarted(exchange);
        expect(owner, NotificationType.RETURN_STARTED, "Перевірте стан книги");
    }

    @Test
    @DisplayName("повернення поштою просить вказати відділення")
    void returnStartedMail() {
        exchange.setDeliveryMethod(DeliveryMethod.MAIL);
        notifier.returnStarted(exchange);
        expect(owner, NotificationType.RETURN_STARTED, "Вкажіть відділення для повернення");
    }

    @Test
    @DisplayName("завершення сповіщає обох учасників")
    void completed() {
        notifier.completed(exchange);
        expect(reader, NotificationType.EXCHANGE_COMPLETED, "відгук про власника");
        expect(owner, NotificationType.EXCHANGE_COMPLETED, "відгук про читача");
    }

    @Test
    @DisplayName("рішення щодо продовження")
    void extensionDecided() {
        notifier.extensionDecided(exchange, true);
        expect(reader, NotificationType.EXTENSION_DECIDED, "продовжено до 20.11.2026");
    }

    @Test
    @DisplayName("відмова у продовженні")
    void extensionRejected() {
        notifier.extensionDecided(exchange, false);
        expect(reader, NotificationType.EXTENSION_DECIDED, "Поверніть книгу до 20.11.2026");
    }

    @Test
    @DisplayName("рішення щодо продовження без дати")
    void extensionWithoutDate() {
        exchange.setDeadline(null);
        notifier.extensionDecided(exchange, true);
        notifier.extensionDecided(exchange, false);
        verify(notificationService).notify(eq(reader), eq(NotificationType.EXTENSION_DECIDED), eq(exchange.getId()),
                argThat(text -> text.endsWith("продовжено")));
        verify(notificationService).notify(eq(reader), eq(NotificationType.EXTENSION_DECIDED), eq(exchange.getId()),
                argThat(text -> text.endsWith("«Лісова пісня»")));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "1, залишився 1 день",
            "2, залишилось 2 дні",
            "5, залишилось 5 днів",
            "11, залишилось 11 днів",
            "12, залишилось 12 днів",
            "21, залишилось 21 день",
            "22, залишилось 22 дні"
    })
    @DisplayName("відмінювання кількості днів")
    void deadlineSoon(long days, String expected) {
        notifier.deadlineSoon(exchange, days);
        expect(reader, NotificationType.DEADLINE_SOON, expected);
    }

    @Test
    @DisplayName("останній день читання")
    void lastDay() {
        notifier.deadlineLastDay(exchange);
        expect(reader, NotificationType.DEADLINE_SOON, "останній день");
    }

    @Test
    @DisplayName("прострочення сповіщає читача й власника")
    void overdue() {
        notifier.deadlineOverdue(exchange);
        expect(reader, NotificationType.DEADLINE_OVERDUE, "минув 20.11.2026");
        expect(owner, NotificationType.DEADLINE_OVERDUE, "не повернув(-ла)");
    }
}
