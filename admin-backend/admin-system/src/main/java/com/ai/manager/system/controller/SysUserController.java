package com.ai.manager.system.controller;



import com.ai.manager.common.result.ApiResult;

import com.ai.manager.common.result.PageResult;

import com.ai.manager.system.domain.entity.SysUser;

import com.ai.manager.system.service.SysUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import java.util.Map;



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



    @GetMapping("/{id}")

    public ApiResult<SysUser> getById(@PathVariable Long id) {

        return ApiResult.ok(sysUserService.getById(id));

    }



    @PutMapping("/{id}")

    public ApiResult<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {

        SysUser user = sysUserService.getById(id);

        if (user == null) {

            return ApiResult.fail(404, "用户不存在");

        }

        String nickname = body.get("nickname");

        if (nickname != null) {

            user.setNickname(nickname);

        }

        sysUserService.updateById(user);

        return ApiResult.ok();

    }

}
