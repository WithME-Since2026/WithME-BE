package yooze.withme.domain.calendar.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.entity.RecurrenceException;
import yooze.withme.domain.calendar.enums.RecurrenceExceptionType;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;

@Service
public class RecurrenceExpander {

    public static final int MAX_OCCURRENCES = 1000;

    /** 반복 규칙을 조회 구간 안의 원본 회차 날짜로 전개한다. */
    public List<LocalDate> expand(
            Recurrence rule,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        validate(rule, anchorDate, from, to);

        LocalDate effectiveTo = rule.getEndType() == RecurrenceEndType.DATE
                && rule.getEndDate().isBefore(to)
                ? rule.getEndDate()
                : to;
        if (effectiveTo.isBefore(from) || effectiveTo.isBefore(anchorDate)) {
            return List.of();
        }

        return switch (rule.getFreq()) {
            case DAILY -> expandDaily(rule, anchorDate, from, effectiveTo);
            case WEEKLY -> expandWeekly(rule, anchorDate, from, effectiveTo);
            case MONTHLY -> expandMonthly(rule, anchorDate, from, effectiveTo);
        };
    }

    /**
     * 전개된 원본 회차에 예외를 적용한다. SKIP은 제거하고, OVERRIDE는 값을 덮어쓰되
     * 옮겨진 날짜가 구간 밖으로 나가면 제거하고 밖에서 안으로 들어오면 추가한다.
     */
    public List<OccurrenceResponse> applyExceptions(
            List<LocalDate> dates,
            Collection<RecurrenceException> exceptions,
            LocalDate from,
            LocalDate to
    ) {
        Map<LocalDate, RecurrenceException> byDate = new HashMap<>();
        for (RecurrenceException exception : exceptions) {
            byDate.put(exception.getOccurrenceDate(), exception);
        }

        List<OccurrenceResponse> occurrences = new ArrayList<>();
        for (LocalDate date : dates) {
            RecurrenceException exception = byDate.remove(date);
            if (exception == null) {
                occurrences.add(OccurrenceResponse.of(date));
            } else if (exception.getExceptionType() == RecurrenceExceptionType.OVERRIDE) {
                addIfWithin(occurrences, OccurrenceResponse.from(exception), from, to);
            }
        }

        // 원본 날짜가 구간 밖이지만 옮겨진 날짜가 구간 안으로 들어오는 회차
        for (RecurrenceException exception : byDate.values()) {
            if (exception.getExceptionType() == RecurrenceExceptionType.OVERRIDE
                    && exception.getOverrideDate() != null) {
                addIfWithin(occurrences, OccurrenceResponse.from(exception), from, to);
            }
        }

        occurrences.sort(Comparator.comparing(OccurrenceResponse::date)
                .thenComparing(OccurrenceResponse::occurrenceDate));
        return occurrences;
    }

    private void addIfWithin(
            List<OccurrenceResponse> occurrences,
            OccurrenceResponse occurrence,
            LocalDate from,
            LocalDate to
    ) {
        if (!occurrence.date().isBefore(from) && !occurrence.date().isAfter(to)) {
            occurrences.add(occurrence);
        }
    }

    private List<LocalDate> expandDaily(
            Recurrence rule,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        List<LocalDate> dates = new ArrayList<>();
        long index = 0;
        if (rule.getEndType() != RecurrenceEndType.COUNT) {
            long days = ChronoUnit.DAYS.between(anchorDate, from);
            if (days > 0) {
                index = (days + rule.getRepeatInterval() - 1) / rule.getRepeatInterval();
            }
        }

        while (rule.getEndType() != RecurrenceEndType.COUNT || index < rule.getEndCount()) {
            LocalDate candidate = anchorDate.plusDays(index * rule.getRepeatInterval());
            if (candidate.isAfter(to)) {
                break;
            }
            if (!candidate.isBefore(from)) {
                add(dates, candidate);
            }
            index++;
        }
        return dates;
    }

