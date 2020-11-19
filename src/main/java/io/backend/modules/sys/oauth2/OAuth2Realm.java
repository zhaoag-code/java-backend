
package io.backend.modules.sys.oauth2;

import io.backend.common.utils.RedisUtils;
import io.backend.modules.sys.entity.SysUserEntity;
import io.backend.modules.sys.entity.SysUserTokenEntity;
import io.backend.modules.sys.service.ShiroService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 认证
 *
 */
@Component
public class OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private ShiroService shiroService;

    @Autowired
    private RedisUtils redisUtils;


    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    /**
     * 授权(验证权限时调用)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUserEntity user = (SysUserEntity)principals.getPrimaryPrincipal();
        Long userId = user.getUserId();

        //用户权限列表
        Set<String> permsSet;
        permsSet = redisUtils.get("permsSet_" + userId, Set.class);
        if(permsSet == null){
            permsSet = shiroService.getUserPermissions(userId);
            redisUtils.set("permsSet_" + userId, permsSet);
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 认证(登录时调用)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getPrincipal();

        //根据accessToken，查询用户信息
        SysUserTokenEntity tokenEntity;
        tokenEntity = redisUtils.get(accessToken, SysUserTokenEntity.class);
        if(tokenEntity == null){
            tokenEntity = shiroService.queryByToken(accessToken);
            if(tokenEntity != null){
                redisUtils.set(tokenEntity.getToken(),tokenEntity);
            }
        }

        //token失效
        if(tokenEntity == null || tokenEntity.getExpireTime().getTime() < System.currentTimeMillis()){
            throw new IncorrectCredentialsException("token失效，请重新登录");
        }

        //查询用户信息
        SysUserEntity user;
        user = redisUtils.get("token_user_" + tokenEntity.getUserId(),SysUserEntity.class);
        if(user == null){
            user = shiroService.queryUser(tokenEntity.getUserId());
            redisUtils.set("token_user_" + user.getUserId(), user);
        }
        //账号锁定
        if(user.getStatus() == 0){
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName());
        return info;
    }
}
