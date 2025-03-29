package com.kyonggi.backend.model.member.dto;

import com.kyonggi.backend.model.member.entity.Member;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class JoinResponseDto {
    private long id;
    private String username;
    private String email;
    private String name;

    public JoinResponseDto (Member member) {
        id = member.getId();
        username = member.getUsername();
        email = member.getEmail();
        name = member.getName();
    }
}