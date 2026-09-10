package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.calendar.dto.response.HolidayResponse;
import yooze.withme.domain.calendar.repository.HolidayRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayQueryService {

    private final HolidayRepository holidayRepository;

    /** 캘린더 조회는 외부 API를 부르지 않고 캐싱된 DB만 본다. */
    public List<HolidayResponse> findBetween(LocalDate from, LocalDate to) {
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(HolidayResponse::from)
                .toList();
    }
}
