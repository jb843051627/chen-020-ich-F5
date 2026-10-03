package com.fc.v2.model.custom.preadmit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fc.v2.model.auto.TIchPreLine;

/**
 * 立线回执：线册一行立不立得起来，逐条点是哪一栏空着、哪一栏的数字不像话，
 * 光回一句"数不对"不算交代。三条分界数一条都缺不得，缺了就立不起来；
 * 只有启用之日、交棒那一栏空着的不算毛病——按一直在场认。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class PreLineSaveReceipt {

    /** 一栏的错处：栏名 + 不像话的说法 */
    public static final class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        /** 栏目标识（与线册字段同名：ruleName/ruleCode/effStart/effEnd/priority/th1Max/th2Max/th3Max） */
        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }

    /** 立没立起来（错处一笔都没有才算立起来） */
    private boolean saved;

    /** 立起来的那一行（没立起来为空） */
    private TIchPreLine line;

    /** 逐栏错处，按册子栏目顺序 */
    private List<FieldError> errors = new ArrayList<>();

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public TIchPreLine getLine() {
        return line;
    }

    public void setLine(TIchPreLine line) {
        this.line = line;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors == null ? new ArrayList<>() : errors;
    }

    public void addError(String field, String message) {
        this.errors.add(new FieldError(field, message));
    }

    public List<FieldError> errorsView() {
        return Collections.unmodifiableList(errors);
    }
}
