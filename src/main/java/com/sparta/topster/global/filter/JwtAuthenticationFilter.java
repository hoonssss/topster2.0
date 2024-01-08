package com.sparta.topster.global.filter;

import static com.sparta.topster.global.exception.ErrorCode.LOGIN_FAILED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.topster.domain.user.dto.login.LoginReq;
import com.sparta.topster.domain.user.dto.login.LoginRes;
import com.sparta.topster.domain.user.entity.UserRoleEnum;
import com.sparta.topster.global.response.RootResponseDto;
import com.sparta.topster.global.security.UserDetailsImpl;
import com.sparta.topster.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/v1/users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
        HttpServletResponse response) throws AuthenticationException {
        try {
            LoginReq requestDto = objectMapper.readValue(request.getInputStream(),
                LoginReq.class);

            return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                    requestDto.getUsername(),
                    requestDto.getPassword(),
                    null
                )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain, Authentication authResult)
        throws IOException {
        String username = ((UserDetailsImpl) authResult.getPrincipal()).getUsername();
        UserRoleEnum role = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getRole();
        String nickname = ((UserDetailsImpl) authResult.getPrincipal()).getUser().getNickname();
        String token = jwtUtil.createToken(username, role);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, token);

        String message = "로그인에 성공했습니다.";
        response.setStatus(200);

        LoginRes loginResponseDto = new LoginRes(username, nickname);
        RootResponseDto<?> responseDto = RootResponseDto.builder()
            .code(String.valueOf(response.getStatus()))
            .message(message)
            .data(loginResponseDto)
            .build();

        String json = objectMapper.writeValueAsString(responseDto);
        PrintWriter writer = response.getWriter();
        writer.println(json);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
        HttpServletResponse response, AuthenticationException failed) throws IOException {
        response.setStatus(401);

        RootResponseDto<?> responseDto = RootResponseDto.builder()
            .code(LOGIN_FAILED.getCode())
            .message(LOGIN_FAILED.getMessage())
            .build();

        String json = objectMapper.writeValueAsString(responseDto);
        PrintWriter writer = response.getWriter();

        writer.println(json);
    }
}
