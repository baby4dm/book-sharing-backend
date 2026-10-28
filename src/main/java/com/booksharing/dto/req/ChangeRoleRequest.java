package com.booksharing.dto.req;

import com.booksharing.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull UserRole role) {
}
