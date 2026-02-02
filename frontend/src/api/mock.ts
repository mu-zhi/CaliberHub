import type {
    SceneVersion,
    Domain,
    Scene,
    LintResult,
} from '../types'
import type { TableSearchResult, TableDetail } from './index'
import dayjs from 'dayjs'

// Mock 数据开关 - 开发时设为 true
export const USE_MOCK = false

// Mock 领域数据
export const MOCK_DOMAINS: Domain[] = [
    { id: 'd0000000-0000-0000-0000-000000000001', domainKey: 'RETAIL_CIF', name: '零售客户', description: '零售个人客户相关的取数场景' },
    { id: 'd0000000-0000-0000-0000-000000000002', domainKey: 'RETAIL_TXN', name: '零售交易', description: '零售交易流水相关的取数场景' },
    { id: 'd0000000-0000-0000-0000-000000000003', domainKey: 'CORP_CIF', name: '对公客户', description: '对公企业客户相关的取数场景' },
    { id: 'd0000000-0000-0000-0000-000000000004', domainKey: 'UNCATEGORIZED', name: '未归类', description: '尚未归类的取数场景' },
]

// Mock 场景数据
export const MOCK_SCENES: Scene[] = [
    {
        id: 's1',
        sceneCode: 'SC-RETAIL-001',
        title: '零售客户基础信息取数',
        domainId: 'd0000000-0000-0000-0000-000000000001',
        domainKey: 'RETAIL_CIF',
        domainName: '零售客户',
        lifecycleStatus: 'ACTIVE',
        hasSensitive: true,
        lastVerifiedAt: dayjs().subtract(30, 'day').toISOString(),
        verifiedBy: 'zhangsan',
        ownerUser: 'zhangsan',
        tags: ['客户', '基础信息'],
        createdAt: dayjs().subtract(60, 'day').toISOString(),
        createdBy: 'zhangsan',
        updatedAt: dayjs().subtract(1, 'day').toISOString(),
        updatedBy: 'zhangsan',
    },
    {
        id: 's2',
        sceneCode: 'SC-RETAIL-002',
        title: '零售交易流水查询',
        domainId: 'd0000000-0000-0000-0000-000000000002',
        domainKey: 'RETAIL_TXN',
        domainName: '零售交易',
        lifecycleStatus: 'ACTIVE',
        hasSensitive: false,
        lastVerifiedAt: dayjs().subtract(200, 'day').toISOString(),
        verifiedBy: 'lisi',
        ownerUser: 'lisi',
        tags: ['交易', '流水'],
        createdAt: dayjs().subtract(300, 'day').toISOString(),
        createdBy: 'lisi',
        updatedAt: dayjs().subtract(200, 'day').toISOString(),
        updatedBy: 'lisi',
    },
    {
        id: 's3',
        sceneCode: 'SC-RETAIL-003',
        title: '客户风险评级数据',
        domainId: 'd0000000-0000-0000-0000-000000000001',
        domainKey: 'RETAIL_CIF',
        domainName: '零售客户',
        lifecycleStatus: 'ACTIVE',
        hasSensitive: true,
        lastVerifiedAt: dayjs().subtract(5, 'day').toISOString(),
        verifiedBy: 'wangwu',
        ownerUser: 'wangwu',
        tags: ['风险', '评级'],
        createdAt: dayjs().subtract(5, 'day').toISOString(),
        createdBy: 'wangwu',
        updatedAt: dayjs().subtract(1, 'day').toISOString(),
        updatedBy: 'wangwu',
    },
]

// Mock 版本数据
export const MOCK_DRAFT: SceneVersion = {
    id: 'v-draft-001',
    sceneId: 's1',
    sceneCode: 'SC-RETAIL-001',
    status: 'DRAFT',
    isCurrent: true,
    versionSeq: 0,
    versionLabel: 'draft',
    title: '零售客户基础信息取数',
    tags: ['客户', '基础信息'],
    ownerUser: 'zhangsan',
    contributors: ['lisi'],
    hasSensitive: true,
    lastVerifiedAt: dayjs().subtract(30, 'day').toISOString(),
    verifiedBy: 'zhangsan',
    content: {
        sceneDescription: '用于获取零售客户的基本信息，包括姓名、证件号、手机号等核心字段，支持按客户号批量查询。',
        caliberDefinition: '客户基础信息包含：客户号、姓名、证件类型、证件号、性别、手机号、创建日期等字段。数据来源于核心系统客户主表。',
        inputParams: [
            { name: 'cust_ids', displayName: '客户号列表', type: 'VARCHAR', required: true, example: "'C001','C002'" },
            { name: 'biz_date', displayName: '业务日期', type: 'DATE', required: false, example: '2024-01-01' },
        ],
        outputSummary: '返回客户号、姓名、证件类型、证件号、性别、手机号、创建日期等字段',
        sqlBlocks: [
            {
                blockId: 'blk-001',
                name: '主查询',
                sql: `SELECT 
  cust_id,
  cust_name,
  id_type,
  id_no,
  gender,
  mobile,
  create_date
FROM cif.t_customer
WHERE cust_id IN (\${cust_ids})
  AND del_flag = '0'`,
            },
        ],
        sourceTables: [
            {
                tableFullname: 'cif.t_customer',
                matchStatus: 'MATCHED',
                isKey: true,
                source: 'EXTRACTED',
                description: '客户主表'
            },
        ],
        sensitiveFields: [
            { fieldFullname: 'cif.t_customer.cust_name', tableName: 't_customer', fieldName: 'cust_name', sensitivityLevel: 'PII', maskRule: 'PARTIAL_MASK' },
            { fieldFullname: 'cif.t_customer.id_no', tableName: 't_customer', fieldName: 'id_no', sensitivityLevel: 'PII', maskRule: 'HASH' },
            { fieldFullname: 'cif.t_customer.mobile', tableName: 't_customer', fieldName: 'mobile', sensitivityLevel: 'PII', maskRule: 'PARTIAL_MASK' },
        ],
        caveats: [
            { id: 'c1', title: '注意分区字段', text: '生产环境需要加上日期分区条件，否则会全表扫描' },
        ],
    },
    createdAt: dayjs().subtract(60, 'day').toISOString(),
    createdBy: 'zhangsan',
    updatedAt: dayjs().subtract(1, 'day').toISOString(),
    updatedBy: 'zhangsan',
}

