package com.ai.manager.system.controller;



import com.ai.manager.common.result.ApiResult;

import com.ai.manager.common.result.PageResult;

import com.ai.manager.system.domain.entity.SysUser;

import com.ai.manager.system.service.SysUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;



/**
 * 系统用户控制器
 *
 * <p>所属模块：系统模块-用户管理</p>
 * <p>API路径前缀：/api/system/users</p>
 * <p>功能描述：提供系统用户的分页查询等用户管理功能</p>
 *
 * @author system
 */
@RestController

@RequestMapping("/api/system/users")

@RequiredArgsConstructor

public class SysUserController {



    private final SysUserService sysUserService;



    /**
     * 分页查询系统用户列表
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/system/users</p>
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @return 系统用户分页结果
     */
    @GetMapping

    public ApiResult<PageResult<SysUser>> list(@RequestParam(required = false) Long page,

                                               @RequestParam(required = false) Long pageSize) {

        return ApiResult.ok(sysUserService.pageUsers(page, pageSize));

    }

}
