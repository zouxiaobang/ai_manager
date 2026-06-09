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



@RestController

@RequestMapping("/api/system/users")

@RequiredArgsConstructor

public class SysUserController {



    private final SysUserService sysUserService;



    @GetMapping

    public ApiResult<PageResult<SysUser>> list(@RequestParam(required = false) Long page,

                                               @RequestParam(required = false) Long pageSize) {

        return ApiResult.ok(sysUserService.pageUsers(page, pageSize));

    }

}

