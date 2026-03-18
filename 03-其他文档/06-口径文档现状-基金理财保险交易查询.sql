找原系统要口径时，一定要确定是LK02还是LW24，两个系统的表名一样，但是前者是基金，后者是理财

1、背景知识：

理财产品一般分为基金、理财、保险、信托部分。
	我们一般说的基金可以认为是代销基金，有基金管理公司发行，银行代为销售的基金产品；
		按照合作方不同可以分为专户基金、信托基金、券商集合基金，按投资对象不同又可以分在股票基金、货币基金、债券类基金等
	理财：分为受托理财(又叫自营理财)，代销理财(SA理财)
	信托：分为信托产品(基金等)、家族信托。其中家族信托不在核心系统中，有一个专门的家族信托系统，相关明细数据需要找该系统开发获取

具体产品：
可以参考wiki杂货铺中的 财富类 代销基金业务  理财总结里，有相关产品名称
	基金  	朝朝盈、月月盈等
	SA理财  招银理财聚宝盆1/2/3/4/5号   朝朝宝需要单独查询
	受托理财  招银理财招赢开鑫宝A/ 朝招金（多元积极型）/聚益生金35天A款/ 朝招金（多元进取型）


2、注意事项：
	1.历史原因，部分受托理财会迁移到SA理财，因此，如果一个产品，业务说是某个理财，结果在该理财表里查不到，可以去另一个理财里查询试试。
	2. 代销理财和代销基金不是同一个东西，代销理财是2020年8月份开始的，因此在SA表里只可以查到20年以后的数据，代销基金就是我们常说的基金了
		而代销产品，包含了代销理财与代销基金
	3. 收益，不同的情况不一样，一般不查，如果非要查询，需要找开发进行讨论确认对应情况的口径与限定条件
	4. 如果要查询该客户的所有历史交易，需要查询客户的开户时间，再确认要不要去历史库或者找开发老师查询
	5. 重要：一般来说，我们会根据客户的aum里存在的产品，去查相关交易，但是这个也不完全准确，如果是久远的数据，可能会漏掉。
			比如，对于客户***********，查最近以及最早(14年6月)的aum都是只有获取存款，
			但是此客户在13年购买了三千万的受托理财，在14年初赎回了，因此在后建的aum表里没有体现出此产品，如果不去查询，可能就忽略掉了


3、查询口径
因财富产品查询涉及产品多、时间跨度大，对于 “开户以来” 的需求，数据不全的可能性比较大，
此时就需要跟分行沟通说明，数据查不全的时候，能否中只查一部分确保数据准确的数据；



一、获取客户基本信息
以下所有的财富产品交易，基本都从这个临时表里出

drop table if exists lc_tmp;
create temporary table lc_tmp         
WITH (orientation = column, colversion = 2.0, compression = middle)
distribute by hash (eac_id)
AS (
	select eac_id,IAC_Id, eac_nm as cust_nm,cust_id
	from MAS_DATA.EAC_RTL_CUST_EAC_INF_S 
	where Dw_Snsh_Dt = current_date-1
	and iac_id in ('99999999')
	and cust_id not like 'CRD%'
)
WITH DATA;


一、基金
口径联系人：基金定投  刘俊金 ，常见基金申赎 龚芬

1.1 基金申购、赎回记录  
分行经常将 购买申请日期、 资金变动日期、基金份额交易日期等日期整混淆，因此分行给的日期可能不太准；
一般来说，都是查询申请日期，就用下面的代码  所以如果查不到的时候，可以稍微往前往后扩大时间范围做查询；

注意，这里只有申购赎回记录，不包含分红 资金变动日期；如果需要上面日期就需要去基金事件交易表查询了。
这里的申请日期，就是真正的客户申请日期


重构后的表
--高斯A使用这个表获取基金产品记录  NLK02_TFE_FND_DEF_T
--申购赎回记录(上面是获取交易记录，比如资金购买、分红等，但是不包含申请时间与日期)，这个包含申请时间
select 
	h.cust_nm 				as 账户名
	,b.eac_id  				as 账户编号
	,b.TRX_OBJ_ID 		as 交易标的编号
	,cast(b.ESTB_TM as date)	申请日期
	,cast(b.ESTB_TM as TIME)	申请时间
	,c.FND_FND_INN				as 基金代码
	,c.FND_FUL_NAM    		as 产品中文名称
	--,c.FND_PROD_TYP_NM 		as 基金产品类型名称
	,c.FND_CRP_MID		as 基金管理公司名称
	,c.FND_CRP_COD		as 基金管理公司代码
	,b.ENTR_AMT				as 委托金额
	,b.ENTR_LOT				as 委托份额
	,b.FND_TRX_AMT 			as 资金交易金额
	,b.FEE_AMT				as 费用金额
	,case TRX_CD 
		when 'FDE1'	then '签署约定书' when 'FDE2'	then '撤销约定书' when 'FDE3'	then '签电子合同' when 'FDE4'	then '签纸质合同' when 'FD01'	then '开户' when 'FD02'	then '关户' when 'FD03'	then '修改资料' when 'FD04'	then '账户冻结' when 'FD05'	then '账户解冻' when 'FD06'	then '挂失' when 'FD07'	then '解挂' when 'FD08'	then '增加交易账号' when 'FD09'	then '撤销交易账号' when 'FD2D'	then '认购利息' when 'FD2F'	then '预约购买' when 'FD20'	then '认购' when 'FD22'	then '申购' when 'FD24'	then '赎回' when 'FD26'	then '转托管' when 'FD27'	then '持仓转入' when 'FD28'	then '持仓转出' when 'FD29'	then '修改分红方式' when 'FD30'	then '认购结果' when 'FD31'	then '基金份额冻结' when 'FD32'	then '基金份额解冻' when 'FD34'	then '非交易过户转入' when 'FD35'	then '非交易过户转出' when 'FD36'	then '基金转换' when 'FD37'	then '基金转换转入' when 'FD38'	then '基金转换转出' when 'FD39'	then '定时定额投资' when 'FD42'	then '强行赎回' when 'FD43'	then '红利' when 'FD44'	then '强行调增' when 'FD45'	then '强行调减' when 'FD49'	then '募集失败' when 'FD5A'	then '定期赎回申请' when 'FD5B'	then '条件申购申请' when 'FD5C'	then '条件赎回申请' when 'FD5D'	then '条件计划撤回' when 'FD50'	then '基金清盘' when 'FD52'	then '撤单' when 'FD59'	then '定期定额申请' when 'FD60'	then '定期定额撤销' when 'FD61'	then '定期定额修改' when 'FD98'	then '快速赎回' when 'FDLB'	then '受益权受让' when 'FDLS'	then '受益权出让'
		else TRX_CD
		end 				as 投资理财交易代码
	,case b.TRX_CHNL_TYP_CD
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		when 'RVC' then '可视柜台'
		else b.TRX_CHNL_TYP_CD 
		end as 交易渠道
		,TRX_BBK_ORG_ID 交易分行机构编号
	,TRX_ORG_ID 交易机构编号
	,coalesce(
		if(d.INT_RCM_PSN_1_NBR = '',NULL,d.INT_RCM_PSN_1_NBR),
		if(d.INT_RCM_PSN_2_NBR = '',NULL,d.INT_RCM_PSN_2_NBR),
		if(d.INT_RCM_PSN_3_NBR = '',NULL,d.INT_RCM_PSN_3_NBR),
		if(d.EXT_RCM_PSN_NBR = '',NULL,d.EXT_RCM_PSN_NBR),
		if(d.SLF_RCM_PSN_NBR = '',NULL,d.SLF_RCM_PSN_NBR),
		--基金定投推荐人需要使用定投协议表关联推荐人表
		if(n.INT_RCM_PSN_1_NBR = '',NULL,n.INT_RCM_PSN_1_NBR),
		if(n.INT_RCM_PSN_2_NBR = '',NULL,n.INT_RCM_PSN_2_NBR),
		if(n.INT_RCM_PSN_3_NBR = '',NULL,n.INT_RCM_PSN_3_NBR),
		if(n.EXT_RCM_PSN_NBR = '',NULL,n.EXT_RCM_PSN_NBR),
		if(n.SLF_RCM_PSN_NBR = '',NULL,n.SLF_RCM_PSN_NBR),
		''
		) as 推荐人 --推荐人编号, 这里推荐人都是空值，就暂时不获取推荐机构编号数据了
	,substring(c.FND_FND_TYP, 5,1)  			as 风险等级
	, ANA_NO 分析码
	, Mth_Ctrl 方式控制   --TOD_CTL_MOD
from OFND_DATA.OFND_FND_TRX_APL_EVT b
inner join lc_tmp  h
	on b.eac_id = h.eac_id


left join NDS_VHIS.NLK02_TFE_FND_DEF_T c --补充到原始口径，没有关联条件，需要再问问看
	on b.TRX_OBJ_ID= 'LK02'||c.FND_FND_INN||c.FND_SAA_COD
	--and substring(c.FND_FND_TYP, 2,1) in ('A','B')  公募或私募由业务自行判断，类型可以看 孙晓梦的聊天记录
left join NDS_VHIS.NLK02_TFD_JJC_DTA_T m
	on b.Rel_Apl_Id = m.JJC_PDC_NBR
	and (m.JJC_PDC_NBR <> '' and m.JJC_PDC_NBR is not null)
left join OFND_DATA.OFND_FND_RCM_INF n
	on m.JJC_PTN_TAG || m.JJC_JJC_SEQ = n.RCM_SRL_NBR
left join OFND_DATA.OFND_FND_RCM_INF d
	on b.MSG_APL_ID = d.RCM_SRL_NBR
where cast(b.ESTB_TM as date) between '2021-07-06' AND '2021-07-08'
--and substring(b.TRX_OBJ_ID,5,6) in (xxxx) 
--and TRX_CD in ('FD20','FD22','FD39')   申购记录，包含定投非定投
--and TRX_CD in ('FD20','FD22') and substring(Mth_Ctrl,1,1) not in ('0','6') --基金申购数据  --,'FD52'沟通后，剔除撤单数据
--and TRX_CD in ('FD22','FD39') and substring(Mth_Ctrl,1,1) in ('0','6')   --刘俊金提供口径，定投产品申购交易数据


高斯分析集群数据,与重构后数据一致
select 
	eac_id
	,b.Msg_Apl_Id 合同编号
	,c.FND_FND_INN				as 基金代码
	,c.FND_FUL_NAM    		as 产品中文名称
	,substring(c.FND_FND_TYP, 5,1) as 风险等级代码
	,c.FND_CRP_MID 基金公司名称
	,case TRX_CD 
		when 'FDE1'	then '签署约定书' when 'FDE2'	then '撤销约定书' when 'FDE3'	then '签电子合同' when 'FDE4'	then '签纸质合同' when 'FD01'	then '开户' when 'FD02'	then '关户' when 'FD03'	then '修改资料' when 'FD04'	then '账户冻结' when 'FD05'	then '账户解冻' when 'FD06'	then '挂失' when 'FD07'	then '解挂' when 'FD08'	then '增加交易账号' when 'FD09'	then '撤销交易账号' when 'FD2D'	then '认购利息' when 'FD2F'	then '预约购买' when 'FD20'	then '认购' when 'FD22'	then '申购' when 'FD24'	then '赎回' when 'FD26'	then '转托管' when 'FD27'	then '持仓转入' when 'FD28'	then '持仓转出' when 'FD29'	then '修改分红方式' when 'FD30'	then '认购结果' when 'FD31'	then '基金份额冻结' when 'FD32'	then '基金份额解冻' when 'FD34'	then '非交易过户转入' when 'FD35'	then '非交易过户转出' when 'FD36'	then '基金转换' when 'FD37'	then '基金转换转入' when 'FD38'	then '基金转换转出' when 'FD39'	then '定时定额投资' when 'FD42'	then '强行赎回' when 'FD43'	then '红利' when 'FD44'	then '强行调增' when 'FD45'	then '强行调减' when 'FD49'	then '募集失败' when 'FD5A'	then '定期赎回申请' when 'FD5B'	then '条件申购申请' when 'FD5C'	then '条件赎回申请' when 'FD5D'	then '条件计划撤回' when 'FD50'	then '基金清盘' when 'FD52'	then '撤单' when 'FD59'	then '定期定额申请' when 'FD60'	then '定期定额撤销' when 'FD61'	then '定期定额修改' when 'FD98'	then '快速赎回' when 'FDLB'	then '受益权受让' when 'FDLS'	then '受益权出让'
		else TRX_CD
		end 	as 交易类别
	,b.DT	交易日期
	,b.TM	交易时间
	,b.ENTR_AMT				as 委托金额
	,case b.TRX_CHNL_TYP_CD 
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		when 'RVC' then '可视柜台'
		else b.TRX_CHNL_TYP_CD 
		end as 交易渠道
	,TRX_ORG_ID  交易机构编号
	,coalesce(
		if(d.INT_RCM_PSN_1_NBR = '',NULL,d.INT_RCM_PSN_1_NBR),
		if(d.INT_RCM_PSN_2_NBR = '',NULL,d.INT_RCM_PSN_2_NBR),
		if(d.INT_RCM_PSN_3_NBR = '',NULL,d.INT_RCM_PSN_3_NBR),
		if(d.EXT_RCM_PSN_NBR = '',NULL,d.EXT_RCM_PSN_NBR),
		if(d.SLF_RCM_PSN_NBR = '',NULL,d.SLF_RCM_PSN_NBR),
		--基金定投推荐人需要使用定投协议表关联推荐人表
		if(n.INT_RCM_PSN_1_NBR = '',NULL,n.INT_RCM_PSN_1_NBR),
		if(n.INT_RCM_PSN_2_NBR = '',NULL,n.INT_RCM_PSN_2_NBR),
		if(n.INT_RCM_PSN_3_NBR = '',NULL,n.INT_RCM_PSN_3_NBR),
		if(n.EXT_RCM_PSN_NBR = '',NULL,n.EXT_RCM_PSN_NBR),
		if(n.SLF_RCM_PSN_NBR = '',NULL,n.SLF_RCM_PSN_NBR),
		''
		) as 推荐人编号
from pdm_data_src.T05_FND_TRX_APL_EVT b
left join NDS_data_src.NLK02_TFE_FND_DEF_T c --补充到原始口径，没有关联条件，需要再问问看
	on b.TRX_OBJ_ID= 'LK02'||c.FND_FND_INN||c.FND_SAA_COD
left join NDS_data_src.NLK02_TFD_JJC_DTA_T m
	on b.Rel_Apl_Id = m.JJC_PDC_NBR
	and (m.JJC_PDC_NBR <> '' and m.JJC_PDC_NBR is not null)
left join pdm_view_src.t03_fnd_rcm_inf n
	on m.JJC_PTN_TAG || m.JJC_JJC_SEQ = n.RCM_SRL
