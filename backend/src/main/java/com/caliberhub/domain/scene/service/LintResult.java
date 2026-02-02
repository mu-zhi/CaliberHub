package com.caliberhub.domain.scene.service;

import com.caliberhub.domain.scene.valueobject.SceneVersionContent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Lint 结果
 */
@Getter
@Builder
public class LintResult {
    
    private final boolean passed;
    private final List<LintIssue> errors;
    private final List<LintIssue> warnings;
    
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
    
    public static LintResult success() {
        return LintResult.builder()
                .passed(true)
                .errors(List.of())
                .warnings(List.of())
                .build();
    }
    
    public static LintResult of(List<LintIssue> errors, List<LintIssue> warnings) {
        return LintResult.builder()
                .passed(errors == null || errors.isEmpty())
                .errors(errors != null ? errors : List.of())
                .warnings(warnings != null ? warnings : List.of())
                .build();
    }
    
    /**
     * Lint 问题
     */
    @Getter
    @Builder
    public static class LintIssue {
        private final String id;
        private final String message;
        private final String path;
        private final String blockId;
        private final Severity severity;
        
        public enum Severity {
            ERROR, WARNING
        }
        
        public static LintIssue error(String id, String message, String path) {
            return LintIssue.builder()
                    .id(id)
                    .message(message)
                    .path(path)
                    .severity(Severity.ERROR)
                    .build();
        }
        
        public static LintIssue error(String id, String message, String path, String blockId) {
            return LintIssue.builder()
                    .id(id)
                    .message(message)
                    .path(path)
                    .blockId(blockId)
                    .severity(Severity.ERROR)
                    .build();
        }
        
        public static LintIssue warning(String id, String message, String path) {
            return LintIssue.builder()
                    .id(id)
                    .message(message)
                    .path(path)
                    .severity(Severity.WARNING)
                    .build();
        }
        
        public static LintIssue warning(String id, String message, String path, String blockId) {
            return LintIssue.builder()
                    .id(id)
                    .message(message)
                    .path(path)
                    .blockId(blockId)
                    .severity(Severity.WARNING)
                    .build();
        }
    }
}
