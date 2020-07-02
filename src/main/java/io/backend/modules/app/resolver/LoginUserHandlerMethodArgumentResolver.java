
package io.backend.modules.app.resolver;

import io.backend.common.utils.RedisUtils;
import io.backend.modules.app.annotation.LoginUser;
import io.backend.modules.app.entity.UserEntity;
import io.backend.modules.app.interceptor.AuthorizationInterceptor;
import io.backend.modules.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 有@LoginUser注解的方法参数，注入当前登录用户
 *
 */
@Component
public class LoginUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(UserEntity.class) && parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
        //获取用户ID
        Object object = request.getAttribute(AuthorizationInterceptor.USER_KEY, RequestAttributes.SCOPE_REQUEST);
        if(object == null){
            return null;
        }

        //获取用户信息
        UserEntity user = userService.getById((Long)object);
        //获取用户信息
        Long userId = (Long)object;
        UserEntity userEntity;
        userEntity = redisUtils.get(AuthorizationInterceptor.USER_KEY + "_" + userId,UserEntity.class);
        if(null == userEntity){
            userEntity = userService.getById(userId);
            redisUtils.set(AuthorizationInterceptor.USER_KEY + "_" + userId,userEntity);
        }

        return user;
    }
}