left join pdm_view_src.t03_fnd_rcm_inf d
	on b.MSG_APL_ID = d.RCM_SRL
where (b.DT between '2020-11-01' and '2021-03-31' or b.ENTR_DT between '2020-11-01' and '2021-03-31')
and b.eac_id in ('9999','9999')


历史库口径  待补充

1.2 基金资金交易、分红记录
注意：这个表里不记录申请日期，不过有全量的申购 赎回 分红 转换等类型的交易；
重构前数据
select 
	h.cust_nm 				as 账户名
	,b.eac_id  				as 账户编号
	,b.TRX_OBJ_ID 		as 交易标的编号
	,c.FND_FND_INN				as 基金代码
	,c.FND_FUL_NAM    		as 产品中文名称
	--,c.FND_PROD_TYP_NM 		as 基金产品类型名称
	,c.FND_CRP_MID		as 基金管理公司名称
	,c.FND_CRP_COD		as 基金管理公司代码
	,b.ENTR_DT 				as 委托日期
	,b.FND_TRX_AMT			as 资金交易金额
	,b.FND_TRX_DT			as 资金交易日期
	,b.FND_CLR_DT			as 资金清算日期
	,b.BARG_LOT				as 成交份额
	,b.BARG_AMT				as 成交金额
	,b.TRD_PRC				as 成交价格
	,b.FND_NAV				as 基金净值
	,b.NAV_DT				as 净值日期
	,b.DT					as 数据日期
	--,b.RCM_PSN_ID			as 推荐人编号1  --这个字段和推荐人无关，段祥越
	,coalesce(
		if(d.INT_RCM_PSN_1_NBR = '',NULL,d.INT_RCM_PSN_1_NBR),
		if(d.INT_RCM_PSN_2_NBR = '',NULL,d.INT_RCM_PSN_2_NBR),
		if(d.INT_RCM_PSN_3_NBR = '',NULL,d.INT_RCM_PSN_3_NBR),
		if(d.EXT_RCM_PSN_NBR = '',NULL,d.EXT_RCM_PSN_NBR),
		if(d.SLF_RCM_PSN_NBR = '',NULL,d.SLF_RCM_PSN_NBR),
		''
		)as 推荐人编号
	,substring(c.FND_FND_TYP, 5,1)  			as 风险等级
	,b.Fee_Amt_1 			as 手续费
	,case TRX_CD 
		when 'FDE1'	then '签署约定书' when 'FDE2'	then '撤销约定书' when 'FDE3'	then '签电子合同' when 'FDE4'	then '签纸质合同' when 'FD01'	then '开户' when 'FD02'	then '关户' when 'FD03'	then '修改资料' when 'FD04'	then '账户冻结' when 'FD05'	then '账户解冻' when 'FD06'	then '挂失' when 'FD07'	then '解挂' when 'FD08'	then '增加交易账号' when 'FD09'	then '撤销交易账号' when 'FD2D'	then '认购利息' when 'FD2F'	then '预约购买' when 'FD20'	then '认购' when 'FD22'	then '申购' when 'FD24'	then '赎回' when 'FD26'	then '转托管' when 'FD27'	then '持仓转入' when 'FD28'	then '持仓转出' when 'FD29'	then '修改分红方式' when 'FD30'	then '认购结果' when 'FD31'	then '基金份额冻结' when 'FD32'	then '基金份额解冻' when 'FD34'	then '非交易过户转入' when 'FD35'	then '非交易过户转出' when 'FD36'	then '基金转换' when 'FD37'	then '基金转换转入' when 'FD38'	then '基金转换转出' when 'FD39'	then '定时定额投资' when 'FD42'	then '强行赎回' when 'FD43'	then '红利' when 'FD44'	then '强行调增' when 'FD45'	then '强行调减' when 'FD49'	then '募集失败' when 'FD5A'	then '定期赎回申请' when 'FD5B'	then '条件申购申请' when 'FD5C'	then '条件赎回申请' when 'FD5D'	then '条件计划撤回' when 'FD50'	then '基金清盘' when 'FD52'	then '撤单' when 'FD59'	then '定期定额申请' when 'FD60'	then '定期定额撤销' when 'FD61'	then '定期定额修改' when 'FD98'	then '快速赎回' when 'FDLB'	then '受益权受让' when 'FDLS'	then '受益权出让'
		else TRX_CD
		end 				as 投资理财交易代码
	,case b.TRX_CHNL_TYP_CD
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		when 'RVC' then '可视柜台'
		else b.TRX_CHNL_TYP_CD 
		end as 交易渠道
	,TRX_BBK_ORG_ID 交易分行机构编号
	,TRX_ORG_ID 交易机构编号
	, ANA_NO 分析码
	, Mth_Ctrl 方式控制   --TOD_CTL_MOD
from pdm_vhis.T05_FND_TRX_RPLY_EVT b  --14年以来数据 ，尽量用这个表，比sum_vhis.T85_FND_TRX_RPLY_EVT 表要全一些 ,也可以关联FND_TRX_RPLY_EVT_ID = EVT_ID获取一些字段
inner join lc_tmp  h
	on b.eac_id = h.eac_id
left join NDS_VHIS.NLK02_TFE_FND_DEF_T c --补充到原始口径，没有关联条件，需要再问问看
	on b.TRX_OBJ_ID= 'LK02'||c.FND_FND_INN||c.FND_SAA_COD
left join pdm_vhis.t03_fnd_rcm_inf d
	on b.MSG_APL_ID = d.RCM_SRL
--where (b.DT between '2021-01-01' and '2021-02-28' or b.ENTR_DT between '2021-01-01' and '2021-02-28')
--注意，这里的DT指的是所有的 交易类型&资金金额变动 发生的日期，比如分红交易的委托日期是00010101，再比如我在9月1日手机上申请购买基金，那么实际上可能DT是9月2日资金变动
--注意，这里的ENTR_DT指的是申购日期，比如客户想要查询某个日期的基金交易，那么一般都要使用这个日期去过滤，因为客户手机上显示的就是申购日期，而不是资金变动日期DT

1.3  历史库数据
对于历史库的数据，因为时间比较久，所以一般不太需要区分申请日期与资金变动日期，把历史交易查出来就好了。
一般，金融平台加银证通的数据，就是最全的了，不过还可能缺少12年及以前的数据，对于12年以前的数据，需要跟分行说明，可能数据有缺失，需知悉；
如果非要12年以前的数据，可以联系龚芬 叶文涛获取


离线表建实验室表

drop table if exists litc_992475.cust_inf;
create table litc_992475.cust_inf
(
	`eac_id`  STRING COMMENT  'eac_id'
	,`IAC_Id`  STRING COMMENT  'IAC_Id'
	,`cust_nm`  STRING COMMENT  'cust_nm'
	,`cust_id`  STRING COMMENT  'cust_id'
)
COMMENT  'cust_inf' STORED AS Parquet;

insert into litc_992475.cust_inf values ('***********', '***********EAP00001', '董小云', '***********'),('***********', '***********EAP00001', '董小云', '***********')

--金融平台,会与高斯基金有重合的地方，需要注意过滤掉重合的日期，max 2016-10-08	min 2013-06-22?
SELECT 
	"金融平台"  `交易平台`
	,h.*
	,a.TRX_DTE `交易日期`
	,a.CLT_NBR `客户号`
	,a.EAC_NBR `户口号`
	,b.PRD_CNM `产品中文名称`
	,a.CCY_NBR `币种`
	,a.TRX_CNL `交易渠道`
	,a.TRX_BRN `交易机构`
	,a.ITR_COD `推荐人编号`
	,a.ITR_BRN `推荐人机构`
	,a.OPR_COD `操作人编号`
	,a.OPR_BRN `操作人机构`
	,case trim(a.FTR_COD)
		when 'FD42'	then '强赎'
		when 'FD22'	then '申购'
		when 'FD43'	then '分红'
		when 'FD24'	then '赎回'
		when 'FD30'	then '认购确认'
		when 'FD98'	then '快赎'
		when 'FD20'	then '认购'
		when 'FD39'	then '定投'
		when 'FD44'	then '强行调增'
		when 'FD45'	then '强行调减'
		when 'FD38'	then '基金转换转出'
		when 'FD37'	then '基金转换转入'
		else a.FTR_COD 
		end `投资理财交易代码`
	,a.TRX_AMT `交易金额`
	,a.TRX_QTY `交易份额`
	,b.PRL_COD `产品线代码`
	,b.FPR_COD `产品代码`
	,c.RSK_GRD  `风险等级`
	,c.CRP_NAM	`基金管理公司名称`
	,c.CRP_COD	`基金管理公司代码`
	--(select PAM_NAM  from UNIDIM.SYPAMDTAP WHERE pam_fat='FITRXCOD' and a.FTR_COD  =PAM_SUB )
FROM LGC_EDW.UDM_FAT_FNXTCFFFD a    -- 2008-01-02	2016-10-08
INNER JOIN LGC_EDW.UDM_DIM_FNXPRDDTA B 
	ON a.PRL_COD  =b.PRL_COD 
	AND a.FPR_COD =b.FPR_COD 
left JOIN LGC_EDW.UDM_DIM_FNXPRDLFD c 
	ON  trim(b.FPR_COD) =trim(c.FPR_COD)
INNER JOIN litc_992475.cust_inf h
	on trim(a.eac_nbr) = h.eac_id
where TRX_DTE < '2014-01-31'  --加上过滤条件，避免与高斯库重复
--and c.RSK_GRD = '5'
ORDER BY a.TRX_DTE,b.FPR_COD;


--银证通，可以查到再早一些的数据，但是12年以前数据不全，需要告知分行； 
--如果非要用，最好联系源系统龚芬、叶文涛，让他们帮忙查一下
--2002-04-04	2013-10-25
select 
	"银证通3"  `交易平台`
	,h.cust_nm as `账户名`
	,ord_dte as `交易日期`
	,TRX_ACC  as `账号`
	,stk_cod  as `产品代码`
	,b.prd_cnm  as `产品名称`
	,case trx_cod 
		when 'J137' then '转托管转入'
		when 'J122' then '申购'
		when 'J124' then '赎回'
		when 'J143' then '分红'
		when 'J036' then '转换确认'
		when 'J020' then '认购确认'
		when 'J022' then '申购'
		when 'J024' then '赎回'
	else trx_cod end as `交易类型`
	,ccy_nbr  as `币种`
	,stk_cnt  as `基金份数`
	,ord_amt as `委托份额`
	,FEE_AMT as `手续费`
	,TAX_AMT as `交易金额`
	,CCY_NBR as `币种`
	,RSK_GRD  as `风险等级`
	,b.PRL_COD `产品线代码`
	,b.FPR_COD `产品代码`
	,c.CRP_NAM	`基金管理公司名称`
	,c.CRP_COD	`基金管理公司代码`
	,a.*
from LGC_EDW.RB_STOCK_JHIS_SUBMIT a  	--2002-04-04	2013-10-25
inner join litc_992475.cust_inf h
	on trim(TRX_ACC) = h.eac_id
left join LGC_EDW.UDM_DIM_FNXPRDDTA b 
	on trim(a.stk_cod) =trim(b.fpr_COD)
left JOIN LGC_EDW.UDM_DIM_FNXPRDLFD c 
	ON  trim(a.stk_cod) =trim(c.FPR_COD)
--where ord_dte < '2007-08-02'   --剔除与银基通重复数据
--and c.RSK_GRD = '5'
;

--银基通  2007-08-02	2013-10-26 ； 这部分可以带着查查看，一般金融平台加银证通的就全了。
SELECT
	"银基通"  `交易平台` 
	,h.*
	,a.TRX_DTE `交易日期`
	,a.CLT_NBR `客户号`
	,a.EAC_NBR `户口号`
	,b.PRD_CNM `产品中文名称`
	,a.CCY_NBR `币种`
	,a.TRX_CNL `交易渠道`
	,a.TRX_BRN `交易机构`
	,a.ITR_COD `推荐人编号`
	,a.ITR_BRN `推荐人机构`
	,a.OPR_COD `操作人编号`
	,a.OPR_BRN `操作人机构`
	,case trim(a.FTR_COD)
	when 'FD42'	then '强赎'
	when 'FD22'	then '申购'
	when 'FD43'	then '分红'
	when 'FD24'	then '赎回'
	when 'FD30'	then '认购确认'
	when 'FD98'	then '快赎'
	when 'FD20'	then '认购'
	when 'FD39'	then '定投'
	when 'FD44'	then '强行调增'
	when 'FD45'	then '强行调减'
	when 'FD38'	then '基金转换转出'
	when 'FD37'	then '基金转换转入'
	else a.FTR_COD 
	end `投资理财交易代码`
	,a.TRX_AMT `交易金额` --有些分红交易类型的值为0，只有份额，有些基金分红就是份额分红，因此也是正常的
	,a.TRX_QTY `交易份额`
	,RSK_GRD  `风险等级`
	,b.PRL_COD `产品线代码`
	,b.FPR_COD `产品代码`
	,c.CRP_NAM	`基金管理公司名称`
	,c.CRP_COD	`基金管理公司代码`
	--  ,(select PAM_NAM  from LGC_EAM.UNIDIM_SYPAMDTAP WHERE pam_fat='FITRXCOD' and a.FTR_COD  =PAM_SUB )
FROM LGC_EDW.UDM_FAT_FNXTRCAFD a  	 -- 2007-08-02	2013-10-26
INNER JOIN LGC_EDW.UDM_DIM_FNXPRDDTA B 
	ON a.PRL_COD  =b.PRL_COD 
	AND a.FPR_COD =b.FPR_COD
left JOIN LGC_EDW.UDM_DIM_FNXPRDLFD c 
	ON  trim(b.FPR_COD) =trim(c.FPR_COD)
INNER JOIN litc_992475.cust_inf h
	on trim(a.EAC_NBR) = h.eac_id
--where TRX_DTE < '2013-06-22' and c.RSK_GRD = '5'  --银基通与金融平台中的数据加一起才是全集，不能简单通过日期去限制
ORDER BY b.FPR_COD,a.TRX_DTE;

下面的代码一般不提供

--基金净值表
select 
DW_DAT_DT	as	`数据日期`
,TRX_OBJ_ID	as	`交易标的编号`
,TRX_OBJ_NM	as	`交易标的名称`
,CCY_CD	as	`币种代码`
,UNT_NAV	as	`单位净值`
,ACM_NAV	as	`累计净值`
,YLD	as	`收益率`
,NAV_DT	as	`净值日期`
from SUM_SVIEW.T82_FND_NAV_DTL  --min(NAV_DT) 2006-11-16  --或 T82_FP_NAV_DTL 受托理财净值
where DW_DAT_DT = '2018-05-15'  
	and trim(TRX_OBJ_ID) = 'LK***********'
	
