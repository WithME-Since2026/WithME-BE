package yooze.withme.domain.group.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import yooze.withme.domain.group.entity.GroupRound;

public record GroupRoundResponse(
        Long roundId,
        LocalDate roundDate,
        LocalTime roundTime,
        String roundLocationName,
        String roundLocationAddress,
        boolean roundChanged,
        LocalDate roundPrevDate,
        LocalTime roundPrevTime,
        String roundLocationPrevName,
        String roundLocationPrevAddress
) {

    public static GroupRoundResponse from(GroupRound round) {
        return new GroupRoundResponse(
                round.getId(),
                round.getRoundDate(),
                round.getRoundTime(),
                round.getRoundLocationName(),
                round.getRoundLocationAddress(),
                round.isRoundChanged(),
                round.getRoundPrevDate(),
                round.getRoundPrevTime(),
                round.getRoundLocationPrevName(),
                round.getRoundLocationPrevAddress()
        );
    }
}
