package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.UserAuth;

public record FindIdResponse(String localId) {

    public static FindIdResponse from(UserAuth userAuth) {
        return new FindIdResponse(userAuth.getLocalId());
    }
}
