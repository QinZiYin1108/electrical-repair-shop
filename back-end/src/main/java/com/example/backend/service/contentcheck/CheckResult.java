package com.example.backend.service.contentcheck;

/** 内容检测结果（文本/图片/文件通用） */
public class CheckResult {

    /** 处理动作：pass-放行，block-拒绝，watch-存疑放行（需复审） */
    private String action;

    /** 阿里云原始审核建议：block/pass/watch */
    private String suggestion;

    /** 阿里云审核标签 */
    private String label;

    /** 阿里云标签描述 */
    private String labelDesc;

    private CheckResult() {}

    public static CheckResult passed() {
        CheckResult r = new CheckResult();
        r.action = "pass";
        r.suggestion = "pass";
        return r;
    }

    public static CheckResult blocked(String suggestion, String label, String labelDesc) {
        CheckResult r = new CheckResult();
        r.action = "block";
        r.suggestion = suggestion;
        r.label = label;
        r.labelDesc = labelDesc;
        return r;
    }

    public static CheckResult watch(String suggestion, String label, String labelDesc) {
        CheckResult r = new CheckResult();
        r.action = "watch";
        r.suggestion = suggestion;
        r.label = label;
        r.labelDesc = labelDesc;
        return r;
    }

    public static CheckResult fromSuggestion(String suggestion, String label, String labelDesc) {
        if ("block".equalsIgnoreCase(suggestion)) {
            return blocked(suggestion, label, labelDesc);
        } else if ("watch".equalsIgnoreCase(suggestion)) {
            return watch(suggestion, label, labelDesc);
        }
        return passed();
    }

    public boolean isPassed() {
        return !"block".equals(action);
    }

    public boolean isWatch() {
        return "watch".equals(action);
    }

    public boolean isBlocked() {
        return "block".equals(action);
    }

    // ==================== getter / setter ====================

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelDesc() {
        return labelDesc;
    }

    public void setLabelDesc(String labelDesc) {
        this.labelDesc = labelDesc;
    }
}
