package ksh.tryptobackend.common.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 유저의 식별자를 컨트롤러 파라미터로 주입한다. {@link AuthInterceptor} 가 세션에서 복원해 둔 userId 를
 * {@link LoginUserArgumentResolver} 가 꺼내 바인딩한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {}
