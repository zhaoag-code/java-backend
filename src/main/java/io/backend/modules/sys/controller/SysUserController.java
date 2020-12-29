
package io.backend.modules.sys.controller;

import io.backend.common.annotation.SysLog;
import io.backend.common.utils.Constant;
import io.backend.common.utils.PageUtils;
import io.backend.common.utils.R;
import io.backend.common.utils.RedisUtils;
import io.backend.common.validator.Assert;
import io.backend.common.validator.ValidatorUtils;
import io.backend.common.validator.group.AddGroup;
import io.backend.common.validator.group.UpdateGroup;
import io.backend.modules.sys.entity.SysUserEntity;
import io.backend.modules.sys.entity.SysUserTokenEntity;
import io.backend.modules.sys.form.PasswordForm;
import io.backend.modules.sys.service.SysUserRoleService;
import io.backend.modules.sys.service.SysUserService;
import io.backend.modules.sys.service.SysUserTokenService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统用户
 *
 */
@RestController
@RequestMapping("/sys/user")
public class SysUserController extends AbstractController {
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private RedisUtils redisUtils;
	@Autowired
	private SysUserTokenService sysUserTokenService;


	/**
	 * 所有用户列表
	 */
	@GetMapping("/list")
	@RequiresPermissions("sys:user:list")
	public R list(@RequestParam Map<String, Object> params){
		//只有超级管理员，才能查看所有管理员列表
		if(getUserId() != Constant.SUPER_ADMIN){
			params.put("createUserId", getUserId());
		}
		PageUtils page = sysUserService.queryPage(params);

		return R.ok().put("page", page);
	}

	/**
	 * 获取登录的用户信息
	 */
	@GetMapping("/info")
	public R info(){
		return R.ok().put("user", getUser());
	}

	/**
	 * 修改登录用户密码
	 */
	@SysLog("修改密码")
	@PostMapping("/password")
	public R password(@RequestBody PasswordForm form){
		Assert.isBlank(form.getNewPassword(), "新密码不为能空");

		//sha256加密
		String password = new Sha256Hash(form.getPassword(), getUser().getSalt()).toHex();
		//sha256加密
		String newPassword = new Sha256Hash(form.getNewPassword(), getUser().getSalt()).toHex();

		//更新密码
		boolean flag = sysUserService.updatePassword(getUserId(), password, newPassword);
		if(!flag){
			return R.error("原密码不正确");
		}

		return R.ok();
	}

	/**
	 * 用户信息
	 */
	@GetMapping("/info/{userId}")
	@RequiresPermissions("sys:user:info")
	public R info(@PathVariable("userId") Long userId){
		SysUserEntity user = sysUserService.getById(userId);

		//获取用户所属的角色列表
		List<Long> roleIdList = sysUserRoleService.queryRoleIdList(userId);
		user.setRoleIdList(roleIdList);

		return R.ok().put("user", user);
	}

	/**
	 * 保存用户
	 */
	@SysLog("保存用户")
	@PostMapping("/save")
	@RequiresPermissions("sys:user:save")
	public R save(@RequestBody SysUserEntity user){
		ValidatorUtils.validateEntity(user, AddGroup.class);

		user.setCreateUserId(getUserId());
		sysUserService.saveUser(user);

		return R.ok();
	}

	/**
	 * 修改用户
	 */
	@SysLog("修改用户")
	@PostMapping("/update")
	@RequiresPermissions("sys:user:update")
	public R update(@RequestBody SysUserEntity user){
		ValidatorUtils.validateEntity(user, UpdateGroup.class);

		user.setCreateUserId(getUserId());
		sysUserService.update(user);
		//修改用户信息需要清除用户的缓存信息
		SysUserTokenEntity tokenEntity = sysUserTokenService.getById(user.getUserId());
		if(tokenEntity != null){
			redisUtils.delete(tokenEntity.getToken());
		}
		redisUtils.delete("token_user_"+user.getUserId());
		redisUtils.delete("permsSet_"+user.getUserId());

		return R.ok();
	}

	/**
	 * 删除用户
	 */
	@SysLog("删除用户")
	@PostMapping("/delete")
	@RequiresPermissions("sys:user:delete")
	public R delete(@RequestBody Long[] userIds){
		if(ArrayUtils.contains(userIds, 1L)){
			return R.error("系统管理员不能删除");
		}

		if(ArrayUtils.contains(userIds, getUserId())){
			return R.error("当前用户不能删除");
		}

		sysUserService.deleteBatch(userIds);
		//删除用户信息需要清除用户的缓存信息
		List<SysUserTokenEntity> tokenEntities = sysUserTokenService.listByIds(Arrays.asList(userIds));
		Set<String> tokens = new HashSet<>();
		for (SysUserTokenEntity tokenEntity: tokenEntities) {
			tokens.add(tokenEntity.getToken());
		}
		redisUtils.deleteByKeys(tokens);
		Set<String> tokenUsers = new HashSet<>();
		Set<String> permSets = new HashSet<>();
		for (int i = 0; i < userIds.length; i++) {
			tokenUsers.add("token_user_"+userIds[i]);
			tokenUsers.add("permsSet_"+userIds[i]);
		}
		redisUtils.deleteByKeys(tokenUsers);
		redisUtils.deleteByKeys(permSets);

		return R.ok();
	}
}
