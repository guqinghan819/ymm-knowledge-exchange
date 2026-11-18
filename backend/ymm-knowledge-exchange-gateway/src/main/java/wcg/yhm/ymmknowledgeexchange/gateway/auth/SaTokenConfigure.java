package wcg.yhm.ymmknowledgeexchange.gateway.auth;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 网关权限入口：先校验登录，再对管理接口检查角色、对发布接口检查权限。
 * @author click33
 */
@Configuration
public class SaTokenConfigure {

    static final String ADMIN_ROLE = "admin";
    static final String NOTE_PUBLISH_PERMISSION = "app:note:publish";
    static final String COMMENT_PUBLISH_PERMISSION = "app:comment:publish";

    static final List<String> ADMIN_PATHS = List.of(
            "/user/role/admin/**",
            "/user/user-role/admin/**",
            "/user/permission/**",
            "/user/user/getUserList",
            "/user/user/delete",
            "/note/note/admin/**",
            "/ai/ai/internal/moderation/**",
            "/channel/channel/admin/**",
            "/topic/topic/admin/**",
            "/relation/fans/admin/**",
            "/search/search/note/rebuild",
            "/search/search/user/rebuild"
    );

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        // 公开访问接口列在登录校验的排除项；管理接口仍需单独满足 admin 角色。
        return new SaReactorFilter()
            // 拦截地址 
            .addInclude("/**")    /* 拦截全部path */
            // 开放地址 
            .addExclude("/favicon.ico")
            // 鉴权方法：每次访问进入 
            .setAuth(obj -> {
                // 登录校验 -- 拦截所有路由，并排除/user/doLogin 用于开放登录 
                SaRouter.match("/**")
                        .notMatch("/auth/loginOrRegister") //排除登录接口
                        .notMatch("/auth/verification/code/send") //排除短信发送接口
                        .notMatch("/channel/channel/get")
                        .notMatch("/note/note/cursor/get")
                        .notMatch("/note/note/detail")
                        .notMatch("/note/note/item/batch")
                        .notMatch("/ai/agent/chat/stream")
                        .notMatch("/search/search/note/list")
                        .notMatch("/search/search/user/list")
                        .check(r -> StpUtil.checkLogin()); //校验是否登录

                ADMIN_PATHS.forEach(path ->
                        SaRouter.match(path, r -> StpUtil.checkRole(ADMIN_ROLE)));
                SaRouter.match("/note/note/publish",
                        r -> StpUtil.checkPermission(NOTE_PUBLISH_PERMISSION));
                SaRouter.match("/comment/comment/publish",
                        r -> StpUtil.checkPermission(COMMENT_PUBLISH_PERMISSION));
            })
            // 异常处理方法：每次setAuth函数出现异常时进入
                //统一异常处理 手动抛出让全局异常处理器处理
            .setError(e -> {
                // 手动抛出异常，抛给全局异常处理器
                if (e instanceof NotLoginException notLoginException) {
                    throw notLoginException;
                } else if (e instanceof NotPermissionException notPermissionException) {
                    throw notPermissionException;
                } else if (e instanceof NotRoleException notRoleException) {
                    throw notRoleException;
                } else if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e);
                }
            })
            ;
    }

}
