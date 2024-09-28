package org.paul.core.filter.gray;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import static org.paul.common.constants.FilterConst.*;

@Slf4j
@FilterAspect(id = GRAY_FILTER_ID,
        name = GRAY_FILTER_NAME,
        order = GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //测试灰度功能
        String gray = ctx.getRequest().getHeaders().get("gray");


        if("true".equals(gray)){
            ctx.setGray(true);
        }

        String clientIp = ctx.getRequest().getClientIp();
        int res = clientIp.hashCode() & 1024 - 1;//等价于对1024取模

        if(res == 1){
            ctx.setGray(true);
        }
    }
}
