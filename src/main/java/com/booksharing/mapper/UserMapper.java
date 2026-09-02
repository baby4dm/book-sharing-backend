package com.booksharing.mapper;

import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.entity.User;
import com.booksharing.dto.res.UserMeResponse;
import com.booksharing.dto.res.UserPublicProfileResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * {@code componentModel = "spring"} - MapStruct згенерує реалізацію як
 * Spring-бін ({@code @Component}), яку можна одразу інжектити в сервіси
 * через конструктор, як і будь-яку іншу залежність.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserPublicProfileResponse toPublicProfile(User user);

    UserMeResponse toMeResponse(User user);

    /**
     * Часткове оновлення: {@code null}-поля в {@code request} залишають
     * відповідні поля {@code user} без змін (не затирають їх null-ом).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateUserProfileRequest request, @MappingTarget User user);
}