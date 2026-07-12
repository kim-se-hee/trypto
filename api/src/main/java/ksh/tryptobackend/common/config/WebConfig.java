package ksh.tryptobackend.common.config;

import java.util.List;
import ksh.tryptobackend.common.web.auth.AuthInterceptor;
import ksh.tryptobackend.common.web.auth.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PATH = "/api/**";

    /** 로그인 없이 접근 가능한 공개 엔드포인트. 여기에 없는 /api 요청은 전부 인증을 강제한다(기본 차단). */
    private static final String[] PUBLIC_PATTERNS = {
        "/api/auth/**", "/api/candles", "/api/exchanges/*/coins", "/api/rankings", "/api/rankings/stats",
    };

    private final AuthInterceptor authInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns(API_PATH).excludePathPatterns(PUBLIC_PATTERNS);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
