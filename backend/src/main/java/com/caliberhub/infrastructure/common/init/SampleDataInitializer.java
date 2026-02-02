package com.caliberhub.infrastructure.common.init;

import com.caliberhub.domain.scene.model.Domain;
import com.caliberhub.domain.scene.model.Scene;
import com.caliberhub.domain.scene.model.SceneVersion;
import com.caliberhub.domain.scene.support.DomainRepository;
import com.caliberhub.domain.scene.support.SceneRepository;
import com.caliberhub.domain.scene.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 示例数据初始化器
 * 应用启动时自动插入3个示例场景
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleDataInitializer implements ApplicationRunner {

        private final SceneRepository sceneRepository;
        private final DomainRepository domainRepository;

        @Override
        public void run(ApplicationArguments args) {
                // 检查是否已有数据
                if (!sceneRepository.findAllActive().isEmpty()) {
                        log.info("示例数据已存在，跳过初始化");
                        return;
                }

                log.info("开始初始化示例场景数据...");

                // 获取领域
                Domain retailCif = domainRepository.findByDomainKey("RETAIL_CIF")
                                .orElseThrow(() -> new RuntimeException("领域不存在: RETAIL_CIF"));
                Domain retailTxn = domainRepository.findByDomainKey("RETAIL_TXN")
                                .orElseThrow(() -> new RuntimeException("领域不存在: RETAIL_TXN"));
                Domain corpCif = domainRepository.findByDomainKey("CORP_CIF")
                                .orElseThrow(() -> new RuntimeException("领域不存在: CORP_CIF"));

                // 示例场景1: 零售客户AUM计算
                createSampleScene1(retailCif);

                // 示例场景2: 零售交易流水汇总
                createSampleScene2(retailTxn);

                // 示例场景3: 对公客户授信额度
                createSampleScene3(corpCif);

                log.info("示例场景数据初始化完成，共创建3个场景");
        }

        private void createSampleScene1(Domain domain) {
                SceneVersionContent content = SceneVersionContent.builder()
                                .sceneDescription("计算零售客户在指定日期的资产管理规模（AUM），包括存款、理财、基金等各类金融资产的加总。")
                                .caliberDefinition("AUM = 活期存款余额 + 定期存款余额 + 理财产品持有市值 + 基金持有市值 + 贵金属持有市值")
                                .applicability("适用于所有零售个人客户，包括普通客户和私行客户")
                                .boundaries("不包含对公账户资产、代发工资待入账金额、冻结资产")
                                .entities(List.of("客户", "账户", "理财产品", "基金"))
                                .inputParams(List.of(
                                                InputParam.of("customer_id", "客户号", "STRING", true, "C00001", "客户唯一标识"),
                                                InputParam.of("stat_date", "统计日期", "DATE", true, "2024-01-15",
                                                                "AUM计算的基准日期")))
                                .constraintsDescription("客户状态必须为正常，账户状态必须为活动")
                                .outputSummary("返回客户AUM总额及各资产类别明细")
                                .sqlBlocks(List.of(
                                                SqlBlock.of("block_001", "主查询-存款类", null,
                                                                "SELECT customer_id, SUM(balance) as deposit_total\n" +
                                                                                "FROM dw.dim_account\n" +
                                                                                "WHERE customer_id = #{customer_id}\n" +
                                                                                "  AND stat_date = #{stat_date}\n" +
                                                                                "  AND account_type IN ('DEMAND', 'FIXED')\n"
                                                                                +
                                                                                "GROUP BY customer_id",
                                                                "存款余额汇总")))
                                .caveats(List.of(
                                                Caveat.of("caveat_001", "理财产品估值说明", "MEDIUM", "理财产品使用T-1日净值估算当前市值"),
                                                Caveat.of("caveat_002", "外币资产汇率", "LOW", "外币资产使用当日中间价折算人民币")))
                                .sourceTables(List.of(
                                                SourceTable.builder()
                                                                .tableFullname("dw.dim_account")
                                                                .matchStatus(TableMatchStatus.MATCHED)
                                                                .keyTable(true)
                                                                .source("EXTRACTED")
                                                                .build()))
                                .sensitiveFields(List.of())
                                .build();

                Scene scene = Scene.createWithCode("aum_calculation", "零售客户AUM计算", domain.getId(), "demo_user", "demo_user");
                scene.saveDraft("零售客户AUM计算", content, "demo_user");

                SceneVersion version = scene.getCurrentVersion();
                version.setOwnerUser("张三");
                version.setTags(List.of("AUM", "零售", "资产"));

                sceneRepository.save(scene);
                log.info("创建示例场景: {}", scene.getSceneCode());
        }

        private void createSampleScene2(Domain domain) {
                SceneVersionContent content = SceneVersionContent.builder()
                                .sceneDescription("查询零售客户在指定时间段内的交易流水汇总，包括转账、消费、取现等交易类型的统计。")
                                .caliberDefinition("交易汇总 = COUNT(交易笔数) + SUM(交易金额)，按交易类型分组统计")
                                .applicability("适用于所有零售个人客户的借记卡和信用卡交易")
                                .boundaries("不包含已撤销交易、冲正交易")
                                .entities(List.of("客户", "交易", "账户"))
                                .inputParams(List.of(
                                                InputParam.of("customer_id", "客户号", "STRING", true, "C00001", "客户唯一标识"),
                                                InputParam.of("start_date", "开始日期", "DATE", true, "2024-01-01",
                                                                "查询起始日期"),
                                                InputParam.of("end_date", "结束日期", "DATE", true, "2024-01-31",
                                                                "查询截止日期")))
                                .constraintsDescription("时间跨度不超过12个月")
                                .outputSummary("返回按交易类型分组的交易笔数和金额汇总")
                                .sqlBlocks(List.of(
                                                SqlBlock.of("block_001", "交易流水汇总", null,
                                                                "SELECT customer_id, txn_type,\n" +
                                                                                "       COUNT(*) as txn_count,\n" +
                                                                                "       SUM(txn_amount) as total_amount\n"
                                                                                +
                                                                                "FROM dw.fact_transaction\n" +
                                                                                "WHERE customer_id = #{customer_id}\n" +
                                                                                "  AND txn_date BETWEEN #{start_date} AND #{end_date}\n"
                                                                                +
                                                                                "  AND txn_status = 'SUCCESS'\n" +
                                                                                "GROUP BY customer_id, txn_type",
                                                                "成功交易按类型汇总")))
                                .caveats(List.of(
                                                Caveat.of("caveat_001", "跨境交易", "MEDIUM", "跨境交易金额已折算为人民币")))
                                .sourceTables(List.of(
                                                SourceTable.builder()
                                                                .tableFullname("dw.fact_transaction")
                                                                .matchStatus(TableMatchStatus.MATCHED)
                                                                .keyTable(true)
                                                                .source("EXTRACTED")
                                                                .build()))
                                .sensitiveFields(List.of(
                                                SensitiveField.create("dw.fact_transaction.card_no",
                                                                SensitivityLevel.PII, MaskRule.HASH)))
                                .build();

                Scene scene = Scene.createWithCode("txn_summary", "零售交易流水汇总", domain.getId(), "demo_user", "demo_user");
                scene.saveDraft("零售交易流水汇总", content, "demo_user");

                SceneVersion version = scene.getCurrentVersion();
                version.setOwnerUser("李四");
                version.setTags(List.of("交易", "流水", "统计"));
                version.setHasSensitive(true);

                sceneRepository.save(scene);
                log.info("创建示例场景: {}", scene.getSceneCode());
        }

        private void createSampleScene3(Domain domain) {
                SceneVersionContent content = SceneVersionContent.builder()
                                .sceneDescription("查询对公企业客户的授信额度使用情况，包括已批授信、已用额度、可用额度等。")
                                .caliberDefinition("可用额度 = 已批授信总额 - 已用额度 - 冻结额度")
                                .applicability("适用于所有对公企业客户，包括集团客户和单一法人客户")
                                .boundaries("不包含个人经营贷、不包含已过期授信")
                                .entities(List.of("企业客户", "授信", "合同"))
                                .inputParams(List.of(
                                                InputParam.of("corp_id", "企业客户号", "STRING", true, "E00001", "企业客户唯一标识"),
                                                InputParam.of("stat_date", "统计日期", "DATE", true, "2024-01-15",
                                                                "额度计算的基准日期")))
                                .constraintsDescription("客户状态必须为正常，授信必须在有效期内")
                                .outputSummary("返回企业各类授信的额度使用明细")
                                .sqlBlocks(List.of(
                                                SqlBlock.of("block_001", "授信额度汇总", null,
                                                                "SELECT corp_id, credit_type,\n" +
                                                                                "       SUM(approved_amount) as approved_total,\n"
                                                                                +
                                                                                "       SUM(used_amount) as used_total,\n"
                                                                                +
                                                                                "       SUM(frozen_amount) as frozen_total,\n"
                                                                                +
                                                                                "       SUM(approved_amount - used_amount - frozen_amount) as available_total\n"
                                                                                +
                                                                                "FROM dw.dim_credit_limit\n" +
                                                                                "WHERE corp_id = #{corp_id}\n" +
                                                                                "  AND stat_date = #{stat_date}\n" +
                                                                                "  AND status = 'ACTIVE'\n" +
                                                                                "GROUP BY corp_id, credit_type",
                                                                "有效授信按类型汇总")))
                                .caveats(List.of(
                                                Caveat.of("caveat_001", "集团额度共享", "HIGH", "集团客户可能存在额度共享，需按集团维度校验"),
                                                Caveat.of("caveat_002", "币种说明", "LOW", "所有额度均以人民币计价")))
                                .sourceTables(List.of(
                                                SourceTable.builder()
                                                                .tableFullname("dw.dim_credit_limit")
                                                                .matchStatus(TableMatchStatus.MATCHED)
                                                                .keyTable(true)
                                                                .source("EXTRACTED")
                                                                .build()))
                                .sensitiveFields(List.of())
                                .build();

                Scene scene = Scene.createWithCode("credit_limit", "对公客户授信额度查询", domain.getId(), "demo_user", "demo_user");
                scene.saveDraft("对公客户授信额度查询", content, "demo_user");

                SceneVersion version = scene.getCurrentVersion();
                version.setOwnerUser("王五");
                version.setTags(List.of("授信", "额度", "对公"));

                sceneRepository.save(scene);
                log.info("创建示例场景: {}", scene.getSceneCode());
        }
}
