import {
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useTheme,
} from '@mui/material'
import { NavLink } from 'react-router-dom'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined'
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined'
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined'
import WarehouseOutlinedIcon from '@mui/icons-material/WarehouseOutlined'
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined'
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined'
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined'
import AutoModeOutlinedIcon from '@mui/icons-material/AutoModeOutlined'
import { useAuth } from '../../context/AuthContext'

const DRAWER_WIDTH = 240

interface NavItem {
  label: string
  path: string
  icon: React.ReactNode
  adminOnly?: boolean
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlinedIcon /> },
  { label: 'Products', path: '/products', icon: <Inventory2OutlinedIcon /> },
  { label: 'Categories', path: '/categories', icon: <CategoryOutlinedIcon /> },
  { label: 'Suppliers', path: '/suppliers', icon: <LocalShippingOutlinedIcon /> },
  { label: 'Inventory', path: '/inventory', icon: <WarehouseOutlinedIcon /> },
  { label: 'Movements', path: '/movements', icon: <SwapHorizOutlinedIcon /> },
  { label: 'Reorder Hub', path: '/reorder', icon: <AutoModeOutlinedIcon /> },
  { label: 'Reports', path: '/reports', icon: <AssessmentOutlinedIcon /> },
  { label: 'Users', path: '/users', icon: <GroupOutlinedIcon />, adminOnly: true },
  { label: 'Activity', path: '/activity', icon: <ReceiptLongOutlinedIcon />, adminOnly: true },
  { label: 'Settings', path: '/settings', icon: <SettingsOutlinedIcon /> },
]

export default function Sidebar() {
  const theme = useTheme()
  const { isAdmin } = useAuth()

  const items = NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin)

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          boxSizing: 'border-box',
          borderRight: '1px solid',
          borderColor: 'divider',
        },
      }}
    >
      <Toolbar>
        <Typography variant="h6" fontWeight={700} color="primary">
          SmartWarehouse
        </Typography>
      </Toolbar>
      <List sx={{ px: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.path}
            component={NavLink}
            to={item.path}
            sx={{
              borderRadius: 2,
              mb: 0.5,
              '&.active': {
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                '& .MuiListItemIcon-root': { color: 'inherit' },
              },
            }}
          >
            <ListItemIcon sx={{ minWidth: 36, color: theme.palette.text.secondary }}>
              {item.icon}
            </ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Drawer>
  )
}
