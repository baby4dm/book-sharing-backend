package com.booksharing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.AddExchangePhotoRequest;
import com.booksharing.dto.req.AddListingPhotoRequest;
import com.booksharing.dto.req.ChangeRoleRequest;
import com.booksharing.dto.req.CreateCommentRequest;
import com.booksharing.dto.req.CreateDisputeRequest;
import com.booksharing.dto.req.CreateExtensionRequest;
import com.booksharing.dto.req.CreateFeedbackRequest;
import com.booksharing.dto.req.CreateListingRequest;
import com.booksharing.dto.req.CreatePostRequest;
import com.booksharing.dto.req.CreateRequestRequest;
import com.booksharing.dto.req.CreateReviewRequest;
import com.booksharing.dto.req.CreateShipmentRequest;
import com.booksharing.dto.req.DecideExtensionRequest;
import com.booksharing.dto.req.LiftRestrictionRequest;
import com.booksharing.dto.req.LoginRequest;
import com.booksharing.dto.req.ModerationReasonRequest;
import com.booksharing.dto.req.OpenDisputeRequest;
import com.booksharing.dto.req.RegisterRequest;
import com.booksharing.dto.req.RejectRequestRequest;
import com.booksharing.dto.req.ResolveBookCatalogEntryRequest;
import com.booksharing.dto.req.ResolveDisputeRequest;
import com.booksharing.dto.req.RestrictUserRequest;
import com.booksharing.dto.req.SendMessageRequest;
import com.booksharing.dto.req.ShipWaybillRequest;
import com.booksharing.dto.req.SubmitPhotosRequest;
import com.booksharing.dto.req.UpdateFeedbackRequest;
import com.booksharing.dto.req.UpdateListingRequest;
import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.dto.res.ExchangeResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.ExchangeRole;
import com.booksharing.enums.ExchangeSort;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.FeedbackTopic;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.RequestSort;
import com.booksharing.enums.RequestStatus;
import com.booksharing.enums.RestrictionType;
import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import com.booksharing.service.AuthService;
import com.booksharing.service.BookCatalogService;
import com.booksharing.service.ChatService;
import com.booksharing.service.DisputeService;
import com.booksharing.service.ExchangeService;
import com.booksharing.service.FeedService;
import com.booksharing.service.FeedbackService;
import com.booksharing.service.FileStorageService;
import com.booksharing.service.ListingService;
import com.booksharing.service.ModerationService;
import com.booksharing.service.NotificationService;
import com.booksharing.service.RequestService;
import com.booksharing.service.ReviewService;
import com.booksharing.service.UserService;
import com.booksharing.support.TestData;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Контролери лише передають параметри в сервіси, тому перевіряємо саме це:
 * правильний сервісний метод, поточного користувача й значення за замовчуванням.
 */
@DisplayName("REST-контролери")
class ControllersTest {

    private final User me = TestData.user("Я");
    private final UUID id = UUID.randomUUID();

    @Test
    @DisplayName("автентифікація")
    void auth() {
        AuthService service = mock(AuthService.class);
        AuthController controller = new AuthController(service);
        RegisterRequest register = new RegisterRequest("a@b.ua", "secret123", "Я");
        LoginRequest login = new LoginRequest("a@b.ua", "secret123");
        controller.register(register);
        controller.login(login);
        verify(service).register(register);
        verify(service).login(login);
    }

    @Test
    @DisplayName("каталог книг")
    void bookCatalog() {
        BookCatalogService service = mock(BookCatalogService.class);
        BookCatalogController controller = new BookCatalogController(service);
        ResolveBookCatalogEntryRequest body = new ResolveBookCatalogEntryRequest("Т", null, null, null, null, null, null);
        controller.search("Кобзар");
        controller.resolve(body);
        controller.getById(id);
        verify(service).search("Кобзар");
        verify(service).resolve(body);
        verify(service).getById(id);
    }

    @Test
    @DisplayName("чати")
    void chats() {
        ChatService service = mock(ChatService.class);
        ChatController controller = new ChatController(service);
        LocalDateTime before = LocalDateTime.now();
        SendMessageRequest message = new SendMessageRequest("Привіт");

        controller.getMyChatRooms(me);
        controller.getUnreadCount(me);
        controller.getByExchange(id, me);
        controller.getWithUser(id, me);
        controller.getRoom(id, me);
        controller.getMessages(id, before, 20, me);
        controller.sendMessage(id, me, message);
        controller.markAsRead(id, me);

        verify(service).getMyChatRooms(me.getId());
        verify(service).getUnreadCount(me.getId());
        verify(service).getRoomByExchange(id, me.getId());
        verify(service).getRoomWithUser(id, me);
        verify(service).getRoom(id, me.getId());
        verify(service).getMessages(id, me.getId(), before, 20);
        verify(service).sendMessage(id, me, message);
        verify(service).markAsRead(id, me.getId());
    }

