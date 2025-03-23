package com.kyonggi.backend.model.member.controller;

import com.kyonggi.backend.model.member.dto.JoinRequestDto;
import com.kyonggi.backend.model.member.dto.JoinResponseDto;
import com.kyonggi.backend.model.member.service.MemberJoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberJoinController {

    private final MemberJoinService joinService;

    @PostMapping("/api/join")
    public JoinResponseDto joinProcess(@RequestBody JoinRequestDto joinRequestDto) {

        return joinService.joinProcess(joinRequestDto);

    }

}
