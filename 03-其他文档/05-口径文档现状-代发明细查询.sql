/*
业务场景描述
====================
代发数据的查询需要先查询客户名下的代发协议号，通过代发协议号来查询对应的代发批次或代发明细。  
如果需要某一个客户的代发明细，就需要查询代发明细表；  
如果需要查询某一个代发批次的代发成功笔数或者代发金额，就可以查询代发批次表。

1、04年前柜面代发不可查，04年之后分行的代发数据逐步接入数仓，2004之前的代发明细数据(包括总行数仓查不到的历史代发)：可查询客户历史交易流水，去交易流水中的交易行查询纸质代发凭证；  
2、所有可用表：CMB03异地备机(零售)：EPCPRTRXP1 /EPCPRTRXP2/ EPHISTRXP1/EPHISTRXP2/PKTRSLOGP(零售)

业务负责人
====================
杨睿/80256937、卿宏伟/80303785

数据获取方法
====================
表名：NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T --协议出款户口表  
数据日期范围： 全量数据  
明细字段： PROTOCOL_NBR AS "合作方协议号", CARD_TYPE AS "户口类型", CARD_INTERNAL_NBR AS "户口内码", CARD_NBR AS "户口号", PAY_ACCT AS "出款账户号码", PAY_NAME AS "付款人简称", PAY_FULL_NAME AS "付款人全称", BASIC_ACCT_FLAG AS "基本账户标志", SPEC_CHAR AS "冗余CHAR数据2", SPEC_VARCHAR AS "冗余VARCHAR2数据2", SPEC_DECIMAL AS "冗余DECIMAL数据2", TECH_BUSINESS_ID AS "业务标识", TECH_TRACE_ID AS "链路标识", TECH_DB_NAME AS "数据库名", TECH_MNT_TIME AS "维护时间", TECH_VERSION AS "数据版本", TECH_RESV AS "预留字段", CLIENT_NBR AS "客户号", DW_ETL_DT AS "翻牌日期", DW_INS_DT AS "插入日期", DW_INS_TM AS "插入时间", DW_UPD_DT AS "更新日期", DW_UPD_TM AS "更新时间"  
限制字段：  
	CARD_NBR：代发协议号绑定的对公户口号，即出款户口号  
	PROTOCOL_NBR： 代发协议号

表名：PDM_VHIS.T03_SF_COOP_AGR_INF_S	--自助缴费合作方协议信息快照  
数据日期范围： 全量数据  
明细字段：DW_SNSH_DT AS "快照日期", AGR_ID AS "合作方协议编号", AGR_MDF AS "合作方协议修饰符", CUST_ID AS "客户编号", ORG_ID AS "机构编号", OPN_DT AS "开户日期", DSTR_ACT_DT AS "销户日期", EFT_DT AS "生效日期", MTU_DT AS "到期日期", AGR_STS_CD AS "协议状态代码", MTU_AUTO_PPN_IND AS "到期自动顺延标志", RCCL_STP_CD AS "对账步骤代码", RCCL_MTH_CD AS "对账方式代码", CTR_ID AS "合同编号", COOP_LMT_ENC AS "合作方额度编码", COOP_TRX_INI_MTH_CD AS "合作方交易发起方式代码", COOP_PVD_TRX_DAT_MTH_CD AS "合作方提供交易数据方式代码", COOP_PVD_TRX_DAT_ENCR_KEY_ENC AS "合作方提供交易数据加密KEY编码", PRD_ENC AS "PRD编码", CUST_EAC_SCP_CD AS "客户户口范围代码", CUST_EAC_DDCT_MTH_CD AS "客户户口扣款方式代码", CUST_TRX_CLS_CD AS "客户交易种类代码", CUST_NM_CHK_MTH_CD AS "客户名核对方式代码", COOP_CUST_ACT_ID AS "客户在合作方账户编号", ADVS_CUST_MTH_CD AS "通知客户方式代码", OWN_CUST_IND AS "我行客户标志", CLS_RSN AS "关闭原因", VTL_AGR_IND AS "虚拟协议标志", FND_TRX_DIR_CD AS "资金交易方向代码", PROD_NBR AS "产品实例号", CTC_NM AS "联系人姓名", CTC_TEL AS "联系电话", CUST_NED_SGN_AGR_IND AS "客户必须签署协议标志", MNT_DT AS "维护日期", BCH_PRT_ORG_ID AS "批量打印的机构编号", INT_AGR_IND AS "行内协议标志"  
限制字段：  
	CUST_ID: 代发协议号绑定的对公客户号  
	AGR_ID：代发协议号


关于代发明细数据，可以通过以下表查询，请注意每张表的数据日期范围：

表名： LGC_EAM.CORINFO_PKTRSLOGP_YEAR  
数据日期范围： 20030923-20041231,日数据最高94262，最低2,总数4013453  
明细字段：  
限制字段：

表名： LGC_EAM.EPHISTRXP1 --合作方交易历史文件  
数据日期范围： 20040707-20071231,日数据最高1080409，最低1,总数242024291  
明细字段：  
限制字段：

表名： LGC_EAM.EPHISTRXP2 --合作方交易历史文件  
数据日期范围： 20040707-20071230,日数据最高662698，最低1,总数100902921，与EPHISTRXP1数据有不同之处  
明细字段：  
限制字段：

表名： LGC_EAM.ETL_FE_EPHISTRXP --合作方交易历史文件  
数据日期范围： 20061001-20070929  
明细字段：  
限制字段：

表名： LGC_EAM.UNICORE_EPHISTRXP_YEAR_20220330--合作方交易历史文件  
数据日期范围： 20041213-20131129，确实12月的数据  
明细字段：  
限制字段：

表名： LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR  
数据日期范围： 20130101-20131231  
明细字段：  
限制字段：

表名： LITC_991176.TMP_LLJ_AGN_TRX_DATA_0308_RES_PRM  
数据日期范围： 20040707-20131205。灵活查询系统整理的代发数据，与UNICORE_EPHISTRXP_YEAR_20220330有不同之处  
明细字段：  
限制字段：
表名： PDM_VHIS.T05_AGN_DTL  
数据日期范围： 20140101-至今  
明细字段： EVT_ID AS "事件编号", AGN_BCH_SEQ AS "代发批次号", DTL_SEQ_NBR AS "明细序号", ORIG_EAC_NBR AS "原户口号码", EAC_NBR AS "户口号码", EAC_NM AS "户口名称", EAC_TYP_CD AS "户口类型代码", EAC_ACT_HST AS "户口账务主机", EAC_OPN_BNK_ORG_ID AS "户口开户行机构编号", EAC_OPN_BNK_NM AS "户口开户行名称", EAC_OPN_PLC_NM AS "户口开户地名称", EAC_SCP AS "户口范围", EAC_CHK_IND AS "户口检查标志", EAC_OPN_BNK_NBR AS "户口开户行号", EAC_SEQ_NBR AS "户口序号", CUST_ID AS "客户编号", ACT_ID AS "账户编号", ACT_CD AS "账户代码", CCY_CD AS "币种代码", CR_TYP_CD AS "钞汇类型代码", TRX_AMT AS "交易金额", RQS_TRX_SRL AS "请求交易流水", TRX_SEQ AS "交易流水号", TRX_SET AS "交易套号", BUS_CLS_CD AS "业务种类代码", AUTO_PAY_ARG_ID AS "自动缴费合约编号", AUTO_PAY_ARG_MDF AS "自动缴费合约修饰符", MCH_AGR_NBR AS "商户协议号", MCH_SRL_NBR AS "商户流水号", CRS_BNK_PAY_IND_NO AS "跨行支付标志码", CRS_BNK_PAY_ITF_CD AS "跨行支付接口代码", CRS_BNK_PAY_BUS_CLS AS "跨行支付业务种类", CRS_BNK_PAY_URG_IND AS "跨行支付加急标志", CRS_BNK_PAY_PRPS_CHNL_CD AS "跨行支付提出通道代码", TRX_TXT_CD AS "交易摘要代码", CUST_SMR AS "客户摘要", TRX_DT AS "交易日期", TRX_TM AS "交易时间", RCV_DT AS "接收日期", RCV_TM AS "接收时间", VAL_RAT_TM AS "起息时间", RVS_OR_SPL_ENTR_TYP_CD AS "冲补账类型代码", RCP_INST_NBR AS "回单实例号", AGN_STS_CD AS "代发状态代码", DEAL_RSL_ERR_NO AS "处理结果错误码", HDL_USR_ID AS "经办用户编号", CHK_USR_ID AS "复核用户编号", RCD_EDTN_TM AS "记录版本时间", RCD_EDTN_NBR AS "记录版本号", EAC_NM_ENCR_IND AS "户口名称加密标志", DATA_SRC_TAG AS "数据来源标志"  
限制字段：  
	AGN_BCH_SEQ：代发批次号  
	EAC_NBR：收款户口号，即员工户口号

*/

