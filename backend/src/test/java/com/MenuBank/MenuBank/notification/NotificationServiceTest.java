package com.MenuBank.MenuBank.notification;

import com.MenuBank.MenuBank.common.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private NotificationService notificationService;

    private UUID ownerId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createMissingIngredient()")
    class CreateMissingIngredient {

        @Test
        @DisplayName("deve criar nova notificação quando não existir uma pendente para o canonical name")
        void shouldCreateNewWhenNoPendingExists() {
            given(notificationRepository.findByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                    ownerId, NotificationType.MISSING_INGREDIENT, "pistache", NotificationStatus.RESOLVED))
                    .willReturn(Optional.empty());
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            Notification result = notificationService.createMissingIngredient("Pistache", "pistache", ownerId);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            then(notificationRepository).should().save(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.getOwnerId()).isEqualTo(ownerId);
            assertThat(saved.getType()).isEqualTo(NotificationType.MISSING_INGREDIENT);
            assertThat(saved.getReferenceData()).isEqualTo("pistache");
            assertThat(saved.getReferenceDisplay()).isEqualTo("Pistache");
            assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getTitle()).contains("Ingrediente");
            assertThat(saved.getMessage()).contains("Pistache");
            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("deve reutilizar notificação existente (não duplicar) quando já existe uma não-resolvida para o mesmo canonical name")
        void shouldNotDuplicateWhenPendingExists() {
            Notification existing = Notification.builder()
                    .id(notificationId)
                    .ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .referenceData("pistache")
                    .referenceDisplay("Pistache")
                    .status(NotificationStatus.UNREAD)
                    .createdAt(Instant.now())
                    .title("Ingrediente não cadastrado")
                    .message("...")
                    .build();
            given(notificationRepository.findByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                    ownerId, NotificationType.MISSING_INGREDIENT, "pistache", NotificationStatus.RESOLVED))
                    .willReturn(Optional.of(existing));

            Notification result = notificationService.createMissingIngredient("Pistache", "pistache", ownerId);

            then(notificationRepository).should(never()).save(any(Notification.class));
            assertThat(result).isSameAs(existing);
        }
    }

    @Nested
    @DisplayName("resolveMissingIngredient()")
    class ResolveMissingIngredient {

        @Test
        @DisplayName("deve marcar notificações pendentes como RESOLVED e setar resolvedAt")
        void shouldResolveAllPendingForCanonicalName() {
            Notification n1 = Notification.builder()
                    .id(UUID.randomUUID()).ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .referenceData("pistache").referenceDisplay("Pistache")
                    .status(NotificationStatus.UNREAD).createdAt(Instant.now())
                    .title("t").message("m").build();
            Notification n2 = Notification.builder()
                    .id(UUID.randomUUID()).ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .referenceData("pistache").referenceDisplay("Pistache")
                    .status(NotificationStatus.READ).createdAt(Instant.now())
                    .title("t").message("m").build();
            given(notificationRepository.findAllByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                    ownerId, NotificationType.MISSING_INGREDIENT, "pistache", NotificationStatus.RESOLVED))
                    .willReturn(List.of(n1, n2));
            given(notificationRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            int resolved = notificationService.resolveMissingIngredient("pistache", ownerId);

            assertThat(resolved).isEqualTo(2);
            assertThat(n1.getStatus()).isEqualTo(NotificationStatus.RESOLVED);
            assertThat(n1.getResolvedAt()).isNotNull();
            assertThat(n2.getStatus()).isEqualTo(NotificationStatus.RESOLVED);
            assertThat(n2.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve retornar 0 quando não há notificações pendentes")
        void shouldReturnZeroWhenNoPending() {
            given(notificationRepository.findAllByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                    ownerId, NotificationType.MISSING_INGREDIENT, "pistache", NotificationStatus.RESOLVED))
                    .willReturn(List.of());

            int resolved = notificationService.resolveMissingIngredient("pistache", ownerId);

            assertThat(resolved).isZero();
            then(notificationRepository).should(never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de notificações ordenadas por data desc do owner autenticado")
        void shouldReturnPagedNotificationsForCurrentUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Notification n = Notification.builder()
                    .id(notificationId).ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .referenceData("pistache").referenceDisplay("Pistache")
                    .status(NotificationStatus.UNREAD)
                    .title("t").message("m").createdAt(Instant.now()).build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId, pageable))
                    .willReturn(new PageImpl<>(List.of(n), pageable, 1));

            var page = notificationService.findAll(pageable);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(notificationId);
            assertThat(page.getContent().get(0).getReferenceDisplay()).isEqualTo("Pistache");
        }
    }

    @Nested
    @DisplayName("unreadCount()")
    class UnreadCount {

        @Test
        @DisplayName("deve retornar contagem de notificações UNREAD do owner autenticado")
        void shouldReturnUnreadCount() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.countByOwnerIdAndStatus(ownerId, NotificationStatus.UNREAD))
                    .willReturn(3L);

            assertThat(notificationService.unreadCount()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("markRead()")
    class MarkRead {

        @Test
        @DisplayName("deve marcar UNREAD como READ")
        void shouldMarkUnreadAsRead() {
            Notification n = Notification.builder()
                    .id(notificationId).ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .status(NotificationStatus.UNREAD)
                    .title("t").message("m").createdAt(Instant.now()).build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findByIdAndOwnerId(notificationId, ownerId))
                    .willReturn(Optional.of(n));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

            notificationService.markRead(notificationId);

            assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
            then(notificationRepository).should().save(n);
        }

        @Test
        @DisplayName("não deve alterar status quando notificação já está RESOLVED")
        void shouldNotChangeStatusWhenAlreadyResolved() {
            Notification n = Notification.builder()
                    .id(notificationId).ownerId(ownerId)
                    .type(NotificationType.MISSING_INGREDIENT)
                    .status(NotificationStatus.RESOLVED)
                    .title("t").message("m").createdAt(Instant.now()).build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findByIdAndOwnerId(notificationId, ownerId))
                    .willReturn(Optional.of(n));

            notificationService.markRead(notificationId);

            assertThat(n.getStatus()).isEqualTo(NotificationStatus.RESOLVED);
            then(notificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve lançar NotificationNotFoundException quando notificação não existe para o owner")
        void shouldThrowWhenNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findByIdAndOwnerId(notificationId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markRead(notificationId))
                    .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("dismiss()")
    class Dismiss {

        @Test
        @DisplayName("deve deletar notificação existente do owner autenticado")
        void shouldDeleteWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findByIdAndOwnerId(notificationId, ownerId))
                    .willReturn(Optional.of(Notification.builder().id(notificationId).ownerId(ownerId).build()));

            notificationService.dismiss(notificationId);

            then(notificationRepository).should().deleteByIdAndOwnerId(notificationId, ownerId);
        }

        @Test
        @DisplayName("deve lançar NotificationNotFoundException quando não existe")
        void shouldThrowWhenNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(notificationRepository.findByIdAndOwnerId(notificationId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.dismiss(notificationId))
                    .isInstanceOf(NotificationNotFoundException.class);
        }
    }
}
