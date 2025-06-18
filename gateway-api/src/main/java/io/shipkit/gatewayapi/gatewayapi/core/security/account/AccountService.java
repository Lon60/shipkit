package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import io.shipkit.gatewayapi.gatewayapi.core.exceptions.AlreadyExistsException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.UnauthorizedException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.AuthPayloadDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.jwt.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthPayloadDTO register(String email, String password) {
        if (accountRepository.count() > 0) {
            throw new BadRequestException("Registration is disabled. Admin account already exists.");
        }
        
        if (accountRepository.findByEmail(email).isPresent()) {
            throw new AlreadyExistsException("Email already taken: " + email);
        }
        Account newAccount = Account.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
        Account account =  accountRepository.save(newAccount);
        String token = jwtService.generateToken(account);
        return new AuthPayloadDTO(token);
    }

    @Transactional(readOnly = true)
    public AuthPayloadDTO login(String username, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            Account account = (Account) auth.getPrincipal();
            String token = jwtService.generateToken(account);
            return new AuthPayloadDTO(token);
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Bad Credentials");
        }
    }

    @Transactional
    public AuthPayloadDTO changePassword(String email, String oldPassword, String newPassword) {
        try {
            // Authenticate with old password first
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, oldPassword)
            );
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Old password is incorrect");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Account not found"));

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        String token = jwtService.generateToken(account);
        return new AuthPayloadDTO(token);
    }
}