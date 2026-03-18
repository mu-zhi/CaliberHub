    # 开户行变更记录

    - 场景描述：未提供（待补充）
    - 口径提供人：胡彦冰、谢午勇
    - 结果字段：未提供（待补充）
    - 注意事项：未提供（待补充）
    - SQL语句

    ```sql
    -- 零售户口开户机构变更-处理控制
    SELECT DISTINCT
    A.BCA_APL_DAT AS "申请日期",
    A.BCA_EAC_NBR AS "户口号码",
    A.BCA_OWN_CLT AS "客户号码",
    C1.ORG_NM AS "旧开户机构名称",
    C2.ORG_NM AS "新开户机构名称",
    C3.ORG_NM AS "申请机构名称",
    A.BCA_APL_TLR AS "申请经办"
    FROM NDS_VHIS.NLU39_DEE_EAC_TRX_T A    -- 变更开户行申请交易表
    INNER JOIN NDS_VHIS.NLW35_WBM_ERT_PRC_T B
        ON A.BCA_EAC_NBR = B.CHG_CARD
        AND B.CHG_NEW_BBK = '532'
        AND A.BCA_APL_DAT = B.SYS_DATE
    LEFT JOIN AMIP_VHIS.AMIP_D_ORG_VAL C1 ON C1.ORG_ID = A.BCA_OLD_BRN
    LEFT JOIN AMIP_VHIS.AMIP_D_ORG_VAL C2 ON C2.ORG_ID = A.BCA_NEW_BRN
    LEFT JOIN AMIP_VHIS.AMIP_D_ORG_VAL C3 ON C3.ORG_ID = A.BCA_APL_BRN
    AND B.SYS_DATE >= '2020-01-01' AND B.PRC_STATUS = 'S'
    WHERE BCA_APL_DAT >= '2020-01-01'
    AND BCA_NEW_BBK = '532'
    ;

    -- 全量数据
    -- Mdf_Ctrl_Sts_cd = 'C' 变更成功，= 'A' 申请成功待变更，= 'D' 申请后柜员撤销，其他为E终变更处理失败
    SELECT  TAC_ID 户内部编号,IAC_MDF 内部户口修饰符,REG_DT 登记日期,REG_SEQ_NBR 登记序号,REG_TM 登记时间,EAC_ID 户口编号,EAC_MDF 户口修饰符,CUST_ID 客户编号,EAC_SEQ_NBR 户口序号,REL_CUST_ID 关联客户编号,REL_EAC_SEQ_NBR 关联户口序号,NEW_BRN_ORG_ID 新开户机构编号,NEW_BBK_ORG_ID 新分行机构编号,NEW_CTY_NO 新城市码,ORIG_BRN_ORG_ID 原开户机构编号,ORIG_BBK_ORG_ID 原分行机构编号,ORIG_CTY_NO 原城市码,LMT_TRX_IND_10 限制交易标志10,LMT_TRX_IND_03 限制交易标志03,EAC_MDF_IND 户口修饰符标志,EAC_MDF_IND_10 张户换行标志10,ACT_MDF_IND_03 张户换行标志03,CNCL_TRX_LMT_IND_10 取消交易限制标志10,CNCL_TRX_LMT_IND_03 取消交易限制标志03,MDF_CTRL_BMP 换行控制位图,REG_USR_ID 登记用户编号,REG_ORG_ID 登记机构编号,CNCL_USR_ID 取消用户编号,CNCL_DT 取消日期,MDF_CTRL_STS_CD 换行控制状态代码,ESP_NO 特殊码,RCD_LATE_UPD_DT 记录最后更新日期,RCD_LATE_UPD_EDTN 记录最后更新版本
    FROM PDM_VHIS.T03_CORE_EAC_MDF_OPN_BNK_CTRL_INP --核心户口变更开户行控制信息  源表 F3EABCTLP
    WHERE EAC_ID = '9999'
    ```

    ---

    # 客户密码修改

    ## 客户密码修改

    - 场景描述：因权限自查需要，需查询CMBRUN用户号（107101）2018年4月密码修改日志。由于CMBRUN界面（9102）仅展示近一年记录，故请总行数仓协助产出
    - 口径来源：吴时芊
    - 结果字段：未提供（待补充）
    - 注意事项：未提供（待补充）
    - SQL语句

    ```sql
    --柜员在柜台修改密码
    select *
    from NDS_VHIS.NLJ52_PTTSKISTP --任务实例当前文件
    where TKIUSRNRB='107101'
    and TKITSKCOD='USUSRCHP' --任务编码等于 'USUSRCHP'为修改密码任务
    and TKITSKDAT between '2018-04-01' and '2018-05-01'
    ;

    -- 客户密码修改
    'P1' 支付密码修改(查询密码重置查询密码),
    'P2' 支付密码修改(取款密码重置支付密码),
    'PP' 支付密码重置,
    'PE' 一网通支付密码错误,
    'PQ' 查询密码错误,
    'PW' 取款密码错误,
    'Q1' 查询密码重置(查询密码重置查询密码),
    'Q2' 查询密码重置(取款密码重置查询密码),
    'W1' 取款密码修改

    --客户所有的密码修改记录
    SELECT DISTINCT
    LOG_OWN_CLT 持有客户号码
    ,LOG_IAC_NBR 户口内码
    ,LOG_EAC_NBR 户口号码
    ,LOG_LOG_TYP 日志类型
    ,LOG_LOG_DAT 登记日期
    ,LOG_LOG_TIM 登记时间
    ,LOG_TLR_NBR 柜员号码
    ,LOG_WKE_COD 作业类别
    ,LOG_CNL_COD 发起渠道编码
    ,LOG_RQS_NBR 通讯请求号
    ,LOG_SPL_C40 加入代码
    ,LOG_RCD_DAT 记录最后更新日期
    ,LOG_RCD_VER 记录最后更新版本
    ,TRAN_TIME 交易时间
    ,SEQ_NO 流水号
    FROM NDS_VHIS.NLL53_ORTODNDW_F3PVLLOGP --用户密码修改日志文件 PW是取款密码错误
    WHERE LOG_OWN_CLT =
    ORDER BY LOG_LOG_DAT;

    --涉诈审
    SELECT DW_DAT_DT AS "数据日期", LOG_PTN_TAG AS "分区标志", LOG_OWN_CLT AS "客户号码", LOG_IAC_NBR AS "户口内码", LOG_EAC_NBR AS "户口号码", LOG_LOG_TYP AS "日志类型", LOG_LOG_DAT AS "登记日期", LOG_LOG_TIM AS "登记时间", LOG_TLR_NBR AS "柜员号码",
        LOG_WKE_COD AS "作业类别", LOG_CNL_COD AS "发起渠道编码", LOG_RQS_NBR AS "通讯请求号", LOG_MSG_COD AS "错误代码", LOG_MSG_TXT AS "错误描述", LOG_DTB_DIV AS "数据分库名称", LOG_MNT_DAT AS "最后修改日期", LOG_MNT_TIM AS "最后修改时间",
        LOG_RCD_VER AS "记录版本", DW_ETL_DT AS "跑批日期", DW_INS_DT AS "插入日期", DW_INS_TM AS "插入时间", DW_UPD_DT AS "更新日期", DW_UPD_TM AS "更新时间"
    FROM NDS_VHIS.NLLW68_DEE_PWL_LOG_T --户口密码变更汇总日志表
    WHERE LOG_OWN_CLT = '1230015692'
    ORDER BY 1
    ```

    ---

    # 零售客户信息变更（含：零售客户维护日志/维护记录）

    零售客户信息变更

    # 业务概述

    ## 业务定义

    CIF是零售客户信息系统
    更多内容请参考文档[客户信息管理](http://dwiki.cmbchina.cn/pages/viewpage.action?pageId=122998626)

    ## 术语定义

    ## 业务/开发负责人

    > 零售CIF：杨刚/80274266
    > 单笔变更开户行：刘贝蒂/01177406

    # 常见数据表

    PDM_VHIS.T05_EAC_MNT_EVT--户口维护日志事件

    ## 表名(范式名.表名)

    - 中文表名：未提供（待补充）
    - 数据时间范围：未提供（待补充）
    - 明细字段：
    - 限制字段：

    # 常见业务场景

    ## 零售客户维护日志/维护记录

    - 场景描述：未提供（待补充）
    - 口径提供人：潘达乐
    - 结果字段：未提供（待补充）
    - 注意事项：因为维护日志的查询根据日期不同，查询方法也不同，所以请确定正确的查询条件
    - SQL语句

    （原始截图引用缺失，已移除图片链接）

    ```sql
    /*
    这部分数据分为1G数据，2G数据，每一分行迁移2G系统的时间点都不一样，具体请参考上图：{分行上线情况统计表（客户信息2G系统）}
    2007年及之前的日志
    需要从维护度更员，联荣宇王萍，陈成吉支持。

    2008-2013的维护日志
    客户关联人以外的维护（包括客户本人的基本信息，地址信息，通讯信息等等）的维护日志查询方法：
        1、用零售客户号查CIPRVDTAP表，得到产品实例号
        2、用产品实例号查PTTSKISTP表，得到任务实例号及其维护任务概要（这里有时间，渠道，操作人等信息）
        3、用任务实例号查PTTTSKFLDP表，得到维护的字段明细（其中，新增加删除操作这块的维护前后值）

    客户关联人的维护记录：
        1、在CICSTDTAP里找到关联人的零售实例号
        2、再用零售实例号在PTTSKISTP里找到对应的任务实例号（这里有时间，渠道，操作人等信息）
        3、用任务实例号在PTTSKFLDP里找到维护前后值（其中，新增加删除操作这块的维护前后值）

    如果是新上线2G系统客户数据，要回溯才有途径到，后续补充查询路径
    2G数据查询：请在核心系统 1199 查询（柜面已废弃）
    */

    --1G数据
    --Step1
    SELECT CLPPRDNBR  --产品实例号
    FROM NDS_VHIS.NLI51_CIPRVDTAP  ----个人客户背景资料
    WHERE CLPCLTNBR = ' ';

    --Step2
    SELECT *
    FROM NDS_VHIS.NLJ52_PTTSKISTP  --任务实例当前文件
    WHERE TKITSKNBR = ;

    --任务实例事件
    SELECT *
    FROM PDM_VHIS.T05_TSK_INST_EVT
    WHERE TSK_INST_NBR = ;

    --任务实例字段维护事件
    SELECT *
    FROM PDM_SVIEW.T05_TSK_INST_FLD_MNT_EVT
    WHERE SUBSTRING(TRIM(INT_RES_VAL),1,10) = ' ' ;

    --根据事件实例号查询任务实例历史文件
    SELECT *
    FROM LGC_EAM.UNICORE_PTTSKISHP_YEAR  A
    WHERE TRIM(TSK_NBR) =
    LEFT JOIN LGC_EAM.UNICORE_PTTSKFLDP_YEAR  B  ON TRIM(A.TSK_NBR) = TRIM(B.TSK_NBR)
    WHERE A.PRT_M BETWEEN '2007-01' AND '2008-12'
    AND TRIM(PRD_NBR) =
    AND TRIM(EVT_NBR) =

    --银行卡业务文件
    SELECT *
    FROM LGC_EDW.ODS_C1C3_DCTRXDTAP_YEAR
    WHERE TRIM(TRXTRXCLT) =

    --2G数据：2013年6月以后的日志，柜面查询代码：1192
    --TOPIC：LU50_P_CFC_CIFCORDB_COR_M3LGTRSD_CDC_SZ
    --CIF客户信息维护事件
    --T05_CIF_CUST_INF_MNT_EVT      零售CIF客户信息维护事件
    --T05_COR_CIF_CUST_INF_MNT_EVT  对公CIF客户信息维护事件
    SELECT  EVT_ID 事件编号,DW_DAT_DT 数据日期,MNT_DTL_ID 维护明细编号,TRX_ID 交易编号,TRX_TYP_CD 交易类型代码,OPR_TYP_CD 操作类型代码,MNT_DTL_TYP_CD 维护明细类型代码,MNT_DTL_CNTNT 维护明细内容,SRC_SYS_CD 来源系统代码,CUST_ID 客户编号,HDL_USR_ID 办理用户号
        ,AUT_USR_ID 授权用户编号,BSN_DT 业务日期,TRX_HPN_TM 交易发生时间,TRX_INI_INI 交易原始发起方,TRX_LAST_INI 交易上一发起方,INI_CHNL_TYP_CD 发起方渠道类型代码,INI_INF 发起方信息,DW_ETL_DT 业务日期
    FROM PDM_VHIS.T05_CIF_CUST_INF_MNT_EVT  零售CIF客户信息维护事件
    WHERE CUST_ID = '99999';

    /*
    一些相关的码值映射
    操作类型代码  Opr_Typ_Cd  A:新增  C:确认  D:删除  E:失效  U:修改
    来源系统：  Src_Sys_Cd  零售客户平台  ADS:证券A  ADS2:证券A  AD3:维护模块  ADS:CMBRU  AE3:查询模块  API:400通用  AWF:工作流  BCC:信用卡  BCM:CVM系统  BFI:理财业务  BIB:中间业务  BNC:企业银行  BOP:金融市场系统  BPL:个人资  BPS:企业平台  BSW:SWIFT系统  CCT:CMBRU  CDB:远程银行  CMB:手机银行  CNA:网站应用  CNB:网上银行  CNF:通知平台  CNR:网银风控  COS:一事通系统  CPI:开放通用  CRV:可视柜台  CSS:自助终端  CWS:微信  SCC:信用卡同步  SCI:AS400同步  WAS:CIF
    */
    ```

    ---

    # 零售客户客户经理变更

    ## 零售客户客户经理变更

    - **场景描述**：因处理账户管理费定价，故需提取客户开户至今的客户经理。
    - **口径来源**：未提供/未提供
    - **结果字段**：未提供（待补充）
    - **限制字段**：
    - **注意事项**：未提供（待补充）
    - **SQL语句**

    ```sql
    select a.USR_NBR
    ,a.USR_REAL_NM
    ,a.MBL_NBR
    ,b.CM_ID  --客户经理编号
    ,c.BLG_CM  --客户经理编号
    from userid a
    inner join pdm_vhis.T01_CORE_IDV_CUST_INF_S b
    on a.doc_nbr=b.doc_nbr
    and b.dw_snsh_dt=current_date-1
    left join brtl_vhis.BRTL_SR_RTL_BBK_CUST_S c
    on b.cust_uid=c.cust_uid
    and c.dw_snsh_dt=current_date-1
    -- and c.BLG_FRS_BBK_ORG_ID='592' 限制只要某分行的客户经理

    select cust_id
    ,a.CUST_NM
    ,a.MVMT_TEL_NBR
    ,a.CM_ID  --客户经理编号
    ,b.BLG_CM  --客户经理编号
    from pdm_vhis.T01_CORE_IDV_CUST_INF_S a --核心个人客户信息快照
    left join brtl_vhis.BRTL_SR_RTL_BBK_CUST_S b --SR_零售分行客户快照
    on a.cust_uid=b.cust_uid
    and b.dw_snsh_dt=current_date-1
    where a.dw_snsh_dt=current_date-1
    and a.cust_id ='1241509932'

    select DW_START_DT    开始日期
    ,CUST_UID    客户UID
    ,BBK_ORG_ID  分行机构编号
    ,BLG_REL_TYP_CD  归属关系类型代码
    ,DW_END_DT   结束日期
    ,BLG_ORG_ID  归属机构编号
    ,BLG_CM  归属客户经理
    ,BLG_CM_PST_ID  归属客户经理岗位编号
    FROM BRTL_VHIS.BRTL_SR_CUST_BLG_REL_H --SR_客户归属关系历史
    WHERE CUST_UID ='99999'

    select *
    from BRTL_VIEW.BRTL_SR_CUST_BLG_CHG_LOG  --SR_客户归属变更日志流水  客户层面而不是账号层面
    where cust_uid='99999'

    select *
    FROM BRTL_VHIS.BRTL_SR_CUST_HDV_PATH_S --SR_客户移交轨迹快照
    WHERE cust_UID ='99999'
    ```

    # 其他原始口径文档

    ```sql
    文档 1
    /*
    NO2024040222435_南昌分行_记账式债券
    因江西人民银行要求数据报送所需，现申请提取3月及年度关于南昌分行个人投资者记账式债券相关数据，具体数据明细请见附件
    */
    SELECT T3.BLG_BK1_ORG_NM 一级分行名称,T3.BLG_BK2_ORG_NM 二级分行名称,T3.BLG_BBK_ORG_NM 基层分行名称,CASE WHEN INVT_PUB_BID_MTH_CD = 'Z'THEN '附息国债'
      WHEN INVT_PUB_BID_MTH_CD = 'B'THEN '地方债'
      WHEN INVT_PUB_BID_MTH_CD = 'P'THEN '国开债'
      ELSE '' END AS 种类 --B地方债，P国开债，Z附息国债
    ,SUM(T1.PST_BAL)  持仓金额     --汇总持仓金额
    FROM OBND_VHIS.OBND_CNT_BD_ACT_POS_INF_S T1 --柜台记账式债券账户持仓信息快照
    INNER JOIN MAS_VHIS.ORG_CORE_ORG_INF_S T3 --核心机构快照
      ON T3.ORG_ID = T1.BRN_ORG_ID
      AND T3.DW_SNSH_DT = CURRENT_DATE - 1
      AND T3.BLG_BK1_ORG_ID = '791'
    LEFT JOIN OBND_VHIS.OBND_CNT_BD_INF_S T2 --柜台记账式债券信息快照
      ON T1.TRX_OBJ_ID = T2.TRX_OBJ_ID --产品表
      AND T2.DW_SNSH_DT= CURRENT_DATE - 1
      AND T2.DW_SRC_SYS = 'LH07'
      AND TRIM(INVT_PUB_BID_MTH_CD) != ''
    WHERE T1.TRX_OBJ_ID LIKE 'LH07%'
    AND T1.DW_SNSH_DT = '2024-03-31'
    AND SUBSTRING(BND_CSTD_ACT_ID,1) < '3'   --第9位小于3表示零售
    GROUP BY 1,2,3,4
    ORDER BY 1,2,3,4
    ;

    SELECT
    T3.BLG_BK1_ORG_NM 一级分行名称,T3.BLG_BK2_ORG_NM 二级分行名称,T3.BLG_BBK_ORG_NM 基层分行名称,COUNT(*) AS 托管户个数
    FROM OBND_VHIS.OBND_BS_ARG_INF_S T1 --电子式储蓄国债合约信息快照
    INNER JOIN MAS_VHIS.ORG_CORE_ORG_INF_S T3 --核心机构快照
      ON T3.ORG_ID = T1.CPT_ACT_OPN_BBK_ORG_ID
      AND T3.DW_SNSH_DT = CURRENT_DATE - 1
      AND T3.BLG_BK1_ORG_ID = '791'
    WHERE T1.DW_SNSH_DT = '2024-03-31'
    AND BND_ACT_CHC_CTRL = 'CNT'
    AND ACT_STS_CD != '2303'
    GROUP BY 1,2,3
    ORDER BY 1,2,3,4
    ;

    SELECT
    T4.BLG_BK1_ORG_NM 一级分行名称,T4.BLG_BK2_ORG_NM 二级分行名称,T4.BLG_BBK_ORG_NM 基层分行名称
    ,CASE WHEN INVT_PUB_BID_MTH_CD = 'Z'THEN '附息国债'
      WHEN INVT_PUB_BID_MTH_CD = 'B'THEN '地方债'
      WHEN INVT_PUB_BID_MTH_CD = 'P'THEN '国开债'
      ELSE '' END AS 种类 --B地方债，P国开债，Z附息国债
    ,SUM(MNT_TRX_AMT) AS 成交规模
    ,COUNT(*) AS 成交笔数,
    SUM(CASE MNT_ANL_CD1 WHEN 'ISU' THEN MNT_TRX_AMT ELSE 0 END) AS 一级市场分销卖出
    FROM NDS_VHIS.NLH07_TZO_TRX_LOG_T T1  --交易日志文件
    INNER JOIN OBND_VHIS.OBND_BS_ARG_INF_S T2 --电子式储蓄国债合约信息快照
      ON T1.MNT_MNT_KE2 = T2.ARG_ID
      AND T2.DW_SNSH_DT = CURRENT_DATE - 1
    INNER JOIN MAS_VHIS.ORG_CORE_ORG_INF_S T4 --核心机构快照
      ON T4.ORG_ID = T2.CPT_ACT_OPN_BBK_ORG_ID
      AND T4.DW_SNSH_DT = CURRENT_DATE - 1
      AND T4.BLG_BK1_ORG_ID = '791'
    LEFT JOIN OBND_VHIS.OBND_CNT_BD_INF_S T3 --柜台记账式债券信息快照
      ON T1.MNT_OBJ_COD = T3.BND_CD --产品表
      AND T3.DW_SNSH_DT= CURRENT_DATE - 1
      AND T3.DW_SRC_SYS = 'LH07'
      AND TRIM(INVT_PUB_BID_MTH_CD) != ''
    WHERE T2.BND_ACT_CHC_CTRL = 'CNT'
    AND T1.DW_INS_DT = CURRENT_DATE
    AND MNT_TRX_COD IN ('BKBB','BKBS') AND MNT_RTN_COD = 'SUC9999'
    AND MNT_MNT_DAT >= '2024-03-01' AND MNT_MNT_DAT <= '2024-03-31'
    AND CUST_TYP_CD = 'CTB10'
    GROUP BY 1,2,3,4
    ORDER BY 1,2,3,4
    ;

    文档 2

    --- 脚本1 ： 汇入汇款特殊审批入账数据

    with LUO_TEMP01 as(
    SELECT
       A.TRD_COD
      ,A.BUS_COD
      ,A.TSK_CNTNT
      ,SUBSTR(ACTL_CPL_TM,10) ACTL_CPL_DT
      ,SPLIT(TSK_TIL,'\\(')[0] CUST_NM
      ,SPLIT(SPLIT(TSK_TIL,'\\(')[1],'\\)')[0] CUST_ID
     FROM ODS_BBK_577.LW91_F51_TSK_NEW A
    WHERE  TRD_COD='103525'
    and TSK_TYP='GJ_Esp_Aprv_Entr'
    )
    ,

    YUNYING_TEMP02 as  (
    SELECT
       A.BUS_CODE
       ,A.TASK_STATE
       ,A.BEGIN_ORGNO
       ,A.START_TIME
       ,A.END_TIME
       ,A.TRANS_ID
       ,A.CNY_NO
       ,A.AMOUNT
       ,A.TASK_ID  -- 业务实例号 ，主键
       ,A.FLOW_ID
       ,B.WORKITEMID   -- 流程实例号  B 表的主键
       ,B.FLOW_NODE  --节点名称 ，主键
       ,B.CHECKIN_TIME  --任务完成时间
       ,B.USER_NO -- 节点用户号
       ,ROW_NUMBER() OVER(PARTITION BY  A.TASK_ID,A.FLOW_ID ORDER BY B.CHECKIN_TIME ASC ) RN
    FROM ODS_BBK_577.LS05_BP_TRANSLIST_VW  A
    INNER JOIN ods_bbk_577.LS05_BP_OPER_DETAIL_VW B ON  A.BUS_CODE=B.BUS_CODE -- A.TASK_ID=B.TASK_ID AND A.FLOW_ID=B.FLOW_ID   --  不能用WORKITEMID关联，否则没数据  ，也不能用这两个实例号关联，否则会有数据
    WHERE A.TRANS_ID ='103525' AND A.TASK_STATE='5'
    )
    ,

    YUNYING_TEMP03 as (
    SELECT
       A.BUS_CODE
       ,A.TASK_STATE
       ,A.BEGIN_ORGNO
       ,A.START_TIME
       ,A.END_TIME
       ,A.TRANS_ID
       ,A.CNY_NO
       ,A.AMOUNT
       ,B.USER_NO AD3_USER_NO
       ,A.CHECKIN_TIME CHECKOUT_TIME1 -- 节点AD2（二期关键审核）完成日期
      ,B.CHECKIN_TIME CHECKOUT_TIME2   -- 节点AD3（二期审经办/网点经办（补充材料））完成日期
    FROM (
    SELECT
    *
    FROM NDS_BBK_577.YUNYING_TEMP02
    WHERE FLOW_NODE ='AD2')  A
    INNER  JOIN NDS_BBK_577.YUNYING_TEMP02 B  ON A.TASK_ID=B.TASK_ID AND A.FLOW_ID= B.FLOW_ID AND A.RN+1=B.RN AND B.FLOW_NODE='AD3'
    )

    SELECT
       A.BUS_CODE  `业务标识码`
       ,A.TASK_STATE `流程状态`
       ,A.BEGIN_ORGNO `业务发起机构`
       ,A.START_TIME `业务发起时间`
       ,A.END_TIME   `业务结束时间`
       ,substr(A.START_TIME,10) `业务发起日期`
       ,substr(A.END_TIME,10)   `业务结束日期`
       ,A.TRANS_ID `业务种类`
       ,concat(A.CNY_NO,'：',e.ccy_chn_nm)   `交易币种`
       ,A.AMOUNT `业务金额`
       ,A.CHECKOUT_TIME1   `节点AD2（二期关键审核经办）完成日期`
       ,A.CHECKOUT_TIME2  `节点AD3（关键审经办/网点经办（补充资料）网点经办（补充资料））完成日期`
       ,(UNIX_TIMESTAMP(A.CHECKOUT_TIME2)-UNIX_TIMESTAMP(CHECKOUT_TIME1))/3600  `各AD2与AD3节点时间差（以小时为单位）`
       ,B.TSK_CNTNT  `通知信息`
       ,A.AD3_USER_NO `AD3节点用户号`
       ,B.CUST_NM `客户姓名`
       ,B.CUST_ID  `客户号`
    FROM   YUNYING_TEMP03 A
    INNER JOIN LUO_TEMP01 B ON A.BUS_CODE=B.BUS_COD
    left join ods_bbk_577.PAM_CCY_INF_S  e on a.CNY_NO=e.ccy_cd
    ;
    ```
