package com.ai.manager.system.service;



import com.ai.manager.common.result.PageResult;

import com.ai.manager.system.domain.entity.SysUser;

import com.baomidou.mybatisplus.extension.service.IService;



/**
 * 系统用户服务接口
 *
 * <p>提供系统用户的分页查询等用户管理功能。</p>
 */
public interface SysUserService extends IService<SysUser> {



    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    PageResult<SysUser> pageUsers(Long page, Long pageSize);

}