--累计实现收益，一般不提供
select 
	eac_id 账户编号
	,TRX_OBJ_ID  交易标的编号
	,b.FND_CD  基金代码
	,ACM_IMPL_ERN 累计实现收益
	,c.PROD_CHN_NM    产品中文名称
	,c.FND_PROD_TYP_NM 基金产品类型名称
from PDM_VHIS.T03_FI_FND_RAC_INF_S b
left join Amip_vhis.AMIP_D_FND_PROD c --基金产品详细表
	on b.TRX_OBJ_ID=c.FND_PROD_CD
where DW_SNSH_DT = current_date-1
and eac_id in (
	'***********',
	'***********'
	)


--基金手续费口径  刘俊金 80274181
--口径不太确认，具体需求具体分析吧
--如果只要购买的
IF  TQH_TRT_DTA_T. TRT_TRX_COD=FD22/FD39
     手续费=TQH_TRT_DTA_T. TRT_FEE_AM1
ELSEIF TQH_TRT_DTA_T. TRT_TRX_COD=FD20
     勾连出FD30的交易（用TOD_APP_NBR勾连出FD30的交易确认），手续费=TQH_TRT_DTA_T. TRT_FEE_AM1
--如果购买的和赎回的都要，那么不用区分交易代码，直接取TQH_TRT_DTA_T.TRT_FEE_AM1即可

注意事项：
朝朝盈2号，与朝朝盈不是同一个产品分红申赎交易，口径如下，口径提供人 叶文涛
--产品快赎配置表，里面产品都是朝朝盈2号的产品，可以与 T05_FND_TRX_RPLY_EVT 用产品内码关联，或者用substring(b.TRX_OBJ_ID,5,6)做筛选也可以
--在基金交易明细代码下添加一句过滤条件就好
	and substring(b.TRX_OBJ_ID,5,6) in (select distinct QSE_FND_INN from  NDS_VHIS.NLK02_TFE_FND_QSE_T) --朝朝盈2号的产品名称
	--或者用 c.fnd_cd in xxx 也可以，交易标的与产品内码是一一对应的
二、SA理财交易
2.1 SA理财与朝朝宝的查询方式
	朝朝宝的查询有些麻烦，可以问一下业务或者先看一下客户有没有购买朝朝宝的产品，如果没有购买或者不需要查，就很好了

	口径提供人： 
		针对交易，有交易申请事件，有交易确认事件，下面使用申请还是确认，都是与开发确认了一下
		朝朝宝 车东    
		SA理财产品交易  胡杰 80290082

	朝朝宝存在两个数据库(TQH、TQA)里，因此需要分别去两个表里查询
	朝朝宝的购买、撤单、快赎等
	TQH_TRX_ORD_T  ==> LW24_P_TQH_TRX_ORD_TMMDD  ==> T05_SA_AGN_FIN_MNG_TRX_APL_EVT SA代销理财交易申请事件
	TQA_TRS_ORD_T  ==> LW24_TQA_TRS_ORD_TMMDD   ==> T05_SA_ZZB_TRX_EVT  SA朝朝宝交易事件  

	朝朝宝的分红
	TQH_TRT_DTA_T  ==> LW24_P_TQH_TRT_DTA_TMMDD ==>  T05_SA_AGN_FIN_MNG_TRX_CFM_EVT  SA代销理财交易确认事件
	TQA_RRT_DTA_T  ==> LW24_TQA_RRT_DTA_TMMDD  ==>  T05_SA_ZZB_DVD_TRX_EVT  SA朝朝宝分红事件

	朝朝宝的持仓
	TQA_RIP_ACT_T  ==> LW24_TQA_RIP_ACT_TMMDD  ==> T03_SA_AGN_FIN_MNG_RAC_INF_S   SA代销理财份额账户信息快照


	SA理财产品交易 
	TQH_TRX_ORD_T  ==> LW24_P_TQH_TRX_ORD_TMMDD  ==> T05_SA_AGN_FIN_MNG_TRX_APL_EVT  SA代销理财交易申请事件

	SA理财产品持仓
	TQH_RIP_ACT_T  ==> LW24_P_TQH_RIP_ACT_TMMDD ==> T03_SA_AGN_FIN_MNG_RAC_INF_S  SA代销理财份额账户信息快照

	对于交易明细与持仓，朝朝宝和SA理财要按不同口径分开查
	考虑到两个产品有重合的表，在查询的时候，需要将另一个产品剔除掉：
	比如 
		查询SA理财产品的交易明细，需要把TQH_TRX_ORD_T表里朝朝宝的产品剔除掉。
		查询朝朝宝的购买赎回等交易，在TQH_TRX_ORD_T里，限定朝朝宝的产品(朝朝宝产品代码是8920-8924，招银理财聚宝盆1号-5号)  



2.2 SA理财交易查询
--重构后数据，可能与重构前数据有重复，需要注意
drop table if exists LC_TMP;
CREATE TEMPORARY TABLE LC_TMP         
WITH (ORIENTATION = COLUMN, COLVERSION = 2.0, COMPRESSION = MIDDLE)
AS (
	select 
		distinct 
		a.eac_nm as cust_nm ,a.cust_uid, a.cust_id, a.eac_id
	from MAS_DATA.EAC_RTL_CUST_EAC_INF_S  a
	inner join MAS_DATA.CST_IDV_DOC_INF_S b
		on a.CUST_ID = b.CUST_ID
		and a.EAC_OPN_DOC_ISU_CNR_CD = b.DOC_ISU_CNR_CD
		and a.EAC_OPN_DOC_CTG_CD = b.DOC_TYP_CD
		and b.doc_nbr in (
			'99999')	 
		and b.DW_SNSH_DT = current_date -1
	where a.DW_SNSH_DT = current_date -1
)
WITH DATA;

select 
	h.cust_nm		as 客户名
	,a.eac_id 		as 账号
	,a.FND_INT_NBR 	as 产品内码
	,b.FND_ENG_NM  	as 产品名称
	,a.FND_TYP_CD 		as 类型
	--,a.TRX_DT 		as 交易日期
	--,a.BOK_ENTR_DT  as 记账日期
	--,substring(ESTB_TM ,1,19) as 订单申请日期
	,cast(a.ESTB_TM as date) 订单申请日期  --即申请日期
	,a.ESTB_TM	订单申请时间
	,APL_FORM_ID 申请单编号
	,a.ENTR_AMT 	as 委托金额
	,a.entr_lot		as 委托份额
	,c.BSC_RSK_LVL 	as 风险等级
	,a.Fee_Amt 		as 费用金额
	,case a.TRX_TYP_CD 
	when 'FD42'	then '强赎'
	when 'FD22'	then '申购'
	when 'FD43'	then '分红'
	when 'FD24'	then '赎回'
	when 'FD30'	then '认购确认'
	when 'FD98'	then '快赎'
	when 'FD20'	then '认购'
	when 'FD52'	then '认购、申购、赎回撤单'
	else a.TRX_TYP_CD 
	end 			as 投资理财交易代码
	,case a.TRX_CHNL_CD  
		when 'RVC'	then '可视柜台'
		when 'TEL'	then '电话语音'
		when 'TGS'	then '托管银行清算系统'
		when 'TRA'	then '招赢通PC'
		when 'TRB'	then '招赢通APP'
		when 'BCK'	then '后台'
		when 'CAL'	then '电话人工'
		when 'CPR'	then '合作方电脑系统'
		when 'DSK'	then '柜台'
		when 'GLC'	then '全球连线'
		when 'ICO'	then '企业银行'
		when 'IEX'	then '个人银行专业版'
		when 'ILC'	then 'ILC'
		when 'INT'	then '个人银行大众版'
		when 'LPS'	then '新个贷系统'
		when 'OTH'	then '其他'
		when 'MPH'	then '手机银行'
	else a.TRX_CHNL_CD   
	end as 		交易渠道
	,a.TRX_STS_CD 		as 状态代码
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
	,d.Slf_Rcm_Psn_Nbr as 推荐人编号
from OFIN_DATA.OFIN_SA_AGN_FIN_MNG_TRX_APL_EVT a
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join OFIN_DATA.OFIN_SA_PROD_INF_S b
	on a.TRX_OBJ_ID=b.TRX_OBJ_ID
	and b.dw_snsh_dt = current_date-1
	--and a.DW_DAT_DT > '2018-01-01'
	and a.FND_INT_NBR not in ('8920','8921','8922','8923','8924','8925','8926','8927','8928','8929')  -- 过滤掉朝朝宝产品
left join OFIN_DATA.OFIN_SA_AGN_FIN_MNG_RCM_INF d
	on a.TRX_SEQ_NBR = d.TRX_SEQ
	and a.TRX_SEQ_NBR <> ''
left join NDS_VHIS.NLW24_TQE_DEF_BSC_T_S c
	on c.DW_SNSH_DT = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_ID
--where a.TRX_TYP_CD in ('FD22','FD30','FD20','FD52')

--重构前数据，与重构后数据一致，可以不保留
drop table if exists LC_TMP;
CREATE TEMPORARY TABLE LC_TMP         
WITH (ORIENTATION = COLUMN, COLVERSION = 2.0, COMPRESSION = MIDDLE)
AS (
	select 
		distinct 
		a.eac_nm as cust_nm ,a.cust_uid, a.cust_id, a.eac_id
	from MAS_DATA_SRC.EAC_RTL_CUST_EAC_INF_S  a
	inner join MAS_DATA_SRC.CST_IDV_DOC_INF_S b
		on a.CUST_ID = b.CUST_ID
		and a.EAC_OPN_DOC_ISU_CNR_CD = b.DOC_ISU_CNR_CD
		and a.EAC_OPN_DOC_CTG_CD = b.DOC_TYP_CD
		and b.doc_nbr in (
			'9999')	 
		and b.DW_SNSH_DT = current_date -1
	where a.DW_SNSH_DT = current_date -1
)
WITH DATA;

NLK02_TQH_TRX_ORD_T
TOD_STS_COD 取值为A  C   D 都代表申请时成功  即STS_CD

--SA理财是20年8月之后开始的，因此只有20年往后的数据
--注意不能加上委托金额不等于0的过滤，这样会过滤掉赎回等交易类型
--这里只包含家族信托的对公理财数据，其他数据需要去别的表查询
select 
	h.cust_nm		as 客户名
	,a.eac_id 		as 账号
	,a.FND_INT_NBR 	as 产品内码
	,b.FND_ENG_NM  	as 产品名称
	,a.FND_TYP 		as 类型
	--,a.TRX_DT 		as 交易日期
	--,a.BOK_ENTR_DT  as 记账日期
	--,substring(ESTB_TM ,1,19) as 订单申请日期
	,cast(a.ESTB_TM as date) 订单申请日期  --即申请日期
	,a.ESTB_TM	订单申请时间
	,APL_FORM_ID 申请单编号
	,a.ENTR_AMT 	as 委托金额
	,a.entr_lot		as 委托份额
	,c.BSC_RSK_LVL 	as 风险等级
	,a.Fee_Amt 		as 费用金额
	,case a.TRX_CD 
	when 'FD42'	then '强赎'
	when 'FD22'	then '申购'
	when 'FD43'	then '分红'
	when 'FD24'	then '赎回'
	when 'FD30'	then '认购确认'
	when 'FD98'	then '快赎'
	when 'FD20'	then '认购'
	when 'FD52'	then '认购、申购、赎回撤单'
	else a.TRX_CD 
	end 			as 投资理财交易代码
	,case a.TRX_CHNL  
		when 'RVC'	then '可视柜台'
		when 'TEL'	then '电话语音'
		when 'TGS'	then '托管银行清算系统'
		when 'TRA'	then '招赢通PC'
		when 'TRB'	then '招赢通APP'
		when 'BCK'	then '后台'
		when 'CAL'	then '电话人工'
		when 'CPR'	then '合作方电脑系统'
		when 'DSK'	then '柜台'
		when 'GLC'	then '全球连线'
		when 'ICO'	then '企业银行'
		when 'IEX'	then '个人银行专业版'
		when 'ILC'	then 'ILC'
		when 'INT'	then '个人银行大众版'
		when 'LPS'	then '新个贷系统'
		when 'OTH'	then '其他'
		when 'MPH'	then '手机银行'
	else a.TRX_CHNL   
	end as 		交易渠道
	,a.STS_CD 		as 状态代码
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
	,d.Slf_Rcm_Psn_Nbr as 推荐人编号
from pdm_data_src.T05_SA_AGN_FIN_MNG_TRX_APL_EVT a
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join pdm_data_src.T02_SA_PROD_INF b
	on a.TRX_OBJ_CD=b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'
	--and a.DW_DAT_DT > '2018-01-01'
	and a.FND_INT_NBR not in ('8920','8921','8922','8923','8924','8925','8926','8927','8928','8929')  -- 过滤掉朝朝宝产品
left join pdm_data_src.T03_SA_AGN_FIN_MNG_RCM_INF d
	on a.TRX_SEQ_NBR = d.Trx_Srl
	and a.TRX_SEQ_NBR <> ''
left join NDS_data_src.NLW24_TQE_DEF_BSC_T_S c
	on c.DW_SNSH_DT = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_CD
--where a.TRX_CD in ('FD22','FD30','FD20','FD52')





	--需要推荐人号码，现提供交易申请事件，经办人、操作员都为空
	select 
		Slf_Rcm_Psn_Nbr
	from PDM_VHIS.T05_SA_AGN_FIN_MNG_TRX_APL_EVT a   --SA代销理财交易申请事件
	inner join PDM_VHIS.T03_SA_AGN_FIN_MNG_RCM_INF b
		on a.TRX_SEQ_NBR = b.Trx_Srl
	where a.eac_id = 'xxxx'
		and TRX_CD not in ('FD43')  --排除分红交易


2.3 朝朝宝交易明细与持仓

--TQ数据库，分红交易
select 
	h.cust_nm		as 客户名
	,a.eac_id  账户编号
	,b.FND_ENG_NM  产品名称
	,Prod_Int_Nbr  产品内码
	,FND_TYP  基金类型
	,BARG_DT  成交日期
	,Entr_Amt  交易金额
	,b.Fnd_NAV 成交净值
	,Barg_Lot 成交份额
	,Barg_Amt 成交金额
	,FEE_AMT_1  手续费
	,c.BSC_RSK_LVL  风险等级
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
from pdm_vhis.T05_SA_AGN_FIN_MNG_TRX_CFM_EVT a   --TQH_TRT_DTA_T  分红从这个表里查询，购买赎回交易数据不准确
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join PDM_vhis.T02_SA_PROD_INF b  --SA代销理财产品信息
	on a.TRX_OBJ_ID=b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'
	--and a.DW_DAT_DT > '2018-01-01'
left join NDS_VHIS.NLW24_TQE_DEF_BSC_T_S c  --SA产品基础信息表
	on c.dw_snsh_dt = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_ID
