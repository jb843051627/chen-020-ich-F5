package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.custom.itemflow.ItemFlowCommand;
import com.fc.v2.service.ITIchItemFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 名录项目申报单 Controller。
 *
 * <p>屏上只有一处口径：前进/后退并成一个 {@code /move} 把手，归在第几格、后一格收不收
 * 全由服务层那一个推进方法回话；纸面上的格号与接口递来的格号都只算意向。页面不留第二处
 * 能挪格次的把手（不再有单独的 advance/rollback）。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "名录项目申报单")
@Controller
@RequestMapping("/ichItemFlow")
public class IchItemFlowController extends BaseController {

    private final String prefix = "admin/ichItemFlow";

    @Autowired
    private ITIchItemFlowService ichItemFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("ichItemFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "名录项目申报单流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("ichItemFlow:list")
    @ResponseBody
    public ResultTable list(TIchItemFlow record) {
        QueryWrapper<TIchItemFlow> queryWrapper = new QueryWrapper<TIchItemFlow>();
        startPage();
        com.github.pagehelper.PageInfo<TIchItemFlow> page =
                new com.github.pagehelper.PageInfo<TIchItemFlow>(
                        ichItemFlowService.selectTIchItemFlowList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "名录项目申报单开单", action = "declare")
    @ApiOperation(value = "开一张申报单", notes = "同一申报只容一张在跑")
    @PostMapping("/declare")
    @RequiresPermissions("ichItemFlow:declare")
    @ResponseBody
    public AjaxResult declare(ItemFlowCommand command) {
        return AjaxResult.successData(200,ichItemFlowService.declare(command));
    }

    @Log(title = "名录项目申报单推进", action = "move")
    @ApiOperation(value = "唯一推进口：前进/后退一格", notes = "advance=true 前进，false 后退；intendedStage 只当意向")
    @PostMapping("/move")
    @RequiresPermissions("ichItemFlow:move")
    @ResponseBody
    public AjaxResult move(Long id, boolean advance, Integer intendedStage, String note) {
        // 挪格次只此一处把手；当前格、收不收由服务层回算，不采信页面格号
        return AjaxResult.successData(200,
                ichItemFlowService.move(id, advance, intendedStage, note, new Date()));
    }

    @Log(title = "名录项目申报单收口", action = "close")
    @ApiOperation(value = "注销/终止收口", notes = "closeType 2注销 3终止；当场锁档")
    @PostMapping("/close")
    @RequiresPermissions("ichItemFlow:close")
    @ResponseBody
    public AjaxResult close(Long id, int closeType, String siteNo, String note) {
        return AjaxResult.successData(200,
                ichItemFlowService.close(id, closeType, siteNo, note, new Date()));
    }

    @Log(title = "名录项目申报单变更", action = "revise")
    @ApiOperation(value = "列入之外的变更另起一版", notes = "旧版转往期，新版重算校验码")
    @PostMapping("/revise")
    @RequiresPermissions("ichItemFlow:revise")
    @ResponseBody
    public AjaxResult revise(Long id, ItemFlowCommand command) {
        return AjaxResult.successData(200,ichItemFlowService.revise(id, command));
    }

    @ApiOperation(value = "一张单此刻全貌", notes = "当前格以服务层回算为准")
    @GetMapping("/detail")
    @RequiresPermissions("ichItemFlow:list")
    @ResponseBody
    public AjaxResult detail(Long id) {
        return AjaxResult.successData(200,ichItemFlowService.view(id));
    }

    @ApiOperation(value = "在册数一本账", notes = "收口单口径与底册在册条目同一回算")
    @GetMapping("/ledger")
    @RequiresPermissions("ichItemFlow:list")
    @ResponseBody
    public AjaxResult ledger() {
        return AjaxResult.successData(200,ichItemFlowService.ledger());
    }
}
