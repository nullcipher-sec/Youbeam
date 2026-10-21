package com.youbeam.core.commons;

import com.youbeam.core.exception.ControllerException;
import com.youbeam.core.exception.ErrorResponses;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Inyecta {@link AuthenticatedUser} en los endpoints que lo declaran y responde 401 si no hay
 * sesión válida, para que ningún endpoint protegido pueda olvidarse de comprobarlo.
 */
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws ControllerException {
        Object user = webRequest.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            throw new ControllerException(ErrorResponses.UNAUTHORIZED);
        }
        return user;
    }
}
