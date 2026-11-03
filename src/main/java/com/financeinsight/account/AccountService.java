package com.financeinsight.account;

import com.financeinsight.common.exception.InvalidRequestException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements UserDetailsService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account loadUserByUsername(String username) throws UsernameNotFoundException {
        return accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Transactional
    public void register(String username, String password) {
        String normalized = username == null ? "" : username.trim();
        if (!normalized.matches("[A-Za-z0-9._-]{3,80}")) {
            throw new InvalidRequestException("Username must be 3–80 characters and use letters, numbers, dot, underscore, or hyphen");
        }
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new InvalidRequestException("Password must be between 12 and 128 characters");
        }
        accounts.lockRegistration();
        if (accounts.existsByUsername(normalized)) {
            throw new InvalidRequestException("That username is already registered");
        }
        boolean firstAccount = accounts.count() == 0;
        Account saved = accounts.saveAndFlush(new Account(normalized, passwordEncoder.encode(password)));
        if (firstAccount) {
            accounts.claimLegacyStatements(saved.getId());
        }
    }
}