    private List<LocalDate> expandWeekly(
            Recurrence rule,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        List<LocalDate> dates = new ArrayList<>();
        EnumSet<DayOfWeek> days = parseDays(rule.getByDays());
        LocalDate anchorWeek = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long blockIndex = 0;
        int seen = 0;

        if (rule.getEndType() != RecurrenceEndType.COUNT) {
            LocalDate fromWeek = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long weeks = ChronoUnit.WEEKS.between(anchorWeek, fromWeek);
            if (weeks > 0) {
                blockIndex = weeks / rule.getRepeatInterval();
            }
        }

        while (true) {
            LocalDate week = anchorWeek.plusWeeks(blockIndex * rule.getRepeatInterval());
            if (week.isAfter(to)) {
                break;
            }

            for (DayOfWeek day : days) {
                LocalDate candidate = week.plusDays(day.getValue() - 1L);
                if (candidate.isBefore(anchorDate)) {
                    continue;
                }
                if (rule.getEndType() == RecurrenceEndType.COUNT) {
                    if (seen >= rule.getEndCount()) {
                        return dates;
                    }
                    seen++;
                }
                if (candidate.isAfter(to)) {
                    return dates;
                }
                if (!candidate.isBefore(from)) {
                    add(dates, candidate);
                }
            }
            blockIndex++;
        }
        return dates;
    }

    private List<LocalDate> expandMonthly(
            Recurrence rule,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        List<LocalDate> dates = new ArrayList<>();
        YearMonth anchorMonth = YearMonth.from(anchorDate);
        YearMonth toMonth = YearMonth.from(to);
        int dayOfMonth = anchorDate.getDayOfMonth();
        long slot = 0;
        int seen = 0;

        if (rule.getEndType() != RecurrenceEndType.COUNT) {
            long months = ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(from));
            if (months > 0) {
                slot = months / rule.getRepeatInterval();
            }
        }

        while (true) {
            if (rule.getEndType() == RecurrenceEndType.COUNT && seen >= rule.getEndCount()) {
                break;
            }
            YearMonth month = anchorMonth.plusMonths(slot * rule.getRepeatInterval());
            if (month.isAfter(toMonth)) {
                break;
            }
            if (dayOfMonth <= month.lengthOfMonth()) {
                LocalDate candidate = month.atDay(dayOfMonth);
                if (!candidate.isBefore(anchorDate)) {
                    if (rule.getEndType() == RecurrenceEndType.COUNT) {
                        seen++;
                    }
                    if (!candidate.isBefore(from) && !candidate.isAfter(to)) {
                        add(dates, candidate);
                    }
                }
            }
            slot++;
        }
        return dates;
    }

    private void validate(
            Recurrence rule,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        if (rule == null || anchorDate == null || from == null || to == null
                || from.isAfter(to) || rule.getFreq() == null || rule.getEndType() == null
                || rule.getRepeatInterval() < 1) {
            throw invalid();
        }

        if (rule.getFreq() == RecurrenceFreq.WEEKLY) {
            parseDays(rule.getByDays());
        } else if (rule.getByDays() != null) {
            throw invalid();
        }

        switch (rule.getEndType()) {
            case NEVER -> {
                if (rule.getEndDate() != null || rule.getEndCount() != null) {
                    throw invalid();
                }
            }
            case DATE -> {
                if (rule.getEndDate() == null || rule.getEndCount() != null
                        || rule.getEndDate().isBefore(anchorDate)) {
                    throw invalid();
                }
            }
            case COUNT -> {
                if (rule.getEndDate() != null || rule.getEndCount() == null
                        || rule.getEndCount() < 1 || rule.getEndCount() > MAX_OCCURRENCES) {
                    throw invalid();
                }
            }
        }
    }

    private EnumSet<DayOfWeek> parseDays(String byDays) {
        if (byDays == null || byDays.isBlank()) {
            throw invalid();
        }

        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String code : byDays.split(",", -1)) {
            DayOfWeek day = switch (code) {
                case "MON" -> DayOfWeek.MONDAY;
                case "TUE" -> DayOfWeek.TUESDAY;
                case "WED" -> DayOfWeek.WEDNESDAY;
                case "THU" -> DayOfWeek.THURSDAY;
                case "FRI" -> DayOfWeek.FRIDAY;
                case "SAT" -> DayOfWeek.SATURDAY;
                case "SUN" -> DayOfWeek.SUNDAY;
                default -> throw invalid();
            };
            if (!days.add(day)) {
                throw invalid();
            }
        }
        return days;
    }

    private void add(List<LocalDate> dates, LocalDate date) {
        if (dates.size() >= MAX_OCCURRENCES) {
            throw invalid();
        }
        dates.add(date);
    }

    private GeneralException invalid() {
        return new GeneralException(ErrorStatus.INVALID_RECURRENCE);
    }
}
