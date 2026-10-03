package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.custom.itemflow.ItemFlowForm;
import com.fc.v2.service.ITIchItemFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 名录项目申报单 Controller（state-machine 形状：四格流转）。
 *
 * <p>页面只有一处能往前推的口子：{@code /push}。纸面上的格号与接口递来的格号都只算意图，
 * 归在第几格、后一格收不收，全在服务层那一个推进方法里定；这里不留第二处挪格次的把手。
 * 后退另走 {@code /pull}，一回只退一格。
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

    @Log(title = "名录项目申报单立单", action = "open")
    @ApiOperation(value = "立单", notes = "同一份申报只容一张在跑的单")
    @PostMapping("/open")
    @RequiresPermissions("ichItemFlow:open")
    @ResponseBody
    public AjaxResult open(ItemFlowForm form) {
        return AjaxResult.success("立单成功").put("data", ichItemFlowService.open(form));
    }

    @Log(title = "名录项目申报单本格上报", action = "report")
    @ApiOperation(value = "本格上报", notes = "同格第二遍不另起一行，留头一遍那句")
    @PostMapping("/report")
    @RequiresPermissions("ichItemFlow:report")
    @ResponseBody
    public AjaxResult report(Long id, ItemFlowForm form) {
        return AjaxResult.success("已收材料").put("data", ichItemFlowService.report(id, form));
    }

    @Log(title = "名录项目申报单前进", action = "push")
    @ApiOperation(value = "前进一格", notes = "唯一的前进口子：只到挨着的那一格，门槛不过不收")
    @PostMapping("/push")
    @RequiresPermissions("ichItemFlow:push")
    @ResponseBody
    public AjaxResult push(Long id, Integer intendedStage) {
        // intendedStage 只当意向，与服务层回算相左由推进方法挡回
        return AjaxResult.success("已前进").put("data",
                ichItemFlowService.pushForward(id, intendedStage, null));
    }

    @Log(title = "名录项目申报单后退", action = "pull")
    @ApiOperation(value = "后退一格", notes = "一回只退一格，旧记压到下面，重走从头攒")
    @PostMapping("/pull")
    @RequiresPermissions("ichItemFlow:pull")
    @ResponseBody
    public AjaxResult pull(Long id) {
        return AjaxResult.success("已后退").put("data", ichItemFlowService.pullBack(id, null));
    }

    @Log(title = "名录项目申报单收口", action = "close")
    @ApiOperation(value = "注销/终止收口", notes = "收口当场锁档")
    @PostMapping("/close")
    @RequiresPermissions("ichItemFlow:close")
    @ResponseBody
    public AjaxResult close(Long id, Integer outcome, String reason) {
        int say = outcome == null ? 0 : outcome;
        return AjaxResult.success("已收口").put("data",
                ichItemFlowService.close(id, say, reason, null));
    }

    @Log(title = "名录项目申报单变更", action = "revise")
    @ApiOperation(value = "变更另起一版", notes = "旧版挪去往期，名录只露最新版，重算校验码")
    @PostMapping("/revise")
    @RequiresPermissions("ichItemFlow:revise")
    @ResponseBody
    public AjaxResult revise(Long id, ItemFlowForm form) {
        return AjaxResult.success("已另起一版").put("data",
                ichItemFlowService.revise(id, form, null));
    }

    @ApiOperation(value = "单据权威全貌", notes = "当前格以服务层回算为准")
    @GetMapping("/detail")
    @RequiresPermissions("ichItemFlow:view")
    @ResponseBody
    public AjaxResult detail(Long id) {
        return AjaxResult.success().put("data", ichItemFlowService.view(id, null));
    }

    @ApiOperation(value = "翻往期各版", notes = "现行那版标 current，余者往期")
    @GetMapping("/versions")
    @RequiresPermissions("ichItemFlow:view")
    @ResponseBody
    public AjaxResult versions(Long id) {
        return AjaxResult.success().put("data", ichItemFlowService.versions(id));
    }

    @ApiOperation(value = "在册项目数对算", notes = "逐格点出的数与底册在册数须合得上")
    @GetMapping("/roster")
    @RequiresPermissions("ichItemFlow:roster")
    @ResponseBody
    public AjaxResult roster() {
        return AjaxResult.success().put("data", ichItemFlowService.roster());
    }
}