where Prod_Int_Nbr in ('8920','8921','8922','8923','8924','8925','8926','8927','8928','8929')  --限定朝朝宝产品
	and trx_cd = 'FD43'  --限定分红交易
order by a.eac_id,Prod_Int_Nbr;


--TA数据库，分红交易
select 
	h.cust_nm		as 客户名
	,a.eac_id  账户编号
	,b.FND_ENG_NM  产品名称
	,a.FND_INT_NBR  产品内码
	,b.FND_TYP_CD  基金类型
	,a.TRX_CFM_DT  成交日期
	,a.PER_STRK_TRX_CFM_AMT  每笔交易确认金额
	,a.FND_ACT_DVD_FND 基金账户红利资金
	,a.CMSN_FEE  手续费
	,c.BSC_RSK_LVL  风险等级
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
from pdm_vhis.T05_SA_ZZB_DVD_TRX_EVT a   --TQH_TRT_DTA_T  分红从这个表里查询，购买赎回交易数据不准确
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join PDM_vhis.T02_SA_PROD_INF b  --SA代销理财产品信息
	on a.TRX_OBJ_ID=b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'
	--and a.DW_DAT_DT > '2018-01-01'
left join NDS_VHIS.NLW24_TQE_DEF_BSC_T_S c  --SA产品基础信息表
	on c.dw_snsh_dt = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_ID
where FND_INT_NBR in ('8920','8921','8922','8923','8924','8925','8926','8927','8928','8929')  --限定朝朝宝产品
order by a.eac_id,FND_INT_NBR;



--TQ数据库  购买赎回等交易
select 
	h.cust_nm				as 客户名
	,a.eac_id  				as 账户编号
	,b.FND_ENG_NM  			as 产品名称
	,FND_INT_NBR  			as 产品内码
	,a.TRX_OBJ_CD 			as 标的编号
	,FND_TYP  				as 基金类型
	,TRX_DT  				as 交易日期
	,ENTR_AMT  				as 委托金额
	,ENTR_LOT  				as 委托份额
	,FEE_AMT  				as 手续费
	,c.BSC_RSK_LVL  		as 风险等级
	,case TRX_CD 
	when 'FD42'	then '强赎'
	when 'FD22'	then '申购'
	when 'FD43'	then '分红'
	when 'FD24'	then '赎回'
	when 'FD30'	then '认购确认'
	when 'FD98'	then '快赎'
	when 'FD20'	then '认购'
	else TRX_CD 
	end 					as 投资理财交易代码
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
from PDM_VHIS.T05_SA_AGN_FIN_MNG_TRX_APL_EVT a   --SA代销理财交易申请事件，这里记录了2022-06年以前的数据
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join PDM_vhis.T02_SA_PROD_INF b  --SA代销理财产品信息
	on a.TRX_OBJ_CD=b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'
	--and a.DW_DAT_DT > '2018-01-01'
left join NDS_VHIS.NLW24_TQE_DEF_BSC_T_S c  --SA产品基础信息表
	on c.dw_snsh_dt = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_CD
where FND_INT_NBR in ('8920','8921','8922','8923','8924','8925','8926','8927','8928','8929')  --朝朝宝产品
	and TRX_CD not in ('FD43')  --排除分红交易
order by a.eac_id,FND_INT_NBR;

--TA数据库，交易事件
select 
	h.cust_nm				as 客户名
	,a.eac_id  				as 账户编号
	,b.FND_ENG_NM  			as 产品名称
	,PROD_INT_NBR  			as 产品内码
	,a.TRX_OBJ_ID 			as 标的编号
	,TRX_DT  				as 交易日期
	,ENTR_AMT  				as 买入份额
	,ENTR_LOT  				as 卖出份额
	,ENTR_PRC  				as 委托价格  --固定为1
	,FEE_AMT  				as 手续费
	,c.BSC_RSK_LVL  		as 风险等级
	,case TRX_CD 
	when 'FD42'	then '强赎'
	when 'FD22'	then '申购'
	when 'FD43'	then '分红'
	when 'FD24'	then '赎回'
	when 'FD30'	then '认购确认'
	when 'FD98'	then '快赎'
	when 'FD20'	then '认购'
	else TRX_CD 
	end 					as 投资理财交易代码
	,b.FND_CMP_NM	as 基金公司名称
	,b.FND_CMP_CD	as 基金公司代码
from pdm_vhis.T05_SA_ZZB_TRX_EVT a     --SA朝朝宝交易事件   这里记录了2022年6月至今的数据
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join PDM_vhis.T02_SA_PROD_INF b  --SA代销理财产品信息
	on a.TRX_OBJ_ID=b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'
	--and a.DW_DAT_DT > '2018-01-01'
left join NDS_VHIS.NLW24_TQE_DEF_BSC_T_S c  --SA产品基础信息表
	on c.dw_snsh_dt = current_date-1
	and c.TRX_OBJ_ID = a.TRX_OBJ_ID;

净值，可以上wiki搜净值，就可以找到净值明细表 T82_FP_NAV_DTL/T82_FND_NAV_DTL --最早日期 2020-08-31 

三、受托理财交易
理论上，是不能严格判断购买与赎回的是同一笔钱，只能通过账户号码+证券账户编号推断可能是同一笔持仓

SELECT 
	 H.CUST_ID					AS 客户号
	,H.CUST_NM					AS 客户名称
	,B.EAC_ID  					AS 户口号
	,'受托理财' 	                AS 产品类型
	,B.PROD_CD 					AS 产品内码
	,C.PROD_CHN_FUL_NM			AS 产品名称
	,B.CUST_ENTR_DT 			AS 交易日期
	,SUBSTRING(B.CUST_ENTR_TM,1,8) AS 交易时间
	,B.FND_BARG_QTY  			AS 交易金额
	,SUBSTR(C.RAT_RSL_1,2,1)  	AS 风险等级
	,CASE 
	 	WHEN B.TRX_TYP_CD IN ('FPBS','FPB2') THEN '入账-分红'
	 	WHEN B.TRX_TYP_CD IN ('FPR2','FPRD') THEN '入账-赎回'
	 	WHEN B.TRX_TYP_CD='FPCL' THEN '入账-到期还本'
	 	ELSE '出账-购买' 
	 END 						AS 交易类型	
    ,CASE B.TRX_CHNL_CD  
		WHEN 'RVC'	THEN '可视柜台'
		WHEN 'TEL'	THEN '电话语音'
		WHEN 'TGS'	THEN '托管银行清算系统'
		WHEN 'TRA'	THEN '招赢通PC'
		WHEN 'TRB'	THEN '招赢通APP'
		WHEN 'BCK'	THEN '后台'
		WHEN 'CAL'	THEN '电话人工'
		WHEN 'CPR'	THEN '合作方电脑系统'
		WHEN 'DSK'	THEN '柜台'
		WHEN 'GLC'	THEN '全球连线'
		WHEN 'ICO'	THEN '企业银行'
		WHEN 'IEX'	THEN '个人银行专业版'
		WHEN 'ILC'	THEN 'ILC'
		WHEN 'INT'	THEN '个人银行大众版'
		WHEN 'LPS'	THEN '新个贷系统'
		WHEN 'OTH'	THEN '其他'
		WHEN 'MPH'	THEN '手机银行'
	ELSE B.TRX_CHNL_CD   
	END AS 		交易渠道
	,B.RCM_USR_ID  		AS 推荐人编号
FROM  OFIN_DATA.OFIN_FP_ENTR_EVT B   --理财购买情况，MIN(CTR_DT) 2004-12-22
INNER JOIN LC_TMP  H
	ON B.EAC_ID = H.EAC_ID
LEFT JOIN  OFIN_DATA.OFIN_FP_SBJ_MAT_INF_S C   --PROD_CD产品代码
	ON B.PROD_CD=C.PROD_CD
	AND C.DW_SNSH_DT = CURRENT_DATE-1
--WHERE  B.CUST_ENTR_DT BETWEEN '${start_dt}' AND '${end_dt}'; -- {理财购买开始日期} AND {理财购买结束日期}
;

--重构前数据，与重构后数据一致，可以不保留
select 
	h.cust_nm					as 客户名称
	,B.EAC_ID  					as 户口编号
	,b.SCR_ACT_NBR 				as 证券账户编号
	,b.ACT_NBR                  as 账户号码
	--,d.Adj_Crd_Intr_Acm 累计收益（仅未结清持仓可参考）
	,b.PROD_TYP_CD  			as 产品类型代码
	,b.Prod_Cd 					as 产品内码
	,c.PROD_CHN_FUL_NM			as 产品中文名称
	,b.TRX_Dt 					as 日期
	,b.CUST_ENTR_DT 			as 客户委托日期  --即订单申请日期
	,b.CUST_ENTR_TM				as 客户委托时间
	,b.Fnd_Barg_Qty  			as 交易金额
	,substr(c.Rat_Rsl_1,1,2)  	as 风险等级
	,b.Fee_Barg_Amt 			as 手续费 
	,case b.ENTR_CHNL_TYP_CD  
		when 'RVC'	then '可视柜台'
		when 'TEL'	then '电话语音'
		when 'TGS'	then '托管银行清算系统'
		when 'TRA'	then '招赢通PC'
		when 'TRB'	then '招赢通APP'
		when 'BCK'	then '后台'
		when 'CAL'	then '电话人工'
		when 'CPR'	then '合作方电脑系统'
		when 'DSK'	then '柜台'
		when 'GLC'	then '全球连线'
		when 'ICO'	then '企业银行'
		when 'IEX'	then '个人银行专业版'
		when 'ILC'	then 'ILC'
		when 'INT'	then '个人银行大众版'
		when 'LPS'	then '新个贷系统'
		when 'OTH'	then '其他'
		when 'MPH'	then '手机银行'
	else b.ENTR_CHNL_TYP_CD   
	end as 		交易渠道
	,b.RCM_USR_ID  		AS 推荐用户编号
	,b.TRX_ORG_ID	as 交易机构编号
	,case 
		when b.TRX_TYP_CD in ('FPBS','FPB2') then '入账-分红'
		when b.TRX_TYP_CD in ('FPR2','FPRD') then '入账-赎回'
		when b.TRX_TYP_CD='FPCL' then '入账-到期还本'
		else '出账-购买' 
	end 						as 交易类型
	,case when b.TRX_TYP_CD in('FPBS','FPR2','FPB2','FPRD' ,'FPCL') 
	then '入账' 
	else '出账' 
	end 						as 交易类型2
from  PDM_VHIS.T05_FI_FP_ENTR_EVT b   --理财购买情况，min(ctr_dt) 2004-12-22
inner join lc_tmp  h
	on b.eac_id = h.eac_id
left join  PDM_vhis.T02_FP_SBJ_MAT_INF c   --PROD_CD产品代码
	on b.Prod_Cd=c.Prod_Cd
	and c.DW_END_DT='9999-12-31'
-- where  b.TRX_TYP_CD not in('FPBS','FPR2','FPB2','FPRD' ,'FPCL')  --出账，购买记录
--where TRX_Dt = '2019-08-14' or CUST_ENTR_DT = '2019-08-14'
--TRX_DT not in ('0001-01-03','1900-01-01','1901-01-01')
	--left join pdm_vhis.T03_FP_RAC_INF_S  d on b.SCR_ACT_NBR = d.SCR_ACT_NBR and b.ACT_NBR = d.act_id and d.dw_snsh_dt = '2022-09-04'
--WHERE  b.Ctr_Dt > '2018-01-01'

--历史库  受托理财交易 ，可能与上面的结果有重复数据,并且数据日期也不准确，可以对上面或者下面的交易日期加上过滤，保证数据是全量不重复的
--不过感觉，高斯库的数据比较全了，最大日期 2016-10-08，最小日期  2004-12-22，与高斯一致
select 
	h.*
	,a.TRX_DTE `交易日期`
	,a.CLT_NBR `客户号`
	,a.EAC_NBR `户口号`
	,b.PRD_CNM `产品中文名称`
	,a.CCY_NBR `币种`
	,a.CFM_TYP `投资理财交易码`
	,a.TRX_AMT `交易金额`
	,a.TRX_QTY `交易份额`
from LGC_EDW.UDM_FAT_FNXTCHLFP a 
inner join LITC_991571.cust_inf h 
	on trim(a.EAC_NBR) = h.eac_id
inner join LGC_EDW.UDM_DIM_FNXPRDLFP b 
	on a.FPR_COD =b.FPR_COD 
--where a.TRX_DTE < '2015-01-01'

--持仓 

--重构前
SELECT B.PRT_D `日期`
	,B.EAC_ID AS `账户名称`
	,SUBSTRING(B.TRX_OBJ_ID,5,6) AS `产品代码`
	,C.PROD_SHT_NM AS `产品名称`
	,SUM(B.ACT_LOT) AS `账户份额`
	,SUM(B.ACT_MKV) AS `账户市值`
FROM SUM_AVIEW.T83_FP_RAC_INF_S B   -----理财持仓
INNER JOIN PDM_AVIEW.T02_FP_SBJ_MAT_INF C
	ON B.TRX_OBJ_ID = C.TRX_OBJ_ID
	AND C.DW_END_DT = '9999-12-31'
WHERE B.EAC_ID IN ('9999')
AND B.PRT_D = '2019-06-30'
GROUP BY B.PRT_D,B.EAC_ID,SUBSTRING(B.TRX_OBJ_ID,5,6),C.PROD_SHT_NM
;

--重构后	
SELECT B.DW_SNSH_DT
	,B.EAC_ID 账户名称
	,SUBSTRING(B.TRX_OBJ_ID,5,6) AS "产品代码"
	,C.PROD_SHT_NM AS 产品名称
	,SUM(B.PST_BAL) AS "账户份额"
	,SUM(B.ACT_MKV) AS "账户市值"
FROM OFIN_VHIS.OFIN_FP_RAC_INF_S B   -----理财持仓
INNER JOIN OFIN_VHIS.OFIN_FP_SBJ_MAT_INF_S C
	ON B.TRX_OBJ_ID = C.TRX_OBJ_ID
	AND C.DW_SNSH_DT = CURRENT_DATE - 1
INNER JOIN LC_AUM_TMP D
	ON B.EAC_ID = D.EAC_ID
WHERE B.DW_SNSH_DT = CURRENT_DATE - 1
GROUP BY 1,2,3,4
;
如果指定说明要收益，那么需要与业务确认收益含义，与开发确认口径才行

四、朝朝盈交易&持仓

--朝朝盈2号，与朝朝盈不是同一个产品，分红申购交易口径如下，口径提供人 叶文涛
--产品快赎配置表，里面产品都是朝朝盈2号的产品，可以与基金确认事件表 T05_FND_TRX_RPLY_EVT 用产品内码关联，或者用substring(b.TRX_OBJ_ID,5,6)做筛选也可以
--在基金交易明细代码下添加一句过滤条件就好
	and substring(b.TRX_OBJ_ID,5,6) in (select distinct QSE_FND_INN from  NDS_VHIS.NLK02_TFE_FND_QSE_T) --朝朝盈2号的产品名称
	--或者用 c.fnd_cd in xxx 也可以，交易标的与产品内码是一一对应的


