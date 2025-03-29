package com.kyonggi.backend.model.member.service;

import com.kyonggi.backend.model.member.dto.JoinRequestDto;
import com.kyonggi.backend.model.member.dto.JoinResponseDto;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MemberJoinService {

    private final MemberRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public MemberJoinService(MemberRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {

        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public JoinResponseDto joinProcess(JoinRequestDto joinRequestDto) {

        String username = joinRequestDto.getUsername();
        String password = joinRequestDto.getPassword();
        String passwordCheck = joinRequestDto.getPasswordCheck();
        String name = joinRequestDto.getName();
        String email = joinRequestDto.getEmail();

        // 1. username 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 2. username 유효성 검사 (영문+숫자, 4~20자)
        if (!username.matches("^[a-zA-Z0-9]{4,20}$")) {
            throw new IllegalArgumentException("아이디는 4~20자의 영문 + 숫자.");
        }

        // 3. password 유효성 검사 (8자 이상, 영문+숫자+특수문자)
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.");
        }

        // 4. password 일치 확인
        if (!password.equals(passwordCheck)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 5. 이메일 형식 검사
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }

        // 6. 이름 유효성 검사 (특수문자 제한)
        if (!name.matches("^[a-zA-Z가-힣]{1,20}$")) {
            throw new IllegalArgumentException("이름은 1~20자의 한글 또는 영문이어야 하며 특수문자는 사용할 수 없습니다.");
        }

        // 7. 회원 생성 및 저장
        Member data = new Member();
        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setName(name);
        data.setEmail(email);
        data.setRole("ROLE_USER");

        Member saveMember = userRepository.save(data);

        return new JoinResponseDto(saveMember);
    }
}

