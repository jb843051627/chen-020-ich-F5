package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.custom.itemflow.ItemFlowCommand;
import com.fc.v2.model.custom.itemflow.ItemFlowLedger;
import com.fc.v2.model.custom.itemflow.ItemFlowView;

/**
 * 名录项目申报单 Service接口。
 *
 * <p>两路来路并成一路：格次只有 {@link #move} 这一个推进口能挪——前进只到紧挨的下一格、
 * 后退只退一格。归在第几格、后一格收不收，全在那一个方法里定；纸上的格号与接口递来的格号
 * （intendedStage）都只算意向。老的取格次方法 {@link #selectTIchItemFlowById(Long)} 签名原样
 * 保留、不塞新参，只收什么给什么；推进换写法另开 {@link #move}，不混进老方法。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIchItemFlowService {

    /** 老方法原样保留：按主键回查单据原貌（不回算、不挪格次、不塞新参）。 */
    TIchItemFlow selectTIchItemFlowById(Long id);

    /** 列表查询（流转台账） */
    List<TIchItemFlow> selectTIchItemFlowList(QueryWrapper<TIchItemFlow> queryWrapper);

    /**
     * 开一张申报单（落在形式核验格，四样随单带上）。同一份申报只容一张在跑的单，
     * 头一张没办完、也没喊停之前，后一张立不住。
     */
    ItemFlowView declare(ItemFlowCommand command);

    /**
     * 唯一的推进口：前进 / 后退都走这里，一次只挪紧挨的一格。
     *
     * @param id            申报单id
     * @param advance       true前进一格 false后退一格
     * @param intendedStage 接口/屏上带出的格号，一律当意向；与入口回算相左按入口的来
     * @param note          这一遍上报卷面那句（同格第二遍不另起一行，仍留头一遍）
     * @param now           判门槛那一刻（公示期满按它回看）；传 null 取服务端当下
     * @return 挪完后入口回算出的权威全貌
     */
    ItemFlowView move(Long id, boolean advance, Integer intendedStage, String note, Date now);

    /**
     * 收口在注销/终止两说上：走不到列入即收口，当场锁档。
     *
     * @param closeType 2注销（在册项目注销，在册数减一，须给在册 siteNo）3终止（不入册不减册）
     * @param siteNo    注销所对的在册项目代号；终止传 null
     */
    ItemFlowView close(Long id, int closeType, String siteNo, String note, Date now);

    /**
     * 列入之外的变更另起一版：保护单位换了、名称正了字。旧版转往期（现行名录不露），
     * 新版按版次加一、重算校验码，从头一格再走一遍。
     */
    ItemFlowView revise(Long id, ItemFlowCommand command);

    /** 一张单此刻全貌（当前格以回算为准） */
    ItemFlowView view(Long id);

    /** 在册数一本账：收口单口径与底册在册条目同一回算，对得齐才算 */
    ItemFlowLedger ledger();

    /** 改卷面上的字：已收口锁档改不动，被拒返回 false（不挪格次） */
    boolean updateContent(Long id, String note);

    /** 删单：已收口锁档删不掉，被拒返回 false；未收口按逻辑删除置 del_flag */
    boolean remove(Long id);
}
