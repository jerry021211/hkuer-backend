package com.hkuer.hkuer.entity.enums;

/**
 * 举报目标类型枚举
 */
public enum ReportTargetType {
    
    POST("POST", "帖子"),
    COMMENT("COMMENT", "评论"),
    USER("USER", "用户"),
    ANSWER("ANSWER", "回答"),
    COURSE_REVIEW("COURSE_REVIEW", "课程评价"),
    COURSE_MATERIAL("COURSE_MATERIAL", "课程资料");
    
    private final String code;
    private final String desc;
    
    ReportTargetType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据 code 获取枚举
     *
     * @param code 类型代码
     * @return 枚举值，如果不存在返回 null
     */
    public static ReportTargetType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ReportTargetType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 检查 code 是否有效
     *
     * @param code 类型代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
    
    /**
     * 获取所有支持的类型描述（用于错误提示）
     *
     * @return 如 "POST, COMMENT, USER"
     */
    public static String getAllCodes() {
        StringBuilder sb = new StringBuilder();
        for (ReportTargetType type : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(type.code);
        }
        return sb.toString();
    }
}
