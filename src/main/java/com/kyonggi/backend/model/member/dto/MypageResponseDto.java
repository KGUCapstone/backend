package com.kyonggi.backend.model.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MypageResponseDto {
    private String username;
    private String name;
}