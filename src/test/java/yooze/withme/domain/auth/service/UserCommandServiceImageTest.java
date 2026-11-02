package yooze.withme.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import yooze.withme.common.exception.GeneralException;

class UserCommandServiceImageTest {

    @Test
    void detectsImageTypeBySignature() {
        assertThat(UserCommandService.imageExtension(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).isEqualTo("jpg");
        assertThat(UserCommandService.imageExtension(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00})).isEqualTo("png");
        assertThatThrownBy(() -> UserCommandService.imageExtension("GIF89a".getBytes()))
                .isInstanceOf(GeneralException.class);
    }
}
