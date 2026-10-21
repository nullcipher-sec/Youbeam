package com.youbeam.core.service;

import com.youbeam.core.Interface.IAuthService;
import com.youbeam.core.Interface.ISessionService;
import com.youbeam.core.entity.User;
import com.youbeam.core.enums.UserStatus;
import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import com.youbeam.core.model.request.LoginRequest;
import com.youbeam.core.model.request.RegisterRequest;
import com.youbeam.core.model.response.LoginResult;
import com.youbeam.core.model.response.UserResponse;
import com.youbeam.core.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Registro e inicio de sesión con contraseñas hasheadas en Argon2id; cada login abre una sesión.
 */
@Service
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ISessionService sessionService;

    /**
     * Hash de una contraseña aleatoria: se compara contra él cuando el usuario no existe
     * para que la respuesta tarde lo mismo y no revele qué cuentas están registradas.
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ISessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) throws ControllerException {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());

        if (userRepository.existsByUsernameOrEmail(username, email)) {
            throw new ControllerException(ErrorResponses.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Dos registros simultáneos con el mismo usuario/email: la restricción UNIQUE decide
            throw new ControllerException(ErrorResponses.USER_ALREADY_EXISTS, ex);
        }
    }

    @Override
    @Transactional
    public LoginResult login(LoginRequest request, String ipAddress, String userAgent)
            throws ControllerException {
        String login = normalize(request.getLogin());
        Optional<User> found = userRepository.findByUsernameOrEmail(login, login);

        if (found.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), dummyHash);
            throw new ControllerException(ErrorResponses.INVALID_CREDENTIALS);
        }

        User user = found.get();
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        // Mismo error para contraseña incorrecta y cuenta no activa: no se revela el estado
        if (!passwordMatches || user.getStatus() != UserStatus.ACTIVE) {
            throw new ControllerException(ErrorResponses.INVALID_CREDENTIALS);
        }

        // Si cambian los parámetros de Argon2, el hash se actualiza en el siguiente login
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setLastLoginAt(LocalDateTime.now());

        String token = sessionService.create(user, ipAddress, userAgent);
        return new LoginResult(UserResponse.from(user), token);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
