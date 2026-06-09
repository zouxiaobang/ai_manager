package com.ai.manager.system.service;



import com.ai.manager.common.result.PageResult;

import com.ai.manager.system.domain.entity.SysUser;

import com.baomidou.mybatisplus.extension.service.IService;



public interface SysUserService extends IService<SysUser> {



    PageResult<SysUser> pageUsers(Long page, Long pageSize);

}

