import type {
  NlpKeywordCreateInput,
  NlpKeywordListOutput,
  NlpKeywordUpdateInput,
  NlpLogPageInput,
  NlpLogPageOutput,
  WecomConfigOutput,
  WecomConfigUpdateInput,
  WecomConnectionTestOutput,
  WecomGroupBindingCreateInput,
  WecomGroupBindingListOutput,
  WecomGroupBindingUpdateInput,
} from '@/types/wecom'
import type { PageOutput } from '@/types/common'
import request from '@/utils/request'

/**
 * 查询企微群绑定配置列表
 * 接口编号：API000021（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群与工单分类绑定管理
 */
export function getWecomGroupBindingList(): Promise<WecomGroupBindingListOutput[]> {
  return request.get<WecomGroupBindingListOutput[]>('/wecom/group-binding/list')
}

/**
 * 新增企微群绑定配置
 * 接口编号：API000022（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置新增
 */
export function createWecomGroupBinding(data: WecomGroupBindingCreateInput): Promise<number> {
  return request.post<number>('/wecom/group-binding/create', data)
}

/**
 * 修改企微群绑定配置
 * 接口编号：API000023（与 Bug简报模块存在历史编号重复，待Task021统一治理）
 * 产品文档功能：4.6.4 企微群机器人工单 - 群绑定配置修改
 */
export function updateWecomGroupBinding(
  id: number,
  data: WecomGroupBindingUpdateInput,
): Promise<void> {
  return request.put<void>(`/wecom/group-binding/update/${id}`, data)
}

/**
 * 查询企业微信连接配置
 * 接口编号：API000422
 * 产品文档功能：SSO一期-企微配置管理
 */
export function getWecomConfigDetail(): Promise<WecomConfigOutput | null> {
  return request.get<WecomConfigOutput | null>('/wecom/config/detail')
}

/**
 * 保存企业微信连接配置
 * 接口编号：API000423
 * 产品文档功能：SSO一期-企微配置管理
 */
export function saveWecomConfig(data: WecomConfigUpdateInput): Promise<number> {
  return request.post<number>('/wecom/config/save', data)
}

/**
 * 测试企业微信连接
 * 接口编号：API000424
 * 产品文档功能：SSO一期-连接测试
 */
export function testWecomConnection(): Promise<WecomConnectionTestOutput> {
  return request.post<WecomConnectionTestOutput>('/wecom/config/test-connect')
}

/**
 * 查询NLP关键词配置列表
 * 接口编号：API000432
 * 产品文档功能：企微自然语言建单 - 关键词管理
 */
export function listNlpKeywords(matchType?: number): Promise<NlpKeywordListOutput[]> {
  return request.get<NlpKeywordListOutput[]>('/wecom/nlp-keyword/list', {
    params: matchType !== undefined ? { matchType } : {},
  })
}

/**
 * 创建NLP关键词配置
 * 接口编号：API000433
 * 产品文档功能：企微自然语言建单 - 新增关键词
 */
export function createNlpKeyword(data: NlpKeywordCreateInput): Promise<number> {
  return request.post<number>('/wecom/nlp-keyword/create', data)
}

/**
 * 更新NLP关键词配置
 * 接口编号：API000434
 * 产品文档功能：企微自然语言建单 - 修改关键词
 */
export function updateNlpKeyword(id: number, data: NlpKeywordUpdateInput): Promise<void> {
  return request.put<void>(`/wecom/nlp-keyword/update/${id}`, data)
}

/**
 * 删除NLP关键词配置
 * 接口编号：API000435
 * 产品文档功能：企微自然语言建单 - 删除关键词
 */
export function deleteNlpKeyword(id: number): Promise<void> {
  return request.del<void>(`/wecom/nlp-keyword/delete/${id}`)
}

/**
 * NLP解析日志分页查询
 * 接口编号：API000436
 * 产品文档功能：企微自然语言建单 - 解析日志
 */
export function pageNlpLogs(params: NlpLogPageInput): Promise<PageOutput<NlpLogPageOutput>> {
  return request.get<PageOutput<NlpLogPageOutput>>('/wecom/nlp-log/page', { params })
}
