package yooze.withme.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.storage.S3ImageStorage;
import yooze.withme.domain.auth.client.KakaoAuthClient;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.notification.service.NotificationCommandService;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImageTest {

    private static final Long USER_ID = 1L;
    private static final String OLD_URL = "https://bucket/profile/1/old.jpg";
    private static final String NEW_URL = "https://bucket/profile/1/new.jpg";
    private static final MockMultipartFile JPEG = new MockMultipartFile(
            "image", "a.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

    @Mock private UserRepository userRepository;
    @Mock private UserAuthRepository userAuthRepository;
    @Mock private UserTokenCommandService userTokenCommandService;
    @Mock private GroupCommandService groupCommandService;
    @Mock private NotificationCommandService notificationCommandService;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private S3ImageStorage s3ImageStorage;
    @Mock private User user;

    @InjectMocks private UserCommandService userCommandService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void detectsImageTypeBySignature() {
        assertThat(UserCommandService.imageExtension(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).isEqualTo("jpg");
        assertThat(UserCommandService.imageExtension(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00})).isEqualTo("png");
        assertThatThrownBy(() -> UserCommandService.imageExtension("GIF89a".getBytes()))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void deletesNewImageOnRollback() {
        givenProfileImageUploaded();

        userCommandService.updateProfileImage(USER_ID, JPEG);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(s3ImageStorage).delete(NEW_URL);
        verify(s3ImageStorage, never()).delete(OLD_URL);
    }

    @Test
    void keepsNewImageAndDeletesOldImageOnCommit() {
        givenProfileImageUploaded();

        userCommandService.updateProfileImage(USER_ID, JPEG);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
            sync.afterCommit();
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        });

        verify(s3ImageStorage).delete(OLD_URL);
        verify(s3ImageStorage, never()).delete(NEW_URL);
    }

    private void givenProfileImageUploaded() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getProfileImg()).thenReturn(OLD_URL);
        when(s3ImageStorage.upload(anyString(), any(byte[].class), anyString())).thenReturn(NEW_URL);
    }
}
