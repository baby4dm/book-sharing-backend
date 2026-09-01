package com.booksharing.model.user;

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