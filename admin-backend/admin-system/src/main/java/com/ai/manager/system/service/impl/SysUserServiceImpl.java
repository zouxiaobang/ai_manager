package com.ai.manager.system.service.impl;



import com.ai.manager.common.result.PageResult;

import com.ai.manager.common.result.PageUtils;

import com.ai.manager.system.domain.entity.SysUser;

import com.ai.manager.system.mapper.SysUserMapper;

import com.ai.manager.system.service.SysUserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;



@Service

public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {



    @Override

    public PageResult<SysUser> pageUsers(Long page, Long pageSize) {

        long p = PageUtils.normalizePage(page);

        long ps = PageUtils.normalizePageSize(pageSize);

        Page<SysUser> entityPage = page(new Page<>(p, ps), new LambdaQueryWrapper<SysUser>()

                .orderByDesc(SysUser::getId));

        return PageUtils.of(entityPage.getRecords(), entityPage.getTotal(), entityPage.getCurrent(), entityPage.getSize());

    }

}

