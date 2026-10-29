import axiosInstance from './axios'
import type { ReorderPriority, ReorderResponse } from '../types'

export interface ReorderParams {
  lookbackDays?: number
  leadTimeDays?: number
  priority?: ReorderPriority
  categoryId?: string
  flaggedOnly?: boolean
}

export const reorderApi = {
  getRecommendations: async (params?: ReorderParams): Promise<ReorderResponse> => {
    const res = await axiosInstance.get<ReorderResponse>('/reorder/recommendations', { params })
    return res.data
  },

  exportCsv: async (params?: ReorderParams): Promise<Blob> => {
    const res = await axiosInstance.get<Blob>('/reorder/export', {
      params,
      responseType: 'blob',
    })
    return res.data
  },
}