// Mock 表搜索结果
export const MOCK_TABLE_SEARCH: TableSearchResult[] = [
    { tableFullname: 'cif.t_customer', metadataTableId: 'mt-001', description: '客户主表', hasSensitiveFields: true },
    { tableFullname: 'cif.t_customer_ext', metadataTableId: 'mt-002', description: '客户扩展信息表', hasSensitiveFields: true },
    { tableFullname: 'txn.t_transaction', metadataTableId: 'mt-003', description: '交易流水表', hasSensitiveFields: false },
    { tableFullname: 'txn.t_transaction_detail', metadataTableId: 'mt-004', description: '交易明细表', hasSensitiveFields: false },
    { tableFullname: 'risk.t_risk_score', metadataTableId: 'mt-005', description: '风险评分表', hasSensitiveFields: true },
]

// Mock 表详情
export const MOCK_TABLE_DETAIL: TableDetail = {
    tableFullname: 'cif.t_customer',
    metadataTableId: 'mt-001',
    schemaName: 'cif',
    tableName: 't_customer',
    description: '客户主表，存储客户基本信息',
    sensitivitySummary: '包含敏感字段：姓名、证件号、手机号',
    fields: [
        { fieldName: 'cust_id', fieldFullname: 'cif.t_customer.cust_id', metadataFieldId: 'mf-001', dataType: 'VARCHAR(32)', description: '客户号', isSensitive: false },
        { fieldName: 'cust_name', fieldFullname: 'cif.t_customer.cust_name', metadataFieldId: 'mf-002', dataType: 'VARCHAR(100)', description: '客户姓名', sensitivityLevel: 'PII', isSensitive: true },
        { fieldName: 'id_type', fieldFullname: 'cif.t_customer.id_type', metadataFieldId: 'mf-003', dataType: 'VARCHAR(2)', description: '证件类型', isSensitive: false },
        { fieldName: 'id_no', fieldFullname: 'cif.t_customer.id_no', metadataFieldId: 'mf-004', dataType: 'VARCHAR(20)', description: '证件号码', sensitivityLevel: 'PII', isSensitive: true },
        { fieldName: 'gender', fieldFullname: 'cif.t_customer.gender', metadataFieldId: 'mf-005', dataType: 'CHAR(1)', description: '性别', isSensitive: false },
        { fieldName: 'mobile', fieldFullname: 'cif.t_customer.mobile', metadataFieldId: 'mf-006', dataType: 'VARCHAR(20)', description: '手机号', sensitivityLevel: 'PII', isSensitive: true },
        { fieldName: 'email', fieldFullname: 'cif.t_customer.email', metadataFieldId: 'mf-007', dataType: 'VARCHAR(100)', description: '电子邮箱', sensitivityLevel: 'PII', isSensitive: true },
        { fieldName: 'create_date', fieldFullname: 'cif.t_customer.create_date', metadataFieldId: 'mf-008', dataType: 'DATE', description: '创建日期', isSensitive: false },
        { fieldName: 'del_flag', fieldFullname: 'cif.t_customer.del_flag', metadataFieldId: 'mf-009', dataType: 'CHAR(1)', description: '删除标志', isSensitive: false },
    ],
}

// Mock Lint 结果
export const createMockLintResult = (hasErrors: boolean = false): LintResult => ({
    passed: !hasErrors,
    errorCount: hasErrors ? 1 : 0,
    warningCount: 1,
    errors: hasErrors ? [
        { id: 'E001', message: '口径定义不能为空', path: 'content.caliber_definition' },
    ] : [],
    warnings: [
        { id: 'W003', message: '注意事项为空，建议补充常见坑点', path: 'content.caveats' },
    ],
})

// 模拟延迟
export const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
