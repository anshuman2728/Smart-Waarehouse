import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Skeleton,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined'
import AutoModeIcon from '@mui/icons-material/AutoMode'
import { reorderApi, type ReorderParams } from '@/api/reorderApi'
import { downloadBlob } from '@/utils/download'
import type { ReorderPriority, ReorderRecommendation, ReorderRule } from '@/types'

function formatCurrency(val: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)
}

function getPriorityChipColor(priority: ReorderPriority): 'error' | 'warning' | 'info' | 'success' {
  switch (priority) {
    case 'CRITICAL':
      return 'error'
    case 'HIGH':
      return 'warning'
    case 'MEDIUM':
      return 'info'
    default:
      return 'success'
  }
}

function getRuleBadgeLabel(rule: ReorderRule): string {
  switch (rule) {
    case 'CRITICAL_DEFICIT':
      return 'Safety Stock Deficit'
    case 'VELOCITY_RUNOUT':
      return 'Depletion Within Lead Time'
    case 'ROP_BREACH':
      return 'Reorder Point Breached'
    case 'HEALTHY':
      return 'Healthy Buffer'
  }
}

export default function ReorderRecommendations() {
  const [lookbackDays, setLookbackDays] = useState<number>(30)
  const [leadTimeDays, setLeadTimeDays] = useState<number>(7)
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL')
  const [flaggedOnly, setFlaggedOnly] = useState<boolean>(true)
  const [inspectItem, setInspectItem] = useState<ReorderRecommendation | null>(null)
  const [isExporting, setIsExporting] = useState<boolean>(false)

  const params: ReorderParams = {
    lookbackDays,
    leadTimeDays,
    flaggedOnly,
    priority: priorityFilter === 'ALL' ? undefined : (priorityFilter as ReorderPriority),
  }

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['reorder-recommendations', params],
    queryFn: () => reorderApi.getRecommendations(params),
  })

  const handleExportCsv = async () => {
    try {
      setIsExporting(true)
      const blob = await reorderApi.exportCsv(params)
      downloadBlob(blob, `reorder-recommendations-${new Date().toISOString().slice(0, 10)}.csv`)
    } catch (err) {
      console.error('Export failed', err)
    } finally {
      setIsExporting(false)
    }
  }

  const summary = data?.summary
  const recommendations = data?.recommendations ?? []

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* Page Title & Actions */}
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1}>
            <AutoModeIcon color="primary" sx={{ fontSize: 32 }} />
            <Typography variant="h5" fontWeight={700}>
              Reorder Recommendations
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Predictive inventory replenishment engine based on outbound consumption velocity, lead time, and Reorder Point (ROP).
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<DownloadOutlinedIcon />}
          onClick={handleExportCsv}
          disabled={isExporting || isLoading || recommendations.length === 0}
        >
          {isExporting ? 'Exporting...' : 'Export CSV'}
        </Button>
      </Stack>

      {/* KPI Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%', boxShadow: 1 }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="overline" color="text.secondary" fontWeight={600}>
                  Flagged Items
                </Typography>
                <ShoppingCartCheckoutIcon color="primary" />
              </Stack>
              <Typography variant="h4" fontWeight={700} sx={{ mt: 1 }}>
                {isLoading ? <Skeleton width={60} /> : summary?.totalFlagged ?? 0}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                out of {summary?.totalEvaluated ?? 0} active products
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%', boxShadow: 1 }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="overline" color="error" fontWeight={600}>
                  Critical Deficits
                </Typography>
                <ErrorOutlineIcon color="error" />
              </Stack>
              <Typography variant="h4" fontWeight={700} color="error.main" sx={{ mt: 1 }}>
                {isLoading ? <Skeleton width={60} /> : summary?.criticalCount ?? 0}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                stock &le; safety threshold
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%', boxShadow: 1 }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="overline" color="warning.main" fontWeight={600}>
                  Lead Time Runout
                </Typography>
                <WarningAmberOutlinedIcon color="warning" />
              </Stack>
              <Typography variant="h4" fontWeight={700} color="warning.main" sx={{ mt: 1 }}>
                {isLoading ? <Skeleton width={60} /> : summary?.highCount ?? 0}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                will exhaust in &le; {leadTimeDays} days
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%', boxShadow: 1 }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="overline" color="text.secondary" fontWeight={600}>
                  Est. Restock Capital
                </Typography>
                <AttachMoneyIcon color="success" />
              </Stack>
              <Typography variant="h4" fontWeight={700} sx={{ mt: 1 }}>
                {isLoading ? <Skeleton width={100} /> : formatCurrency(summary?.totalEstimatedRestockCost ?? 0)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                to replenish all flagged items
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Control / Filter Bar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="lookback-label">Demand Window</InputLabel>
            <Select
              labelId="lookback-label"
              value={lookbackDays}
              label="Demand Window"
              onChange={(e) => setLookbackDays(Number(e.target.value))}
            >
              <MenuItem value={7}>Past 7 Days</MenuItem>
              <MenuItem value={14}>Past 14 Days</MenuItem>
              <MenuItem value={30}>Past 30 Days (Default)</MenuItem>
              <MenuItem value={60}>Past 60 Days</MenuItem>
              <MenuItem value={90}>Past 90 Days</MenuItem>
            </Select>
          </FormControl>

          <TextField
            size="small"
            type="number"
            label="Supplier Lead Time (Days)"
            value={leadTimeDays}
            onChange={(e) => setLeadTimeDays(Math.max(1, Math.min(90, Number(e.target.value) || 1)))}
            sx={{ width: { xs: '100%', md: 190 } }}
            inputProps={{ min: 1, max: 90 }}
          />

          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel id="priority-label">Priority</InputLabel>
            <Select
              labelId="priority-label"
              value={priorityFilter}
              label="Priority"
              onChange={(e) => setPriorityFilter(e.target.value)}
            >
              <MenuItem value="ALL">All Priorities</MenuItem>
              <MenuItem value="CRITICAL">Critical Only</MenuItem>
              <MenuItem value="HIGH">High Only</MenuItem>
              <MenuItem value="MEDIUM">Medium Only</MenuItem>
              <MenuItem value="LOW">Low (Healthy)</MenuItem>
            </Select>
          </FormControl>

          <FormControlLabel
            control={
              <Switch
                checked={flaggedOnly}
                onChange={(e) => setFlaggedOnly(e.target.checked)}
                color="primary"
              />
            }
            label="Flagged Only"
            sx={{ ml: { md: 'auto' } }}
          />
        </Stack>
      </Paper>

      {/* Error state */}
      {isError && (
        <Paper sx={{ p: 3, mb: 3, bgcolor: 'error.light', color: 'error.contrastText' }}>
          <Typography variant="body1">
            Failed to load reorder recommendations: {(error as Error)?.message || 'Unknown error'}
          </Typography>
        </Paper>
      )}

      {/* Recommendations Table */}
      <TableContainer component={Paper} sx={{ boxShadow: 1 }}>
        <Table sx={{ minWidth: 800 }}>
          <TableHead sx={{ bgcolor: 'action.hover' }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Priority</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Product</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Category / Supplier</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>Stock / Min</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>Daily Demand</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>Days of Cover</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>ROP</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>Recommended Order</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>Est. Cost</TableCell>
              <TableCell align="center" sx={{ fontWeight: 700 }}>Why Flagged?</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, idx) => (
                <TableRow key={idx}>
                  <TableCell colSpan={10}>
                    <Skeleton height={40} />
                  </TableCell>
                </TableRow>
              ))
            ) : recommendations.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 6 }}>
                  <Typography variant="body1" color="text.secondary">
                    No products match the selected reorder criteria.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              recommendations.map((item) => (
                <TableRow key={item.productId} hover>
                  <TableCell>
                    <Chip
                      size="small"
                      label={item.priority}
                      color={getPriorityChipColor(item.priority)}
                      sx={{ fontWeight: 700 }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                      {item.productName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      SKU: {item.sku}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{item.categoryName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.supplierName}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight={600}>
                      {item.currentStock}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      min: {item.safetyStock}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2">{item.avgDailyDemand.toFixed(2)}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      units/day
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    {item.daysOfCover !== null ? (
                      <Chip
                        size="small"
                        variant="outlined"
                        color={item.daysOfCover <= leadTimeDays ? 'error' : item.daysOfCover <= leadTimeDays * 2 ? 'warning' : 'default'}
                        label={`${item.daysOfCover.toFixed(1)} days`}
                      />
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        &infin;
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight={600}>
                      {item.reorderPoint}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      units
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight={700} color="primary.main">
                      +{item.recommendedQuantity}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight={600}>
                      {formatCurrency(item.estimatedCost)}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="View formula and explanation">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={() => setInspectItem(item)}
                      >
                        <InfoOutlinedIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Why Flagged? Formula & Explanation Inspector Dialog */}
      <Dialog
        open={Boolean(inspectItem)}
        onClose={() => setInspectItem(null)}
        maxWidth="sm"
        fullWidth
      >
        {inspectItem && (
          <>
            <DialogTitle>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="h6" fontWeight={700}>
                    {inspectItem.productName}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    SKU: {inspectItem.sku} &bull; {inspectItem.categoryName}
                  </Typography>
                </Box>
                <Chip
                  label={inspectItem.priority}
                  color={getPriorityChipColor(inspectItem.priority)}
                  sx={{ fontWeight: 700 }}
                />
              </Stack>
            </DialogTitle>
            <DialogContent dividers>
              {/* Triggered Rule & Reason */}
              <Box sx={{ mb: 2.5, p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                  <CalculateOutlinedIcon color="primary" />
                  <Typography variant="subtitle2" fontWeight={700}>
                    Applied Rule: {getRuleBadgeLabel(inspectItem.ruleApplied)} ({inspectItem.ruleApplied})
                  </Typography>
                </Stack>
                <Typography variant="body2" color="text.primary">
                  {inspectItem.reason}
                </Typography>
              </Box>

              {/* Exact Numerical Inputs */}
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>
                Calculation Inputs &amp; Metrics
              </Typography>
              <Grid container spacing={1.5} sx={{ mb: 2.5 }}>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Current Stock</Typography>
                    <Typography variant="h6" fontWeight={700}>{inspectItem.ruleInputs.currentStock}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Safety Stock</Typography>
                    <Typography variant="h6" fontWeight={700}>{inspectItem.ruleInputs.safetyStock}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Daily Demand</Typography>
                    <Typography variant="h6" fontWeight={700}>{inspectItem.ruleInputs.avgDailyDemand.toFixed(2)}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Lead Time</Typography>
                    <Typography variant="h6" fontWeight={700}>{inspectItem.ruleInputs.leadTimeDays}d</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Reorder Point</Typography>
                    <Typography variant="h6" fontWeight={700}>{inspectItem.ruleInputs.reorderPoint}</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6} sm={4}>
                  <Paper variant="outlined" sx={{ p: 1.5, textAlign: 'center' }}>
                    <Typography variant="caption" color="text.secondary">Days of Cover</Typography>
                    <Typography variant="h6" fontWeight={700}>
                      {inspectItem.ruleInputs.daysOfCover !== null ? `${inspectItem.ruleInputs.daysOfCover.toFixed(1)}d` : 'None'}
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              {/* Step by Step Formula */}
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1 }}>
                Formula Breakdown
              </Typography>
              <Stack spacing={1}>
                <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1.5 }}>
                  <Typography variant="caption" color="text.secondary" fontWeight={600}>
                    1. Reorder Point (ROP = Demand &times; Lead Time + Safety Stock)
                  </Typography>
                  <Typography variant="body2" fontFamily="monospace">
                    {inspectItem.ruleInputs.reorderPointFormula}
                  </Typography>
                </Box>
                <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1.5 }}>
                  <Typography variant="caption" color="text.secondary" fontWeight={600}>
                    2. Days of Cover (DoC = Current Stock / Demand)
                  </Typography>
                  <Typography variant="body2" fontFamily="monospace">
                    {inspectItem.ruleInputs.daysOfCoverFormula}
                  </Typography>
                </Box>
                <Box sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1.5 }}>
                  <Typography variant="caption" color="text.secondary" fontWeight={600}>
                    3. Recommended Order Quantity (ROQ)
                  </Typography>
                  <Typography variant="body2" fontFamily="monospace">
                    {inspectItem.ruleInputs.recommendedQuantityFormula}
                  </Typography>
                </Box>
              </Stack>
            </DialogContent>
            <DialogActions sx={{ p: 2 }}>
              <Button onClick={() => setInspectItem(null)}>Close</Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </Box>
  )
}
