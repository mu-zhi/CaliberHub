package com.cmbchina.datadirect.caliber.application.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlTableParseSupportTest {

    @Test
    void shouldParseSourceAndTargetTablesByAstFirst() {
        String sql = """
                INSERT INTO dm_target_result
                SELECT a.cust_id, b.acct_no
                FROM dm_customer_info a
                LEFT JOIN dm_account b ON a.cust_id = b.cust_id
                WHERE a.dt >= '2026-01-01';
                """;

        SqlTableParseSupport.ParseResult result = SqlTableParseSupport.parse(sql);

        assertThat(result.sourceTables()).contains("dm_customer_info", "dm_account");
        assertThat(result.targetTables()).contains("dm_target_result");
        assertThat(result.parseErrors()).isEmpty();
    }

    @Test
    void shouldFallbackRegexForRoughSql() {
        String sql = "select * from NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T t join PDM_VHIS.T05_AGN_DTL d on t.id=d.id";

        SqlTableParseSupport.ParseResult result = SqlTableParseSupport.parse(sql);

        assertThat(result.sourceTables()).contains("NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T", "PDM_VHIS.T05_AGN_DTL");
    }

    @Test
    void shouldParseMergeUsingWithoutSetAsTable() {
        String sql = """
                MERGE INTO dm_target_result t
                USING dm_customer_info c
                ON t.id = c.id
                WHEN MATCHED THEN UPDATE SET t.flag = 1;
                """;

        SqlTableParseSupport.ParseResult result = SqlTableParseSupport.parse(sql);

        assertThat(result.targetTables()).containsExactly("dm_target_result");
        assertThat(result.sourceTables()).contains("dm_customer_info");
        assertThat(result.targetTables()).doesNotContain("SET");
    }
}