```sql
-- 查询示例

-- Step 1: 查询代发协议号

-- 方法 1：根据公司户口号查询代发协议号
SELECT PROTOCOL_NBR AS "合作方协议号", CARD_TYPE AS "户口类型", CARD_INTERNAL_NBR AS "户口内码", CARD_NBR AS "户口号", PAY_ACCT AS "出款账户号码", PAY_NAME AS "付款人简称", PAY_FULL_NAME AS "付款人全称", BASIC_ACCT_FLAG AS "基本账户标志", SPEC_CHAR AS "冗余CHAR数据2", SPEC_VARCHAR AS "冗余VARCHAR2数据2", SPEC_DECIMAL AS "冗余DECIMAL数据2", TECH_BUSINESS_ID AS "业务标识", TECH_TRACE_ID AS "链路标识", TECH_DB_NAME AS "数据库名", TECH_MNT_TIME AS "维护时间", TECH_VERSION AS "数据版本", TECH_RESV AS "预留字段", CLIENT_NBR AS "客户号", DW_ETL_DT AS "翻牌日期", DW_INS_DT AS "插入日期", DW_INS_TM AS "插入时间", DW_UPD_DT AS "更新日期", DW_UPD_TM AS "更新时间"
FROM NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T  --协议出款户口表
WHERE CARD_NBR IN ();


-- 方法 2：根据公司客户号查询代发协议号
SELECT DW_SNSH_DT AS "快照日期", AGR_ID AS "合作方协议编号", AGR_MDF AS "合作方协议修饰符", CUST_ID AS "客户编号", ORG_ID AS "机构编号", OPN_DT AS "开户日期", DSTR_ACT_DT AS "销户日期", EFT_DT AS "生效日期", MTU_DT AS "到期日期", AGR_STS_CD AS "协议状态代码", MTU_AUTO_PPN_IND AS "到期自动顺延标志", RCCL_STP_CD AS "对账步骤代码", RCCL_MTH_CD AS "对账方式代码", CTR_ID AS "合同编号", COOP_LMT_ENC AS "合作方额度编码", COOP_TRX_INI_MTH_CD AS "合作方交易发起方式代码", COOP_PVD_TRX_DAT_MTH_CD AS "合作方提供交易数据方式代码", COOP_PVD_TRX_DAT_ENCR_KEY_ENC AS "合作方提供交易数据加密KEY编码", PRD_ENC AS "PRD编码", CUST_EAC_SCP_CD AS "客户户口范围代码", CUST_EAC_DDCT_MTH_CD AS "客户户口扣款方式代码", CUST_TRX_CLS_CD AS "客户交易种类代码", CUST_NM_CHK_MTH_CD AS "客户名核对方式代码", COOP_CUST_ACT_ID AS "客户在合作方账户编号", ADVS_CUST_MTH_CD AS "通知客户方式代码", OWN_CUST_IND AS "我行客户标志", CLS_RSN AS "关闭原因", VTL_AGR_IND AS "虚拟协议标志", FND_TRX_DIR_CD AS "资金交易方向代码", PROD_NBR AS "产品实例号", CTC_NM AS "联系人姓名", CTC_TEL AS "联系电话", CUST_NED_SGN_AGR_IND AS "客户必须签署协议标志", MNT_DT AS "维护日期", BCH_PRT_ORG_ID AS "批量打印的机构编号", INT_AGR_IND AS "行内协议标志"
FROM PDM_VHIS.T03_SF_COOP_AGR_INF_S	--自助缴费合作方协议信息快照
WHERE DW_SNSH_DT= CURRENT_DATE -1
AND CUST_ID IN ('');

-- Step 2: 根据代发协议号查询代发明细

-- Step 3: 根据代发协议号查询代发批次

企业银行3.8 ，NBSADAP（零售客户入账清单）需关联NBSARQP（代发企业信息，该表历史库无数据）
去取代发企业名称
注 数据日期:19980413-20061124,日数据最高1234554，最低1,总数11964561，数据不连续，且SDEYDT 大部分为空
SELECT *
FROM LGC_EAM.ETL_FE_NBSADAP
WHERE SDEYDT > 20050721 AND SDEYDT < 20080501
AND TRIM(SDRBAC) = '';

--注 数据日期:20030923-20041231,日数据最高94262，最低2,总数4013453
SELECT *
FROM LGC_EAM.CORINFO_PKTRSLOGP_YEAR ;

--数据时间范围：20061001-20070929
SELECT EPXTRSKEY `事务键`,EPXTSKNBR `任务实例号`,EPXCMMRQS `通讯请求号`,EPXTRXSEQ `交易序号`,EPXCNVNBR `合作方协议号`,EPXBTHNBR `交易批次号`,EPXCPRREF `合作方流水号`,EPXEACFLG `他行户口标志`,EPXEACNBR `客户户口号`,EPXCLTNAM `客户名`,EPXCPRACT `客户在合作方帐号`,EPXTXTCOD `交易摘要码`,EPXTRXTXT `交易摘要`,EPXCCYNBR `交易货币`,EPXCCYTYP `钞汇标志`,EPXCSHTAG `现金标志`,EPXBOKDIR `记帐方向`,EPXTRXAMT `交易金额`,EPXCPRAMT `合作方要求的交易金额`,EPXTRXNBR `交易流水号`,EPXTRXSET `交易套号`,EPXRVSTAG `冲帐标志`,EPXTRXSTS `交易状态`,EPXERRCOD `处理结果错误码`,EPXTRXDAT `交易日期`,EPXCNVUNT `合作方清算单元`,EPXRCDVER `记录更新版本序号`,EPXSPLC40 `特殊码`,EPXRCDSTS `记录状态`
FROM LGC_EAM.ETL_FE_EPHISTRXP
GROUP BY EPXTRXDAT
ORDER BY EPXTRXDAT;


--注 数据日期:20040707-20071231,日数据最高1080409，最低1,总数242024291
SELECT EPXTRSKEY `事务键`,EPXTSKNBR `任务实例号`,EPXCMMRQS `通讯请求号`,EPXTRXSEQ `交易序号`,EPXCNVNBR `合作方协议号`,EPXBTHNBR `交易批次号`,EPXCPRREF `合作方流水号`,EPXEACFLG `他行户口标志`,EPXEACNBR `客户户口号`,EPXCLTNAM `客户名`,EPXCPRACT `客户在合作方帐号`,EPXTXTCOD `交易摘要码`,EPXTRXTXT `交易摘要`,EPXCCYNBR `交易货币`,EPXCCYTYP `钞汇标志`,EPXCSHTAG `现金标志`,EPXBOKDIR `记帐方向`,EPXTRXAMT `交易金额`,EPXCPRAMT `合作方要求的交易金额`,EPXTRXNBR `交易流水号`,EPXTRXSET `交易套号`,EPXRVSTAG `冲帐标志`,EPXTRXSTS `交易状态`,EPXERRCOD `处理结果错误码`,EPXTRXDAT `交易日期`,EPXCNVUNT `合作方清算单元`,EPXRCDVER `记录更新版本序号`,EPXSPLC40 `特殊码`,EPXRCDSTS `记录状态`
FROM LGC_EAM.EPHISTRXP1   --20040707至20081231 注意：数据不连续
WHERE TRIM(EPXEACNBR) = ''

--注 数据日期:20040707-20071230,日数据最高662698，最低1,总数100902921
SELECT EPXTRSKEY `事务键`,EPXTSKNBR `任务实例号`,EPXCMMRQS `通讯请求号`,EPXTRXSEQ `交易序号`,EPXCNVNBR `合作方协议号`,EPXBTHNBR `交易批次号`,EPXCPRREF `合作方流水号`,EPXEACFLG `他行户口标志`,EPXEACNBR `客户户口号`,EPXCLTNAM `客户名`,EPXCPRACT `客户在合作方帐号`,EPXTXTCOD `交易摘要码`,EPXTRXTXT `交易摘要`,EPXCCYNBR `交易货币`,EPXCCYTYP `钞汇标志`,EPXCSHTAG `现金标志`,EPXBOKDIR `记帐方向`,EPXTRXAMT `交易金额`,EPXCPRAMT `合作方要求的交易金额`,EPXTRXNBR `交易流水号`,EPXTRXSET `交易套号`,EPXRVSTAG `冲帐标志`,EPXTRXSTS `交易状态`,EPXERRCOD `处理结果错误码`,EPXTRXDAT `交易日期`,EPXCNVUNT `合作方清算单元`,EPXRCDVER `记录更新版本序号`,EPXSPLC40 `特殊码`,EPXRCDSTS `记录状态`
FROM LGC_EAM.EPHISTRXP2   20040707至20071231 注意：数据不连续
WHERE TRIM(EPXEACNBR) = '';

2009-2012
	SELECT
	TRX_DAT `交易日期`,TSK_NBR `任务实例号`,CMM_RQS `通讯请求号`,TRX_SEQ `交易序号`,CNV_NBR `合作方协议号`,BTH_NBR `交易批次号`,CPR_REF `合作方流水号` ,EAC_FLG `他行户口标志 `,EAC_NBR `客户户口号`,CLT_NAM `户口名`,CPR_ACT `客户在合作方账号`,TXT_COD `交易摘要码`,TRX_TXT `交易摘要`,CCY_NBR `交易货币`,CCY_TYP `钞汇标志`,CSH_TAG `现金标志`,BOK_DIR `记账方向`,TRX_AMT `交易金额`,CPR_AMT `合作方要求交易金额`,TRX_NBR `交易流水号`,TRX_SET `交易套号`,RVS_TAG `冲账标志`,TRX_STS `交易状态`,ERR_COD `处理结果错误码`,CNV_UNT `合作方清算单元`,RCD_VER `记录版本号`,SPL_C40 `特数码40`,RCD_STS `记录状态`
	FROM LGC_EAM.UNICORE_EPHISTRXP_YEAR --数据日期 20090101-20121231
	WHERE TRIM(CNV_NBR) IN ('','')   -- 协议号
	AND TRX_STS='S'
	AND TRX_DAT BETWEEN '2011-01-01' AND '2012-12-31'
	ORDER BY 1;

	2013-2014
	代发批次文件 LGC_EDW.ODS_C3A3_A2DPABTHP_YEAR
	户口文件	LGC_EDW.ODS_C3A3_A2DPABTHP_YEAR
	此数据可能缺数
	SELECT DTLBTHNBR `代发批次号码`,DTLBTHDTL	`明细序号`,DTLEACSRC	`原户口号码`,DTLEACNBR	`户口号码`,DTLEACNAM	`户口名称`,DTLEACTYP	`户口类型`,DTLEACHST	`户口账务主机`,DTLEACBNK	`户口开户行`,DTLEACBRD	`户口开户行号`,DTLEACBNM	`户口开户行名称`,DTLEACCTY	`户口开户地名称`,DTLEACRGN	`户口范围`,DTLNAMCHK	`户口检查标志`,DTLCLTNBR	`客户号码`,DTLIACNBR	`户口序号`,DTLACTNBR	`账户号码`,DTLACTCOD	`账户代码`,DTLCCYNBR	`货币号码`,DTLCCYTYP	`钞汇标志`,DTLTRSAMT	`交易金额`,DTLTRSSEQ	`交易流水`,DTLSETSEQ	`交易套号`,DTLBUSTYP	`业务种类`,DTLCPRCNV	`商户协议号`,DTLCPRREF	`商户流水号`,DTLKPSCOD	`跨行支付标识码`,DTLKPSINT	`跨行支付接口代码`,DTLKPSTYP	`跨行支付业务种类`,DTLKPSFST	`跨行支付加急标志`,DTLKPSPCH	`跨行支付提出通道`,DTLTXTC2G	`2G交易摘要代码`,DTLTXTCLT	`客户摘要`,DTLTRSDAT	`交易日期`,DTLTRSTIM	`交易时间`,DTLRCVDAT	`接收日期`,DTLRCVTIM	`接收时间`,DTLVALDAT	`起息时间`,DTLRVSTAG	`冲账标志`,DTLRRCIST	`回单实例号`,DTLSTSCOD	`状态`,DTLERRCOD	`处理结果错误码`,DTLOPRTLR	`经办柜员`,DTLCHKTLR	`复核柜员`,DTLSPC040	`冗余代码`,DTLRCDDAT	`记录最后更新日期`,DTLRCDVER	`记录最后更新版本`
	FROM LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR A   --2013年
	WHERE TRIM(A.DTLCPRCNV ) IN ('')
	AND TRIM(PRT_M )BETWEEN '2013-09'AND'2013-12'
	AND TRIM(DTLEACNBR) IN ('') ;
	20041213-20131129
	数据日期20041213-20131129 此数据可能缺数
	SELECT DISTINCT 	EPXTRSKEY `事务键`,EPXTSKNBR `任务实例号`,EPXCMMRQS `通讯请求号`,EPXTRXSEQ `交易序号`,EPXCNVNBR `合作方协议号`,EPXBTHNBR `交易批次号`,EPXCPRREF `合作方流水号`,EPXEACFLG `他行户口标志`,EPXEACNBR `客户户口号`,EPXCLTNAM `客户名`,EPXCPRACT `客户在合作方帐号`,EPXTXTCOD `交易摘要码`,EPXTRXTXT `交易摘要`,EPXCCYNBR `交易货币`,EPXCCYTYP `钞汇标志`,EPXCSHTAG `现金标志`,EPXBOKDIR `记帐方向`,EPXTRXAMT `交易金额`,EPXCPRAMT `合作方要求的交易金额`,EPXTRXNBR `交易流水号`,EPXTRXSET `交易套号`,EPXRVSTAG `冲帐标志`,EPXTRXSTS `交易状态`,EPXERRCOD `处理结果错误码`,EPXTRXDAT `交易日期`,EPXCNVUNT `合作方清算单元`,EPXRCDVER `记录更新版本序号`,EPXSPLC40 `特殊码`,EPXRCDSTS `记录状态`
	FROM LGC_EAM.UNICORE_EPHISTRXP_YEAR_20220330
	WHERE TRIM(EPXCNVNBR) = '199829'
	AND EPXTRXDAT BETWEEN '2013-06-01' AND '2013-06-30'
	ORDER BY EPXTRXDAT;
	利军提供 此数据可能缺数
	SELECT DISTINCT
	BCH_SEQ `批次号`,DTL_SEQ_NBR `批次序号`,EAC_NBR `卡号`,EAC_NM `姓名`,EAC_TYP_CD `客户类型`,CUST_UID `客户UID`,CUST_ID `客户号`,EAC_SEQ_NBR `户口序号`,OPN_ORG_ID `开户机构编号`,OPN_DOC_TYP_CD `开户证件类型`,OPN_DOC_NBR `开户证件号码`,OPP_CUST_ID `对手客户号`,OPP_EAC_TYP_CD `对手客户类型`,OPP_EAC_ID `对手户口号`,OPP_EAC_MDF `对手户口类型`,OPP_NM `对手名称`,OPP_OPN_BNK_NM `对手开户行`,CCY_CD `交易币种`,TRX_AMT `交易金额`,TRX_SEQ `交易流水号`,TRX_SET `交易套号`,BUS_CLS_CD `交易摘要`,MCH_AGR_NBR ,TRX_TXT_CD `交易摘要代码`,CUST_SMR `客户摘要`,TRX_DT `交易日期`,TRX_TM `交易时间`,RCV_DT `接收日期`,RCV_TM `接收时间`,STS_CD `交易状态`,EAC_OPN_BNK_NM `客户开户行`,BCH_CMT `批次摘要`
	FROM LITC_991176.TMP_LLJ_AGN_TRX_DATA_0308_RES_PRM	--数据日期 20040707-20131205
	WHERE TRX_DT BETWEEN '2006-09-27' AND '2013-12-31'
	AND TRIM(MCH_AGR_NBR) IN ('J97321','099425')
	ORDER BY TRX_DT;
2014-至今 代发明细
SELECT DISTINCT
EVT_ID 事件编号,AGN_BCH_SEQ 代发批次号,DTL_SEQ_NBR 明细序号,ORIG_EAC_NBR 原户口号码,EAC_NBR 户口号码,EAC_NM 户口名称,EAC_TYP_CD 户口类型代码,EAC_OPN_BNK_ORG_ID 户口开户行机构编号,EAC_OPN_BNK_NM 户口开户行名称,EAC_OPN_PLC_NM 户口开户地名称,EAC_SCP 户口范围,EAC_OPN_BNK_NBR 户口开户行号,CUST_ID 客户编号,CCY_CD 币种代码,TRX_AMT 交易金额,TRX_SEQ 交易流水号,TRX_SET 交易套号,BUS_CLS_CD 业务种类代码,AUTO_PAY_ARG_ID 自动缴费合约编号,AUTO_PAY_ARG_MDF 自动缴费合约修饰符,MCH_AGR_NBR 商户协议号,MCH_SRL_NBR 商户流水号,CRS_BNK_PAY_IND_NO 跨行支付标志码,CRS_BNK_PAY_ITF_CD 跨行支付接口代码,CRS_BNK_PAY_BUS_CLS 跨行支付业务种类,CRS_BNK_PAY_URG_IND 跨行支付加急标志,CRS_BNK_PAY_PRPS_CHNL_CD 跨行支付提出通道代码,TRX_TXT_CD 交易摘要代码,CUST_SMR 客户摘要,TRX_DT 交易日期,TRX_TM 交易时间,RCV_DT 接收日期,RCV_TM 接收时间,VAL_RAT_TM 起息时间,RVS_OR_SPL_ENTR_TYP_CD 冲补账类型代码,RCP_INST_NBR 回单实例号,AGN_STS_CD 代发状态代码,DEAL_RSL_ERR_NO 处理结果错误码,HDL_USR_ID 经办用户编号,CHK_USR_ID 复核用户编号
FROM PDM_VHIS.T05_AGN_DTL  --数据日期 20131224-至今
WHERE AUTO_PAY_ARG_ID IN ('99999')
AND TRX_DT BETWEEN '2020-01-01' AND  '2023-02-12'
ORDER BY TRX_DT;

SELECT EVT_ID AS "事件编号", AGN_BCH_SEQ_NBR AS "代发批次号", DTL_SEQ_NBR AS "明细序号", ORIG_EAC_ID AS "原户口编号", EAC_ID AS "户口编号", EAC_NM AS "户口名称", EAC_TYP_CD AS "户口类型代码", EAC_OPN_BNK_ORG_ID AS "户口开户行机构编号", EAC_OPN_BNK_NM AS "户口开户行名称", EAC_OPN_PLC_NM AS "户口开户地名称", EAC_SCP_CD AS "户口范围代码", EAC_OPN_BNK_ID AS "户口开户行编号", EAC_SEQ_NBR AS "户口序号", CUST_ID AS "客户编号", ACT_ID AS "账户编号", ACT_CD AS "账户代码", CCY_CD AS "币种代码", CR_TYP_CD AS "钞汇类型代码", TRX_AMT AS "交易金额", RQS_TRX_SRL_NBR AS "请求交易流水号", TRX_SRL_NBR AS "交易流水号", TRX_SET AS "交易套号", BUS_CLS_CD AS "业务种类代码", AUTO_PAY_ARG_ID AS "自动缴费合约编号", MCH_AGR_ID AS "商户协议编号", MCH_SRL_NBR AS "商户流水号", CRS_BNK_PAY_CD AS "跨行支付代码", CRS_BNK_PAY_ITF_CD AS "跨行支付接口代码", CRS_BNK_PAY_BUS_CLS_CD AS "跨行支付业务种类代码", CRS_BNK_PAY_URG_IND AS "跨行支付加急标志", TRX_TXT_CD AS "交易摘要代码", CUST_SMR_DSCR AS "客户摘要描述", TRX_DT AS "交易日期", TRX_TM AS "交易时间", RCV_DT AS "接收日期", RVS_OR_SPL_ENTR_TYP_CD AS "冲补账类型代码", AGN_STS_CD AS "代发状态代码", DEAL_RSL_ERR_CD AS "处理结果错误代码", HDL_USR_ID AS "经办用户编号", CHK_USR_ID AS "复核用户编号", RCD_EDTN_TM AS "记录版本时间", EAC_NM_ENCR_IND AS "户口名称加密标志"
FROM OAGN_VHIS.OAGN_AGN_DTL_EVT
WHERE AUTO_PAY_ARG_ID IN ('99999')
AND TRX_DT BETWEEN '2021-01-01' AND '2024-12-03'
order by TRX_DT
;

代发批次
SELECT  EVT_ID 事件编号,AUTO_PAY_ARG_ID 自动缴费合约编号,AUTO_PAY_ARG_MDF 自动缴费合约修饰符,AGN_BCH_SEQ 代发批次号,BCH_TYP_CD 批次类型代码,DAT_SRC 数据来源,ESTB_DT 建立日期,ESTB_TM 建立时间,DLV_TO_BNK_DT 送银行日期,BCH_CMT 批次说明,BUS_REF 业务参考号,MNG_ACT_PROD_ID 管理会计产品编号,BUS_KEY_VAL 业务键值,BUS_CLS_CD 业务种类代码,CHNL_BUS_CLS_CD 渠道业务种类代码,CRS_BNK_PAY_IND_NO 跨行支付标志码,CRS_BNK_PAY_ITF_CD 跨行支付接口代码,CRS_BNK_PAY_BUS_CLS 跨行支付业务种类,CRS_BNK_PAY_URG_IND 跨行支付加急标志,CRS_BNK_PAY_PRPS_CHNL_CD 跨行支付提出通道代码,PAY_AGR_NBR 付款人协议号,VTL_AGR_IND 虚拟协议标志,PAY_CUST_ID 付款人客户编号,PAY_EAC_TYP_CD 付款人户口类型代码,PAY_EAC_ID 付款人户口编号,PAY_EAC_MDF 付款人户口修饰符,PAY_NM 付款人名称,CUST_SHT_NM 客户简称,PAY_OPN_BNK_NM 付款人开户行名称,PMT_MTH_CD 付款方式代码,FEE_CPT_EAC_ID 付费资金户口编号,FEE_CPT_EAC_MDF 付费资金户口修饰符,PMT_VCH_CLS_CD 付款凭证种类代码,PMT_VCH_ID 付款凭证编号,PMT_VCH_AMT 付款凭证金额,PMT_VCH_DT 付款凭证日期,PMT_CPT_EAC_NBR 付款资金户口号,RFND_MTH_CD 退款方式代码,RFND_CPT_EAC_NBR 退款资金户口号,RFND_AMT 退款金额,FEE_IND 付费标志,BUS_FEE_FEE_MTH_CD 业务费用计费方式代码,BUS_FEE_DCNT_RAT 业务费用折扣率,BUS_FEE_CHRG_ITM_ID 业务费用收费项目编号,CRS_BNK_FEE_FEE_MTH_CD 跨行费用计费方式代码,CRS_BNK_FEE_DCNT_RAT 跨行费用折扣率,SCCB_CHRG_ITM_ID 跨行同城收费项目编号,CRS_BNK_RMT_CHRG_ITM_ID 跨行异地收费项目编号,PAY_MTH_CD 付费方式代码,FEE_AMT 费用金额,BUS_FEE 业务费用,SCCB_FEE 跨行同城费用,CRS_BNK_RMT_FEE 跨行异地费用,BBK_ORG_ID 分行机构编号,ORG_ID 机构编号,CCY_CD 币种代码,CR_TYP_CD 钞汇类型代码,INT_ACT_BOK_ENTR_VCH_ID 内部会计记账凭证编号,TOT_CNT 总笔数,TOT_AMT 总金额,SUC_CNT 成功笔数,SUC_AMT 成功金额,ACTL_SUC_CNT 实际成功笔数（剔除退票）,ACTL_SUC_AMT 实际成功金额（剔除退票）,CHK_SUC_CNT 检查成功笔数,CHK_SUC_AMT 检查成功金额,RCV_CNT 已接收笔数,RCV_AMT 已接收金额,THS_TM_CNT 本次笔数,THS_TM_AMT 本次金额,RCV_TMS 已接收次数,TOT_TMS 总次数,RCV_DT 接收日期,RCV_TM 接收时间,CRSP_BCH_BCH_SEQ 对接批量批次号码,CRSP_BCH_ACT_VCH_ID 对接批量会计凭证编号,SCCB_TOT_CNT 他行同城总笔数,SCCB_TOT_AMT 他行同城总金额,SCCB_SUC_CNT 他行同城成功笔数,SCCB_SUC_AMT 他行同城成功金额,OTH_BNK_RMT_TOT_CNT 他行异地总笔数,OTH_BNK_RMT_TOT_AMT 他行异地总金额,OTH_BNK_RMT_SUC_CNT 他行异地成功笔数,OTH_BNK_RMT_SUC_AMT 他行异地成功金额,RCV_PER_STRK_MAX_AMT 收款人每笔最大金额,WTHR_CHK_RCV_NM_IND 是否核对收款人名称代码,SND_ADVS_INF_IND 发送通知信息标志,RCV_INT_EAC_SCP 收款人行内户口范围,RCV_EXT_EAC_SCP 收款人行外户口范围,TRX_TXT_CD 交易摘要代码,CPL_DT 完成日期,CPL_TM 完成时间,ASYN_JOB_SRL_NBR_CHK 异步作业流水号CHK,ASYN_JOB_SRL_NBR_00 异步作业流水号00,ASYN_JOB_SRL_NBR_10 异步作业流水号10,ASYN_JOB_SRL_NBR_30 异步作业流水号30,ASYN_JOB_SRL_NBR_40 异步作业流水号40,HDL_USR_ID 经办用户编号,CHK_USR_ID 复核用户编号,AUT_USR_ID 授权用户编号,BUS_STS_CD 业务状态代码,AGN_EXE_STS_CD 代发执行状态代码,AGN_STS_CD 代发状态代码,RDD_NO 冗余码,RCD_EDTN_TM 记录版本时间,RCD_EDTN_NBR 记录版本号,CTG_ACT_NBR 分类账户编码,DAT_SRC_IND 数据来源标志
FROM PDM_VHIS.T05_AGN_BCH_EVT
WHERE AUTO_PAY_ARG_ID = 'EE7098'
AND AGN_BCH_SEQ IN ('40011L33ZE')  --代发批次

代发增值税
SELECT BATCH_MAIN_NBR 批次号,FEE_AMOUNT 总费用,ADD_VAL_AMOUNT 增值税金额,(FEE_AMOUNT-ADD_VAL_AMOUNT) 实际费用
FROM NDS_VHIS.NLJ54_AGF_FEE_STD_T  --收费信息表-标准业务
WHERE BATCH_MAIN_NBR IN(
	SELECT  DISTINCT AGN_BCH_SEQ
	FROM PDM_VHIS.T05_AGN_BCH_EVT
	WHERE AUTO_PAY_ARG_ID = '642828'
	AND ESTB_DT BETWEEN '2023-01-01' AND '2023-05-05' 代发批次
)
```