--朝朝盈、理财、基金、保险等收益类产品的本金，收益，及总收益。	
	--购买赎回交易，此表高斯b才是全量的数据,14-22年
	select 
		h.*
		,EVT_ID 事件编号,DW_DAT_DT 数据日期,STS_CD 状态代码,SND_STS_IND 发送状态标志,ENTR_DT 委托日期,ENTR_SEQ_NBR 委托序号,ZHAOZHAOYING_BUS_TYP_CD 朝朝盈业务类型代码,JOB_DT 工作日期,BCH_NBR 批号,a.CUST_ID 客户编号,a.EAC_ID 户口编号,EAC_MDF 户口修饰符,EAC_INF 户口信息,TRX_ACT_NBR 交易账号,TA_ACT_NBR TA账号,TRX_SEQ_NBR 交易序号,TRX_LOT 交易份额,TRX_AMT 交易金额,CCY_CD 币种代码,CR_TYP_CD 钞汇类型代码,XPC_RCV_DT 预计到账日期,DT_1 日期1,SRL_1 流水1,SRL_2 流水2,AMT_1 金额1,AMT_2 金额2,BOK_ENTR_SRL 记账流水,ENTR_NBR_SET 记账套号,BOK_ENTR_DT 记账日期,VAL_DT 起息日期,CLR_BBK_ID 清算分行编号,CLR_ORG_ID 清算机构编号,MCHN_DT 机器日期,MCHN_TM 机器时间,RCD_STS 记录状态,RCD_EDTN 记录版本,SMR_1 摘要1,SMR_2 摘要2,SMR_3 摘要3,RSV_FLD 保留字段,CUST_RSK_LVL 客户风评等级,TRX_CHN 交易渠道
		,BUS_CD 业务代码
		,case BUS_CD 
			when 'FD42'	then '强赎'
			when 'FD22'	then '申购'
			when 'FD43'	then '分红'
			when 'FD24'	then '赎回'
			when 'FD30'	then '认购确认'
			when 'FD98'	then '快赎'
			when 'FD20'	then '认购'
			else BUS_CD 
			end 					as 业务代码名称
	from PDM_VHIS.T05_FND_ZHAOZHAOYING_TRX_EVT a --LW22_TFU_TTX_DTA_T  朝朝盈交易申请文件，只查到了4笔信息
	inner join lc_tmp  h
		on a.EAC_ID = h.EAC_ID;

	--分红交易信息 通过400和X86结合一起查询,得到全量数据
	--2022-06-至今
	select 
		h.*,
			a.DVD_PTN_TAG 分区因子,a.DVD_STS_COD 状态,a.DVD_TRX_SER 交易序号,a.DVD_STS_SND 发送状态,a.DVD_PRD_COD 产品代码,a.DVD_CLR_DAT 工作日期,a.DVD_BCH_NBR 批号,a.DVD_EAC_NBR 户口号,a.DVD_CLT_NBR 客户号,a.DVD_IAC_NBR 卡内码,a.DVD_XAC_NBR 交易账号,a.DVD_RAC_NBR 登记账号,a.DVD_TRS_NUM 交易顺序号,a.DVD_TRX_QTY 交易份额,a.DVD_TRX_AMT 交易金额,a.DVD_TRX_COD 交易代码,a.DVD_CCY_NBR 币种,a.DVD_CCY_TYP 钞汇标记,a.DVD_FUN_DAT 预计到账日期,a.DVD_TRX_DT1 日期1,a.DVD_TRX_SQ1 流水1,a.DVD_TRX_SQ2 流水2,a.DVD_TRX_AM1 金额1,a.DVD_TRX_AM2 金额2,a.DVD_TRS_SET 记账套号,a.DVD_TRS_SEQ 记账序号,a.DVD_TRS_SER 记账交易流水,a.DVD_TRS_CNO 清算流水,a.DVD_LGR_DAT 记账日期,a.DVD_VAL_DAT 起息日期,a.DVD_BBK_NBR 清算分行,a.DVD_BRN_NBR 清算机构,a.DVD_MAC_DAT 机器日期,a.DVD_MAC_TIM 机器时间,a.DVD_REC_STS 记录状态,a.DVD_REC_VER 记录版本
	from NDS_VHIS.NLW22_TFU_DVD_DTA_T  a --朝朝盈分红交易文件
	inner join lc_tmp h
		on a.DVD_EAC_NBR = h.eac_id;

	--2015-10-2022-06
	select 
		h.*,
			a.DVDSTSCOD 状态,a.DVDSNDSTS 发送状态,a.DVDORDDAT 委托日期,a.DVDORDSEQ 委托序号,a.DVDPRDCOD 产品代码,a.DVDCLRDAT 工作日期,a.DVDBCHNBR 批号,a.DVDCLTNBR 客户号,a.DVDIACNBR 卡内码,a.DVDEACNBR 户口信息,a.DVDXACNBR 交易账号,a.DVDRACNBR ＴＡ账号,a.DVDTRSNUM 交易序号,a.DVDTRXQTY 交易份额,a.DVDTRXAMT 交易金额,a.DVDTRXCOD 业务代码,a.DVDCCYNBR 币种,a.DVDCCYTYP 钞汇,a.DVDFUNDAT 预计到账日期,a.DVDTRXDT1 日期1,a.DVDTRXSQ1 流水1,a.DVDTRXSQ2 流水2,a.DVDTRXAM1 金额1,a.DVDTRXAM2 金额2,a.DVDTRSSEQ 记账流水,a.DVDTRSSET 记账套号,a.DVDLGRDAT 记账日期,a.DVDVALDAT 起息日期,a.DVDBBKNBR 清算分行,a.DVDBRNNBR 清算机构,a.DVDMACDAT 机器日期,a.DVDMACTIM 机器时间,a.DVDRECSTS 记录状态,a.DVDRECVER 记录版本,a.DVDNARBK1 摘要1,a.DVDNARBK2 摘要2,a.DVDNARBK3 摘要3
	from NDS_VHIS.NLK02_FUDVDDTAP  a --朝朝盈分红交易文件
	inner join lc_tmp h
		on a.DVDcltnbr||dvdiacnbr = h.iac_id;



	--历史库数据,2016-10以前
	select 
DVDSTSCOD `状态`,DVDSNDSTS `发送状态`,DVDORDDAT `委托日期`,DVDORDSEQ `委托序号`,DVDPRDCOD `产品代码`,DVDCLRDAT `工作日期`,DVDBCHNBR `批号`,DVDCLTNBR `客户号`,DVDIACNBR `卡内码`,DVDEACNBR `户口信息`,DVDXACNBR `交易账号`,DVDRACNBR `ＴＡ账号`,DVDTRSNUM `交易序号`,DVDTRXQTY `交易份额`,DVDTRXAMT `交易金额`,DVDTRXCOD `业务代码`,DVDCCYNBR `币种`,DVDCCYTYP `钞汇`,DVDFUNDAT `预计到账日期`,DVDTRXDT1 `日期1`,DVDTRXSQ1 `流水1`,DVDTRXSQ2 `流水2`,DVDTRXAM1 `金额1`,DVDTRXAM2 `金额2`,DVDTRSSEQ `记账流水`,DVDTRSSET `记账套号`,DVDLGRDAT `记账日期`,DVDVALDAT `起息日期`,DVDBBKNBR `清算分行`,DVDBRNNBR `清算机构`,DVDMACDAT `机器日期`,DVDMACTIM `机器时间`,DVDRECSTS `记录状态`,DVDRECVER `记录版本`,DVDNARBK1 `摘要1`,DVDNARBK2 `摘要2`,DVDNARBK3 `摘要3`	
from LGC_EDW.ODS_C1C3_FUDVDDTAP  a --朝朝盈分红交易文件
	where DVDcltnbr||dvdiacnbr in
	(
	--iacid
	)
)
)



五、保险交易
BIINTCRT 自助渠道 - 投保交易

保险双录，有一个专门的保单与双录关联信息表，NDS_VHIS.NLH52_TBI_VDO_RCD_T，具体可以问问
 
 
--全量的保险交易信息，保险没有收益
select 
	h.cust_nm		as 客户名
	,a.eac_id  账户编号
	,a.TRX_CD  交易代码
	,b.INSU_BRED_NM  产品名称
	,case b.PROD_TYP_CD
		when '07540100' then '定期寿险'
		when '07540200' then '终身寿险'
		when '07540300' then '两全险'
		when '07540400' then '养老年金'
		when '07540500' then '子女教育年金'
		when '07540600' then '意外险'
		when '07540700' then '重大疾病保险'
		when '07540800' then '医疗保险'
		when '07541000' then '护理保险'
		when '07541100' then '投资型财险'
		when '07541200' then '车险'
		when '07541300' then '财产保障险'
		when '07541500' then '信用险'
		when '07541600' then '其他保险'
		when '07541700' then '税延养老年金'
	else b.PROD_TYP_CD   
	end as 		产品类型代码
	,a.TRX_DT  交易日期
	,a.TRX_TM  as tm--交易时间
	,a.TRX_SEQ 交易流水号
	,a.TRX_CHNL_TYP_CD 交易渠道类型代码
	,case a.TRX_CHNL_TYP_CD
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		else a.TRX_CHNL_TYP_CD 
		end as 交易渠道
	,a.AMT  金额  
	,a.HDL_USR_ID 经办用户编号
	,a.HDL_ORG_ID 经办机构编号
	,a.INSU_CMP_ID 保险公司编号
	,a.CMSN_FEE  手续费
	,b.EXT_PROD_ID 外部产品编号
	,b.INSU_BRED_ID 险种编号
	,b.INSU_BRED_RSK_LVL_CD 险种风险级别代码
	,a.INSU_CMP_INSU_PLCY_NBR 保险公司保单号
	,b.INSU_BRED_EFT_DT 险种生效日期
	,b.INSU_BRED_NVLD_DT 险种失效日期
from OISU_DATA.OISU_YINBAOTONG_TRX_EVT a
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join OISU_DATA.OISU_YINBAOTONG_INSU_BRED_INF_S b
	on a.TRX_OBJ_ID = b.TRX_OBJ_ID
	and b.dw_snsh_dt=current_date -1

重构前数据
--全量的保险交易信息，保险没有收益
select 
	h.cust_nm		as 客户名
	,a.eac_id  账户编号
	,a.TRX_CD  交易代码
	,b.PROD_NM  产品名称
	,case b.PROD_TYP_CD
		when '07540100' then '定期寿险'
		when '07540200' then '终身寿险'
		when '07540300' then '两全险'
		when '07540400' then '养老年金'
		when '07540500' then '子女教育年金'
		when '07540600' then '意外险'
		when '07540700' then '重大疾病保险'
		when '07540800' then '医疗保险'
		when '07541000' then '护理保险'
		when '07541100' then '投资型财险'
		when '07541200' then '车险'
		when '07541300' then '财产保障险'
		when '07541500' then '信用险'
		when '07541600' then '其他保险'
		when '07541700' then '税延养老年金'
	else b.PROD_TYP_CD   
	end as 		产品类型代码
	,a.TRX_DT  交易日期
	,a.TRX_TM  as tm--交易时间
	,a.TRX_SEQ 交易流水号
	,a.TRX_CHNL_TYP_CD 交易渠道类型代码
	,case a.TRX_CHNL_TYP_CD
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		else a.TRX_CHNL_TYP_CD 
		end as 交易渠道
	,a.AMT  金额  
	,a.HDL_USR_ID 经办用户编号
	,a.HDL_ORG_ID 经办机构编号
	,a.INSU_CMP_ID 保险公司编号
	,a.CMSN_FEE  手续费
	,b.EXT_PROD_ID 外部产品编号
	,b.INSU_BRED_ID 险种编号
	,b.INSU_BRED_RSK_LVL_CD 险种风险级别代码
	,a.INSU_CMP_INSU_PLCY_NBR 保险公司保单号
	,b.INSU_BRED_EFT_DT 险种生效日期
	,b.INSU_BRED_NVLD_DT 险种失效日期
from pdm_vhis.T05_YINBAOTONG_TRX_EVT a
inner join lc_tmp  h
	on a.eac_id = h.eac_id
inner join PDM_VHIS.T02_YINBAOTONG_INSU_BRED_INF b
	on a.TRX_OBJ_ID = b.TRX_OBJ_ID
	and b.DW_END_DT='9999-12-31'



保险双录，有一个专门的保单与双录关联信息表，NDS_VHIS.NLH52_TBI_VDO_RCD_T，具体可以问问

--保单信息，到期日，承保日期等   口径提供人 陈庆华
select  
a.*
,BBI_VAL_DAT 投保日期
,BBI_PRO_DAT 承保日期
,BBI_EXP_DAT 到期日

 from  LITC_991571.TMP04 a
left  join  NDS_VHIS.NLH52_TBI_BIL_DTA_T b
on a.val1 = b.BBI_BIL_INS


		,g.Rcm_Usr_Id_1 --推荐编号
			,b.INSU_BRED_RSK_LVL_CD --险种风险级别代码
			,e.RSK_ENDU_CD
			,e.SYS_CD
			,e.ASES_START_DT
			,e.ASES_ORIG_TM
			,e.ASES_MTU_DT
			,rank()OVER(PARTITION BY h.eac_id,b.PROD_NM,a.TRX_DT,a.AMT ORDER BY ASES_START_DT DESC ,ASES_ORIG_TM DESC) as row1  --按卡号做分作，对满足条件的认证时间做排序，取最新的风评
		from pdm_vhis.T05_YINBAOTONG_TRX_EVT a
		inner join lc_tmp  h
			on a.eac_id = h.eac_id
		inner join PDM_VHIS.T02_YINBAOTONG_INSU_BRED_INF b
			on a.TRX_OBJ_ID = b.TRX_OBJ_ID
			and b.DW_END_DT='9999-12-31'
		left join pdm_Vhis.T03_BIP_ARG_RCM_INF g
			on a.BIP_Arg_Id = g.Arg_Id
			and a.Trx_Seq = g.Trx_Seq
		left join PDM_VHIS.T01_CUST_IRT_ASES_INF e
				on h.cust_id=e.CUST_ID
				and e.SYS_CD IN ('FI')
				and e.ASES_START_DT<=a.TRX_DT
				and e.ASES_MTU_DT>=a.TRX_DT
		where (a.TRX_DT between '2023-01-01' and '2024-04-30')
			--and AMT > 0  --购买数据
			and a.TRX_CD not in ('BITRXDR2','BITRXDR3','BITRXSYD','BIVTMDR3','BIDRWSN2','BIDRWSY2','BIDWNDRW','BIINTDCN','BIINTDRW','BIINTSCY') --剔除交易类型中包含“撤单”的数据
			--and INSU_BRED_RSK_LVL_CD in ('R3','R4','R5')  --保险需要全量数据
			
				)
	where row1=1 
	--and sys_cd is null
)
WITH DATA;
--陈庆华提供
推荐号在 TBI_TRX_SAL_T 这张表，交易表你们知道用哪张吧
select 
* 
from cmb.TBI_TRX_DTA_T 
inner join cmb.TBI_TRX_SAL_T 
on BTX_BIL_NBR = BSA_BIL_NBR 
and BTX_TRX_NBR = BSA_TRX_NBR
where BTX_TRX_DAT >= 20230101;

