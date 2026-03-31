package com.cmbchina.datadirect.caliber.application.service.command;

import java.util.Locale;

final class ImportCandidateCodeSupport {

    private ImportCandidateCodeSupport() {
    }

    static String sceneCandidateCode(String taskId, int sceneIndex) {
        return "SC-" + compactTaskPrefix(taskId) + "-" + String.format(Locale.ROOT, "%03d", sceneIndex + 1);
    }

    static String evidenceCandidateCode(String taskId, int evidenceIndex) {
        return "EV-" + compactTaskPrefix(taskId) + "-" + String.format(Locale.ROOT, "%03d", evidenceIndex + 1);
    }

    private static String compactTaskPrefix(String taskId) {
        String compactId = taskId == null ? "" : taskId.replace("-", "");
        return compactId.length() > 8 ? compactId.substring(0, 8) : compactId;
    }
}
