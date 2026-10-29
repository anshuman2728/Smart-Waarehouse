import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import ReorderRecommendations from './ReorderRecommendations'
import * as reorderApiModule from '@/api/reorderApi'
import type { ReorderResponse } from '@/types'

const mockReorderResponse: ReorderResponse = {
  lookbackDays: 30,
  leadTimeDays: 7,
  summary: {
    totalEvaluated: 2,
    totalFlagged: 1,
    criticalCount: 1,
    highCount: 0,
    mediumCount: 0,
    totalEstimatedRestockCost: 1075.0,
  },
  recommendations: [
    {
      productId: 'prod-001',
      productName: 'Precision Servo Motor',
      sku: 'SRV-001',
      categoryId: 'cat-01',
      categoryName: 'Motors',
      supplierId: 'sup-01',
      supplierName: 'ServoTech Corp',
      supplierEmail: 'sales@servotech.com',
      unitPrice: 25.0,
      currentStock: 5,
      safetyStock: 10,
      leadTimeDays: 7,
      avgDailyDemand: 2.0,
      daysOfCover: 2.5,
      reorderPoint: 24,
      recommendedQuantity: 43,
      estimatedCost: 1075.0,
      priority: 'CRITICAL',
      ruleApplied: 'CRITICAL_DEFICIT',
      reason: 'Critical deficit: Stock (5) is at or below safety stock threshold (10).',
      ruleInputs: {
        currentStock: 5,
        safetyStock: 10,
        avgDailyDemand: 2.0,
        leadTimeDays: 7,
        reorderPoint: 24,
        daysOfCover: 2.5,
        reorderPointFormula: '(2.00 * 7) + 10 = 24 ROP',
        daysOfCoverFormula: '5 / 2.00 = 2.5 days',
        recommendedQuantityFormula: 'max(48 - 5, 1) = 43 units',
      },
    },
  ],
}

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ReorderRecommendations />
    </QueryClientProvider>
  )
}

describe('ReorderRecommendations', () => {
  it('renders page header, KPI cards, and recommendations table', async () => {
    vi.spyOn(reorderApiModule.reorderApi, 'getRecommendations').mockResolvedValue(mockReorderResponse)

    renderWithClient()

    expect(screen.getByText('Reorder Recommendations')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('Precision Servo Motor')).toBeInTheDocument()
    })

    expect(screen.getByText('SKU: SRV-001')).toBeInTheDocument()
    expect(screen.getByText('+43')).toBeInTheDocument()
    expect(screen.getByText('CRITICAL')).toBeInTheDocument()
  })

  it('opens "Why Flagged?" formula inspector dialog on click', async () => {
    vi.spyOn(reorderApiModule.reorderApi, 'getRecommendations').mockResolvedValue(mockReorderResponse)

    renderWithClient()

    await waitFor(() => {
      expect(screen.getByText('Precision Servo Motor')).toBeInTheDocument()
    })

    const inspectBtn = screen.getByTestId('InfoOutlinedIcon').closest('button')
    expect(inspectBtn).toBeTruthy()
    if (inspectBtn) {
      fireEvent.click(inspectBtn)
    }

    await waitFor(() => {
      expect(screen.getByText(/Safety Stock Deficit/)).toBeInTheDocument()
    })

    expect(screen.getByText('(2.00 * 7) + 10 = 24 ROP')).toBeInTheDocument()
    expect(screen.getByText('5 / 2.00 = 2.5 days')).toBeInTheDocument()
  })
})
