package com.example.bi_backend.model.enums;

import lombok.Getter;

/**
 * 用户角色
 */
@Getter
public enum UserRoleEnums {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    UserRoleEnums(String text, String value) {
        this.text = text;
        this.value = value;
    }

}
