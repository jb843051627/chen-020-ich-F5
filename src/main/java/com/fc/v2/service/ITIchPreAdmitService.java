package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchPreDeclare;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.preadmit.DeclareVerdict;
import com.fc.v2.model.custom.preadmit.PreAdmitReport;
import com.fc.v2.model.custom.preadmit.PreDeclareInput;
import com.fc.v2.model.custom.preadmit.PreLineSaveReceipt;
import com.fc.v2.model.custom.preadmit.StaleHeadRef;

/**
 * 传承人申报准入 Service接口——四问的说法只从这一个判据出口出来，
 * 别处不再立第二道能给结论的门：
 * <ol>
 *   <li>某位申报人在申请注明那一刻依哪条线判（{@link #judgeOne} / {@link #judgeBatch}）；</li>
 *   <li>线册当刻一条也够不着时给不给结论（不给：无结论，代号与启用之日空着）；</li>
 *   <li>几条线同顺位时听谁的（让位顺位高者；再同听代号字数靠后者，尺里定死）；</li>
 *   <li>两头线并到一处取哪一头（两头各自都过才算过，越了亮那头的数）。</li>
 * </ol>
 * 按线代号取值的老路在 {@link ITIchPreLineService} 里保持不动；
 * 本接口的 {@link #presentLines}/{@link #locateAt} 是另开的查线之道，另以新名承接。
 *
 * @author fuce
 * @date 2026-10-03
 */
public interface ITIchPreAdmitService {

    /**
     * 判一份：按申报自己注明的那一刻回看线册，结论落库（主表一笔 + 两头各一笔），
     * 线代号与启用之日随结论钉死。无可用线时回无结论，不抓邻近线凑数。
     */
    DeclareVerdict judgeOne(PreDeclareInput input);

    /**
     * 判一批：线册只装载这一回，在场名单（按 at 回看）与逐条判定
     * （各按各申报注明的那一刻回看）同一回算出来；逐条落库，两回的数不一样即错。
     */
    PreAdmitReport judgeBatch(List<PreDeclareInput> inputs, Date at);

    /** 另开的查线之道：at 时刻在场的线（让位顺位降序、代号字数靠后在前） */
    List<TIchPreLine> presentLines(Date at);

    /** 另开的查线之道：at 时刻说话的那条线；当刻一条也够不着回 null（不给结论） */
    TIchPreLine locateAt(Date at);

    /**
     * 来历不对的旧案逐条：屏上挂着线代号、线册里在该申报注明那一天却查不到
     * 在场那条的两头明细（去年那两份照停用线判下来的要一眼认得出来）。
     */
    List<StaleHeadRef> listStaleRefs();

    /** 立线：逐栏校验后立；立不起来回执逐栏点名，光回"数不对"不算交代 */
    PreLineSaveReceipt appendLine(TIchPreLine form);

    /** 按下旧线：翻成已停用。仍在册可翻，只是不进现行判定 */
    int retireLine(Long id);

    /** 回执旧档翻看：这一回没过与后来补过各留各的，先前那头的提示仍在 */
    List<TIchPreDeclare> listDeclares(String applicantCode);

    /** 翻一笔旧档：读已钉死的两头明细回话（只读旧字，不按今线重判） */
    DeclareVerdict loadVerdict(Long declareId);
}
