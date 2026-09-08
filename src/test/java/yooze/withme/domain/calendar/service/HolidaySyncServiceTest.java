package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import yooze.withme.common.notify.DiscordErrorNotifier;
import yooze.withme.common.properties.HolidayProperties;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.enums.HolidayType;
import yooze.withme.domain.calendar.repository.HolidayRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HolidaySyncServiceTest {

    private static final int YEAR = 2026;

    @Mock
    private HolidayApiClient holidayApiClient;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DiscordErrorNotifier discordErrorNotifier;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void skipsWhenServiceKeyMissing() {
        HolidaySyncService service = service(new HolidayProperties("", true));

        assertThat(service.sync(YEAR)).isFalse();
        verifyNoInteractions(holidayApiClient, stringRedisTemplate);
    }

    @Test
    void skipsWhenAnotherInstanceHoldsLock() {
        givenLock(false);
        HolidaySyncService service = service(usableProperties());

        assertThat(service.sync(YEAR)).isFalse();
        verifyNoInteractions(holidayApiClient);
    }

    @Test
    void upsertsExistingHolidayInsteadOfInserting() {
        givenLock(true);
        Holiday fetched = holiday(HolidayType.HOLIDAY, true);
        Holiday existing = holiday(HolidayType.NATIONAL, false);
        when(holidayApiClient.fetch(eq(HolidayType.HOLIDAY), anyInt())).thenReturn(List.of(fetched));
        when(holidayApiClient.fetch(eq(HolidayType.NATIONAL), anyInt())).thenReturn(List.of());
        when(holidayApiClient.fetch(eq(HolidayType.ANNIVERSARY), anyInt())).thenReturn(List.of());
        when(holidayApiClient.fetch(eq(HolidayType.SOLAR_TERM), anyInt())).thenReturn(List.of());
        when(holidayApiClient.fetch(eq(HolidayType.SUNDRY), anyInt())).thenReturn(List.of());
        when(holidayRepository.findByDateAndName(fetched.getDate(), fetched.getName()))
                .thenReturn(Optional.of(existing));

        assertThat(service(usableProperties()).sync(YEAR)).isTrue();

        ArgumentCaptor<Holiday> captor = ArgumentCaptor.forClass(Holiday.class);
        verify(holidayRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getType()).isEqualTo(HolidayType.HOLIDAY);
        assertThat(captor.getValue().isRestDay()).isTrue();
    }

    @Test
    void notifiesAndSwallowsApiFailure() {
        givenLock(true);
        when(holidayApiClient.fetch(any(HolidayType.class), anyInt()))
                .thenThrow(new IllegalStateException("공공데이터포털 500"));

        assertThat(service(usableProperties()).sync(YEAR)).isFalse();
        verify(discordErrorNotifier).notify(any(), any(Throwable.class));
    }

    @Test
    void syncIfEmptySkipsWhenYearAlreadyCached() {
        when(holidayRepository.existsByDateBetween(
                LocalDate.of(YEAR, 1, 1),
                LocalDate.of(YEAR, 12, 31)
        )).thenReturn(true);

        service(usableProperties()).syncIfEmpty(YEAR);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    private void givenLock(boolean acquired) {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(acquired);
    }

    private HolidayProperties usableProperties() {
        return new HolidayProperties("test-key", true);
    }

    private Holiday holiday(HolidayType type, boolean restDay) {
        return Holiday.builder()
                .date(LocalDate.of(YEAR, 8, 15))
                .name("광복절")
                .type(type)
                .restDay(restDay)
                .build();
    }

    private HolidaySyncService service(HolidayProperties properties) {
        return new HolidaySyncService(
                holidayApiClient,
                holidayRepository,
                properties,
                stringRedisTemplate,
                discordErrorNotifier,
                new TransactionTemplate(transactionManager)
        );
    }
}
