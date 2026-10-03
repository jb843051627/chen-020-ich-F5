package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.custom.itemflow.ItemFlowForm;
import com.fc.v2.model.custom.itemflow.ItemFlowView;
import com.fc.v2.model.custom.itemflow.ItemVersionView;
import com.fc.v2.model.custom.itemflow.RosterView;

/**
 * 名录项目申报单 Service接口（state-machine 形状：四格流转）。
 *
 * <p>格次与挪格只有一处口径：{@link #pushForward}。老的查格次方法
 * {@link #selectTIchItemFlowById} 签名原样保留、不收新参；纸上的格号与接口递来的格号
 * 都只算意图。推进的新写法另由 {@link #pushForward} 担，{@link #advance} 只作旧签名保留、
 * 事事仍回这一个推进口子，不另设第二处能挪格次的把手。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIchItemFlowService {

    /** 按主键查询单据原貌（老方法签名原样保留，不收新参；格子里的 stage 只作参考列） */
    TIchItemFlow selectTIchItemFlowById(Long id);

    /** 列表查询（流转台账） */
    List<TIchItemFlow> selectTIchItemFlowList(QueryWrapper<TIchItemFlow> queryWrapper);

    /**
     * 立单：同一份申报（declareNo）底下只容一张在跑的单，
     * 头一张没办完也没喊停（未收口）之前，后一张立不住。
     */
    ItemFlowView open(ItemFlowForm form);

    /**
     * 本格上报：把材料并到单据，身处第几格由推进方法点已过之格得出（表单里没有格号）。
     * 同一格第二次上报不另起一行，卷面仍留头一遍那句。
     */
    ItemFlowView report(Long id, ItemFlowForm form);

    /**
     * 唯一的前进口子：仅推进到挨着的那一格，后一格收不收看本格门槛回不回话。
     *
     * @param id            单据id
     * @param intendedStage 接口/纸面带出的目标格，一律当意向；与回算出的紧挨下一格相左即拒
     * @param at            推进那一刻（公示格按它掐日子走完没走完）
     */
    ItemFlowView pushForward(Long id, Integer intendedStage, Date at);

    /**
     * 唯一的后退口子：一回只退一格，退回后该格先前所记压到下面，重走从这格再从头攒。
     */
    ItemFlowView pullBack(Long id, Date at);

    /**
     * 收口在注销/终止两说：走不到列入而落在这两说即收口，收口当场锁档。
     *
     * @param outcome 2注销 3终止
     */
    ItemFlowView close(Long id, int outcome, String reason, Date at);

    /**
     * 列入之外的变更另起一版：保护单位换了、名称正了字。旧版从现行名录挪开转到往期，
     * 名录上只露最新那一版；新版重算校验码，新旧同码便是错的。
     */
    ItemVersionView revise(Long id, ItemFlowForm form, Date at);

    /** 一张单此刻权威全貌（当前格、本格门槛、逐格留痕） */
    ItemFlowView view(Long id, Date at);

    /** 翻这一张单的各版（现行那版标 current，余者往期） */
    List<ItemVersionView> versions(Long id);

    /** 在册项目数当场对算：逐格点出的现行版数与底册在册数须同一回装载里合得上 */
    RosterView roster();

    /**
     * 旧签名保留：推进一档。内部一律回 {@link #pushForward} 这一个口子，
     * 不另设规矩；目标格按回算、时刻取当下。
     */
    TIchItemFlow advance(Long id, String remark);

    /** 旧签名保留：回退一档。内部一律回 {@link #pullBack} 这一个口子。 */
    TIchItemFlow rollback(Long id, String remark);

    /** 修改卷面那句：收口锁档后改不动，被拒返回 false（以推进口子的回话为准） */
    boolean updateContent(Long id, String remark);

    /** 删除单据：收口锁档的整张删不掉，被拒返回 false */
    boolean remove(Long id);
}
