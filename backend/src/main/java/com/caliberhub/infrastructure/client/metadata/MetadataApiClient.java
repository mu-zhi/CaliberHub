package com.caliberhub.infrastructure.client.metadata;

import com.caliberhub.application.support.MetadataSupport;
import com.caliberhub.infrastructure.common.cache.SimpleCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 元数据适配器 Mock 实现
 * 用于本地开发和联调
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataApiClient implements MetadataSupport {
    
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_PREFIX = "metadata:table:";
    
    private final SimpleCache cache;
    
    // Mock 数据
    private static final Map<String, TableDetail> MOCK_TABLES = new ConcurrentHashMap<>();
    
    // 黑名单正则
    private static final List<Pattern> BLACKLIST_PATTERNS = List.of(
        Pattern.compile("^tmp_.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^temp_.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.tmp_.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.temp_.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^personal\\..*", Pattern.CASE_INSENSITIVE)
    );
    
    static {
        // 初始化 Mock 数据
        initMockData();
    }
    
    private static void initMockData() {
        // 零售客户表
        MOCK_TABLES.put("cif.t_customer", new TableDetail(
            "cif.t_customer",
            "mt-001",
            "cif",
            "t_customer",
            "零售客户主表",
            "包含敏感字段",
            List.of(
                new FieldInfo("cust_id", "cif.t_customer.cust_id", "mf-001", "VARCHAR(32)", "客户号", null, false),
                new FieldInfo("cust_name", "cif.t_customer.cust_name", "mf-002", "VARCHAR(100)", "客户姓名", "PII", true),
                new FieldInfo("id_no", "cif.t_customer.id_no", "mf-003", "VARCHAR(18)", "身份证号", "PII", true),
                new FieldInfo("mobile", "cif.t_customer.mobile", "mf-004", "VARCHAR(20)", "手机号", "PII", true),
                new FieldInfo("create_date", "cif.t_customer.create_date", "mf-005", "DATE", "创建日期", null, false)
            )
        ));
        
        MOCK_TABLES.put("cif.t_customer_ext", new TableDetail(
            "cif.t_customer_ext",
            "mt-002",
            "cif",
            "t_customer_ext",
            "客户扩展信息表",
            null,
            List.of(
                new FieldInfo("cust_id", "cif.t_customer_ext.cust_id", "mf-010", "VARCHAR(32)", "客户号", null, false),
                new FieldInfo("occupation", "cif.t_customer_ext.occupation", "mf-011", "VARCHAR(50)", "职业", null, false)
            )
        ));
        
        // 交易表
        MOCK_TABLES.put("txn.t_transaction", new TableDetail(
            "txn.t_transaction",
            "mt-003",
            "txn",
            "t_transaction",
            "交易流水表",
            "包含敏感字段",
            List.of(
                new FieldInfo("txn_id", "txn.t_transaction.txn_id", "mf-020", "VARCHAR(32)", "交易号", null, false),
                new FieldInfo("cust_id", "txn.t_transaction.cust_id", "mf-021", "VARCHAR(32)", "客户号", null, false),
                new FieldInfo("amount", "txn.t_transaction.amount", "mf-022", "DECIMAL(18,2)", "交易金额", "FINANCIAL", true),
                new FieldInfo("txn_date", "txn.t_transaction.txn_date", "mf-023", "DATE", "交易日期", null, false),
                new FieldInfo("counter_party", "txn.t_transaction.counter_party", "mf-024", "VARCHAR(100)", "交易对手", "PII", true)
            )
        ));
        
        // 账户表
        MOCK_TABLES.put("acct.t_account", new TableDetail(
            "acct.t_account",
            "mt-004",
            "acct",
            "t_account",
            "账户信息表",
            "包含敏感字段",
            List.of(
                new FieldInfo("acct_no", "acct.t_account.acct_no", "mf-030", "VARCHAR(32)", "账号", "FINANCIAL", true),
                new FieldInfo("cust_id", "acct.t_account.cust_id", "mf-031", "VARCHAR(32)", "客户号", null, false),
                new FieldInfo("balance", "acct.t_account.balance", "mf-032", "DECIMAL(18,2)", "余额", "FINANCIAL", true)
            )
        ));
    }
    
    @Override
    public List<TableSearchResult> searchTables(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return MOCK_TABLES.values().stream()
                .filter(t -> t.tableFullname().toLowerCase().contains(lowerKeyword) 
                        || t.description().toLowerCase().contains(lowerKeyword))
                .map(t -> new TableSearchResult(
                        t.tableFullname(),
                        t.metadataTableId(),
                        t.description(),
                        t.fields().stream().anyMatch(FieldInfo::isSensitive)
                ))
                .toList();
    }
    
    @Override
    public TableDetail getTableDetail(String tableFullname) {
        if (tableFullname == null) {
            return null;
        }
        return MOCK_TABLES.get(tableFullname.toLowerCase());
    }
    
    @Override
    public MatchResult matchTable(String tableFullname) {
        if (tableFullname == null || tableFullname.isBlank()) {
            return MatchResult.notFound(tableFullname);
        }
        
        String normalized = tableFullname.toLowerCase();
        
        // 检查黑名单
        for (Pattern pattern : BLACKLIST_PATTERNS) {
            if (pattern.matcher(normalized).matches()) {
                return MatchResult.blacklisted(tableFullname, "表名匹配黑名单规则: " + pattern.pattern());
            }
        }
        
        // 查找表
        TableDetail detail = MOCK_TABLES.get(normalized);
        if (detail != null) {
            return MatchResult.matched(tableFullname, detail.metadataTableId(), detail.description());
        }
        
        return MatchResult.notFound(tableFullname);
    }
}
