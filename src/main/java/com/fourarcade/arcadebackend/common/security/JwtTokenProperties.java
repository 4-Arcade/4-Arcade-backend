package com.fourarcade.arcadebackend.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/*
* JWT 설정값 매핑 클래스
* application.properties 에 적어둔 'jwt.xxx' 설정값들을 이 객체로 가져옴
* */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenProperties {

    // jwt.secret-base64 매핑
    private String secretBase64;

    // jwt.access-ttl-minutes 매핑
    private long accessTtlMinutes;

    // jwt.issuer 매핑
    private String issuer;

    // jwt.audience 매핑
    private String audience;

    // jwt.clock-skew-seconds 매핑 (properties에 없으면 기본값 30초 적용)
    private long clockSkewSeconds = 30;

}