推荐人1 BSA_INT_DC1，推荐号，可以再去关联韩亚老师那边的推荐人表获取推荐人明细
双录人 BSA_VCD_MAG.






七、推荐人编号
推荐人从推荐表里获取，表中 cust_id、eac_id基本都是空的 ，需要用基金交易回报事件表里的MSG_APL_ID 关联推荐流水号，
select * from pdm_vhis.t03_fnd_rcm_inf 
where RCM_SRL in('***********','***********') --对应T05_FND_TRX_RPLY_EVT中的 MSG_APL_ID 报文申请编号 


基金推荐编号
	从 T05_FND_TRX_APL_EVT  的 RCM_PSN_ID 字段获取，但是这个编号可能有乱码，具体进展需要等卢聪看一下

SA理财推荐编号
	SA理财交易，用 T05_SA_AGN_FIN_MNG_TRX_APL_EVT 表查询
	--口径提供人 黄少梅
	推荐编号，用申报表的 TRX_SEQ_NBR 流水号 关联 T03_SA_AGN_FIN_MNG_RCM_INF的Trx_Srl这个字段，取 Slf_Rcm_Psn_Nbr 为推荐人编号 
	如果查询记录不多，可以直接到推荐表查询就好
	
	--需要推荐人号码，现提供交易申请事件，经办人、操作员都为空
	select 
		Slf_Rcm_Psn_Nbr
	from PDM_VHIS.T05_SA_AGN_FIN_MNG_TRX_APL_EVT a   --SA代销理财交易申请事件
	inner join PDM_VHIS.T03_SA_AGN_FIN_MNG_RCM_INF b
		on a.TRX_SEQ_NBR = b.Trx_Srl
	where a.eac_id = 'xxxx'
		and TRX_CD not in ('FD43')  --排除分红交易

八、双录信息

--口径提供人  仲维鲁  基金理财保险都可以从这里出
--这部分数据只有19年往后的，之前的可以找仲再协助查一下看看	


--对于产品交易，可以使用下面代码处理  
--但是无法直接过滤是基金 还是理财，因为表里没有这个类型字段，所以如果大批量的查询，可能会出现发散的情况，需要注意
left join NDS_VHIS.NLF39_RMS_CT_DBL_RCD_CSLD_MRG_SRL m
	on a.eac_id = m.Cust_Card_Nbr
	and a.TRX_DT = m.DBL_RCD_DT
left join NDS_VHIS.NLF39_RMS_CT_DBL_RCD_CSLD_PROD_MAP_INF n
	on  m.Src_Sys_Dbl_Rcd_Srl_Id = n.SRC_SYS_DBL_RCD_SRL_ID   --双录流水号
	and m.CHNL_CD = n.CHNL_CD --双录渠道

select 
m.CUST_UID	客户UID
,m.CUST_CARD_NBR	客户卡号
,m.CUST_NM	客户姓名
,m.CUST_FIN_RSK	客户理财风评
,m.CUST_PE_RSK	客户私募风评
,m.CUST_BLG_BRN_ORG_ID	客户管户网点机构编号
,m.CUST_BLG_BRN_ORG_NM	客户管户网点机构名称
,m.CUST_BLG_CM_USR_ID	客户管户客户经理用户编号
,m.CUST_BLG_CM_NM	客户管户客户经理姓名
,m.DBL_RCD_DT	双录日期
,m.DBL_RCD_START_TM	双录开始时间
,m.DBL_RCD_END_TM	双录结束时间
,m.BUS_TYP_CD	业务类型代码
,m.BUS_TYP_NM	业务类型名称
,m.DBL_RCD_STS_CD	双录状态代码 --(Y：有效，N：无效，C：待确认)
,n.PROD_NM 产品名称
,n.AMT 金额
,m.SRC_SYS_DBL_RCD_SRL_ID 双录流水号
,m.CHNL_CD 双录渠道
from NDS_VHIS.NLF39_RMS_CT_DBL_RCD_CSLD_MRG_SRL m
inner join lc_tmp b
	on m.Cust_Card_Nbr = b.eac_id --如果按照cust_id关联可能会发散，这里eac_id不为空的概率比较大
left join NDS_VHIS.NLF39_RMS_CT_DBL_RCD_CSLD_PROD_MAP_INF n
	on m.Src_Sys_Dbl_Rcd_Srl_Id = n.SRC_SYS_DBL_RCD_SRL_ID   --双录流水号
	and m.CHNL_CD = n.CHNL_CD --双录渠道
where m.DBL_RCD_DT between '2023-01-01' and '2024-11-10'



select 
*
from NDS_VHIS.NLF39_RMS_CT_DBL_RCD_CSLD_MRG_SRL
where (cust_id in ('1100119750') or Cust_Card_Nbr in ('***敏感信息系统已自动屏蔽***','***敏感信息系统已自动屏蔽***','***敏感信息系统已自动屏蔽***','***敏感信息系统已自动屏蔽***'))
Cust_Id 你用这个字段搜客户号
Cust_Card_Nbr 这个搜卡号


-- 基金双录流水号，龚芬，如果这里也没有查到，那就是没有了
	SELECT * FROM NDS_VHIS.NLK02_TQH_TRX_MRK_T  WHERE MRK_CLT_NBR='客户号' AND MRK_MAK_TYP in('DC','VD')   可以尝试查一下，咨询了段祥越，这个表的数据就是全量数据了，没有就是没有了
	
	当MRK_MAK_TYP=DC时，MRK_DEV_NB2保存双录流水
	当MRK_MAK_TYP=VD,且MRK_NAR_EXT=空，MRK_DEV_NB1保存设备号
	MRK_DEV_NB2保存双录流水
	当MRK_MAK_TYP=VD且MRK_NAR_EXT.C11=Y
	MRK_IPP_DTA保存双录渠道码
	MRK_DEV_NB1保存双录流水
	MRK_DEV_NB2保存设备号
	MRK_NAR_EXT前10位保存产品序号
	
	尝试使用套号关联看看
	select 
	*
	from 
	NDS_VHIS.NLK02_TQH_TRX_MRK_T
	where MRK_TRX_SET in (
		select 
			distinct b.Msg_Apl_Id
		from pdm_vhis.T05_FND_TRX_APL_EVT b
		inner join lc_tmp  h
			on b.eac_id = h.eac_id
		where (b.DT between '2001-12-14' and '2024-02-05' or b.ENTR_DT between '2001-12-14' and '2024-02-05')
	)
	 AND MRK_MAK_TYP in('DC','VD')
	

受托理财双录信息 
	背景：该客户购买了一笔受托理财，需要证据证明是他本人购买的。 于是想要通过双录来证明。
	--口径提供人 程明斐 80374433 
	如果在可视柜台渠道的话，可以查询这张表： PDM_VHIS.T05_VOC_RCD_EVT 
	手机银行购买时没有强制关联我们的双录，所以可能没有双录信息



SA理财双录信息
	--口径提供人 黄少梅
	NLW24_TQH_VDO_DTA_T
	此表记录了双录信息，如果交易有双录记录，那么就可以通过交易流水关联获取到，即 T05_SA_AGN_FIN_MNG_TRX_APL_EVT 表的 TRX_SEQ_NBR 关联 VDO_TRX_SER

	select 
	a.*
	from NDS_VHIS.NLW24_TQH_VDO_DTA_T a
	inner join pdm_vhis.T05_SA_AGN_FIN_MNG_TRX_APL_EVT b
	on a.VDO_TRX_SER = b.TRX_SEQ_NBR
	and b.eac_id in ('xxxx'	)






十、勾选电子合同日志
思路：先确认客户购买交易记录、产品信息；  再去获取查询日志数据，进一步可以在excel全量搜索产品代码，看看是否有相关埋点记录，再联系app开发人员 尝试翻译一下
理财：杨经纬 80274829  、吴迪
基金： 董禹威 80296305、林娅莉 手机银行，//  杨雷，80274764  ；公募基金  李季 80274991


--表负责人 胡江涛
MCOA_DATA.MCOA_CNL_D_MBL_OPR_STAT 手机银行APP操作统计表  客户有操作就有记录，但不记录地理位置

-- 表负责人 陶涛 8309740
AGIA_CUST_LGN_STREET_TMP_CN   用户每日登录国内四级地址信息表  客户有登录就有数据，最早21年3月21日
NLT24_AGIA_COE_CUST_LGN_INF_BIZ_V1  用户登录信息数据表  此表只记录客户近半年的登录行为，最细粒度是客户+地理位置；


日志表口径提供人  孔丘逸80234729  下面几个表也是咨询她
pdm_vhis.T05_IDV_NB_USR_OPR_EVT a --个人网银用户操作事件，数据 包含手机银行登录成功 发生交易成功 + 大众网银(日志少)的登录 交易日志，数据可以做参考，不做准确数据分析

下面几个表都是手机银行各行为日志，包含成功与失败的数据
PDM_VHIS.T05_MBL_BNK_LGN_JNL --手机银行登录日志 ，可以使用 Log_File_Abr =  'LUI' 来做登录筛选？
P_CPDM_VC.T05_MBL_BNK_TRX_JNL  --手机银行交易日志
P_CPDM_VC.T05_MBL_BNK_QRY_JNL --手机银行查询日志

这个表要咨询其他组的人
P_CPDM_VC.T05_MBL_BNK_EVT_TRC_JNL		--手机银行客户端埋点日志





drop table if exists txr_tmp;
create temporary table txr_tmp         
WITH (orientation = column, colversion = 2.0, compression = middle)
distribute by hash (eac_id)
AS (
	--申购赎回记录(上面是获取交易记录，比如资金购买、分红等，但是不包含申请时间与日期)，这个包含申请时间
	select 
		distinct 
		h.cust_nm
		,b.eac_id
		,h.cust_id
		,h.cust_uid
		,b.DT
		,c.USR_NBR
	from pdm_vhis.T05_FND_TRX_APL_EVT b
	inner join lc_tmp  h
		on b.eac_id = h.eac_id
	INNER JOIN PDM_VHIS.T01_AIO_NET_USR_INF_S c	 
		ON c.DOC_NBR =h.OPN_DOC_NBR 
		AND c.DW_SNSH_DT = CURRENT_DATE - 1  ----一网通客户信息快照
	where (b.DT BETWEEN '2021-04-01' and '2021-07-31' or b.ENTR_DT BETWEEN '2021-04-01' and '2021-07-31')
	and substring(b.TRX_OBJ_ID,5,6) in ('011783')
	and ENTR_AMT > 0

)
WITH DATA;



--获取操作日志，这里高斯A数据最全，但是也只有16年往后的数据，并且还缺少18 19年的数据，需要去历史库查询；  灵活系统可以查到全量时间段的数据，但是没有地理位置
select 
h.cust_nm 客户名称
,h.eac_id 交易卡号
,h.cust_id 客户号
,h.cust_uid 客户uid
,h.DT 交易日期
,h.USR_NBR 一网通号
,EVT_ID 事件编号,DW_DAT_DT 数据日期,JNL_SEQ_NBR 日志序号,USR_TYP_CD 用户类型代码,EQP_TYP_CD 设备类型代码,BUS_SYS_TYP_CD 业务系统类型代码,EDTN_NBR 版本号,TMN_INF 终端信息
,a.TRX_CD 交易代码
,b._DESCRIPTION 交易类型名称
,JNL_TM 日志时间,LGN_ID_TYP_CD 登录编号类型代码,LGN_ID 登录编号,PHY_ID_TYP_CD 物理编号类型代码,PHY_ID 物理编号,INN_IP_ADR 互联网IP地址,CARD_TYP_CD 卡类型代码,a.EAC_ID 户口编号,CARD_BRN_ORG_ID 卡分行机构编号,CCY_CD 币种代码,TRX_AMT 交易金额,TRX_FEE_AMT 交易费用金额,TRX_TXT 交易摘要,SSN_ID 会话ID,SYS_EDTN 系统版本,GEO_LOC_INF 地理位置信息,APPIP APPIP,CUST_MBL_NBR 客户手机号码,RCM_ACTV_ID 推荐活动ID,CHNL_NO 渠道码,FIL_FORM_IND 填单标志,RCM_PSN_BBK_ID 推荐人分行编号,RCM_PSN_ORG_ID 推荐人机构编号,UTM_ID UTM_ID,NEW_OLD_USR_IND 新老用户标志
from pdm_vhis.T05_IDV_NB_USR_OPR_EVT a --个人网银用户操作事件
inner join  txr_tmp h
	on (a.lgn_id = h.USR_NBR or a.lgn_id = h.eac_id)
	and a.DW_DAT_DT = h.DT
LEFT JOIN NDS_VHIS.NLA05_TRANSCODE B
	ON A.TRX_CD = B.TRANSCODE
	AND B.ONLINE_STATUS  = '应用中'


select 
	distinct eac_id
from sum_vhis.t83_eac_inf_s 
where Dw_Snsh_Dt = current_date-1
	and cust_id in ('7930008213')
		
substring(b.DT ||' '|| b.TM,1,16)

-- 一般交易都是通过企业网银购买，可以直接查网银日志，然后根据交易日期匹配即可
-- 一般用ip地址取匹配属于银行的ip，还是客户自己的ip
-- 此表可以查到交易ip地址、交易代码等信息
-- 穆永超 谷默/80174962  与系统开发老师确认，经纬度信息并不保真，因为地理位置来自客户授权，是可以伪造的

--注意，这里是按照身份证号关联的，但是最好按照客户的手机号关联，遇到过客户身份证号错误但是手机号换了，只能查到之前21年的记录，查不到换号以后的记录了
SELECT DISTINCT A.CUST_NM,OPN_DOC_NBR,B.USR_NBR--,C.IEX_USR_NBR
FROM SUM_VHIS.T83_EAC_INF_S A
INNER JOIN PDM_VHIS.T01_AIO_NET_USR_INF_S B	 
	ON B.DOC_NBR =A.OPN_DOC_NBR 
	AND B.DW_SNSH_DT = CURRENT_DATE - 1  ----一网通客户信息快照
--INNER JOIN PDM_VHIS.T01_IEX_USR  C
	--ON A.OPN_DOC_NBR=C.DOC_NBR
	--AND C.DW_END_DT='9999-12-31'
