package com.kyonggi.backend.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JoinRequestDto {

    private String username;
    private String password;
    private String passwordCheck;
    private String email;
    private String name;

}
