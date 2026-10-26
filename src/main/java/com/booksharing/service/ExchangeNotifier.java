package com.booksharing.service;

import com.booksharing.entity.Exchange;
import com.booksharing.entity.ShipmentInfo;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeNotifier {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationService notificationService;
    public void handoverReady(Exchange e) {
        String text = e.getDeliveryMethod() == DeliveryMethod.MAIL
                ? "Власник підготував «" + title(e) + "» до відправки. Вкажіть відділення для доставки"
                : "Власник підготував «" + title(e) + "» до передачі. Домовтеся про зустріч у чаті";
        notificationService.notify(e.getReader(), NotificationType.HANDOVER_READY, e.getId(), text);
    }

    public void shipmentAddressAdded(Exchange e, ShipmentDirection direction) {
        if (direction == ShipmentDirection.TO_READER) {
            notificationService.notify(e.getOwner(), NotificationType.SHIPMENT_ADDRESS_ADDED, e.getId(),
                    e.getReader().getName() + " вказав(-ла) відділення для «" + title(e)
                            + "» — можна відправляти");
        } else {
            notificationService.notify(e.getReader(), NotificationType.SHIPMENT_ADDRESS_ADDED, e.getId(),
                    "Власник вказав відділення для повернення «" + title(e)
                            + "». Відправте книгу й додайте накладну");
        }
    }

    public void bookShipped(Exchange e, ShipmentInfo shipment) {
        String where = describe(shipment);
        if (shipment.getDirection() == ShipmentDirection.TO_READER) {
            notificationService.notify(e.getReader(), NotificationType.BOOK_SHIPPED, e.getId(),
                    e.getOwner().getName() + " відправив(-ла) «" + title(e) + "»" + where);
        } else {
            notificationService.notify(e.getOwner(), NotificationType.BOOK_SHIPPED, e.getId(),
                    e.getReader().getName() + " відправив(-ла) «" + title(e) + "» назад" + where);
        }
    }

    public void bookReceived(Exchange e) {
        LocalDate due = e.getDueDate();
        notificationService.notify(e.getOwner(), NotificationType.BOOK_RECEIVED, e.getId(),
                e.getReader().getName() + " отримав(-ла) «" + title(e) + "»"
                        + (due != null ? ". Термін читання — до " + DATE.format(due) : ""));
    }

    public void returnStarted(Exchange e) {
        String text = e.getDeliveryMethod() == DeliveryMethod.MAIL
                ? e.getReader().getName() + " готовий(-а) повернути «" + title(e)
                + "». Вкажіть відділення для повернення"
                : e.getReader().getName() + " повернув(-ла) «" + title(e)
                + "». Перевірте стан книги й підтвердіть повернення";
        notificationService.notify(e.getOwner(), NotificationType.RETURN_STARTED, e.getId(), text);
    }

    public void completed(Exchange e) {
        notificationService.notify(e.getReader(), NotificationType.EXCHANGE_COMPLETED, e.getId(),
                "Обмін «" + title(e) + "» завершено. Залиште відгук про власника");
        notificationService.notify(e.getOwner(), NotificationType.EXCHANGE_COMPLETED, e.getId(),
                "«" + title(e) + "» знову у вас, обмін завершено. Залиште відгук про читача");
    }

    public void extensionDecided(Exchange e, boolean approved) {
        LocalDate due = e.getDueDate();
        String text = approved
                ? "Термін читання «" + title(e) + "» продовжено"
                + (due != null ? " до " + DATE.format(due) : "")
                : "Власник не погодив продовження для «" + title(e) + "»"
                + (due != null ? ". Поверніть книгу до " + DATE.format(due) : "");
        notificationService.notify(e.getReader(), NotificationType.EXTENSION_DECIDED, e.getId(), text);
    }


    public void deadlineSoon(Exchange e, long daysLeft) {
        notificationService.notify(e.getReader(), NotificationType.DEADLINE_SOON, e.getId(),
                "До кінця терміну читання «" + title(e) + "» " + daysLeftText(daysLeft)
                        + " (до " + DATE.format(e.getDueDate()) + "). Не встигаєте — попросіть продовження");
    }

    public void deadlineLastDay(Exchange e) {
        notificationService.notify(e.getReader(), NotificationType.DEADLINE_SOON, e.getId(),
                "Сьогодні останній день читання «" + title(e) + "». Поверніть книгу або попросіть продовження");
    }

    public void deadlineOverdue(Exchange e) {
        notificationService.notify(e.getReader(), NotificationType.DEADLINE_OVERDUE, e.getId(),
                "Термін читання «" + title(e) + "» минув " + DATE.format(e.getDueDate())
                        + ". Поверніть книгу якнайшвидше");
        notificationService.notify(e.getOwner(), NotificationType.DEADLINE_OVERDUE, e.getId(),
                e.getReader().getName() + " не повернув(-ла) «" + title(e) + "» вчасно. Напишіть у чат");
    }

    private static String title(Exchange e) {
        return e.getListing().getBookCatalogEntry().getTitle();
    }

    private static String daysLeftText(long days) {
        long mod10 = days % 10;
        long mod100 = days % 100;
        String word;
        if (mod10 == 1 && mod100 != 11) {
            word = "день";
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            word = "дні";
        } else {
            word = "днів";
        }
        return (days == 1 ? "залишився " : "залишилось ") + days + " " + word;
    }

    private static String describe(ShipmentInfo s) {
        StringBuilder sb = new StringBuilder();
        String carrier = s.getCarrier() == ShipmentCarrier.NOVA_POSHTA ? "Нова Пошта"
                : s.getCarrier() == ShipmentCarrier.UKRPOSHTA ? "Укрпошта"
                : null;
        if (carrier != null) {
            sb.append(carrier);
        }
        if (s.getBranchNumber() != null && !s.getBranchNumber().isBlank()) {
            sb.append(sb.isEmpty() ? "відділення " : " ").append("№").append(s.getBranchNumber().strip());
        }
        if (s.getCity() != null && !s.getCity().isBlank()) {
            sb.append(sb.isEmpty() ? "" : ", ").append(s.getCity().strip());
        }
        return sb.isEmpty() ? "" : ": " + sb;
    }
}