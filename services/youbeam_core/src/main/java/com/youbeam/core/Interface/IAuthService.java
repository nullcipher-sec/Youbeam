package com.youbeam.core.Interface;

import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.model.request.LoginRequest;
import com.youbeam.core.model.request.RegisterRequest;
import com.youbeam.core.model.response.UserResponse;

/**
 * Registro e inicio de sesión de usuarios.
 */
public interface IAuthService {

    UserResponse register(RegisterRequest request) throws ControllerException;

    UserResponse login(LoginRequest request) throws ControllerException;
}