WHERE a.DW_SNSH_DT = CURRENT_DATE - 1 
AND A.eac_id IN  ('6214860101667505');




--获取操作日志，这里高斯A数据最全，但是也只有16年往后的数据，并且还缺少18 19年的数据，需要去历史库查询；  灵活系统可以查到全量时间段的数据，但是没有地理位置
select 
EVT_ID 事件编号,DW_DAT_DT 数据日期,JNL_SEQ_NBR 日志序号,USR_TYP_CD 用户类型代码,EQP_TYP_CD 设备类型代码,BUS_SYS_TYP_CD 业务系统类型代码,EDTN_NBR 版本号,TMN_INF 终端信息
,a.TRX_CD 交易代码
,b._DESCRIPTION 交易类型名称
,JNL_TM 日志时间,LGN_ID_TYP_CD 登录编号类型代码,LGN_ID 登录编号,PHY_ID_TYP_CD 物理编号类型代码,PHY_ID 物理编号,INN_IP_ADR 互联网IP地址,CARD_TYP_CD 卡类型代码,EAC_ID 户口编号,CARD_BRN_ORG_ID 卡分行机构编号,CCY_CD 币种代码,TRX_AMT 交易金额,TRX_FEE_AMT 交易费用金额,TRX_TXT 交易摘要,SSN_ID 会话ID,SYS_EDTN 系统版本,GEO_LOC_INF 地理位置信息,APPIP APPIP,CUST_MBL_NBR 客户手机号码,RCM_ACTV_ID 推荐活动ID,CHNL_NO 渠道码,FIL_FORM_IND 填单标志,RCM_PSN_BBK_ID 推荐人分行编号,RCM_PSN_ORG_ID 推荐人机构编号,UTM_ID UTM_ID,NEW_OLD_USR_IND 新老用户标志
from pdm_vhis.T05_IDV_NB_USR_OPR_EVT a --个人网银用户操作事件 
LEFT JOIN NDS_VHIS.NLA05_TRANSCODE B
	ON A.TRX_CD = B.TRANSCODE
	AND B.ONLINE_STATUS  = '应用中'
	--where eac_id in ('***********','***********','***********','***********','***********')
	where lgn_id in ('040167383505','6214860122340728') --有时候登录编号既有lgn_id，还包含卡号，因此最好都加上，比较全
	and DW_DAT_DT in ('2021-06-22','2021-07-22','2021-07-15')


高斯分析
--获取操作日志，这里高斯A数据最全，但是也只有16年往后的数据，并且还缺少18 19年的数据，需要去历史库查询；  灵活系统可以查到全量时间段的数据，但是没有地理位置
select 
EVT_ID 事件编号,DW_DAT_DT 数据日期,JNL_SEQ_NBR 日志序号,USR_TYP_CD 用户类型代码,EQP_TYP_CD 设备类型代码,BUS_SYS_TYP_CD 业务系统类型代码,EDTN_NBR 版本号,TMN_INF 终端信息
,a.TRX_CD 交易代码
,b._DESCRIPTION 交易类型名称
,JNL_TM 日志时间,LGN_ID_TYP_CD 登录编号类型代码,LGN_ID 登录编号,PHY_ID_TYP_CD 物理编号类型代码,PHY_ID 物理编号,INN_IP_ADR 互联网IP地址,CARD_TYP_CD 卡类型代码,EAC_ID 户口编号,CARD_BRN_ORG_ID 卡分行机构编号,CCY_CD 币种代码,TRX_AMT 交易金额,TRX_FEE_AMT 交易费用金额,TRX_TXT 交易摘要,SSN_ID 会话ID,SYS_EDTN 系统版本,GEO_LOC_INF 地理位置信息,APPIP APPIP,CUST_MBL_NBR 客户手机号码,RCM_ACTV_ID 推荐活动ID,CHNL_NO 渠道码,FIL_FORM_IND 填单标志,RCM_PSN_BBK_ID 推荐人分行编号,RCM_PSN_ORG_ID 推荐人机构编号,UTM_ID UTM_ID,NEW_OLD_USR_IND 新老用户标志
from pdm_view_src.T05_IDV_NB_USR_OPR_EVT a --个人网银用户操作事件 
LEFT JOIN NDS_data_src.NLA05_TRANSCODE B
	ON A.TRX_CD = B.TRANSCODE
	AND B.ONLINE_STATUS  = '应用中'
	--where eac_id in ('***********','***********','***********','***********','***********')
	where lgn_id in ('050173082985','6231365167388888') --有时候登录编号既有lgn_id，还包含卡号，因此最好都加上，比较全
	and DW_DAT_DT in ('2021-06-22','2021-07-22','2021-07-15')
十一、理财账户流水

--查询理财账户口径，D+群里有聊天记录
select 
acct_nbr
from NDS_VHIS.NLW57_DRT_ACT_INFO_T
where CLIENT_ID = '1100119750'
and acct_code = '10008'


口径提供人  陈旭明  段祥越 

select 
EVT_ID 事件编号,DW_DAT_DT 数据日期,TRX_STS_CD 交易状态代码,TRX_TYP_CD 交易类型代码,TRX_BOK_ENTR_DT 交易记账日期,TRX_SRL_NBR_SET 交易流水套号,TRX_PST_DT 交易过账日期,BUS_TYP_CD 业务类型代码,BUS_CD 业务代码,BOK_ENTR_BBK_ORG_ID 记账分行机构编号,BOK_ENTR_ORG_ID 记账机构编号,ACT_BBK_ORG_ID 账户分行机构编号,ACT_ORG_ID 账户机构编号,TRX_CD 交易代码,ACT_ID 账户编号,ACT_MDF 账户修饰符,ACT_ITM_ID 账户科目编号,CCY_CD 币种代码,FND_CCY_MKT_CD 基金货币市场代码,FND_CRD_AND_DBT_DIR_CD 资金借贷方向代码,FND_TRX_BAL 资金交易余额,FND_ONL_TRX_BAL 资金联机交易余额,FND_TRX_RQS_AMT 资金交易请求金额,FND_AUT_CFM_AMT 资金授权确认金额,FND_VAL_DT 资金起息日期,FND_CLR_MTH_CD 资金清算方式代码,FND_CLR_DT 资金清算日期,FND_CLR_TM 资金清算时间,FND_LMT_TYP_CD 资金额度类型代码,FND_LMT_AUT_QTY 资金额度授权数量,FND_LMT_CFM_QTY 资金额度确认数量,FND_LMT_TRX_QTY 资金额度交易数量,PRC_TYP_CD 价格类型代码,PRC 价格,INTR_RAT_CD 利率代码,INTR_RAT 利率,TA_NO TA码,SA_NO SA码,TRX_MKT_CD 交易市场代码,WRNT_ACT_NBR 权证账户号码,WRNT_NO 权证代码,WRNT_TRST_ACT_ID 权证托管账户编号,WRNT_TRX_CTR_NBR 权证交易合同号码,WRNT_CRD_AND_DBT_DIR_CD 权证借贷方向代码,WRNT_TRX_AMT 权证交易金额,WRNT_ONL_BAL 权证联机余额,WRNT_AUT_RQS_AMT 权证授权请求金额,WRNT_AUT_CFM_AMT 权证授权确认金额,WRNT_CLR_MTH_CD 权证清算方式代码,WRNT_CLR_DT 权证清算日期,WRNT_CLR_TM 权证清算时间,ANA_1_CD 分析1代码,CCY_1_CD 币种1代码,XPN_AMT_1 扩展金额1,ANA_2_CD 分析2代码,CCY_2_CD 币种2代码,XPN_AMT_2 扩展金额2,ANA_3_CD 分析3代码,CCY_3_CD 币种3代码,XPN_AMT_3 扩展金额3,RVS_OR_SPL_ENTR_TYP_CD 冲补账类型代码,REL_TRX_TRX_DT 关联交易交易日期,REL_TRX_SEQ 关联交易流水号,BRK_NBR 经纪号码,BOK_ENTR_OPR 记账操作员,AUT_OPR 授权操作员,PSW_VRF_MTH 密码校验方式,TRX_SRC_CD 交易来源代码,TRX_SRC_SIT 交易来源地点,QRY_LVL 查询级别,STAT_NO 统计码,OUR_SMR 我方摘要,YOUR_SMR 你方摘要,XPN_SMR 扩展摘要,DT 日期,TM 时间,ESP_NO_20 特殊码20,ACT_TYP_CD 账户类型代码,ACT_BOK_ENTR_IND 会计记账标志,RCALC_BAL_IND 重算余额标志,ADVS_SRL_NBR_NO 通知序列号码,ESP_NO_80 特殊码80,PRGR_IND 在途代码,TRX_SEQ 交易流水号
 from PDM_VHIS.T05_FI_TRX_EVT
where act_id in ('110011975091015','110011975093211','110011975091011')
and DW_DAT_DT between '2021-08-01' and '2021-12-31'

这个表数据不全，需要使用带有日期后缀的表去历史库查询
select *from PDM_AVIEW.T05_FI_TRX_EVT_20190808
where trim(act_id) = '110594418491011'
and prt_d between '2015-01-01' and '2016-12-31'


离线里查到了
select *from P_CPDM_VC.T05_FI_TRX_EVT
where act_id = '110594418491011'
and prt_dt between '2015-01-01' and '2016-12-31'



十三、定投设置

NDS_VHIS.NLK02_TFD_JRQ_DTA_T 定投申请文件，包含定投的申请、修改、状态变更记录
定投设置时间，参考《北京分行_定投设置时间_参考建立时间字段》文件中的“建立时间字段”；
勾选超风险标记，参考《北京分行_定投设置时间_参考建立时间字段》文件中的风控标记，第一个字母为Y，代表客户勾选了超风险提示；

select 
JRQ_PTN_TAG 分区标志,JRQ_TRX_SER 交易流水,JRQ_JJC_SEQ 协议合同序号,JRQ_PDC_VER 协议合同版本,JRQ_EAC_NBR 户口号,JRQ_CLT_NBR 客户号,JRQ_IAC_NBR 户口序号,JRQ_TAC_NBR 协议编号,JRQ_POS_NBR 分仓代码,JRQ_SYS_COD 系统来源,JRQ_TAA_COD TA代码,JRQ_SAA_COD SA代码,JRQ_FND_COD 产品代码,JRQ_FND_EXT 产品外部代码,JRQ_PRD_COD 产品编码,JRQ_TRX_DAT 交易日期,JRQ_TRX_COD 交易码,JRQ_ORD_AMT 申请金额,JRQ_ORD_QTY 申请份额,JRQ_ORD_PRC 申请价格,JRQ_CCY_NBR 货币代码,JRQ_CCY_TYP 钞汇标记,JRQ_REL_TAA 关联TA代码,JRQ_REL_SAA 关联SA代码,JRQ_REL_FND 关联基金代码,JRQ_PRI_SEQ 优先级,JRQ_PLN_STS 计划状态,JRQ_FUN_SRC 使用资金来源,JRQ_BUS_CLS 业务分类,JRQ_PDB_CTL 定期定额控制,JRQ_PDP_COD 定期定额代码,JRQ_PDB_FRE 定期定额周期,JRQ_PDB_DAY 固定投资日,JRQ_PDB_ITV 定投间隔,JRQ_NXT_PLN 下一期号,JRQ_PDB_DAT 下一投资日,JRQ_PDB_PTY 计划属性,JRQ_PLN_RMK 计划说明,JRQ_MAT_DAT 期满日期,JRQ_MAT_CNT 期满次数,JRQ_MAT_AMT 期满金额,JRQ_MAT_QTY 期满份额,JRQ_STS_RSN 终止原因,JRQ_DAT_RSN 终止日期,JRQ_EXP_YLD 期满收益率,JRQ_DSC_SAA SA费用折扣,JRQ_CLT_RSK 客户风险等级,JRQ_CTL_COD 风控标记,JRQ_CTL_MOD 方式控制,JRQ_CTL_SEL 赎回控制,JRQ_CTL_DAT 控制日期,JRQ_CTL_AMT 控制金额,JRQ_CTL_QTY 控制份额,JRQ_CTL_SET 关联套号,JRQ_CTL_SEQ 关联流水,JRQ_EXT_APP 外部流水,JRQ_BAK_COD 备用代码,JRQ_BAK_CD2 备用代码2,JRQ_BAK_DAT 备用日期,JRQ_BAK_DT2 备用日期2,JRQ_BAK_AMT 备用金额1,JRQ_BAK_AM2 备用金额2,JRQ_ENT_OPR 录入操作员,JRQ_CHK_OPR 复核操作员,JRQ_TRX_BBK 交易分行,JRQ_TRX_BRN 交易部门,JRQ_TRX_CHN 交易渠道,JRQ_TRX_ADR 交易地点,JRQ_SUB_CHN 子渠道,JRQ_BRK_NBR 经纪号码,JRQ_BUS_SRC 业务来源,JRQ_BUS_SER 业务来源流水,JRQ_DLR_NAM 经办人姓名,JRQ_DLR_TYP 经办人类型,JRQ_DLR_NBR 经办人号码,JRQ_SAL_TYP 推荐人类型,JRQ_SAL_NBR 推荐人代码,JRQ_INT_VER 接口版本,JRQ_WKE_COD 作业条,JRQ_NAR_OUR 我方摘要,JRQ_NAR_YOU 你方摘要,JRQ_NAR_EXT 扩展摘要,JRQ_OPN_DTM 建立时间,JRQ_UPD_DTM 更新时间,JRQ_REC_STS 记录状态,JRQ_REC_VER 记录版本,JRQ_SPC_080 保留字段,DW_SRC_TBL 来源分表
from NDS_VHIS.NLK02_TFD_JRQ_DTA_T 
where JRQ_CLT_NBR = '1106106263'
and JRQ_TRX_COD = 'FD59'


