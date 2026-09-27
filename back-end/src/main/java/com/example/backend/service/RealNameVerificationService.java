package com.example.backend.service;

/** 实名核验服务接口 */
public interface RealNameVerificationService {

    /**
     * 核验姓名 + 身份证号是否匹配
     *
     * @param realName 真实姓名
     * @param idCard 18 位身份证号
     * @return 核验结果
     */
    VerificationResult verify(String realName, String idCard);

    /** 核验结果 */
    class VerificationResult {
        /** 是否核验通过 */
        private final boolean passed;

        /** 结果码（来自 API） */
        private final String resultCode;

        /** 结果描述 */
        private final String description;

        public VerificationResult(boolean passed, String resultCode, String description) {
            this.passed = passed;
            this.resultCode = resultCode;
            this.description = description;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getResultCode() {
            return resultCode;
        }

        public String getDescription() {
            return description;
        }
    }
}
