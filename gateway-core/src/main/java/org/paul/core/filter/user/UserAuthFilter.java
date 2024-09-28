package org.paul.core.filter.user;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.cookie.Cookie;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.ResponseException;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import java.io.FileFilter;

import static org.paul.common.constants.FilterConst.*;

/**
 * 用户鉴权过滤器
 */
@Slf4j
@FilterAspect(id = USER_AUTH_FILTER_ID,
        name = USER_AUTH_FILTER_NAME,
        order = USER_AUTH_FILTER_ORDER)
public class UserAuthFilter implements Filter {
    private static final String SECRET_KEY = "";
    private static final String COOKIE_KEY = "";

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //尝试从rule中获取用户过滤器的配置，如果为空，则表明不需要用户鉴权，直接返回
        if (ctx.getRule().getFilterConfig(USER_AUTH_FILTER_ID) == null) {
            return;
        }

        String token = ctx.getRequest().getCookie(COOKIE_KEY).toString();
        if (StringUtils.isBlank(token)) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }

        try {
            //解析用户id，并把用户id传给下游
            long userId = parseUserId(token);
            ctx.getRequest().setUserId(userId);
        } catch (Exception e) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }


    }

    private long parseUserId(String token) {
        Jwt jwt = Jwts.parser().setSigningKey(SECRET_KEY).parse(token);
        return Long.parseLong(((DefaultClaims)jwt.getBody()).getSubject());
    }
}
