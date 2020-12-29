
package io.backend.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.backend.common.exception.BackendException;
import io.backend.common.utils.Constant;
import io.backend.common.utils.PageUtils;
import io.backend.common.utils.Query;
import io.backend.common.utils.RedisUtils;
import io.backend.modules.sys.dao.SysRoleDao;
import io.backend.modules.sys.entity.SysRoleEntity;
import io.backend.modules.sys.service.SysRoleMenuService;
import io.backend.modules.sys.service.SysRoleService;
import io.backend.modules.sys.service.SysUserRoleService;
import io.backend.modules.sys.service.SysUserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色
 *
 */
@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {
	@Autowired
	private SysRoleMenuService sysRoleMenuService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private RedisUtils redisUtils;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String roleName = (String)params.get("roleName");
		Long createUserId = (Long)params.get("createUserId");

		IPage<SysRoleEntity> page = this.page(
				new Query<SysRoleEntity>().getPage(params),
				new QueryWrapper<SysRoleEntity>()
						.like(StringUtils.isNotBlank(roleName),"role_name", roleName)
						.eq(createUserId != null,"create_user_id", createUserId)
		);

		return new PageUtils(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveRole(SysRoleEntity role) {
		role.setCreateTime(new Date());
		this.save(role);

		//检查权限是否越权
		checkPrems(role);

		//保存角色与菜单关系
		sysRoleMenuService.saveOrUpdate(role.getRoleId(), role.getMenuIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysRoleEntity role) {
		this.updateById(role);

		//检查权限是否越权
		checkPrems(role);

		//更新角色需要清除用户缓存的权限列表
		Set<String> keys  = redisUtils.keys("permsSet_*");
		redisUtils.deleteByKeys(keys);

		//更新角色与菜单关系
		sysRoleMenuService.saveOrUpdate(role.getRoleId(), role.getMenuIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBatch(Long[] roleIds) {
		//删除角色
		this.removeByIds(Arrays.asList(roleIds));

		//删除角色与菜单关联
		sysRoleMenuService.deleteBatch(roleIds);

		//删除角色与用户关联
		sysUserRoleService.deleteBatch(roleIds);

		//删除角色需要清除用户缓存的权限列表
		Set<String> keys  = redisUtils.keys("permsSet_*");
		redisUtils.deleteByKeys(keys);
	}


	@Override
	public List<Long> queryRoleIdList(Long createUserId) {
		return baseMapper.queryRoleIdList(createUserId);
	}

	/**
	 * 检查权限是否越权
	 */
	private void checkPrems(SysRoleEntity role){
		//如果不是超级管理员，则需要判断角色的权限是否超过自己的权限
		if(role.getCreateUserId() == Constant.SUPER_ADMIN){
			return ;
		}

		//查询用户所拥有的菜单列表
		List<Long> menuIdList = sysUserService.queryAllMenuId(role.getCreateUserId());

		//判断是否越权
		if(!menuIdList.containsAll(role.getMenuIdList())){
			throw new BackendException("新增角色的权限，已超出你的权限范围");
		}
	}
}
