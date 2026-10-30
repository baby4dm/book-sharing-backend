package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.booksharing.dto.res.ChatEvent;
import com.booksharing.dto.res.NotificationEvent;
import com.booksharing.entity.ModerationAction;
import com.booksharing.entity.User;
import com.booksharing.enums.ModerationActionType;
import com.booksharing.enums.ModerationTargetType;
import com.booksharing.repository.ModerationActionRepository;
import com.booksharing.support.TestData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("Допоміжні сервіси")
class InfrastructureServicesTest {

    @Nested
    @DisplayName("журнал модерації")
    class ModerationLog {

        private final ModerationActionRepository repository = mock(ModerationActionRepository.class);
        private final ModerationLogService service = new ModerationLogService(repository);

        @Test
        @DisplayName("зберігає дію з коротким витягом тексту")
        void logsAction() {
            User moderator = TestData.user("Модератор");
            User target = TestData.user("Автор");
            UUID postId = UUID.randomUUID();

            service.log(moderator, ModerationActionType.DELETE_POST, ModerationTargetType.POST, postId, target,
                    "Спам", "  багато\n\n   пробілів  ");

            ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
            verify(repository).save(captor.capture());
            ModerationAction action = captor.getValue();
            assertThat(action.getModerator()).isEqualTo(moderator);
            assertThat(action.getTargetUser()).isEqualTo(target);
            assertThat(action.getTargetId()).isEqualTo(postId);
            assertThat(action.getDetails()).isEqualTo("багато пробілів");
        }

        @Test
        @DisplayName("довгий текст обрізається до 300 символів")
        void excerpt() {
            assertThat(ModerationLogService.excerpt(null)).isNull();
            assertThat(ModerationLogService.excerpt("x".repeat(300))).hasSize(300);
            String cut = ModerationLogService.excerpt("x".repeat(400));
            assertThat(cut).hasSize(300).endsWith("…");
        }
    }

    @Nested
    @DisplayName("розсилка подій через WebSocket")
    class Publishers {

        private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        private final UUID userId = UUID.randomUUID();

        @AfterEach
        void clear() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("без транзакції подія надсилається одразу")
        void sendsImmediately() {
            new ChatEventPublisher(template).sendTo(userId, ChatEvent.ofTyping(UUID.randomUUID(), userId));
            new NotificationEventPublisher(template).sendTo(userId, NotificationEvent.readAll());
            verify(template).convertAndSendToUser(eq(userId.toString()), eq(ChatEventPublisher.USER_QUEUE), any(Object.class));
            verify(template).convertAndSendToUser(eq(userId.toString()), eq(NotificationEventPublisher.USER_QUEUE), any(Object.class));
        }

        @Test
        @DisplayName("у транзакції подія надсилається після коміту")
        void sendsAfterCommit() {
            TransactionSynchronizationManager.initSynchronization();

            new ChatEventPublisher(template).sendTo(userId, ChatEvent.ofTyping(UUID.randomUUID(), userId));
            new NotificationEventPublisher(template).sendTo(userId, NotificationEvent.readAll());
            verifyNoInteractions(template);

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(2);
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            verify(template).convertAndSendToUser(eq(userId.toString()), eq(ChatEventPublisher.USER_QUEUE), any(Object.class));
            verify(template).convertAndSendToUser(eq(userId.toString()), eq(NotificationEventPublisher.USER_QUEUE), any(Object.class));
        }
    }

    @Nested
    @DisplayName("сховище файлів")
    class FileStorage {

        @TempDir
        Path dir;

        @Test
        @DisplayName("файл зберігається з унікальним ім'ям і розширенням")
        void stores() throws IOException {
            FileStorageService storage = new FileStorageService(dir.toString());
            String name = storage.store(new MockMultipartFile("file", "photo.JPG", "image/jpeg", new byte[]{1, 2, 3}));
            assertThat(name).endsWith(".JPG").hasSizeGreaterThan(36);
            assertThat(Files.readAllBytes(dir.resolve(name))).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("файл без розширення чи назви")
        void noExtension() {
            FileStorageService storage = new FileStorageService(dir.toString());
            assertThat(storage.store(new MockMultipartFile("file", "README", "text/plain", new byte[]{1}))).doesNotContain(".");
            assertThat(storage.store(new MockMultipartFile("file", null, "text/plain", new byte[]{1}))).doesNotContain(".");
        }

        @Test
        @DisplayName("порожній файл відхиляється")
        void empty() {
            FileStorageService storage = new FileStorageService(dir.toString());
            assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("помилка читання перетворюється на зрозумілий виняток")
        void ioError() throws IOException {
            FileStorageService storage = new FileStorageService(dir.toString());
            MultipartFile broken = mock(MultipartFile.class);
            when(broken.isEmpty()).thenReturn(false);
            when(broken.getOriginalFilename()).thenReturn("a.png");
            when(broken.getInputStream()).thenThrow(new IOException("диск"));
            assertThatThrownBy(() -> storage.store(broken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Не вдалося зберегти");
        }

        @Test
        @DisplayName("неможливо створити директорію")
        void cannotCreateDirectory() throws IOException {
            Path file = Files.createFile(dir.resolve("not-a-dir"));
            assertThatThrownBy(() -> new FileStorageService(file.resolve("sub").toString()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
