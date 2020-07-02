
package io.backend.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.backend.common.utils.R;
import io.backend.modules.sys.entity.SysUserTokenEntity;

/**
 * 用户Token
 *
 */
public interface SysUserTokenService extends IService<SysUserTokenEntity> {

	/**
	 * 生成token
	 * @param userId  用户ID
	 */
	R createToken(long userId);

	/**
	 * 退出，修改token值
	 * @param userId  用户ID
	 */
	void logout(long userId);

}
