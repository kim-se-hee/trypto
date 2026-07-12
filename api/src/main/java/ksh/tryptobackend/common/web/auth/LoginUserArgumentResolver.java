package ksh.tryptobackend.common.web.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @LoginUser Long userId} 파라미터에 {@link AuthInterceptor} 가 요청 속성에 담아 둔 userId 를 주입한다.
 * 세션 조회는 인터셉터에서 이미 끝났으므로 여기서는 속성만 꺼낸다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class) && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        return webRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }
}
