package com.fc.v2.model.custom.itemflow;

/**
 * 在册项目数的当场对算：一个数顺着现行版次逐格点出，一个数顺着底册当场点出，
 * 两回须是同一回算出来的装载——合得上才算数。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class RosterView {

    /** 顺着申报单现行列入版点出的在册数（随列入加一、随注销减一） */
    private int byRoster;

    /** 顺着名录底册 status=在册 当场点出的数 */
    private int byLedger;

    /** 两处是不是同一个数（且同一回装载点出） */
    private boolean consistent;

    public int getByRoster() {
        return byRoster;
    }

    public void setByRoster(int byRoster) {
        this.byRoster = byRoster;
    }

    public int getByLedger() {
        return byLedger;
    }

    public void setByLedger(int byLedger) {
        this.byLedger = byLedger;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }
}
