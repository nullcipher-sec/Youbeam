package com.youbeam.core.commons;

import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

/**
 * Inyecta el usuario autenticado en los endpoints que lo declaran:
 * <ul>
 *   <li>{@code AuthenticatedUser}: sesión obligatoria; si no la hay se responde 401, así ningún
 *       endpoint protegido puede olvidarse de comprobarlo.</li>
 *   <li>{@code Optional<AuthenticatedUser>}: sesión opcional, para endpoints públicos que
 *       muestran más información al usuario identificado (p. ej. ver un video).</li>
 * </ul>
 */
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.nestedIfOptional().getNestedParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws ControllerException {
        Object user = webRequest.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (parameter.getParameterType() == Optional.class) {
            return Optional.ofNullable(user);
        }
        if (user == null) {
            throw new ControllerException(ErrorResponses.UNAUTHORIZED);
        }
        return user;
    }
}