两表可以通过JRQ_JJC_SEQ关联JJC_JJC_SEQ
定投协议文件，可以看到定投协议号与合同号
select 
JJC_PTN_TAG 分区因子,JJC_JJC_SEQ 协议序号,JJC_PDC_NBR 协议合同号,JJC_PDC_VER 协议合同版本,JJC_STS_COD 协议状态,JJC_CLT_NBR 客户号,JJC_IAC_NBR 户口序号,JJC_EAC_NBR 户口号,JJC_TAC_NBR 协议编号,JJC_TAC_TYP 协议类型,JJC_POS_NBR 分仓代码,JJC_SYS_COD 系统代码,JJC_TAA_COD TA代码,JJC_SAA_COD SA代码,JJC_FND_COD 产品代码,JJC_FND_EXT 产品外部代码,JJC_ORD_AMT 申请金额,JJC_ORD_QTY 申请份额,JJC_ORD_PRC 申请价格,JJC_CCY_NBR 货币代码,JJC_CCY_TYP 钞汇标记,JJC_REL_TAA 关联TA代码,JJC_REL_SAA 关联SA代码,JJC_REL_FND 关联基金代码,JJC_PRI_SEQ 优先级,JJC_FUN_SRC 使用资金来源,JJC_BUS_CLS 业务分类,JJC_PDB_CTL 定期定额控制,JJC_PDP_COD 定期定额代码,JJC_PDB_FRE 定期定额周期,JJC_PDB_DAY 固定投资日,JJC_CUR_PLN 当前期号,JJC_NXT_PLN 下一期号,JJC_PDB_LDT 上一投资日,JJC_PDB_DAT 下一投资日,JJC_PDB_PTY 计划属性,JJC_PDB_DLY 宽限期,JJC_PDB_FTL 终止次数,JJC_MAT_DAT 期满日期,JJC_MAT_CNT 期满次数,JJC_MAT_AMT 期满金额,JJC_MAT_QTY 期满份额,JJC_STS_RSN 终止原因,JJC_DAT_RSN 终止日期,JJC_EXP_YLD 期满收益率,JJC_SUM_OFT 连续失败周期数,JJC_SUM_OST 连续成功次数,JJC_SUM_OSC 累计委托次数,JJC_SUM_RTC 累计回报次数,JJC_SUM_AMT 累计投资金额,JJC_SUM_QTY 累计投资份额,JJC_SUM_FEE 累计确认费用,JJC_SUM_OFC 累计失败次数,JJC_SUM_ZAM 累计支付金额,JJC_SUM_ZCN 累计支付成功次数,JJC_DEA_DAT 最近确认日期,JJC_CUR_DAT 市值日期,JJC_CUR_MKT 客户市值,JJC_LDE_DAT 上次市值日期,JJC_LDE_MKT 上次客户市值,JJC_LST_DAT 最近执行的日期,JJC_LST_RST 最近一次执行结果,JJC_JDT

两表可以通过JRQ_JJC_SEQ关联JJC_JJC_SEQ
定投协议文件，可以看到定投协议号与合同号
select 
JJC_PTN_TAG 分区因子,JJC_JJC_SEQ 协议序号,JJC_PDC_NBR 协议合同号,JJC_PDC_VER 协议合同版本,JJC_STS_COD 协议状态,JJC_CLT_NBR 客户号,JJC_IAC_NBR 户口序号,JJC_EAC_NBR 户口号,JJC_TAC_NBR 协议编号,JJC_TAC_TYP 协议类型,JJC_POS_NBR 分仓代码,JJC_SYS_COD 系统代码,JJC_TAA_COD TA代码,JJC_SAA_COD SA代码,JJC_FND_COD 产品代码,JJC_FND_EXT 产品外部代码,JJC_ORD_AMT 申请金额,JJC_ORD_QTY 申请份额,JJC_ORD_PRC 申请价格,JJC_CCY_NBR 货币代码,JJC_CCY_TYP 钞汇标记,JJC_REL_TAA 关联TA代码,JJC_REL_SAA 关联SA代码,JJC_REL_FND 关联基金代码,JJC_PRI_SEQ 优先级,JJC_FUN_SRC 使用资金来源,JJC_BUS_CLS 业务分类,JJC_PDB_CTL 定期定额控制,JJC_PDP_COD 定期定额代码,JJC_PDB_FRE 定期定额周期,JJC_PDB_DAY 固定投资日,JJC_CUR_PLN 当前期号,JJC_NXT_PLN 下一期号,JJC_PDB_LDT 上一投资日,JJC_PDB_DAT 下一投资日,JJC_PDB_PTY 计划属性,JJC_PDB_DLY 宽限期,JJC_PDB_FTL 终止次数,JJC_MAT_DAT 期满日期,JJC_MAT_CNT 期满次数,JJC_MAT_AMT 期满金额,JJC_MAT_QTY 期满份额,JJC_STS_RSN 终止原因,JJC_DAT_RSN 终止日期,JJC_EXP_YLD 期满收益率,JJC_SUM_OFT 连续失败周期数,JJC_SUM_OST 连续成功次数,JJC_SUM_OSC 累计委托次数,JJC_SUM_RTC 累计回报次数,JJC_SUM_AMT 累计投资金额,JJC_SUM_QTY 累计投资份额,JJC_SUM_FEE 累计确认费用,JJC_SUM_OFC 累计失败次数,JJC_SUM_ZAM 累计支付金额,JJC_SUM_ZCN 累计支付成功次数,JJC_DEA_DAT 最近确认日期,JJC_CUR_DAT 市值日期,JJC_CUR_MKT 客户市值,JJC_LDE_DAT 上次市值日期,JJC_LDE_MKT 上次客户市值,JJC_LST_DAT 最近执行的日期,JJC_LST_RST 最近一次执行结果,JJC_JDT_DAT 计划生成日期,JJC_CTL_RMK 备注信息,JJC_CTL_MOD 控制模式,JJC_CTL_COD 控制码,JJC_NTF_DAT 通知日期,JJC_NTF_CNT 通知次数,JJC_CTL_DAT 控制日期,JJC_CTL_DT2 控制日期2,JJC_CTL_AMT 控制金额,JJC_CTL_AM2 控制金额2,JJC_CTL_QTY 控制份额,JJC_CTL_QT2 控制份额2,JJC_CTL_CNT 控制次数,JJC_CTL_CN2 控制次数,JJC_CTL_RAT 控制费率,JJC_CTL_SET 关联套号,JJC_CTL_SEQ 关联流水,JJC_TRX_CHN 交易渠道,JJC_TRX_ADR 交易地点,JJC_CTR_BBK 协议分行,JJC_CTR_BRN 协议部门,JJC_BUS_SRC 业务来源,JJC_BUS_SER 业务来源流水,JJC_SAL_TYP 推荐类型,JJC_SAL_NBR 推荐号码,JJC_ENT_OPR 录入操作员,JJC_CHK_OPR 复核操作员,JJC_WKE_COD 作业条,JJC_INT_VER 接口版本,JJC_OPN_DTM 建立时间,JJC_UPD_DTM 更新时间,JJC_REC_STS 记录状态,JJC_REC_VER 记录版本,JJC_NAR_OUR 我方摘要,JJC_NAR_YOU 你方摘要,JJC_NAR_EXT 扩展摘要,JJC_SPC_COD 保留字段
from 
NDS_VHIS.NLK02_TFD_JJC_DTA_T  --定投协议文件
where JJC_CLT_NBR = '1106106263'  --客户号
十四、重构前的表
基金
--申购赎回记录
select 
	h.cust_nm 				as 账户名
	,b.eac_id  				as 账户编号
	,b.TRX_OBJ_ID 		as 交易标的编号
	,b.DT	申请日期
	,b.TM	申请时间
	,c.FND_FND_INN				as 基金代码
	,c.FND_FUL_NAM    		as 产品中文名称
	--,c.FND_PROD_TYP_NM 		as 基金产品类型名称
	,c.FND_CRP_MID		as 基金管理公司名称
	,c.FND_CRP_COD		as 基金管理公司代码
	,b.ENTR_AMT				as 委托金额
	,b.ENTR_LOT				as 委托份额
	,b.FND_TRX_AMT 			as 资金交易金额
	,b.FEE_AMT				as 费用金额
	,case TRX_CD 
		when 'FDE1'	then '签署约定书' when 'FDE2'	then '撤销约定书' when 'FDE3'	then '签电子合同' when 'FDE4'	then '签纸质合同' when 'FD01'	then '开户' when 'FD02'	then '关户' when 'FD03'	then '修改资料' when 'FD04'	then '账户冻结' when 'FD05'	then '账户解冻' when 'FD06'	then '挂失' when 'FD07'	then '解挂' when 'FD08'	then '增加交易账号' when 'FD09'	then '撤销交易账号' when 'FD2D'	then '认购利息' when 'FD2F'	then '预约购买' when 'FD20'	then '认购' when 'FD22'	then '申购' when 'FD24'	then '赎回' when 'FD26'	then '转托管' when 'FD27'	then '持仓转入' when 'FD28'	then '持仓转出' when 'FD29'	then '修改分红方式' when 'FD30'	then '认购结果' when 'FD31'	then '基金份额冻结' when 'FD32'	then '基金份额解冻' when 'FD34'	then '非交易过户转入' when 'FD35'	then '非交易过户转出' when 'FD36'	then '基金转换' when 'FD37'	then '基金转换转入' when 'FD38'	then '基金转换转出' when 'FD39'	then '定时定额投资' when 'FD42'	then '强行赎回' when 'FD43'	then '红利' when 'FD44'	then '强行调增' when 'FD45'	then '强行调减' when 'FD49'	then '募集失败' when 'FD5A'	then '定期赎回申请' when 'FD5B'	then '条件申购申请' when 'FD5C'	then '条件赎回申请' when 'FD5D'	then '条件计划撤回' when 'FD50'	then '基金清盘' when 'FD52'	then '撤单' when 'FD59'	then '定期定额申请' when 'FD60'	then '定期定额撤销' when 'FD61'	then '定期定额修改' when 'FD98'	then '快速赎回' when 'FDLB'	then '受益权受让' when 'FDLS'	then '受益权出让'
		else TRX_CD
		end 				as 投资理财交易代码
	,case b.TRX_CHNL_TYP_CD
		when 'TEL' then '电话语音'
		when 'TGS' then '托管银行清算系统'
		when 'BCK' then '后台'
		when 'CAL' then '电话人工'
		when 'DSK' then '柜台'
		when 'FSY' then '金融平台'
		when 'ICO' then '企业银行'
		when 'IEX' then '个人银行专业版'
		when 'INT' then '个人银行大众版'
		when 'MPH' then '手机银行'
		when 'RVC' then '可视柜台'
		else b.TRX_CHNL_TYP_CD 
		end as 交易渠道
		,TRX_BBK_ORG_ID 交易分行机构编号
	,TRX_ORG_ID 交易机构编号
	--,b.RCM_PSN_ID			as 推荐人编号1  --这个字段和推荐人无关，段祥越
	,coalesce(
		if(d.INT_RCM_PSN_1_NBR = '',NULL,d.INT_RCM_PSN_1_NBR),
		if(d.INT_RCM_PSN_2_NBR = '',NULL,d.INT_RCM_PSN_2_NBR),
		if(d.INT_RCM_PSN_3_NBR = '',NULL,d.INT_RCM_PSN_3_NBR),
		if(d.EXT_RCM_PSN_NBR = '',NULL,d.EXT_RCM_PSN_NBR),
		if(d.SLF_RCM_PSN_NBR = '',NULL,d.SLF_RCM_PSN_NBR),
		--基金定投推荐人需要使用定投协议表关联推荐人表
		if(n.INT_RCM_PSN_1_NBR = '',NULL,n.INT_RCM_PSN_1_NBR),
		if(n.INT_RCM_PSN_2_NBR = '',NULL,n.INT_RCM_PSN_2_NBR),
		if(n.INT_RCM_PSN_3_NBR = '',NULL,n.INT_RCM_PSN_3_NBR),
		if(n.EXT_RCM_PSN_NBR = '',NULL,n.EXT_RCM_PSN_NBR),
		if(n.SLF_RCM_PSN_NBR = '',NULL,n.SLF_RCM_PSN_NBR),
		''
		) as tuijiaren --推荐人编号, 这里推荐人都是空值，就暂时不获取推荐机构编号数据了
	,substring(c.FND_FND_TYP, 5,1)  			as 风险等级
	, ANA_NO 分析码
	, Mth_Ctrl 方式控制   --TOD_CTL_MOD
from pdm_vhis.T05_FND_TRX_APL_EVT b
inner join lc_tmp  h
	on b.eac_id = h.eac_id
left join NDS_VHIS.NLK02_TFE_FND_DEF_T c --补充到原始口径，没有关联条件，需要再问问看
	on b.TRX_OBJ_ID= 'LK02'||c.FND_FND_INN||c.FND_SAA_COD
	--and substring(c.FND_FND_TYP, 2,1) in ('A','B')  公募或私募由业务自行判断，类型可以看 孙晓梦的聊天记录
left join NDS_VHIS.NLK02_TFD_JJC_DTA_T m
	on b.Rel_Apl_Id = m.JJC_PDC_NBR
	and (m.JJC_PDC_NBR <> '' and m.JJC_PDC_NBR is not null)
left join pdm_vhis.t03_fnd_rcm_inf n
	on m.JJC_PTN_TAG || m.JJC_JJC_SEQ = n.RCM_SRL
left join pdm_vhis.t03_fnd_rcm_inf d
	on b.MSG_APL_ID = d.RCM_SRL
where (b.DT between '2023-10-01' and '2024-01-31' or b.ENTR_DT between '2023-10-01' and '2024-01-31')
and substring(b.TRX_OBJ_ID,5,6) in (xxxx) 
--and TRX_CD in ('FD20','FD22','FD39')   申购记录，包含定投非定投
	--and TRX_CD in ('FD20','FD22') and substring(Mth_Ctrl,1,1) not in ('0','6') --基金申购数据  --,'FD52'沟通后，剔除撤单数据
	--and TRX_CD in ('FD22','FD39') and substring(Mth_Ctrl,1,1) in ('0','6')   --刘俊金提供口径，定投产品申购交易数据
left join NDS_VHIS.NLK02_TFE_FND_DEF_T c --补充到原始口径，没有关联条件，需要再问问看
	on b.TRX_OBJ_ID= 'LK02'||c.FND_FND_INN||c.FND_SAA_COD
	--and substring(c.FND_FND_TYP, 2,1) in ('A','B')  公募或私募由业务自行判断，类型可以看 孙晓梦的聊天记录
left join NDS_VHIS.NLK02_TFD_JJC_DTA_T m
	on b.Rel_Apl_Id = m.JJC_PDC_NBR
	and (m.JJC_PDC_NBR <> '' and m.JJC_PDC_NBR is not null)
left join pdm_vhis.t03_fnd_rcm_inf n
	on m.JJC_PTN_TAG || m.JJC_JJC_SEQ = n.RCM_SRL
left join pdm_vhis.t03_fnd_rcm_inf d
	on b.MSG_APL_ID = d.RCM_SRL
where (b.DT between '2023-10-01' and '2024-01-31' or b.ENTR_DT between '2023-10-01' and '2024-01-31')
and substring(b.TRX_OBJ_ID,5,6) in (xxxx) 
--and TRX_CD in ('FD20','FD22','FD39')   申购记录，包含定投非定投
	--and TRX_CD in ('FD20','FD22') and substring(Mth_Ctrl,1,1) not in ('0','6') --基金申购数据  --,'FD52'沟通后，剔除撤单数据
	--and TRX_CD in ('FD22','FD39') and substring(Mth_Ctrl,1,1) in ('0','6')   --刘俊金提供口径，定投产品申购交易数据
