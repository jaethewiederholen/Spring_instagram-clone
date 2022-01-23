package cloneproject.Instagram.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cloneproject.Instagram.dto.member.JwtDto;
import cloneproject.Instagram.dto.member.LoginRequest;
import cloneproject.Instagram.dto.member.RegisterRequest;
import cloneproject.Instagram.dto.member.UpdatePasswordRequest;
import cloneproject.Instagram.entity.member.Member;
import cloneproject.Instagram.entity.redis.EmailCode;
import cloneproject.Instagram.exception.AccountDoesNotMatchException;
import cloneproject.Instagram.exception.InvalidJwtException;
import cloneproject.Instagram.exception.MemberDoesNotExistException;
import cloneproject.Instagram.exception.UseridAlreadyExistException;
import cloneproject.Instagram.repository.MemberRepository;
import cloneproject.Instagram.util.JwtUtil;
import cloneproject.Instagram.vo.RefreshToken;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberAuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtil jwtUtil;
    private final EmailCodeService emailCodeService;
    private final MemberRepository memberRepository;
    
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    
    @Transactional(readOnly = true)
    public boolean checkUsername(String username){
        if(memberRepository.existsByUsername(username)){
            return false;
        }
        return true;
    }

    @Transactional
    public boolean register(RegisterRequest registerRequest){
        if(memberRepository.existsByUsername(registerRequest.getUsername())){
            throw new UseridAlreadyExistException();
        }
        String username = registerRequest.getUsername();
        EmailCode emailCode = emailCodeService.findByUsername(username);
        if(emailCode == null || !emailCode.getCode().equals(registerRequest.getCode()) || !emailCode.getEmail().equals(registerRequest.getEmail())){
            return false;
        }
        emailCodeService.deleteEmailCode(emailCode);
        Member member = registerRequest.convert();
        String encryptedPassword = bCryptPasswordEncoder.encode(member.getPassword());
        member.setEncryptedPassword(encryptedPassword);
        memberRepository.save(member);
        return true;
    }

    public void sendEmailConfirmation(String username, String email){
        if(memberRepository.existsByUsername(username)){
            throw new UseridAlreadyExistException();
        }
        emailCodeService.sendEmailConfirmationCode(username, email);
    }

    @Transactional
    public JwtDto login(LoginRequest loginRequest){
        try{
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            JwtDto jwtDto = jwtUtil.generateTokenDto(authentication);
            RefreshToken refreshToken = RefreshToken.builder()
                                                .value(jwtDto.getRefreshToken())
                                                .build();
            Member member = memberRepository.findById(Long.valueOf(authentication.getName()))
                                            .orElseThrow(MemberDoesNotExistException::new);
            member.setRefreshToken(refreshToken);
            memberRepository.save(member);
            
            return jwtDto;
        }catch(BadCredentialsException e){
            throw new AccountDoesNotMatchException();
        }
    }

    @Transactional
    public JwtDto reisuue(String refreshTokenString){
        if(!jwtUtil.validateRefeshJwt(refreshTokenString)){
            throw new InvalidJwtException();
        }
        Authentication authentication;
        try{
            authentication = jwtUtil.getAuthentication(refreshTokenString, false);
        } catch(JwtException e){
            throw new InvalidJwtException();
        }
        Member member = memberRepository.findById(Long.valueOf(authentication.getName()))
                                        .orElseThrow(MemberDoesNotExistException::new);
        RefreshToken refreshToken = member.getRefreshToken();
        if(!refreshToken.getValue().equals(refreshTokenString)){
            throw new InvalidJwtException();
        }
        
        JwtDto jwtDto = jwtUtil.generateTokenDto(authentication);

        refreshToken.updateTokenValue(jwtDto.getRefreshToken());
        memberRepository.save(member);

        return jwtDto;
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest updatePasswordRequest){
        final String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findById(Long.valueOf(memberId))
                                    .orElseThrow(MemberDoesNotExistException::new);
        boolean oldPasswordCorrect = bCryptPasswordEncoder.matches(updatePasswordRequest.getOldPassword()
                                                                , member.getPassword());
        if(!oldPasswordCorrect){
            throw new AccountDoesNotMatchException();
        }
        String encryptedPassword = bCryptPasswordEncoder.encode(updatePasswordRequest.getNewPassword());
        member.setEncryptedPassword(encryptedPassword);              
        memberRepository.save(member);
    }

}
