## 业务概述

### 业务定义

#### 借记户口

我行户口按开户人不同,可以分为个人户口和单位户口。个人户口按账户的性质,可以分为借记户口和贷记户口。借记户口包括借记卡一卡通、存折、存单以及财富账户。

借记卡具有转账结算、存取现金、购物消费、理财等功能,存款计算利息,但不能透支。借记卡即一卡通," 一卡通 " 是基于客户号管理的、以真实姓名开户的银行借记卡,集本外币、定活期、多储种、多币种和多功能于一身的个人综合理财工具：可以为个人客户提供一卡多户、全国通存通兑、定期存款自动转存、自动转账、商户消费、ATM 存取款、多渠道业务查询、网上银行业务、网上支付、自助缴费、代发代扣、证券转账、基金买卖、国债买卖、外汇买卖、黄金买卖、自助贷款以及其他个人理财业务等 30 多项业务功能。关于借记卡制卡请参考 [文档](https://doc.cmbchina.com/f/v?id=zkrFS)

一卡通根据不同的标准,可以有不同分类,目前常用的分类标准为：按卡片等级划分、按制卡模式划分和按品牌划分。

当前银行账户分为三种：Ⅰ类户是全功能的银行结算账户,Ⅱ类户可以通过电子方式办理资金划转、购买投资理财产品、办理限定金额的消费和缴费支付等,Ⅲ类账户仅能办理小额消费及缴费支付业务。

#### 财富账户

财富账户是本行继一卡通后推出的一种新生代的管理型银行账户,不同于一般功能性的银行卡。它本身既是一个整合了多币种存款、全方位投资以及自动理财功 能为一体的无实物卡片的银行账户,同时也通过与任何银行卡或银行账户建立勾连,将资金统一调拨到有需要的银行卡或银行账户中,统一安排资金的使用,形成集 中的现金管理和投资管理。

财富账户的好处 ：  
    便利：多个账户合一,全方位的投资理财账户  
    收益：资金集中,自动理财,有更高的利率回报  
    安全：独立的账户不直接接触公共环境、移动数字证书安全保障、通知服务等多重安全机制,确保资金安全  
    实用：充分结合投资交易和日常收支的需求,让客户做好资产集中配置与财富管理。  
    灵活：多种转账方式,资金运用灵活轻松  
    方便：经由互联网、客户经理、电话银行,客户可随时交易、转账及查询  
    清楚：账户总览、盈亏报告、业务账单等查询功能,清楚告诉客户资产分布、收入来源、支出去向和盈亏状况。

财富账户与一卡通的区别：
- 业务层面
1. 财富账户开户的前提条件,需要有专业版证书（鉴于这个条件,财富账户开户前,必须在我行已开通了一卡通）。
2. 无实物卡片,仅有财富账号。
3. 财富账户为 9 位户口号,前三位为城市码; 新一卡通是 16 位户口号。
- 系统层面
1. 财富账户开户在金融平台,一卡通开户在业务平台。
2. 财富账户与一卡通的开户信息、账户信息以及交易记录分别记录在不同的数据表中。
3. 财富账户的资金账户在核心系统里没有专户对应,按网点、核算种类和币种等开立团结户。

#### 存单种类

**存单**：与存折可以办理活期业务不同,目前存单只能受理单一币种的定期业务,根据开立定期的种类不同,存单可以分为个人整存整取定期存单、个人通知存款存单两种,对应的凭证种类均为 1501 本行存单。

- 个人整存整取定期存单：可以开立本币、外币的个人整存整取定期存单,一个币种对应一个存单。
- 个人通知存款存单：只能开始人民币账户,不支持开立外币存单账户。

存单开户时,系统也提供凭密码、凭印鉴图章、无限制三种取款方式,客户自行在柜台可以选择设置。存单没有查询密码,存单不能在电话银行、自助终端进行账户查询,存单办理业务只能亲临柜台进行办理。

我行存折号为 17 位,组成规则为：10 位客户号 +2 位户口前缀（户口前缀：标识是个人还是对公户,有 01 及 80 两个值,01 表示个人户,80 表示对公户）+5 位顺序号（最后一位为校验位）,共 17 位,如存单户口号：75566250750100135。

#### 存折种类

存折：与一卡通可以办理多币种、多储种的集合功能不同,存折只能受理单一币种的活期储蓄业务,按照存折账户性质的不同,可以分为储蓄存折和结算存折,对应的凭证类型均为 1801 本行存折。

- 储蓄存折：只能办理人民币现金业务,即只能开人民币活期账户,且只能做存取现交易,不能办理转账业务、不能开立外币储蓄存折,且开户时必须存入现金才能开户成功。
- 结算存折：可以开立人民币、外币存折,外币可以开现钞或现汇户,一本存折对应一个币种的活期户; 可以办理存取现、转账业务; 开立人民币或外币现钞户时,需要存入现金才能开户; 开立外币现汇户时,可在开户后再转账。  
开立存折时,存折有三种支付方式：凭密码、凭图章印鉴、不需要支取凭证,而只有凭

密码支取方式的存折才能在全行内进行通存通兑业务,其他两种支取方式的存折,只能在全行内进行通存业务,不能进行通兑业务。

存折不用单独设置查询密码,其查询密码与取款密码一致,登陆网银大众版时,查询密码处即输入的是取款密码。

存折虽然没有一卡通这种多币种、多储种的集合功能,但存折可以直观的展现对应活期户的交易记录。

**存折与存单的区别在于**：存折一般用于多次存取的情况,用于连续性的储蓄种类; 存单则一般多用于一次性存取的情况。

**存折号码的组成规则**：10 位客户号 +2 位货币号 +3 位顺序号（最后一位为校验位）,共 15 位,如存折户口号：99999。

#### 什么是户口系统？

 1.基本概念  
户口：银行向客户提供的，客户在使用银行某些特定产品时的载体，是银行为客户开通的逻辑意义上的服务媒介（Service Media）。

 2.按存在形式分类  
    1）实体户口（磁条卡，IC 卡，存折，存单等）  
    2）非实体户口（电子一卡通、I 理财、一闪通等）

 3.按级别分类  
     1）主户口: 直接拥有资金的户口。  
     2）子户口: 对应一个主户口（一闪通、附属卡、卡配折、单位卡等）

4.系统层面  
    1）户口 =  拥有客户＋户口序号＋顺序号  
       拥有客户：户口的所有者。  
       资金客户：户口使用资金的所有者。  
       关联客户：子户口对应的主户口所有者。  
    2）主户口： 资金客户=拥有客户  
    3）子户口： 资金客户=关联户口对应的资金客户

#### 户口系统做了什么？

客户使用一卡通、存折、存单、对公账号等在我行办理各种业务时，他们只需提供卡给柜员，即可办理各种业务，对于客户来讲，所有业务是发生在卡上面的，包括资金业务（存取现等）和非资金业务（冻结、挂失等）。一卡通内可以包含各种币种的活期账户、定期账户、理财账户等，柜员实际在进行业务操作时，要根据不同的业务来进行不同的操作，比如挂失后，一卡通内所有账户都不能进行操作，而对于存取现，只在相应币种的账户里进行。表面上看，我们的业务有可能发生在一卡通上，也有可能发生在账户上，没什么规律，这将很不利于产品模型的设计。  
经过分析，我们发现所有的业务都是发生在客户可以知道的一个最小单位上，这个最小单位我们称为户口。通过户口，我们可以建立统一的业务模型，使户口用于管理业务，账户用于管理余额。  
户口系统存储户口基础信息及各属性信息，并提供查询或维护户口信息作业条供外围业务系统调用。请参考 [文档](https://doc.cmbchina.com/f/v?id=NWIuo)

### 术语释义

#### 卡片等级

指各类卡片开户时对资产准入的要求,根据这个要求,可以对各类借记卡进行划分,目前分为普卡、金卡（5w）、金葵花卡 (50w)、钻石卡 (500w) 以及私人卡 (1000W) 这五个等级。

#### 账户分类

- Ⅰ类户是全功能的银行结算账户,存款人可以办理存款、购买投资理财产品、支取现金、转账、消费及缴费支付等。目前大部分银行在营业网点为客户开立的具有实体介质的账户,例如借记卡、活期一本通等,均为Ⅰ类户。Ⅰ类账户是三类账户中功能最全的,相当于 " 大钱柜 ",主要的资金家底都在上面,不必带出门,对安全性要求高。与Ⅰ类账户办理渠道不同,Ⅱ类、Ⅲ类账户可以通过网上银行和手机银行等电子渠道提交开户申请。
- Ⅱ类户可以通过电子方式办理资金划转、购买投资理财产品、办理限定金额的消费和缴费支付等,单日支付额度不能超过 1 万,但购买理财产品的额度不限。且Ⅱ类户不能用于取现,也不能绑定账户转账,相当于 " 钱包 ",用于日常的开销。
- Ⅲ类账户仅能办理小额消费及缴费支付业务,Ⅲ类账户余额不得超过 1000 元。Ⅲ类户与Ⅱ类户最大的区别是仅能办理小额消费及缴费支付,不得办理其他业务。Ⅲ类账户相当于 " 零钱包 ",用于金额不大,频次较高的交易,如闪付、免密支付、二维码支付等。  
对于三类账户的 " 分工 ",业内人士曾形象地用大白话 " 翻译 ":Ⅰ类账户就像是老婆用的,什么金融业务都能办;Ⅱ类账户就像是自己用的,只能投资理财缴费不能取走现金;Ⅲ类账户就像是给小孩用的,只能做一些小额支付。


#### 一卡通制卡模式

分为预制模式和定制模式两种,根据这个划分标准,可以将我行银行卡划分为预制卡和定制卡两大类。
- 预制卡：卡片开户时不需要提前申请,可以事先将卡片信息通过制卡系统驱动制卡机完成的半成品卡。卡片制好后,会调剂分配给柜员,柜员开卡时,任意从柜员箱中取出一张即可,客户不能自行选择卡号。  
    预制卡开户可以一次性完成,客户只要来柜台一次即可全部办理。  
    目前,预制卡包括普卡、各联名普卡、银联金卡、各联名金卡。

- 定制卡：卡片开户需要提前申请,依据资产达标情况和主管授权、审批情况判定是否可以产生制卡数据（生成制卡数据后才可制卡）。卡片制好卡后也会调剂给开卡柜员,柜员开卡时,需根据客户开户申请单从柜员箱中取申请单上特定卡号的卡片来开卡。在开户申请时,除申请 PP 卡外,我行向客户提供挑选卡号的服务。开户时会主动询问客户对卡号的要求,请客户选择卡号; 客户可对卡号后四到六位进行指定,系统自动生成满足客户要求的号码。如客户不需要,则由系统随机产生卡号。对于贵宾登机 PP 卡,系统不提供选号功能,全部由系统自动产生。  
    定制卡开户需要分 2 步进行（开户申请、开户）,客户需要来柜台 2 次才可全部办理,客户申请成功后,一般于 7 日后才能拿申请单再来柜台开卡。  
    目前,定制卡包含：国际金卡、银联金葵花卡、国际金葵花卡、钻石卡、私人银行卡、PP 卡。

- PP 卡：一种附属卡,提供全球贵宾登机服务,除该登机服务外无任何其他功能,没有卡片等级之分,国际金葵花、钻石卡、私人卡持卡客户可以凭主卡记录去申请,其他卡种不允许申请 PP 卡,PP 卡状态（活动/关户）主要由主卡确定。

**品牌划分**：是指按卡片所属的发卡组织来,按此规则划分,一卡通可以分为：银联卡、MASTER 卡、VISA 卡三类,其中 MASTER 卡和 VISA 卡统称为国际卡。一般来说一卡通只能归属于一个组织。  
    **银联卡**：是指符合统一业务规范和技术标准要求,并且在指定位置印有 " 银联 " 字样的 [银行卡](http://baike.baidu.com/view/128688.htm)。加印 " 银联 " 标识的银行卡,必须符合中国人民银行规定的统一业务规范和技术标准,并经中国人民银行批准可以在有 " 银联 " 标识的终端进行交易,境外交易时走 " 银联 " 系统,清算币种为人民币。境外交易结算时,消费额直接由当地货币兑换成人民币,结算的汇率按照中国外汇管理局公布的当日人民币汇率中间价为准,目前银联在境外进行结算时不收取任何的货币结算费。  
    **国际卡**：是带有 " 银联 " 和国际组织（"VISA" 或 "MasterCard"）标识的多币种全球通借记卡。该卡在一卡通现有功能的基础上,增加了境外使用（取款、消费）的功能。在卡面指定位置印有 "MasterCard" 或 "VISA" 字样,可以即可以在 "VISA" 或 "MasterCard" 标识的终端进行交易境。外交易,走 "VISA" 或 "MasterCard" 系统,清算币种为美元。境外交易结算时,要先将消费额由当地货币转成美元,再由美元兑换成人民币进行结算,结算的汇率是由 VISA 或 MASTERCARD 根据当日全球的汇率进行综合而定,同时要加收 1%—2% 的货币结算费。
        
国际卡均可在境内、外使用,境内使用时,其使用的范围及使用的规定与 " 一卡通 " 普卡相同; 境外只能在有 " 银联 "、"VISA" 或 "MasterCard" 标识的受理点及机具上进行取现、余额查询和消费业务。

### 业务/开发负责人

    可视柜台开户：刘贝蒂/01177406、俞根伟/80274874
    批量开户信息：蒋健勋
    零售户口：谢鑫、谢华勇

## 常见数据表

零售客户开户信息查询及零售客户管理

外部户口修饰符,用来定义外部户口类型如以下：  
10201：核心户口（我行境内借记卡） 10203：信用卡户口（我行信用卡） 10202：财富户口 102AA：制卡（我行境内借记卡）20902- 机构清算户口  
T83_EAC_INF_S.EAC_CD：302 个人活期存款账户、602 个人定期存款账户

零售户口：F3EACDTAP 、CSEASDTAP（短户口） 、DEE_EAC_INF_T（X86）以及后面加入的 LAA_LTX_ACM_T（个贷）、LUL_LTX_ACM_T(联合贷 - 网商）,LUL_CLT_ACM_T（联合贷 - 字节等）。对应新表：EAC_RTL_CUST_EAC_INF_S

T03_CUST_ACT_INF_S 客户账户信息快照如何区分零售活期和零售定期？  
    这里面包含了所有零售和对公的账户,可以按 ACT_CTG_CDIN('RD','RT') 分别取零售活期和零售定期。

关于查询零售客户 三类户的开户渠道,相关产品是王婷玲,可将工单转至 禹一郎处理

涉及到的表名  
    CST_COR_CUST_BAS_INF_S 单位客户基本信息快照  
    CST_COR_CUST_DOC_INF_S 单位客户证件信息快照  
    CST_COR_CUST_REL_INF_S 单位客户关联方信息快照  
    CST_CRDC_BAS_INF_S 信用卡客户基础信息快照  
    CST_IDV_DOC_INF_S 零售客户证件信息快照  
    CST_IDV_BAS_INF_S 零售客户基本信息快照  
    CST_IDV_CID_UID_MAP_S 零售客户 CID_UID 映射快照  
    CST_IDV_CUST_NEW_OLD_UID_MAP_S 零售客户新旧 UID 映射快照  
    CST_IDV_DOC_UID_MAP_S 零售客户证件 UID 映射快照  
    CST_IDV_FIX_TEL_INF_S 零售客户固定电话信息快照  
    CST_IDV_MBL_NBR_INF_S 零售客户手机号码信息快照  
    CST_IDV_MBL_NBR_INF_HIS 零售客户手机号码信息历史  
    CST_IDV_PHY_ADR_INF_S 零售客户物理地址信息快照  
    CST_IDV_NTW_CTC_INF_S 零售客户网络地址信息快照  
    CST_IDV_PBC2_ACDM_DEG_INF 零售客户 UID 级二代征信学历信息  
    CST_IDV_REL_REL_S 零售客户关联关系快照  
    CST_IDV_SNS_UID_INF 敏感客户信息  
    CST_IDV_UID_LVL_INF_S 零售客户 UID 级信息快照  
    CST_IDV_UID_MAP_S 零售客户 UID 映射快照  
    NLR14_USER 招呼用户表  
    NLU14_ENTRYLISTMAP_S 实体所在名单表  
    NLU14_WATCHLIST_S 监视名单列表  
    NLU50_AIF_COR_S_BKKEYL_T_S 零售客户风险名单键值列表  
    NLU50_AIF_COR_S_IDCRSS_T_S 零售客户初始客户列表  
    NLU50_AIF_COR_S_MGTAGS_T_S 零售客户标签信息表  
    PAM_ACDM_DEG_CD_CNV_MAP 学历代码转换映射

### 表名 (模式名.表名)

- 数据时间范围：[表的最早和最晚有效数据日期]
- 明细字段：
- 限制字段：

## 常见业务场景

### 查询户口信息

- 场景描述：查询零售客户的开户信息,包括旧 400 开户（1G）和 2G 以来开户信息。
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：全时段的开户数据存储在不同表中,请按照不同情况索引查询
- SQL 语句

```sql
--历史开户：这部分数据可分为 1G换卡、1G开户
-- 1G开户信息(20060101之前)
SELECT DISTINCT
EAC_ID,CURGCD `区域码`,CUCUNO `客户号`,CUBRNO `部门号`,CUCUTP `客户类别`,CUIDNO `证件号`,CUIDTP `证件类别`,CUCUTL `客户称谓`,CUNAME `客户名称`,CUCNCD `国家码`,CUPSCD `邮编`,CUADD1 `地址`,CUADD2 `地址`,CUADD3 `地址`,CUEMAL `E-MAIL地址`,CUHMTL `电话(H)`,CUOFTL `电话(O)`,CUFXNO `传真号`,CUANCD `分析码`,CUFQCD `频率码`,CUDSKY `密码 `,CUPSWD `进帐密码`,CUPSQR `查询密码`,CUSUCD `支取方式`,CUPSBK `凭证号`,CUOPDT `开户日`,CUOPUS `开户用户`,CUMTDT `维护日`,CUMTUS `维护用户`,CUCLDT `关户日`,CUCLUS `关户用户`,CUPEDT `冻结日`,CUPEUS `冻结用户`,CUPECD `冻结原因`,CUPONT `DECIMAL`,CUSP10 `特殊码10`,CUSTCD `状态`
FROM LITC_991176.VVCUIFP_LEAN
WHERE TRIM(EAC_ID) = '';

-- 1G换卡(20060101之前)
SELECT SPACRG `区域码`,SPBRNO `部门号`,SPACNO `参照号`,SPPSBK `8 位凭证`,SPPSBN `16 位凭证`,SPPSCG `16 位新卡换卡标记`,SPADIT `科目`,SPPSCD `凭证类别`,SPPTBL `打印余额`,SPPTDT `打印日期`,SPPTRF `打印流水`,SPPTPG `打印页号`,SPPTLN `打印行号`,SPPTUN `未打印数`,SPIBDT `部门领用日期`,SPIBUS `部门领用用户`,SPINDT `用户领用日期`,SPINUS `领用用户`,SPOPDT `使用日期`,SPOPUS `使用用户`,SPCLDT `结束日期`,SPCLUS `结束用户`,SPSVCD `CVC`,SPSPST `使用状态`,SPPSTP `凭证类型`,SPUPDT `修改日期`,SPSP10 `特殊码`,SPSTCD `记录状态`
FROM LGC_EAM.STACMB_VVSDCTP
WHERE TRIM(SPACRG)= '0027'
AND TRIM(SPBRNO) = '55'
AND TRIM(SPACNO) LIKE '%55020993%'
;

SELECT DISTINCT
CURGCD `区域码`,CUCUNO `客户号`,CUBRNO `部门号`,CUCUTP `客户类别`,CUIDNO `证件号`,CUIDTP `证件类别`,CUCUTL `客户称谓`,CUNAME `客户名称`,CUCNCD `国家码`,CUPSCD `邮编`,CUADD1 `地址`,CUADD2 `地址`,CUADD3 `地址`,CUEMAL `E-MAIL 地址`,CUHMTL `电话1`,CUOFTL `电话2`,CUFXNO `传真号`,CUANCD `分析码`,CUFQCD `频率码`,CUDSKY `密码`,CUPSWD `进帐密码`,CUPSQR `查询密码`,CUSUCD `支取方式`,CUPSBK `凭证号`,CUOPDT `开户日`,CUOPUS `开户用户`,CUMTDT `维护日`,CUMTUS `维护用户`,CUCLDT `关户日`,CUCLUS `关户用户`,CUPEDT `冻结日`,CUPEUS `冻结用户`,CUPECD `冻结原因`,CUPONT `积分`,CUSP10 `特殊码`,CUSTCD `状态`
FROM LGC_EAM.STACMB_VVCUIFP
WHERE TRIM(CUIDNO) = ‘’;

SELECT 
A.参数值 参数值
,COALESCE(C.EAC_ID,'') 卡号
,COALESCE(C.EAC_NM,'') 户口名称
,COALESCE(C.CARD_LVL,'') 卡片等级
,COALESCE(C.EAC_STS,'') 卡片状态
,COALESCE(CAST(C.OPN_DT AS VARCHAR),'') 开户日期
,COALESCE(CAST(C.EFT_DT AS VARCHAR),'') 生效日期
,A.参数值 证件号码
FROM LSHR_A003_SRC.CUST_EAC_INPUT_TABLE02_20250210095411  A
LEFT JOIN MAS_DATA_SRC.CST_IDV_DOC_INF_S B  --零售客户证件信息快照
    ON A.参数值 = B.DOC_NBR
    AND B.DW_SNSH_DT  = CURRENT_DATE - 1
LEFT JOIN ASUB_DATA_SRC.ASUB_CUST_EAC_INF_S C   --零售户口基础信息快照表
    ON B.CUST_ID = C.CUST_ID
    AND C.DW_SNSH_DT  = CURRENT_DATE - 1

SELECT DW_SNSH_DT AS "快照日期", EAC_ID AS "卡号", EAC_NM AS "户口名称", CUST_UID AS "客户UID", CUST_ID AS "客户编号", INT_ACT_NO AS "户口内码", SEQ_NBR AS "顺序号", IAC_ID AS "内部户口编号", CARD_TYP AS "卡片类别", CARD_LVL AS "卡片等级", EAC_STS AS "卡片状态", VCH_CLS_CD AS "凭证种类", OPN_DT AS "开户日期", DSTR_ACT_DT AS "关户日期", ACTV_DT AS "激活日期", ACTV_IND AS "激活标志", EFT_DT AS "生效日期", NVLD_DT AS "失效日期", CNAPS AS "联行号", OPN_CHNL AS "开户渠道", OPN_ORG_ID AS "开户机构编号", OPN_ORG_NM AS "开户机构名称", OPN_USR_ID AS "开户经办人编号", OPN_USR_NM AS "开户经办人名称", CM_ID AS "客户经理编号", CM_NM AS "客户经理名称", MNG_ORG_ID AS "管理机构编号", MNG_ORG_NM AS "管理机构名称", BCH_OPN_IND AS "批量开户标志", SLP_IND AS "睡眠标志", BLST_IND AS "黑名单标志", SSP_GM AS "中止柜面标志", SSP_FGM AS "中止非柜面标志", SSP_ALL AS "中止所有业务标志", IS_FRZ AS "是否冻结(最近一次)", IS_OFC_FRZ AS "是否有权机关冻结(最近一次)", HOLD_CODE AS "冻结代码(最近一次)", HOLD_REASON_REMARK AS "冻结原因(最近一次)"
FROM ASUB_VHIS.ASUB_CUST_EAC_INF_S    --零售户口基础信息快照表
WHERE DW_SNSH_DT= CURRENT_DATE -1
AND EAC_ID = '99999'

--开户城市
SELECT 
A.EAC_NM 客户名称,A.EAC_ID 客户户口号,B.EAC_CTY_COD 开户城市,C.BLG_PVC_NM||C.CTY_NM 省份城市
FROM LITC_991571.TEMP1 A
LEFT JOIN NDS_VHIS.NLW68_DEE_EAC_INF_T_S B  --零售户口信息表A
    ON A.EAC_ID = B.EAC_EAC_NBR
    AND B.DW_SNSH_DT = CURRENT_DATE - 1
LEFT JOIN MAS_VHIS.PAM_CTY_CD_INF_S C
    ON B.EAC_CTY_COD = C.CTY_CD
    AND C.DW_SNSH_DT = CURRENT_DATE - 1
    AND B.EAC_CTY_COD IS NOT NULL

SELECT 
B.DOC_NBR 证件号
,COALESCE(C.EAC_ID,'') 卡号
,COALESCE(C.EAC_NM,'') 户口名称
,COALESCE(C.CARD_LVL,'') 卡片等级
,COALESCE(C.EAC_STS,'') 卡片状态
,COALESCE(CAST(C.OPN_DT AS VARCHAR),'') 开户日期
,COALESCE(CAST(C.EFT_DT AS VARCHAR),'') 生效日期
FROM MAS_VHIS.CST_IDV_DOC_INF_S B   --零售客户证件信息快照
LEFT JOIN ASUB_VHIS.ASUB_CUST_EAC_INF_S C   --零售户口基础信息快照表
    ON B.CUST_ID = C.CUST_ID
    AND C.DW_SNSH_DT  = CURRENT_DATE - 2
WHERE B.DW_SNSH_DT  = CURRENT_DATE - 1
AND B.DOC_NBR IN ('9999')

SELECT DISTINCT
S.SNO 序号
,COALESCE(A.CUST_UID,'') 客户UID
,S.CUST_ID 客户编号
,COALESCE(A.EAC_NM,'') 客户名称
,COALESCE(C.CM_ID,'') 客户经理编号
,COALESCE(C.MNG_BRN_ID,'') 管理机构编号
,COALESCE(E.ORG_NM,'') 管理机构名称
,COALESCE(C.GDR_CD,'') 性别
,COALESCE(C.MVMT_TEL_NBR,'') 移动电话
,COALESCE(C.JOB_UNT_NM,'') 单位名称
,COALESCE(D.FLSH_LOAN_IND,'N') 闪电贷标志
,COALESCE(B.LST_AGN_CN_NAME,'') 最近一次代发企业名称
FROM LITC_991571.TEMP1 S
LEFT JOIN ASUB_VHIS.ASUB_CUST_EAC_INF_S A
    ON S.CUST_ID = A.CUST_ID
    AND A.DW_SNSH_DT = CURRENT_DATE - 1
LEFT JOIN 
(
SELECT CUST_ID,LST_AGN_CN_NAME
FROM ASUB_VHIS.ASUB_CUST_AGN_PGT_INF_S
WHERE DW_SNSH_DT = CURRENT_DATE - 1
AND LST_AGN_CN_NAME IS NOT NULL
) B
    ON S.CUST_ID = B.CUST_ID
LEFT JOIN MAS_VHIS.CST_IDV_BAS_INF_S C
    ON S.CUST_ID = C.CUST_ID
    AND C.DW_SNSH_DT = CURRENT_DATE - 1
LEFT JOIN 
(
SELECT CUST_ID,FLSH_LOAN_IND
FROM SUM_VHIS.T83_PL_BUS_ARG_INF_S
WHERE DW_SNSH_DT = CURRENT_DATE - 1
AND FLSH_LOAN_IND  = 'Y'
) D
    ON S.CUST_ID = D.CUST_ID
LEFT JOIN MAS_VHIS.ORG_CORE_ORG_INF_S E
     ON E.ORG_ID = C.MNG_BRN_ID
     AND E.DW_SNSH_DT = CURRENT_DATE - 1
ORDER BY 1
;

SELECT DW_DAT_DT AS "数据日期", CUST_UID AS "客户UID", BBK_ORG_ID AS "分行机构编号", DAT_CRT_TM AS "数据创建时间", BFR_CHG_BLG_ORG_ID AS "变更前归属机构编号", BFR_CHG_BLG_CM_ID AS "变更前归属客户经理编号", BFR_CHG_CARD_GRD_CD AS "变更前卡等级代码", AFT_CHG_BLG_ORG_ID AS "变更后归属机构编号", AFT_CHG_BLG_CM_ID AS "变更后归属客户经理编号", AFT_CHG_CARD_GRD_CD AS "变更后卡等级代码", CHG_INI_ID AS "变更发起方编号", CHG_TYP_CD AS "变更类型代码", CHG_DTL_TYP_CD AS "变更明细类型代码", RSN_CD AS "原因代码", RSN_SMR_DSCR AS "原因摘要描述", CHG_SRL_ID AS "变更流水编号", BFR_PST_ID AS "变更前岗位编号", AFT_PST_ID AS "变更后岗位编号"
FROM MROM_VHIS.MROM_CG_BG_CUST_BLG_CHG_LOG  --客户归属变更日志
WHERE CUST_UID = 'PNCIF3711941897'
AND DW_DAT_DT BETWEEN '2020-01-01' AND '2024-12-31'
ORDER BY 1

--世纪经典卡查询余额
--口径提供人：陈成高/00010479,有交易，且最后一笔交易金额为零，即说明该卡已兑付
SELECT MTACRG `开户地`,MTTSRG `发生地`,MTACNO `帐号`,MTBRNO `部门号`,MTCUNO `客户号`,MTSPBN `16 位卡号`,MTACIT `科目`,MTCYNO `货币号`,MTACTP `业务类别`,MTLVEQ `查询级别`,MTANC1 `分析码`,MTANC2 `分析码`,MTTSCD `交易码`,MTTSTX `交易描述`,MTPRID `处理码`,MTTSRM `交易注解`,MTTSTP `交易类别`,MTAMCD `金额码`,MTAMTS `交易金额`,MTOLBL `联机余额`,MTEXCD `兑付码`,MTEXCY `货币号`,MTEXAM `交易金额`,MTRFSE `套录号`,MTRFNO `流水号`,MTRVSB `冲帐标记`,MTNAOU `我方摘要`,MTNAYR `你方摘要`,MTPTMK `未打印标记`,MTPTPG `页号`,MTPTLN `行号`,MTPTDT `打印日期`,MTUSID `进帐用户`,MTCKUS `复核用户`,MTAUUS `授权用户`,MTRMLU `进帐地点`,MTEYDT `进帐日期`,MTEYTM `进帐时间`,MTSP10 `特殊码１０`,MTSP12 `特殊码１０`,MTSTCD `状态`,MTINTP `利率类别`,MTNOMH `存期`,MTVLDT `起息日`,MTRSAC `收付方帐号`,MTRSNM `收付方名称`,MTRSBK `收付方行名`,MTRSNA `摘要`,MTRSSY `信息来源`,MTRSSP `备注`
FROM LGC_EAM.STACMB_VHTSDTP
WHERE TRIM(MTSPBN) in('075599004493','075599104494','075599204494','075599204490','075599104491','075599004490')
ORDER BY MTEYDT


贺岁卡余额及交易

--口径提供人：陈成高/00010479,有交易，且最后一笔交易金额为零，即说明该卡已兑付
--贺岁卡余额
SELECT SPACRG `区域码`,SPBRNO `部门号`,SPACNO `参照号`,SPPSBK `8 位凭证`,SPPSBN `16 位凭证`,SPPSCG `16 位新卡换卡标记`,SPADIT `科目`,SPPSCD `凭证类别`,SPPTBL `打印余额`,SPPTDT `打印日期`,SPPTRF `打印流水`,SPPTPG `打印页号`,SPPTLN `打印行号`,SPPTUN `未打印数`,SPIBDT `部门领用日期`,SPIBUS `部门领用用户`,SPINDT `用户领用日期`,SPINUS `领用用户`,SPOPDT `使用日期`,SPOPUS `使用用户`,SPCLDT `结束日期`,SPCLUS `结束用户`,SPSVCD `CVC`,SPSPST `使用状态`,SPPSTP `凭证类型`,SPUPDT `修改日期`,SPSP10 `特殊码`,SPSTCD `记录状态`
FROM LGC_EAM.STACMB_VVSDCTP
WHERE TRIM(SPPSBK) IN('9999')
;
--贺岁卡交易
SELECT MTACRG `开户地`,MTTSRG `发生地`,MTACNO `帐号`,MTBRNO `部门号`,MTCUNO `客户号`,MTSPBN `16 位卡号`,MTACIT `科目`,MTCYNO `货币号`,MTACTP `业务类别`,MTLVEQ `查询级别`,MTANC1 `分析码`,MTANC2 `分析码`,MTTSCD `交易码`,MTTSTX `交易描述`,MTPRID `处理码`,MTTSRM `交易注解`,MTTSTP `交易类别`,MTAMCD `金额码`,MTAMTS `交易金额`,MTOLBL `联机余额`,MTEXCD `兑付码`,MTEXCY `货币号`,MTEXAM `交易金额`,MTRFSE `套录号`,MTRFNO `流水号`,MTRVSB `冲帐标记`,MTNAOU `我方摘要`,MTNAYR `你方摘要`,MTPTMK `未打印标记`,MTPTPG `页号`,MTPTLN `行号`,MTPTDT `打印日期`,MTUSID `进帐用户`,MTCKUS `复核用户`,MTAUUS `授权用户`,MTRMLU `进帐地点`,MTEYDT `进帐日期`,MTEYTM `进帐时间`,MTSP10 `特殊码１０`,MTSP12 `特殊码１０`,MTSTCD `状态`,MTINTP `利率类别`,MTNOMH `存期`,MTVLDT `起息日`,MTRSAC `收付方帐号`,MTRSNM `收付方名称`,MTRSBK `收付方行名`,MTRSNA `摘要`,MTRSSY `信息来源`,MTRSSP `备注`
FROM LGC_EAM.STACMB_VHTSDTP
WHERE MTSPBN IN ('00249999') -- 区域码+参照号
```

### 查询账户信息

- 场景描述：可查询客户的资金账户信息：记账余额、核算机构、冻结、透支、账户状态、位图等。
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：全时段的开户数据存储在不同表中,请按照不同情况索引查询
- SQL 语句

```sql

--[TRX]定期交易文件
    SELECT *
    FROM LGC_EAM.UNICORE_TDTRXDTAP_YEAR
    WHERE TRIM(EAC_NBR) =;

--[EAC] 短户口关系文件
    SELECT *
    FROM LGC_EAM.UNICORE_CSEASDTAP
    WHERE TRIM(EAC_SHR) =;

--参数翻译
    SELECT *
    FROM LGC_EAM.ETL_FE_SYPAMDTAP
    WHERE TRIM(PAMPAMROT) = 'TDSSWATD'

--个人定期账户表(历史)
    SELECT *
    FROM LGC_EAM.UDM_FAT_H_ACPACTTIM
    WHERE TRIM(EAC_NBR) =
    LIMIT 100

--[TDS]定期主文件2012
    SELECT *
    FROM LGC_EAM.UNICORE_TDTDSDTAP_YEAR
    WHERE TRIM(EAC_NBR) =
    LIMIT 100

select *
from NDS_VHIS.NLW35_WRT_ACCT_BAL_T_S --客户账户余额表
where CLIENT_ID = '99999' --客户号码
and CARD_INTERNAL_NBR = '99999' --户口内码
```

### 查询开户影像

- 场景描述：查询零售客户的银行卡业务记录,包含换卡、挂失、注销等一系列银行卡操作。
- 口径提供人：谢华勇/80274711
- 结果字段：OBJ_ID 对象编号,OBJ_TYP_CD 对象类型代码,BLP_ID 影像编号,REG_DT 登记日期,REG_TM 登记时间,UPD_DT 更新日期,UPD_TM 更新时间,RCD_STS_CD 记录状态代码,RCD_DT 记录日期,OBLG_FLD 预留字段
- 限制字段：REG_DT,OBJ_ID,RCD_STS_CD
- 注意事项：先由总行数仓查询到影像 ID,然后转邓可为/80245460 查询影像资料
- SQL 语句

```sql
-- 开户影像ID
SELECT DISTINCT
OBJ_ID 对象编号,OBJ_TYP_CD 对象类型代码,BLP_ID 影像编号,REG_DT 登记日期,REG_TM 登记时间,UPD_DT 更新日期,UPD_TM 更新时间,RCD_STS_CD 记录状态代码,RCD_DT 记录日期,OBLG_FLD 预留字段
FROM PDM_VHIS.T03_EAC_BLP_INF  -- 户口影像信息
WHERE OBJ_ID = ''

SELECT IMG_PTN_TAG AS "分区标志", IMG_CLT_NBR AS "客户号", IMG_OBJ_TYP AS "对象类型", IMG_OBJ_NBR AS "对象号码", IMG_REG_DAT AS "登记日期", IMG_REG_TIM AS "登记时间", IMG_IMG_IDT AS "影像ID"
FROM NDS_VHIS.NLU39_DEE_IMG_DTA_T   --影像信息表
WHERE IMG_CLT_NBR = '99999'

```
### 查询历史存单

- 场景描述：查询零售客户的历史存单
- 口径提供人：
- 结果字段：
- 限制字段：
- 注意事项：通过旧系统 18 位身份证号 (去掉年份和最后一位) 查询历史存单
- SQL 语句

```sql
--通过旧系统18位身份证号(去掉年份和最后一位)查询历史存单

SELECT
CURGCD `区域码`,CUCUNO `客户号`,CUBRNO `部门号`,CUCUTP `客户类别`,CUIDNO `证件号 FOR 18`,CUIDTP `证件类别`,CUCUTL `客户称谓`,CUNAME `客户名称`,CUCNCD `国家码`,CUPSCD `邮编`,CUADD1 `地址`,CUADD2 `地址`,CUADD3 `地址`,CUEMAL `E-MAIL 地址`,CUHMTL `电话 (H)`,CUOFTL `电话 (O)`,CUFXNO `传真号`,CUANCD `分析码`,CUFQCD `频率码`,CUDSKY `密码`,CUPSWD `进帐密码`,CUPSQR `查询密码`,CUSUCD `支取方式`,CUPSBK `凭证号`,CUOPDT `开户日`,CUOPUS `开户用户`,CUMTDT `维护日`,CUMTUS `维护用户`,CUCLDT `关户日`,CUCLUS `关户用户`,CUPEDT `冻结日`,CUPEUS `冻结用户`,CUPECD `冻结原因`,CUPONT `积分`,CUSP10 `特殊码10`,CUSTCD `状态`
FROM LGC_EAM.STACMB_VVCUIFP
WHERE CUCUNO LIKE '%XXXXXX%'       --模糊匹配存单编号
```

### 查询银行卡业务记录

- 场景描述：查询零售客户的银行卡业务记录,包含换卡、挂失、注销等一系列银行卡操作。
- 口径提供人：谢华勇/80274711
- 结果字段：[该业务场景下必需的所有结果字段]
- 限制字段：BUS_TYP_CD：97402097 书面挂失换卡,97502097 防伪换卡,97503097 损坏换卡,97504097 到期换卡,97508097 其他换卡,97509097 电子卡配卡。
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
-- 示例SQL

SELECT  EVT_ID AS `事件编号`, TRX_SET AS `交易套号`, TRX_SEQ AS `交易流水号`, TRX_STS_CD AS `交易状态代码`, TRX_CARD_NBR AS `交易卡号`, CUST_ID AS `客户编号`, EAC_SEQ_NBR AS `户口序号`, BUS_TYP_CD AS `业务类型代码`, REF_CARD_NBR AS `参考卡号`, REF_DT AS `参考日期`, OLD_CARD_VCH_CLS_CD AS `旧卡凭证种类代码`, NEW_CARD_PICK_NBR_MTH_CD AS `新卡选号方式代码`, NEW_CARD_VCH_CLS_CD AS `新卡凭证种类代码`, NEW_CARD_VCH_NBR AS `新卡凭证号码`, NEW_CARD_VCH_SEQ_NBR AS `新卡凭证序号`, AFT_CHK_RSN_CD AS `事后核查原因代码`, VCH_FRZ_DEAL_CD AS `凭证冻结处理代码`, EAC_FRZ_CD AS `户口冻结代码`, EAC_FRZ_SRL_NBR AS `户口冻结流水号`, CUST_BLG_MTH_CD AS `归户方式代码`, APL_ID AS `申请编号`, APL_BBK_ORG_ID AS `申请分行机构编号`, APL_ORG_ID AS `申请机构编号`, APL_DT AS `申请日期`, CARD_CLCN_BBK_ORG_ID AS `领卡分行机构编号`, CARD_CLCN_ORG_ID AS `领卡机构编号`, AGN_IND AS `代领标志`, BUS_SMR AS `业务摘要`, HDL_BBK_ORG_ID AS `经办分行机构编号`, HDL_ORG_ID AS `经办机构编号`, HDL_USR_ID AS `经办用户编号`, HDL_DT AS `经办日期`, TSK_INST_NBR AS `任务实例号`, RCD_STS_CD AS `记录状态代码`, REF_NO_1 AS `参考号码1`, REF_NO_2 AS `参考号码2`, REF_NO_3 AS `参考号码3`, CMSN_IND AS `代办标志`, CMSN_SRL_NBR AS `代办流水号`, FRZ_SRL_NBR AS `冻结流水号`, FRZ_NBR_SET AS `冻结套号`, INI_TYP_CD AS `发起方类型代码`, CHK_USR_ID AS `复核用户编号`, MCHN_DT AS `机器日期`, MCHN_TM AS `机器时间`, TRX_SRC_CD AS `交易来源代码`, TRX_SRL AS `交易流水`, TRX_SEQ_NBR AS `交易序号`, OLD_CARD_VCH_SEQ_NBR AS `旧卡凭证序号`, CARD_CLS_CD AS `卡片种类代码`, OPN_BBK_ORG_ID AS `开户分行机构编号`, OPN_ORG_ID AS `开户机构编号`, XPN_CD AS `扩展代码`, SND_CARD_IND AS `上门送卡标志`, UP_AND_DN_TYP_CD AS `升降类型代码`, AFT_CHK_RSL_CD AS `事后核查结果代码`, AUT_USR_ID AS `授权用户编号`, SBM_APL_IND AS `提交申请标志`, AFT_MNT_VAL AS `维护后值`, BFR_MNT_VAL AS `维护前值`, DBT_CARD_BUS_IND_CD AS `借记卡业务标志代码`, OBLG_DOC_IND AS `预留证件标志`, OBLG_FLD AS `预留字段`, DFT_PSW_IND AS `预设密码代码`, PCTR_IND AS `预约标志`, NVLD_IND AS `作废标志`, RCD_DT AS `记录日期`
FROM PDM_AVIEW.T05_BNK_CARD_BUS_EVT        --银行卡业务事件    1995-07-01
WHERE TRIM(CUST_ID) = '9999'
AND RCD_DT = '2018-10-07'
;

SELECT TRX_SRL_NBR AS "交易流水号", CUST_ID AS "客户编号", INT_ACT_NO AS "户口内码", TRX_SEQ_NBR AS "交易序号", TRX_STS_CD AS "交易状态代码", BUS_TYP_CD AS "业务类型代码", TRX_CARD_NBR AS "交易卡号", REF_CARD_NBR AS "参考卡号",


### 查询三类账户

- **场景描述**：查询账户的具体分类,一类户、二类户及三类户
- **口径来源：谢华勇/80274711  
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
--2022-08-02 李照强更新口径 

--卡类别查询（受上云影响,主机表已不适用,可使用PDM表判断卡类别）
T03_CORE_CUST_EAC_ATCH_INF_S     核心客户户口附属信息表      
包含 Ind_Bmp 标志位图, POS限额与ATM限额等附属信息, 可通过Ind_Bmp来区分具体为二类户还是三类户, 即 :  
SUBSTR(IND_BMP,11,1)='1'   —  I类户  
SUBSTR(IND_BMP,11,1)='2'   —  II类户  
SUBSTR(IND_BMP,11,1)='3'   —  Ⅲ类户

--区分二类户实体卡与电子卡
口径1：基于上述口径,可以关联外部户口信息表里的EAC_CD,来区分二类户是电子卡还是实体卡。‘386’：II类户（电子一卡通）,‘388’：一网通账户
口径2：-- 提供人
用卡号关联卡文件 DEE_CARD_T,判断CAD_NPS_STS,A或者P表示电子卡,T或者空表示实体卡  
对应pdm表T03_DBT_CARD_INF_S  NO_CARD_IND无卡标志,用MED_ID关联

```

### 查询客户被查询记录/柜员日志

- **场景描述**：可查询客户的资金账户信息：记账余额、核算机构、冻结、透支、账户状态、位图等。
- **口径提供人：**
- **结果字段**：
- **注意事项**：
- **SQL 语句**

```sql
/*
客户信息查询-核心系统
核心系统1351如何区分客户查询和银行查询：F3QRYELT作业条有一个查询方标志,B银行查询C客户查询（颜滔/Z0005301）; 
实际在柜员行为事件表中,通过任务编码来判断,任务编码：10063001银行查询10063201客户查询（谢华勇/80274711）
PDM_VHIS.T05_TLR_BHV_EVT    --柜员行为事件 已下线 20191101-20230217
SUM_VHIS.T85_TLR_OPR_EVT    --柜员操作事件 替代表 20121122-至今
*/
SELECT DW_DAT_DT `数据日期`,EVT_ID `事件编号`,DAT_SRC_TYP_CD `数据来源类型代码`,HPN_DT `发生日期`,TSK_INST_NBR `任务实例号`,TSK_INST_NBR_SET `任务实例套号`,TSK_ENC `任务编码`,JOB_SLIP_ENC `工作条编码`,OPR_STP_CD `操作步骤代码`,PRD_NBR `产品实例号`,PRD_CD `产品代码`,USR_DAY_STL_STAT_CD `用户日结统计代码`,USR_TYP_CD `用户类型代码`,HPN_TM `发生时间`,OPR_USR_ID `操作用户编号`,USR_AFL_ORG_ID `用户所属机构编号`,CHK_USR_ID `复核用户编号`,AUT_USR_ID `授权用户编号`,CHNL_TYP_CD `渠道类型代码`,EQP_ADR_ID_TYP_CD `设备地址编号类型代码`,EQP_ADR_ID `设备地址编号`,CMN_RQS_NBR `通讯请求号`,CCY_CD `币种代码`,TRX_AMT `交易金额`,RVS_SPL_ENTR_TYP_CD `冲补账类型代码`,ASYN_BUS_TYP_CD `异步业务类型代码`,ERR_NO `错误码`,RCD_STS_CD `记录状态代码`,BUS_SYS_KEY_VAL `业务系统键值`,CUST_ID `客户编号`,EAC_ID `户口编号`,VCH_CLS_CD `凭证种类代码`,VCH_ID `凭证编号`,CR_TYP_CD `钞汇类型代码`,TRX_TXT_NO `交易摘要码`,DOC_NBR `证件号码`,SWG_IND `刷卡标志`,DTL_DAT_SRC_TYP_CD `明细数据来源类型代码`,DATA_SRC_TAG `数据来源标志`
FROM SUM_AVIEW.T85_TLR_OPR_EVT
WHERE PRT_D BETWEEN '2017-06-01' AND '2017-09-30'
AND CUST_ID = '999999'

-- 柜员非密查询客户明细(1192)
SELECT DW_DAT_DT AS "数据日期", TLR_PTN_TAG AS "分区标志", TLR_TLR_NBR AS "操作柜员", TLR_REG_DAT AS "登记日期", TLR_REG_TIM AS "登记时间", TLR_NTK_COD AS "日结代码", TLR_WKE_COD AS "作业条码", TLR_CLT_NBR AS "客户号码", TLR_EAC_NBR AS "户口号码", TLR_PSB_COD AS "凭证种类", TLR_PSB_NBR AS "凭证号码", TLR_CCY_NBR AS "货币号码", TLR_CCY_TYP AS "钞汇标志", TLR_TRS_AMT AS "交易金额", TLR_TXT_COD AS "交易摘要代码", TLR_CTF_CNR AS "证件国别", TLR_CTF_TYP AS "证件类型", TLR_CTF_NBR AS "证件号码", TLR_PSB_FLG AS "刷卡标志", TLR_BUS_RQS AS "业务流水", TLR_BRN_NBR AS "操作机构", TLR_RTN_COD AS "返回结果码", TLR_AUT_TLR AS "授权柜员", TLR_CHK_TLR AS "复核柜员", TLR_SPL_080 AS "冗余代码", TLR_RCD_DAT AS "记录日期", TLR_RCD_VER AS "记录版本"
FROM NDS_VHIS.NLU39_DEC_TLR_LOG_T --柜员行为日志表
WHERE TLR_CLT_NBR = '999999'
AND DW_DAT_DT > '2024-12-31'
ORDER BY 5,6;

--查询作业条码
SELECT DISTINCT WKE_ENC AS `作业条编码`, WKE_NM AS `作业条名称`
FROM PDM_AVIEW.T00_VBS_WKE_DEF_PARM --新系统作业条定义参数表
WHERE WKE_ENC IN ('DCCADALL')

--对公部分：2299
--按照经办柜员拆分分行数据
DROP TABLE IF EXISTS TLR_TMP; --8350
CREATE TEMPORARY TABLE TLR_TMP
WITH (ORIENTATION = COLUMN, COLVERSION = 2.0, COMPRESSION = MIDDLE)
DISTRIBUTE  BY  HASH(TLR_ID)
AS
SELECT DW_SNSH_DT,TLR_ID,TLR_NM,A.ORG_ID,B.ORG_NM
FROM SUM_VHIS.T84_TLR_INF_S  A --柜员信息快照
INNER JOIN AMIP_VHIS.AMIP_D_ORG_VAL B
    ON A.ORG_ID = B.ORG_ID
    AND B.FRS_LVL_BBK_ORG_ID = '571'
WHERE A.DW_SNSH_DT= CURRENT_DATE - 1
GROUP BY 1,2,3,4,5
;

DROP TABLE IF EXISTS RES_TMP;--1105292
CREATE TEMPORARY TABLE RES_TMP
WITH (ORIENTATION = COLUMN, COLVERSION = 2.0, COMPRESSION = MIDDLE)
AS
SELECT DISTINCT
A.MAINTAIN_SEQ 序列号
,B.ORG_ID 内部机构号
,B.ORG_NM 所在机构名称
,A.OPERATE_TELLER 查询员工编号
,COALESCE(B.TLR_NM,'')  查询员工姓名
,REPLACE(DW_DAT_DT,'-','') 查询日期
,SUBSTRING(REPLACE(CAST(OPR_TIME AS TIME),':',''),1,6) 查询时间
,A.BUSINESS_REMARK 查询事由
,A.CLIENT_ID 客户统一编号
,E.CUST_CHN_NM 被查询客户名称
,'无' 被查询客户账号
,'无' 被查询账户类别
,A.AUTHORIZE_TELLER 授权员工编号
,'无' 授权员工姓名
,'否' 有无经客户授权
,'否' 客户授权类型
,'20240521' 采集日期
FROM NDS_VHIS.NLI07_WCM_CARD_QUERY_LOG_T A
INNER JOIN TLR_TMP B
    ON A.OPERATE_TELLER = B.TLR_ID
LEFT JOIN PDM_VHIS.T04_CORE_USR_INF_S D
    ON A.OPERATE_TELLER = RIGHT(D.USR_ID,6)
    AND D.DW_SNSH_DT = CURRENT_DATE - 1
INNER JOIN PDM_VHIS.T01_CORE_COR_CUST_INF_S E
    ON E.CUST_ID = A.CLIENT_ID
    AND E.DW_SNSH_DT = CURRENT_DATE - 1
--LEFT JOIN NDS_VHIS.NLW35_WLG_CARD_CODE_T EE   ON EE.OLD_CARD_CODE = E.EAC_CD  AND E.EAC_CD IS NOT NULL    AND E.EAC_CD <> ''
WHERE UPPER(A.OPR_API_NAME) = 'QUERYBASICBYCARDNUMBER'
AND DW_DAT_DT BETWEEN '2023-01-01' AND '2024-03-31'
AND SUBSTRING(REPLACE(CAST(OPR_TIME AS TIME),':',''),1,6) > ''
ORDER BY 6,7;
```


### 查询批量开户

- 场景描述：[详细描述业务场景的业务逻辑和背景]
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
批量开户
批量开户有三张表
个人表：DCOBIDTAP、DEB_BOP_IDV_T 日期：IDV_RCD_DAT
    PDM_VHIS.T03_BCH_OPN_IDV_DAT_INF        --批量开户个人数据信息
        高斯AB：20130812-至今
        历史库：20130812-20220721
公共表：DCOBPDTAP、DEB_BOP_PUB_T 日期：PUB_RCD_DAT
    PDM_VHIS.T03_BCH_OPN_PUB_DAT_INF        --批量开户公共数据
        高斯AB：20191129-至今
        历史库：20191129-20220721
    PDM_VHIS.T03_BCH_OPN_INF        --批量开户信息
        高斯AB：20121214-至今
        历史库：20121214-20200411
批次表：DCBCHFLCP、DEB_BCH_DTA_T 日期：FLW_BCH_DAT
    PDM_VHIS.T03_BCH_OPN_DBT_CARD_BCH_DAT_INF       --借记卡批次数据文件
        高斯AB：20191129-至今
        历史库：20191129-20220721
    PDM_VHIS.T05_BCH_PRCS_MNG_CUR_EVT       --批次流程管理当前事件
        高斯AB：20130704-至今
        历史库：20130704-20220721
```

### 查询借记卡等级

- 场景描述：[详细描述业务场景的业务逻辑和背景]
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
SELECT DISTINCT
A.EAC_ID AS "卡号"
,B.OPN_DT AS "开户日期"
,C.ORG_NM AS "开户机构"
,CASE WHEN SUBSTR(D.IND_BMP,11,1)='1' THEN '一类户'
WHEN SUBSTR(D.IND_BMP,11,1)='2' THEN '二类户'
WHEN SUBSTR(D.IND_BMP,11,1)='3' THEN '三类户'
ELSE '未识别到类型'
END AS "卡种类"
,CASE CARD_GRD_CD WHEN '020' THEN '金卡    '
       WHEN '060' THEN '钻石卡'
       WHEN '010' THEN '普卡'
       WHEN '080' THEN '私人银行卡'
       WHEN '040' THEN '金葵花卡'
       ELSE '未知种类'
       END AS "卡等级"
,CASE WHEN B.EAC_STS = '2001' THEN '账户-活动'
    WHEN B.EAC_STS = '2004' THEN '账户-关户'
    WHEN B.EAC_STS = '3001' THEN '户口-冻结'
    WHEN B.EAC_STS = '3002' THEN '户口-关户'
    WHEN B.EAC_STS = '3003' THEN '户口-活动'
    WHEN B.EAC_STS = '3005' THEN '户口-撤销'
    WHEN B.EAC_STS = '3006' THEN '户口-待确认'
    ELSE '未知状态'
    END AS "卡片状态"
,ACTV_IND AS "激活标志"
FROM LITC_991571.TEMP1 A
LEFT JOIN SUM_VHIS.T83_EAC_INF_S B
    ON A.EAC_ID = B.EAC_ID
    AND B.DW_SNSH_DT= CURRENT_DATE -1
LEFT JOIN AMIP_VHIS.AMIP_D_ORG_VAL C
    ON C.ORG_ID = B.OPN_ORG_ID
LEFT JOIN PDM_VHIS.T03_CORE_CUST_EAC_ATCH_INF_S D
    ON D.EAC_ID = A.EAC_ID
    AND D.DW_SNSH_DT = CURRENT_DATE -1
LEFT JOIN BRTL_VHIS.BRTL_SR_RTL_BBK_CUST E
    ON E.BLG_CARD_NBR = A.EAC_ID
    AND E.DW_UPD_DT = CURRENT_DATE
ORDER BY 1;
```


### 查询单人最高持卡等级

- 场景描述：[详细描述业务场景的业务逻辑和背景]
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
SELECT A.CUST_ID
,CASE WHEN MAX(RTL_CARD_LVL_CD)='010' THEN '普卡'
WHEN MAX(RTL_CARD_LVL_CD)='020' THEN '金卡'
WHEN MAX(RTL_CARD_LVL_CD)='040' THEN '金葵花卡'
WHEN MAX(RTL_CARD_LVL_CD)='060' THEN '钻石卡'
WHEN MAX(RTL_CARD_LVL_CD)='080' THEN '私人卡'
ELSE '' END AS "最高持卡等级"
FROM USERID1 A
LEFT JOIN BRTL_VIEW.BRTL_PH_EAC_INF_S B
    ON A.EAC_ID=B.EAC_ID
    AND B.DW_SNSH_DT=CURRENT_DATE-1
GROUP BY 1;

```

### 查询信用卡等级

- 场景描述：[详细描述业务场景的业务逻辑和背景]
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
--信用卡等级
SELECT B.CARD_CLS_NM
FROM BRTL_VIEW.BRTL_PH_CRD_CARD_BAS_INF_S A
LEFT JOIN BCRD_VHIS.BCRD_B_CARD_TYP_PARM B
    ON B.CARD_TYP_CD = A.CARD_TYP_CD
    AND B.CARD_AFNT_CD = A.ID_CD
WHERE DW_SNSH_DT= CURRENT_DATE - 1
```

### 查询客户基础信息

- 场景描述：查询客户的手机号、地址、单位等客户维度信息
- 口径提供人：[姓名]/[一事通]
- 结果字段：|机构号|年龄|性别|学历|婚姻状况|所在单位|职业|职位|证件|联系方式|
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
--手机号和年龄匹配
SELECT DISTINCT         
B.CUST_NM AS "姓名",
A.MVMT_TEL_NBR
 AS "手机号",
B.CUST_UID AS "客户UID",
B.CUST_AGE AS "年龄"
FROM LITC_991571.TMP1 A
LEFT JOIN PDM_VHIS.T01_CORE_IDV_CUST_INF_S B 
    ON A.MVMT_TEL_NBR = B.MVMT_TEL_NBR 
    AND B.DW_SNSH_DT =CURRENT_DATE -1

--客户地址信息
SELECT CLATRSKEY 事务键,CLACLTNBR 客户号,CLAADRNBR 地址编号,CLAADRTYP 地址类别,CLABUSCOD 业务代码,CLAADRFMT 地址格式,CLACLTADR 客户地址,CLAADRRCV 收件人,CLARCVTTL 收件人称谓,CLACTYCOD 城市码,CLAPVCCOD 省份码,CLACNRCOD 三位国家代码,CLAPOSCOD 邮编,CLAFLGDTA 标注,CLAEVTNBR 事件实例号,CLASPLC22 备注,CLARCDVER 记录更新版本序号,CLAMNTDAT 维护日期,CLARCDSTS 记录状态
FROM NDS_VHIS.NLI51_CIADRDTAP       
WHERE CLACLTNBR = '99999'
;

--客户物理地址历史_CIF零售客户
SELECT CUST_ID 客户编号,ADR_ID 地址编号,DW_START_DT 开始日期,PHY_ADR_TYP_CD 物理地址类型代码,CTY_CD 城市代码,ADR_FMT_TYP_CD 地址格式类型代码,ADR_DTL_INF 地址详细信息,CNR_CD 国家地区代码,PVC_CD 省份代码,RCV_SALUT_CD 收件人称谓代码,RCV_NM 收件人名称,PST_CD 邮政编码,VLD_PRD_BGN_DT 有效期起始日期,VLD_PRD_TMN_DT 有效期终止日期
FROM PDM_VHIS.T01_CUST_PHY_ADR_HIS_CIF_RTL_CUST     
WHERE CUST_ID= '999999'
;


--查询开户行联行号
SELECT DISTINCT
A.EAC_ID 开户行
,B.OPN_ORG_ID 开户机构
,C.ORG_NM 开户机构名
,D.CNAPS 联行号
FROM LITC_991571.TEMP1 A
INNER JOIN SUM_VHIS.T83_EAC_INF_S B
    ON A.EAC_ID = B.EAC_ID
LEFT JOIN AMIP_VHIS.AMIP_D_ORG_VAL C
    ON C.ORG_ID = B.OPN_ORG_ID
LEFT JOIN PDM_VHIS.T04_ORG_CNAPS_INF_HIS D
    ON B.OPN_ORG_ID=D.ORG_ID
    AND CNAPS LIKE '308%'
WHERE DW_SNSH_DT= CURRENT_DATE -1
;

--UNIQUE_USER_ID
SELECT DISTINCT
A.CUST_ID
,A.CUST_UID
,B.UNIQUEUSERID
FROM SUM_VHIS.T83_EAC_INF_S A
INNER JOIN NDS_VHIS.NLA06_ID_EXTID B
    ON A.OPN_DOC_NBR = B.IDNO
WHERE A.DW_SNSH_DT = CURRENT_DATE - 1
AND A.CUST_ID =
;

--学籍
NDS_VHIS.NLB02_T_XXW_AUTH_INFO  --学信网认证成功信息表
NDS_VHIS.NLB02_T_STUDENT_INFO       --用户信息表
NDS_VHIS.NLB02_T_UNIVERSITY_INFO    --学校信息表

SELECT DISTINCT
A.TEL_NO AS "手机号"
,B.STUDENT_ID AS "用户号"
,C.UNIVERSITY_ID AS "学校编号"
,D.UNIVERSITY_NAME AS "学校名称"
FROM LITC_991571.RLC_TMP A
LEFT JOIN NDS_VHIS.NLB02_T_STUDENT_INFO B ON A.TEL_NO = B.TEL_NO --用户信息表
LEFT JOIN NDS_VHIS.NLB02_T_XXW_AUTH_INFO C ON C.STUDENT_ID = B.STUDENT_ID  --学信网认证成功信息表
LEFT JOIN NDS_VHIS.NLB02_T_UNIVERSITY_INFO D ON C.UNIVERSITY_ID = D.UNIVERSITY_ID
ORDER BY 3,1,2
;
```
### 查询管户经理/客户经理

- **场景描述**：[详细描述业务场景的业务逻辑和背景]
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
--一个客户在一个分行会有一个管护经理
select
from brtl_vhis.brtl_sr_rtl_bbk_cust_s       --客户与管护经理关系表（零售客户）
where DW_Snsh_Dt = current_date - 1


--私行客户管户
SELECT DW_START_DT AS "开始日期", CUST_UID AS "客户UID", BBK_ORG_ID AS "分行机构编号", BLG_REL_TYP_CD AS "归属关系类型代码", DW_END_DT AS "结束日期", BLG_ORG_ID AS "归属机构编号", BLG_CM_ID AS "归属客户经理编号", BLG_CM_PST_ID AS "归属客户经理岗位编号"
FROM MROM_VHIS.MROM_CG_BG_CUST_BLG_REL_H    --客户归属关系历史
WHERE BLG_CM_ID = '01035057'
AND DW_START_DT >= '2016-01-01'
```

### 查询客户挂失/凭证更换列表

- 场景描述：本行柜面综合业务处理系统中 "1351" 模块 " 个人户口查询 " 中,需要调取客户 XXX（账号：）" 挂失/凭证更换列表 " 全部内容
- 口径提供人：谢华勇
- 结果字段：
- 注意事项：DCTRXDTAP,用卡号先获取客户号 + 户口序号（F3EacDtap）然后用客户号 + 户口序号关联 DCTRXDTAP 获取交易。TrxBusTyp 的具体含义参考 SYPAMDTAP 的 PamPamNam where PamPmarot = 'DCBUSTYP' and PamPamSub = TrxBusTYp
- SQL 语句

### 查询户口代码含义

- 场景描述：[详细描述业务场景的业务逻辑和背景]
- 口径提供人：[姓名]/[一事通]
- 结果字段：[该业务场景下必需的所有结果字段]
- 注意事项：[说明常见的取数条件或该场景的边界]
- SQL 语句

```sql
SELECT *
FROM NDS_VHIS.NLW35_WLG_CARD_CODE_T       --全局户口代码-户口代码定义表
WHERE OLD_CARD_CODE = '602'
```

### 统计凭证种类的制卡及在库数量

- **场景描述**：根据业务提供的凭证种类编号,查询这些凭证种类在 2019 年一季度的制卡数量以及 3 月底的在库数量
- **口径来源：孙学良
- **结果字段**：
- **限制字段**：
- **注意事项**：
- **SQL 语句**

```sql
--统计制卡数量
sel  Vch_Cls_Cd, sum(Suc_Into_Whs_Qty)as "数量"  
from pdm_vhis.T05_CARD_PRT_BCH_MNG_EVT    
where Into_Whs_Dt between '2019-01-01' and '2019-03-31'  and Card_Prt_Sts_Cd ='F'   and Vch_Cls_Cd in 
  ( '4602', '4603', '461N', '4624', '4631', '4632', '4633', '4634', '4635', '4636', '4637', '4638', '4639', '4640', '4650', '46WN', '470A', '470E', '470F', '470G', '47QK', '47QL', '47QM', '47QN', '47QQ', '47QS', '47QT', '47QU', '47QV', '47QW', '47QX', '47QY', '47QZ', '47RB', '47SN', '47SP', '47T3', '490Y', '49W5', '49W6', '49W7' ) 
group by Vch_Cls_Cd ;

--统计在库数量
 sel Vch_Cls_Cd, sum(Invtr_Avl_Vch_Nbr) as "数量"
 from pdm_vhis.T10_VCH_INVTR_ACT_INF_S 
 where dw_snsh_dt='2019-03-31' and Vch_Cls_Cd  in   ( '4602', '4603', '461N', '4624', '4631', '4632', '4633', '4634', '4635', '4636', '4637', '4638', '4639', '4640', '4650', '46WN', '470A', '470E', '470F', '470G', '47QK', '47QL', '47QM', '47QN', '47QQ', '47QS', '47QT', '47QU', '47QV', '47QW', '47QX', '47QY', '47QZ', '47RB', '47SN', '47SP', '47T3', '490Y', '49W5', '49W6', '49W7' ) 
group by Vch_Cls_Cd ;


--历史口径：统计我行发卡以及在库卡片时,需要从凭证系统中的表统计,这样实际统计到的才是实际凭证签发数量。以下口径由孙学良提供
--1 在库
SELECT * FROM pspsbdtap WHERE PSDMDMSTS = 'U' AND PSDTRFSTS = 'X' 

--2 已发给客户（含已销号）
SELECT * FROM pspsbdtap WHERE NOT (PSDMDMSTS = 'U'  AND PSDTRFSTS = 'X' )

--3 已申请制卡但未入库的
a,查找已申请制卡 SELECT * FROM psmgtdtap WHERE PSMMGTSTS = 'F'  OR  PSMMGTSTS = 'W'  
b,查找已入库     SELECT * FROM pspsbdtap
c,a-b 即为已申请制卡但未入库。
```

### 客户号合并

- **场景描述**：如果客户在系统中有两个客户号 有些业务无法办理 需先办理合并客户号的操作 ,统计已经办理过合并客户号的数据
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
select 
B.DW_Dat_Dt,A.CLT_NUM,C.TLRBRNNBR
    from NDS_VHIS.NLU50_M3IDCRSS a--交叉索引表
inner join PDM_VHIS.T05_CIF_CUST_INF_MNT_EVT b --CIF客户信息维护事件
    on  B.Mnt_Dtl_Typ_Cd = 'CLTMRG' 
    AND B.Cust_Id = A.CLT_NUM
INNER JOIN NDS_VHIS.NLJ51_USTLRDTAP_S C 
    ON C.DW_SNSH_DT = '2018-04-15' 
    AND C.TLRTLRNBR = B.Hdl_Usr_Id
WHERE a.CLT_PID <> a.CLT_ORG_PID 
AND a.APL_SYS_COD = '1'
;
```
### 查询伪冒开户数据

- **场景描述**：为掌握我行伪冒账户数据情况,现需提取如下数据,CMBRUN 柜面综合业务处理系统中业务代码 1970" 户口重新核实功能 " 模块下核实结果为 " 假名 " 的业务笔数。
- **口径来源：谢鑫/274171
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
F3EACSTAP 文件STATAGMAP标志位图第2-3位为'50',且对应 F3EACDTAP 文件中EacBbkBrn 开户分行为'451'和'459'的记录  
（F3EACSTAP和F3EACDTAP通过持有客户号+户口内码进行关联）
```

### 查询 E 餐通企业下客户明细数据

- **场景描述**：为开展 E 餐通客户后续经营所需,现申请提取 2022.07.05 的石家庄分行 E 餐通企业下客户明细数据
- **口径来源：樊荣/80311030
- **结果字段**：含客户所属企业,客户手机号,客户 UID
- **限制字段**：
- **注意事项**：
- **SQL 语句**

```sql
select A.ENTERPRISE_ID,A.ENTERPRISE_NAME,B.USER_ID  AS "职工ID",USER_NAME  as "职工姓名",USER_MOBILE,C.CUST_UID   
FROM NDS_VHIS.NLW69_EC_ENTERPRISE A  
INNER JOIN NDS_VHIS.NLW69_EC_USER_2 B --普通用户表
    ON A.ENTERPRISE_ID=B.USER_ENTERPRISE   
    AND B.USER_ENTERPRISE<>''  
    AND B.USER_ENTERPRISE IS NOT NULL  
left join SUM_VHIS.T81_IDV_UID_LVL_CUST_INF_S C  
    ON B.USER_MOBILE=C.Mvmt_Tel_Nbr   
    AND C.DW_SNSH_DT=DATE-1  
WHERE ENTERPRISE_REGION ='SJZ'
```

### 招财金账户开立

- **场景描述**：因客户所需,现申请提取客户侯 X 静,其 2012 年签订我行招财金代理个人客户实物黄金交易业务协议书的数据
- **口径来源：
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
select DW_Snsh_Dt 快照日期,Arg_Id 合约编号,Arg_Mdf 合约修饰符,ARG_STS_CD 合约状态代码,Bnk_Act_Nbr 银行帐号,EAC_MDF 户口修饰符,Opn_Dt 开户日期,Dstr_Act_Dt 销户日期,Opn_Org_Id 开户机构编号,BBK_Org_Id 分行机构编号,Usr_Id 用户编号,Pub_Or_Prv_Typ_Cd 公私类型代码,SMS_Opn_Ind 短信开通标志,Gld_Trx_Org_Cust_Id 金交所客户编号,Opn_Gld_Trx_Org_Cust_Ind 开户金交所客户标志,Cust_Ctg_Cd 客户类别代码,Cust_Gdr 客户性别,Cust_Nm 客户名称,Cust_Adr 客户地址,Cust_Mbl 客户手机,Cust_Tel 客户电话,Cust_Eml 客户电子邮箱,Doc_Typ_Cd 证件类型代码,Doc_Nbr 证件号码,Fax 传真,Act_Typ_Cd 账户类型代码,Opn_Trx_Chnl_Cd 开通交易渠道代码,Opr_Nm 经办人姓名,Opr_Doc_Typ_Cd 经办人证件类型代码,Opr_Doc_Nbr 经办人证件号码,Entp_Cd 企业代码,Cust_Sht_Nm 客户简称,LGP_Rprs 法人代表,LGP_Typ_Cd 法人类型代码,Plg_Ind 质权人标志,Reg_Cpt 注册资本,Entp_Typ_Cd 企业类型代码,Tax_Pay_Id 纳税人识别号,VAT_Com_Tax_Pay_Ind 是否为增值税一般纳税人标志,Tax_Pay_Adr 纳税人地址,Tax_Pay_Tel 纳税人电话,Tax_Pay_Bnk_Cd 纳税人银行代码,Tax_Pay_Bnk_Opn_Inf 纳税人银行开户信息,Tax_Pay_Act_Nbr 纳税人账号,Fch_Gds_Psn_Nm 提货人姓名,Fch_Gds_Psn_Doc_Typ_Cd 提货人证件类型代码,Fch_Gds_Psn_Doc_Nbr 提货人证件号码,Bas_Mrgn 基础保证金,Cust_Lvl_Cd 客户级别代码,Opr 操作人,DATA_SRC DATASRC,Cust_Sts_Cd 客户状态代码,Zon_Cd 地区代码,Trsf_Bas_Mrgn_Ind 转基础保证金标志,New_Opn_Ind 新开户标志,Opn_Ini_Chnl 开户发起渠道,Mnt_Dt 维护日期,Dstr_Act_Dt_1 销户日期 
from PDM_VHIS.t03_sge_trx_arg_inf_s --上海黄金交易合约信息快照
where dw_snsh_dt='2022-09-21'  
and bnk_act_nbr ='9999999999'
```

### 查询卡类别（通过卡 Bin）

- **场景描述**：[详细描述业务场景的业务逻辑和背景]
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
--收单平台有一张卡BIN的表,可以通过卡BIn号和一个字段区分出卡的类型
--D-借记卡 C-贷记卡 Z-准贷记卡 Y-预付卡 O-其他
select COALESCE(substr(BINSPC20,7,1),'O') (title '卡类型代码 CHAR(1)')
from  NDS_VHIS.NLG50_SSBINTBLP;
```

### 查询外币携带证

- **场景描述**：5531 外币携带证
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
/*
一 数据文件:PSTRXHISP（凭证历史交易文件）,PSPSBDTAP+PSPSBHISP(凭证文件+历史文件）,PTTSKISHP(任务历史文件）
二 条件
    1 PSTRXHISP的PSXTSKCOD='PSGNLHDL' AND PSXUSBNUM < 0获取签发交易,同时根据PSXTRXDAT（交易日期）,PSXBRNNBR(机构号）进行筛选,获取凭证种类,凭证号,签发日期、处理机构。
    2 通过任务实例号与PTTSKISHP关联获取经办柜员、复核柜员。3,通过凭证种类和凭证号与PSPSBDTAP的凭证种类和凭证号关联,获取当前凭证状态。
注意：注意交易文件的凭证号是起始和截至号码  
PSXSTTPSB          起始凭证号       
PSXSTTSEQ          起始凭证顺序号   
PSXENDPSB          结束凭证号       
PSXENDSEQ          结束凭证顺序号
*/
```

### 查询网经服共管客户名单

- **场景描述**：提取网经服共管的客户 uid + bbk,查询网经服共管客户名单
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
select CUST_UID "客户uid",EAC_BLG_BBK_ID "户口归属分行编号"
from BRTL_VHIS.BRTL_SR_CUST_BLG_REL_S
where DW_SNSH_DT = current_date - 1
and Blg_Rel_Typ_Cd = '008'   --网经服共管标签
```

### 查询线上申请卡片但未领卡的客户

- **场景描述**：西安分行申请提示线上申请卡片,客户未领卡：1.未解除绑定,2.解除绑定 情况。
- **口径来源：谢华勇
- **结果字段**：客户申请时间、申请凭证种类、客户姓名、卡号、入库网点、入库时间、入库批次、是否解除绑定
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
/*
(1)10主机,C3DTA/DCTRXDTAP文件 substr(TRXSPLC40,6,2)= 'A8' and TRXAPLBBK list '129' '910' '912' '918' and TRXTRXSTS = 'A' and TRXAPLDAT range 20130101 20181231。筛选出交易卡号、凭证种类、机器日期、机器时间、申请分行、申请机构字段。  
(2)用客户号TRXTRXCLT关联A3DTA/E3CIFCLTP文件,筛选出CLTCLTNAM字段。  
(3)用交易卡号字段TRXTRXCAD关联03主机VBSDTAP/PSPSBDTAP文件。筛选出机构号即为入库网点、最后维护日期即为入库时间、凭证当前的流转状态若为X表示已入库否则未入库。  
(4)用交易卡号字段TRXTRXCAD关联03主机VBSDTAP/PSMGTDTAP文件。直接筛选出制卡批次号字段  
(5)用交易卡号关联10主机C3DTA/DCCADWBDP文件,查出来的记录是已解绑的数据。不在DCCADWBDP文件中的其余数据是未绑定的数据。
*/
```

### 统计一卡通挂失换卡直邮数据

- **场景描述**：请协助查询 2019 年 1 月以经办支行为单位,提取一卡通直邮业务数量、双金及以上客户在柜面办理挂失及换卡业务数量。
- **口径来源：[姓名]/[一事通]
- **结果字段**：[该业务场景下必需的所有结果字段]
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
/*
统计方法：DCTRXDTAP文件,其中,TrxAplBrn是换卡申请网点（以此分组）;TrxBusTyp like '9750%' or '974020%'的数据是成功办理换卡挂失业务总量。1、DCTRXDTAP.TrxSplC40第8位,是'M'表示直邮。其他非直邮。2、TrxRefCad(旧卡卡号）关联DCCADDTAP.CadCadNbr,CadCadTyp list '11' '21' '31' '41' '61' '71' '81' '91'是双金及以上级别卡。
*/
```



### 统计账户的户口核实信息

- **场景描述**：按零售部要求请在联系客户前先通过机房维护,统计下这些账户的户口核实结果信息
- **口径来源：[姓名]/[一事通]
- **结果字段**：户口所在机构、户口号、开户证件类别（例：P01：居民身份证）、开户证件国别、开户证件号码、姓名、M2 电话、家庭电话、户口核实结果、核实时间、核实用户、核实说明、核实方式、行员标记、预警标志、管控原因
- **限制字段**：
- **注意事项**：[说明常见的取数条件或该场景的边界]
- **SQL 语句**

```sql
/*
户口核实结果：F3EACSTAP文件STATAGMAP字段第02-03位
核实柜员：F3EACSTAP文件STATLRMAP字段第1-6位
核实日期：F3EACSTAP文件STADAT003字段
核实说明：F3EACSTAP文件STATAGMAP字段第05-06位
核实方式：F3EACSTAP文件STATAGMAP字段第4位
预警标志：F3EACDTAP文件EACWRMFLG字段  
户口所在机构:F3EACDTAP文件EacBrnNbr   开户机构
户口号：F3EACDTAP文件EacEacShr   户口号码
开户证件类别:F3EACDTAP文件EacCtfTyp   开户证件类别
开户证件国别:F3EACDTAP文件EacCnrCod   证件签发国家
开户证件号码：根据F3EACDTAP文件中的EacOwnClt   持有客户号码查询E3CIFCLTP文件的CltCltPid   客户 PID,  
    再根据客户PID查询E3CIFCTFP文件中的CtfCtfIdc   证件号码  
    姓名:根据F3EACDTAP文件中的EacOwnClt   持有客户号码查询E3CIFCLTP文件中的CltCltNam   客户名称
M2电话：根据F3EACDTAP文件中的EacOwnClt   持有客户号码查询E3CIFCLTP文件中的CltM2nMbl   个人手机号码 (M2)
家庭电话：不知道怎么查,问潘达乐
行员标记：F3EACDTAP文件EacStfFlg   行员标志
管控原因：F3WRMDTAP 文件 WrmCltCau   管控原因
*/

select distinct  
a.eac_id,  
d.Opn_Org_Id as "开户机构编号",  
d.EAC_Opn_Doc_Ctg_Cd as "开户证件类别",  
d.EAC_Opn_Doc_Isu_Cnr_Cd as "开户证件国别",  
g.opn_doc_nbr as "开户证件号码",  
d.EAC_Nm as "姓名",  
e.Tel_Nbr as "M2电话",  
f.Hous_Tel_Nbr as "家庭电话",  
substr(c.Ind_Bmp,2,2) as "核实结果",  
c.Tlr_Bmp as "核实柜员",--substr(c.Tlr_Bmp,1,6),  
c.Dt_003 as "核实日期",  
substr(c.Ind_Bmp,5,2) as "核实说明",  
substr(c.Ind_Bmp,4,1) as "核实方式",  
d.Emp_Ind as "行员标志",  
d.Wrn_Ind as "预警标志",  
j.Mng_And_Ctrl_Rsn as "管控原因"  
from tmp_eac a  
left join PDM_VHIS.T03_CORE_CUST_EAC_INF_S d  --核心客户户口信息快照
     on a.eac_id = d.eac_id  
     and d.DW_Snsh_Dt = '2017-03-31'  
left join pdm_vhis.T03_EAC_MNG_INF_S c  --户口管理信息快照
     on d.cust_id = c.cust_id  
     and d.EAC_Seq_Nbr = c.IAC_Id  
     and c.DW_Snsh_Dt = date - 1  
left join pdm_vhis.T01_CUST_CMN_TEL_HIS e  --客户通讯电话历史_对公客户
     on d.cust_id = e.cust_id  
     and e.dw_start_dt <= date -1  
     and e.dw_end_dt > date -1  
     and e.Cmn_Tel_Typ_Cd = 'M2'  
left join SUM_VHIS.T81_CORE_IDV_CUST_INF as f  --核心个人客户信息快照
     on d.cust_id = f.cust_id  
left join sum_vhis.t83_eac_inf_s as g  
     on a.eac_id = g.eac_id  
     and g.dw_snsh_dt = date -1  
left join PDM_VHIS.T03_ACT_MNG_AND_CTRL_REG_FILE as j  --账户管控登记文件
     on d.cust_id = j.Cust_Nbr_No  
     and d.EAC_Seq_Nbr = j.Int_Act_No
```








