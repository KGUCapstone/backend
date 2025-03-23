package com.kyonggi.backend.model.member.service;

import com.kyonggi.backend.model.member.dto.CustomUserDetails;
import com.kyonggi.backend.model.member.entity.Member;
import com.kyonggi.backend.model.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository userRepository;

    public CustomUserDetailsService(MemberRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Member userData = userRepository.findByUsername(username).get();

        if (userData != null) {

            return new CustomUserDetails(userData);
        }


        return null;
    }
}
