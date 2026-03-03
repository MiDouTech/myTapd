import { getDefectCategoryDict, getLogicCauseDict } from '@/api/bugreport'
import type { DefectCategoryOutput, LogicCauseTreeOutput } from '@/types/bugreport'

let logicCauseCache: LogicCauseTreeOutput[] | null = null
let defectCategoryCache: DefectCategoryOutput[] | null = null

let logicCausePromise: Promise<LogicCauseTreeOutput[]> | null = null
let defectCategoryPromise: Promise<DefectCategoryOutput[]> | null = null

export async function getCachedLogicCauseDict(force = false): Promise<LogicCauseTreeOutput[]> {
  if (force) {
    logicCauseCache = null
  }
  if (logicCauseCache) {
    return logicCauseCache
  }
  if (!logicCausePromise) {
    logicCausePromise = getLogicCauseDict()
      .then((result) => {
        logicCauseCache = result || []
        return logicCauseCache
      })
      .finally(() => {
        logicCausePromise = null
      })
  }
  return logicCausePromise
}

export async function getCachedDefectCategoryDict(force = false): Promise<DefectCategoryOutput[]> {
  if (force) {
    defectCategoryCache = null
  }
  if (defectCategoryCache) {
    return defectCategoryCache
  }
  if (!defectCategoryPromise) {
    defectCategoryPromise = getDefectCategoryDict()
      .then((result) => {
        defectCategoryCache = result || []
        return defectCategoryCache
      })
      .finally(() => {
        defectCategoryPromise = null
      })
  }
  return defectCategoryPromise
}
