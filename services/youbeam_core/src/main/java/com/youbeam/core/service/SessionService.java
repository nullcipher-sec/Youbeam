package com.youbeam.core.service;

import com.youbeam.core.Interface.ISessionService;
import com.youbeam.core.entity.User;
import com.youbeam.core.entity.UserSession;
import com.youbeam.core.enums.SessionStatus;
import com.youbeam.core.enums.UserStatus;
import com.youbeam.core.repository.UserSessionRepository;
import com.youbeam.core.util.SessionTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sesiones con token aleatorio de 256 bits; en la base de datos solo queda {@code SHA-256(token)}.
 */
@Service
public class SessionService implements ISessionService {

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_IP_LENGTH = 45;

    private final UserSessionRepository sessionRepository;
    private final Duration sessionDuration;

    public SessionService(UserSessionRepository sessionRepository,
                          @Value("${youbeam.session.duration}") Duration sessionDuration) {
        this.sessionRepository = sessionRepository;
        this.sessionDuration = sessionDuration;
    }

    @Override
    @Transactional
    public String create(User user, String ipAddress, String userAgent) {
        String token = SessionTokens.generate();

        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash(SessionTokens.hash(token));
        session.setIpAddress(truncate(ipAddress, MAX_IP_LENGTH));
        session.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH));
        session.setExpiresAt(LocalDateTime.now().plus(sessionDuration));
        sessionRepository.save(session);

        return token;
    }

    @Override
    @Transactional
    public Optional<User> findActiveUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSession> found = sessionRepository.findByTokenHash(SessionTokens.hash(token));
        if (found.isEmpty() || found.get().getStatus() != SessionStatus.ACTIVE) {
            return Optional.empty();
        }

        UserSession session = found.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            return Optional.empty();
        }

        User user = session.getUser();
        return user.getStatus() == UserStatus.ACTIVE ? Optional.of(user) : Optional.empty();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
