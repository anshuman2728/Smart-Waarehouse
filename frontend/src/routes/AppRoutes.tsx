import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from '../components/layout/Layout'
import ProtectedRoute from './ProtectedRoute'
import AdminRoute from './AdminRoute'
import Loading from '../components/common/Loading'

const Login = lazy(() => import('../pages/Login'))
const Landing = lazy(() => import('../pages/Landing'))
const Dashboard = lazy(() => import('../pages/Dashboard'))
const Categories = lazy(() => import('../pages/Categories'))
const Products = lazy(() => import('../pages/Products'))
const Suppliers = lazy(() => import('../pages/Suppliers'))
const Inventory = lazy(() => import('../pages/Inventory'))
const Movements = lazy(() => import('../pages/Movements'))
const Users = lazy(() => import('../pages/Users'))
const Activity = lazy(() => import('../pages/Activity'))
const Settings = lazy(() => import('../pages/Settings'))
const Reports = lazy(() => import('../pages/Reports'))
const ReorderRecommendations = lazy(() => import('../pages/ReorderRecommendations'))

export default function AppRoutes() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/products" element={<Products />} />
            <Route path="/suppliers" element={<Suppliers />} />
            <Route path="/inventory" element={<Inventory />} />
            <Route path="/movements" element={<Movements />} />
            <Route path="/reorder" element={<ReorderRecommendations />} />
            <Route element={<AdminRoute />}>
              <Route path="/users" element={<Users />} />
              <Route path="/activity" element={<Activity />} />
            </Route>
            <Route path="/settings" element={<Settings />} />
            <Route path="/reports" element={<Reports />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