    @Test
    @DisplayName("чат через WebSocket")
    void chatWebSocket() {
        ChatService service = mock(ChatService.class);
        UserRepository users = mock(UserRepository.class);
        ChatWebSocketController controller = new ChatWebSocketController(service, users);
        Principal principal = () -> me.getId().toString();
        SendMessageRequest message = new SendMessageRequest("Привіт");
        when(users.findById(me.getId())).thenReturn(Optional.of(me));

        controller.send(id, message, principal);
        controller.typing(id, principal);

        verify(service).sendMessage(id, me, message);
        verify(service).notifyTyping(id, me.getId());
        Principal unknown = () -> UUID.randomUUID().toString();
        assertThatThrownBy(() -> controller.send(id, message, unknown)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ExchangeResponse exchangeResponse(UUID ownerId, UUID readerId) {
        return new ExchangeResponse(id, null, "Книга", ownerId, "Власник", readerId, "Читач", DeliveryMethod.PICKUP,
                null, null, ExchangeStatus.IN_READING, List.of(), List.of(), List.of(), null, List.of(), List.of(),
                null, null, null, null, null, null, List.of(), 14, null, 0, null, null, null, false, null);
    }

    @Test
    @DisplayName("обмін бачать учасники й модератори")
    void exchangeAccess() {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeController controller = new ExchangeController(service);
        User other = TestData.user("Інший");
        when(service.getById(id)).thenReturn(exchangeResponse(me.getId(), other.getId()));

        assertThat(controller.getById(id, me)).isNotNull();
        assertThat(controller.getById(id, other)).isNotNull();
        assertThat(controller.getById(id, TestData.user("Модератор", UserRole.MODERATOR))).isNotNull();
        assertThat(controller.getById(id, TestData.user("Адмін", UserRole.ADMIN))).isNotNull();
        assertThatThrownBy(() -> controller.getById(id, TestData.user("Сторонній")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("дії з обміном")
    void exchangeActions() {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeController controller = new ExchangeController(service);
        UUID other = UUID.randomUUID();
        SubmitPhotosRequest photos = new SubmitPhotosRequest(List.of("/a.jpg"));
        AddExchangePhotoRequest photo = new AddExchangePhotoRequest("/a.jpg", null);
        OpenDisputeRequest dispute = new OpenDisputeRequest("Пошкоджено", List.of("/a.jpg"));
        CreateExtensionRequest extension = new CreateExtensionRequest(5, null);
        CreateShipmentRequest shipment = new CreateShipmentRequest(ShipmentDirection.TO_READER, "Я", "+380",
                ShipmentCarrier.NOVA_POSHTA, "Рівне", "1");
        ShipWaybillRequest waybill = new ShipWaybillRequest("/w.jpg");

        controller.getMyExchanges(me);
        controller.submitHandoverPhotos(id, me, photos);
        controller.confirmReceived(id, me, photos);
        controller.submitReturnPhotos(id, me, photos);
        controller.confirmReturn(id, me);
        controller.addDisputePhoto(id, me, photo);
        controller.openDispute(id, me, dispute);
        controller.requestExtension(id, me, extension);
        controller.decideExtension(id, other, me, true, new DecideExtensionRequest("Так"));
        controller.decideExtension(id, other, me, false, null);
        controller.createShipment(id, me, shipment);
        controller.shipWaybill(id, other, me, waybill);
        controller.searchMyExchanges(me, "q", ExchangeRole.ALL, null, null, ExchangeSort.DEADLINE, 0, 10);

        verify(service).getMyExchanges(me.getId());
        verify(service).submitHandoverPhotos(id, me.getId(), photos);
        verify(service).confirmReceived(id, me.getId(), photos);
        verify(service).submitReturnPhotos(id, me.getId(), photos);
        verify(service).confirmReturn(id, me.getId());
        verify(service).addDisputePhoto(id, me.getId(), photo);
        verify(service).openDispute(id, me.getId(), dispute);
        verify(service).requestExtension(id, me.getId(), extension);
        verify(service).decideExtension(id, other, me.getId(), true, "Так");
        verify(service).decideExtension(eq(id), eq(other), eq(me.getId()), eq(false), isNull());
        verify(service).createShipment(id, me.getId(), shipment);
        verify(service).shipWaybill(id, other, me.getId(), waybill);
        verify(service).searchMyExchanges(me.getId(), "q", ExchangeRole.ALL, null, null, ExchangeSort.DEADLINE, 0, 10);
    }

    @Test
    @DisplayName("скарги учасників")
    void disputes() {
        DisputeService service = mock(DisputeService.class);
        ExchangeDisputeController controller = new ExchangeDisputeController(service);
        CreateDisputeRequest body = new CreateDisputeRequest("Опис");
        controller.fileDispute(id, me, body);
        controller.getById(id, me);
        verify(service).fileDispute(id, me.getId(), body);
        verify(service).getById(id, me.getId());
    }

    @Test
    @DisplayName("звернення: IP-адреса й поточний користувач")
    void feedback() {
        FeedbackService service = mock(FeedbackService.class);
        FeedbackController controller = new FeedbackController(service);
        CreateFeedbackRequest body = new CreateFeedbackRequest(FeedbackTopic.BUG, "a@b.ua", "Текст", null, null);
        UpdateFeedbackRequest update = new UpdateFeedbackRequest(FeedbackStatus.RESOLVED, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.7");

        controller.create(body, null, request);
        controller.search(FeedbackStatus.NEW, null, 0, 20);
        controller.update(id, update, me);

        verify(service).create(body, null, "192.168.0.7");
        verify(service).search(FeedbackStatus.NEW, null, 0, 20);
        verify(service).update(id, update, me);
    }

    @Test
    @DisplayName("оголошення: власні не показуються в загальному каталозі")
    void listings() {
        ListingService service = mock(ListingService.class);
        ListingController controller = new ListingController(service);
        PageRequest page = PageRequest.of(0, 12);
        CreateListingRequest create = new CreateListingRequest(id, "x", List.of("PICKUP"), null, null, null, null);
        UpdateListingRequest update = new UpdateListingRequest(null, null, ListingStatus.ARCHIVED, null, null, null);
        AddListingPhotoRequest photo = new AddListingPhotoRequest("/p.jpg");

        controller.search(null, null, null, null, "q", null, page);
        controller.search(null, null, null, null, "q", me, page);
        controller.getCities();
        controller.getMyListings(null, null, null, null, null, me, page);
        controller.getByOwner(id, page);
        controller.getById(id, null);
        controller.create(me, create);
        controller.update(id, me, update);
        controller.delete(id, me);
        controller.addPhoto(id, me, photo);

        verify(service).search(null, null, null, null, "q", null, page);
        verify(service).search(null, null, null, null, "q", me.getId(), page);
        verify(service).getAvailableCities();
        verify(service).getMyListings(me.getId(), null, null, null, null, null, page);
        verify(service).getByOwner(id, null, null, null,
                List.of(ListingStatus.AVAILABLE, ListingStatus.RESERVED, ListingStatus.IN_EXCHANGE), null, false, page);
        verify(service).getById(id, null);
        verify(service).create(me.getId(), create);
        verify(service).update(id, me.getId(), update);
        verify(service).delete(id, me.getId());
        verify(service).addPhoto(id, me.getId(), photo);
    }

    @Test
    @DisplayName("заявки")
    void requests() {
        RequestService service = mock(RequestService.class);
        ListingRequestController listingRequests = new ListingRequestController(service);
        RequestController controller = new RequestController(service);
        CreateRequestRequest create = new CreateRequestRequest(14, "PICKUP", null);
        RejectRequestRequest reject = new RejectRequestRequest("Ні");

        listingRequests.getQueue(id);
        listingRequests.submit(id, me, create);
        controller.getMyRequests(me, "q", DeliveryMethod.MAIL, RequestStatus.PENDING, RequestSort.NEWEST, 0, 10);
        controller.getReceivedRequests(me, null, null, null, RequestSort.RATING, 1, 5);
        controller.approve(id, me);
        controller.reject(id, me, reject);
        controller.cancel(id, me);

        verify(service).getQueueForListing(id);
        verify(service).submit(id, me.getId(), create);
        verify(service).getMyRequests(me.getId(), "q", DeliveryMethod.MAIL, RequestStatus.PENDING, RequestSort.NEWEST, 0, 10);
        verify(service).getReceivedRequests(me.getId(), null, null, null, RequestSort.RATING, 1, 5);
        verify(service).approve(id, me.getId());
        verify(service).reject(id, me.getId(), reject);
        verify(service).cancel(id, me.getId());
    }

    @Test
    @DisplayName("модерація: параметри статусу й області журналу")
    void moderation() {
        DisputeService disputes = mock(DisputeService.class);
        ModerationService service = mock(ModerationService.class);
        ModerationController controller = new ModerationController(disputes, service);
        ResolveDisputeRequest resolve = new ResolveDisputeRequest(DisputeStatus.DISMISSED, "x");
        RestrictUserRequest restrict = new RestrictUserRequest(RestrictionType.TEMPORARY, 3, "x");
        LiftRestrictionRequest lift = new LiftRestrictionRequest("x");
        ChangeRoleRequest role = new ChangeRoleRequest(UserRole.MODERATOR);
        ModerationReasonRequest reason = new ModerationReasonRequest("Спам");

        controller.summary();
        controller.listDisputes("OPEN");
        controller.listDisputes("resolved");
        controller.getDispute(id);
        controller.resolve(id, me, resolve);
        controller.searchUsers("q", UserStatus.BLOCKED, 0, 20);
        controller.getUser(id);
        controller.restrictUser(id, me, restrict);
        controller.liftRestriction(id, me, lift);
        controller.changeRole(id, me, role);
        controller.hiddenListings(0, 20);
        controller.hideListing(id, me, reason);
        controller.restoreListing(id, me);
        controller.deletePost(id, me, reason);
        controller.deleteComment(id, me, reason);
        controller.actions("content", 0, 20);
        controller.actions("all", 0, 20);

        verify(service).summary();
        verify(service).listDisputes(true);
        verify(service).listDisputes(false);
        verify(service).getDispute(id);
        verify(disputes).resolve(id, me.getId(), resolve);
        verify(service).searchUsers("q", UserStatus.BLOCKED, 0, 20);
        verify(service).getUser(id);
        verify(service).restrictUser(me.getId(), id, restrict);
        verify(service).liftRestriction(me.getId(), id, lift);
        verify(service).changeRole(me.getId(), id, role);
        verify(service).hiddenListings(0, 20);
        verify(service).hideListing(me.getId(), id, reason);
        verify(service).restoreListing(me.getId(), id);
        verify(service).deletePost(me.getId(), id, reason);
        verify(service).deleteComment(me.getId(), id, reason);
        verify(service).actions(true, 0, 20);
        verify(service).actions(false, 0, 20);
    }

    @Test
    @DisplayName("сповіщення")
    void notifications() {
        NotificationService service = mock(NotificationService.class);
        NotificationController controller = new NotificationController(service);
        when(service.countUnread(me.getId())).thenReturn(3L);

        controller.getMyNotifications(me, null, true, 10);
        assertThat(controller.getUnreadCount(me).count()).isEqualTo(3);
        assertThat(controller.markRead(id, me).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.markAllRead(me).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).getMyNotifications(me.getId(), null, true, 10);
        verify(service).markRead(id, me.getId());
        verify(service).markAllRead(me.getId());
    }

    @Test
    @DisplayName("стрічка: модератор видаляє чужі пости")
    void posts() {
        FeedService service = mock(FeedService.class);
        PostController controller = new PostController(service);
        User moderator = TestData.user("Модератор", UserRole.MODERATOR);
        User admin = TestData.user("Адмін", UserRole.ADMIN);
        CreatePostRequest post = new CreatePostRequest("Текст", null);
        CreateCommentRequest comment = new CreateCommentRequest("Коментар");
        PageRequest page = PageRequest.of(0, 10);

        controller.getFeed(page, me);
        controller.createPost(me, post);
        controller.deletePost(id, me);
        controller.deletePost(id, moderator);
        controller.deletePost(id, admin);
        controller.like(id, me);
        controller.unlike(id, me);
        controller.getComments(id);
        controller.createComment(id, me, comment);

        verify(service).getFeed(page, me);
        verify(service).createPost(me, post);
        verify(service).deletePost(id, me.getId(), false);
        verify(service).deletePost(id, moderator.getId(), true);
        verify(service).deletePost(id, admin.getId(), true);
        verify(service).likePost(id, me);
        verify(service).unlikePost(id, me);
        verify(service).getComments(id);
        verify(service).createComment(id, me, comment);
    }

    @Test
    @DisplayName("відгуки й профілі")
    void reviewsAndUsers() {
        ReviewService reviews = mock(ReviewService.class);
        UserService users = mock(UserService.class);
        ReviewController reviewController = new ReviewController(reviews);
        UserController userController = new UserController(users, reviews);
        CreateReviewRequest review = new CreateReviewRequest(5, null);
        UpdateUserProfileRequest profile = new UpdateUserProfileRequest("Нове ім'я", null, null, null, null, null);

        reviewController.create(id, me, review);
        userController.getPublicProfile(id);
        userController.getReviews(id, 0, 10);
        userController.getMyProfile(me);
        userController.updateMyProfile(me, profile);

        verify(reviews).create(id, me.getId(), review);
        verify(users).getPublicProfile(id);
        verify(reviews).getForUser(id, 0, 10);
        verify(users).getMyProfile(me);
        verify(users).updateMyProfile(me.getId(), profile);
    }

    @Test
    @DisplayName("завантаження файлу повертає повну адресу")
    void upload() {
        FileStorageService storage = mock(FileStorageService.class);
        UploadController controller = new UploadController(storage);
        ReflectionTestUtils.setField(controller, "backendBaseUrl", "http://localhost:8080");
        when(storage.store(any())).thenReturn("abc.jpg");

        assertThat(controller.upload(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1})).url())
                .isEqualTo("http://localhost:8080/uploads/abc.jpg");
    }
}
