export interface AuthResponse {
  accessToken: string
  tokenType: string
  roles: string[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phoneNumber?: string
}

export interface Category {
  id: string
  name: string
  description: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CategoryRequest {
  name: string
  description?: string
  active?: boolean
}

export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'

export interface User {
  id: string
  firstName: string
  lastName: string
  email: string
  phoneNumber: string | null
  enabled: boolean
  roles: UserRole[]
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phoneNumber?: string
  role: UserRole
}

export interface UpdateUserRequest {
  firstName: string
  lastName: string
  email: string
  phoneNumber?: string
  enabled: boolean
  roles: UserRole[]
}

export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

export type ProductStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface Product {
  id: string
  name: string
  sku: string
  description: string | null
  price: number
  quantity: number
  minStock: number
  status: ProductStatus
  categoryId: string
  categoryName: string
  supplierId: string | null
  supplierName: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name: string
  sku: string
  description?: string
  price: number
  quantity: number
  minStock: number
  categoryId: string
  supplierId?: string
  active?: boolean
}

export interface Supplier {
  id: string
  name: string
  contactName: string | null
  email: string | null
  phone: string | null
  address: string | null
  active: boolean
  productCount: number
  createdAt: string
  updatedAt: string
}

export interface SupplierRequest {
  name: string
  contactName?: string
  email?: string
  phone?: string
  address?: string
  active?: boolean
}

export type MovementType = 'INBOUND' | 'OUTBOUND' | 'ADJUSTMENT'

export interface StockMovement {
  id: string
  productId: string
  productName: string
  sku: string
  type: MovementType
  quantity: number
  beforeQuantity: number
  afterQuantity: number
  reason: string
  performedBy: string
  createdAt: string
}

export interface MovementRequest {
  productId: string
  quantity: number
  reason: string
}

export interface RecentMovement {
  id: string
  productName: string
  sku: string
  type: MovementType
  quantity: number
  reason: string
  performedBy: string
  createdAt: string
}

export interface MonthlyMovementStats {
  month: string
  inbound: number
  outbound: number
}

export interface LowStockProduct {
  id: string
  name: string
  sku: string
  quantity: number
  minStock: number
}

export interface CategoryProductCount {
  name: string
  productCount: number
}

export interface DashboardSummary {
  totalProducts: number
  totalCategories: number
  lowStockCount: number
  outOfStockCount: number
  lowStockProducts: LowStockProduct[]
  productsPerCategory: CategoryProductCount[]
  recentMovements: RecentMovement[]
  monthlyMovements: MonthlyMovementStats[]
}

export interface AppSettings {
  warehouseName: string
  currency: string
  lowStockThreshold: number
  notificationsEnabled: boolean
  updatedAt: string
}

export interface SettingsRequest {
  warehouseName: string
  currency: string
  lowStockThreshold: number
  notificationsEnabled: boolean
}

export type StockAlertType = 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface StockAlert {
  productId: string
  productName: string
  sku: string
  quantity: number
  minStock: number
  type: StockAlertType
  createdAt: string
}

export interface AuditLogEntry {
  id: string
  action: string
  entityType: string
  entityId: string | null
  details: string | null
  performedBy: string
  createdAt: string
}

export type NotificationType = 'USER_MESSAGE' | 'STOCK_ALERT'

export interface AppNotification {
  id: string
  senderId: string | null
  senderName: string | null
  recipientId: string
  title: string
  message: string
  type: NotificationType
  read: boolean
  createdAt: string
}

export interface SendNotificationRequest {
  recipientId: string
  title: string
  message: string
}

export interface UserOption {
  id: string
  firstName: string
  lastName: string
  email: string
}

export type ReorderPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type ReorderRule = 'CRITICAL_DEFICIT' | 'VELOCITY_RUNOUT' | 'ROP_BREACH' | 'HEALTHY'

export interface RuleInputs {
  currentStock: number
  safetyStock: number
  avgDailyDemand: number
  leadTimeDays: number
  reorderPoint: number
  daysOfCover: number | null
  reorderPointFormula: string
  daysOfCoverFormula: string
  recommendedQuantityFormula: string
}

export interface ReorderRecommendation {
  productId: string
  productName: string
  sku: string
  categoryId: string | null
  categoryName: string
  supplierId: string | null
  supplierName: string
  supplierEmail: string | null
  unitPrice: number
  currentStock: number
  safetyStock: number
  leadTimeDays: number
  avgDailyDemand: number
  daysOfCover: number | null
  reorderPoint: number
  recommendedQuantity: number
  estimatedCost: number
  priority: ReorderPriority
  ruleApplied: ReorderRule
  reason: string
  ruleInputs: RuleInputs
}

export interface ReorderSummary {
  totalEvaluated: number
  totalFlagged: number
  criticalCount: number
  highCount: number
  mediumCount: number
  totalEstimatedRestockCost: number
}

export interface ReorderResponse {
  lookbackDays: number
  leadTimeDays: number
  summary: ReorderSummary
  recommendations: ReorderRecommendation[]
}

