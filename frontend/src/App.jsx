import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  getCategories,
  getSummary,
  getTransactions,
  getTrends,
  registerAccount,
  setApiCredentials,
  clearApiCredentials,
  updateCategory,
  uploadStatement,
} from './api.js'

const CATEGORY_OPTIONS = [
  'INCOME', 'HOUSING', 'GROCERIES', 'TRANSPORT', 'UTILITIES', 'HEALTHCARE',
  'SHOPPING', 'DINING', 'TRANSFER', 'REFUND', 'FEES', 'OTHER', 'UNCATEGORIZED',
]
const CATEGORY_COLORS = ['#316a58', '#74a28c', '#a4c3a8', '#d7a85c', '#b7a3d2', '#6e91a8', '#d98171', '#bac6bd']
const PAGE_SIZE = 8

function isoDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function initialRange() {
  const today = new Date()
  const start = new Date(today.getFullYear(), today.getMonth() - 11, 1)
  return { from: isoDate(start), to: isoDate(today) }
}

function label(value = '') {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (char) => char.toUpperCase())
}

function formatCurrency(amount, currencyCode) {
  if (!currencyCode) return '—'
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: currencyCode, maximumFractionDigits: 2, minimumFractionDigits: 0,
    }).format(Number(amount || 0))
  } catch {
    return `${currencyCode} ${Number(amount || 0).toLocaleString('en-IN')}`
  }
}

function Icon({ name, size = 19, strokeWidth = 1.8 }) {
  const paths = {
    overview: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /></>,
    transactions: <><path d="M8 6h13M8 12h13M8 18h13" /><path d="M3 6h.01M3 12h.01M3 18h.01" /></>,
    insight: <><path d="M3 3v18h18" /><path d="m7 14 4-4 4 3 6-7" /></>,
    upload: <><path d="M12 16V4" /><path d="m7 9 5-5 5 5" /><path d="M4 20h16" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.7 2.94-.08-.02a1.7 1.7 0 0 0-1.8.54l-.05.07h-3.4l-.03-.08a1.7 1.7 0 0 0-1.45-1.1 1.7 1.7 0 0 0-1.56.67l-.05.07-2.95-1.7.02-.08a1.7 1.7 0 0 0-.54-1.8l-.07-.05V13l.08-.03a1.7 1.7 0 0 0 1.1-1.45 1.7 1.7 0 0 0-.67-1.56l-.07-.05 1.7-2.95.08.02a1.7 1.7 0 0 0 1.8-.54l.05-.07h3.4l.03.08a1.7 1.7 0 0 0 1.45 1.1 1.7 1.7 0 0 0 1.56-.67l.05-.07 2.95 1.7-.02.08a1.7 1.7 0 0 0 .54 1.8l.07.05v3.4z" /></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    arrow: <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
    download: <><path d="M12 4v12" /><path d="m7 11 5 5 5-5" /><path d="M4 20h16" /></>,
    file: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6M8 13h8M8 17h8" /></>,
    refresh: <><path d="M20 7v5h-5" /><path d="M4 17v-5h5" /><path d="M5.6 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 12.4-3" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    wallet: <><rect x="3" y="5" width="18" height="15" rx="2" /><path d="M3 9h18M16 15h2" /><path d="M6 5V3h12" /></>,
    trend: <><path d="m3 16 6-6 4 4 8-9" /><path d="M15 5h6v6" /></>,
    logout: <><path d="M10 17l5-5-5-5" /><path d="M15 12H3" /><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6" /></>,
    dots: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.overview}</svg>
}

function currencyList(summary) {
  return summary?.currencies || []
}

function initials(username = '') {
  return username.slice(0, 2).toUpperCase() || 'ME'
}

function MetricCard({ title, amount, currency, footnote, icon, tone, suffix }) {
  return (
    <article className={`metric-card metric-${tone}`}>
      <div className="metric-top">
        <span>{title}</span>
        <span className="metric-icon"><Icon name={icon} size={17} /></span>
      </div>
      <div className="metric-value">{suffix ? (amount == null ? '—' : `${Number(amount).toFixed(1)}${suffix}`) : formatCurrency(amount, currency)}</div>
      <div className="metric-footnote">{footnote}</div>
    </article>
  )
}

function CashflowChart({ data, currency }) {
  const width = 760
  const height = 248
  const left = 46
  const right = 12
  const top = 16
  const bottom = 36
  const chartWidth = width - left - right
  const chartHeight = height - top - bottom
  const maxValue = Math.max(1, ...data.flatMap((row) => [Number(row.income), Number(row.netExpenses)]))
  const step = data.length ? chartWidth / data.length : chartWidth
  const barWidth = Math.min(14, step * 0.23)
  const grid = [0, 0.33, 0.66, 1]

  if (!data.length) return <div className="chart-empty">Upload a statement with transactions to see your monthly cash flow.</div>

  return (
    <div className="chart-wrap">
      <svg className="cashflow-svg" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`Monthly income and expenses in ${currency}`}>
        {grid.map((ratio) => {
          const y = top + chartHeight * ratio
          return <g key={ratio}><line x1={left} x2={width - right} y1={y} y2={y} className="chart-gridline" /><text x={left - 8} y={y + 4} textAnchor="end" className="chart-axis-label">{ratio === 0 ? '0' : `${Math.round(maxValue * (1 - ratio) / 1000)}k`}</text></g>
        })}
        {data.map((row, index) => {
          const x = left + step * index + step / 2
          const incomeHeight = Number(row.income) / maxValue * chartHeight
          const expenseHeight = Math.max(0, Number(row.netExpenses)) / maxValue * chartHeight
          return <g key={`${row.currencyCode}-${row.month}`}>
            <rect x={x - barWidth - 2} y={top + chartHeight - incomeHeight} width={barWidth} height={incomeHeight} rx="4" className="bar-income"><title>{row.month} income: {formatCurrency(row.income, currency)}</title></rect>
            <rect x={x + 2} y={top + chartHeight - expenseHeight} width={barWidth} height={expenseHeight} rx="4" className="bar-expense"><title>{row.month} net expenses: {formatCurrency(row.netExpenses, currency)}</title></rect>
            {(index === 0 || index === data.length - 1 || index % 2 === 0) && <text x={x} y={height - 10} textAnchor="middle" className="chart-axis-label">{new Date(`${row.month}-15T12:00:00`).toLocaleDateString('en', { month: 'short' })}</text>}
          </g>
        })}
      </svg>
      <div className="chart-legend"><span><i className="legend-dot income-dot" />Income</span><span><i className="legend-dot expense-dot" />Net expenses</span></div>
    </div>
  )
}

function CategoryChart({ categories, currency }) {
  const data = categories.filter((item) => item.currencyCode === currency && Number(item.amount) > 0)
  const total = data.reduce((sum, item) => sum + Number(item.amount), 0)
  const radius = 53
  const circumference = 2 * Math.PI * radius
  let offset = 0
  return (
    <div className="category-content">
      <div className="donut-holder">
        <svg viewBox="0 0 140 140" className="donut" role="img" aria-label="Spending by category">
          <circle cx="70" cy="70" r={radius} fill="none" stroke="#edf0eb" strokeWidth="17" />
          {total > 0 && data.map((item, index) => {
            const length = Number(item.amount) / total * circumference
            const segment = <circle key={`${item.category}-${item.currencyCode}`} cx="70" cy="70" r={radius} fill="none" stroke={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} strokeWidth="17" strokeDasharray={`${length} ${circumference - length}`} strokeDashoffset={-offset} transform="rotate(-90 70 70)" />
            offset += length
            return segment
          })}
        </svg>
        <div className="donut-center"><strong>{formatCurrency(total, currency)}</strong><span>spent</span></div>
      </div>
      <div className="category-legend">
        {data.length ? data.map((item, index) => (
          <div className="category-legend-row" key={`${item.category}-${item.currencyCode}`}>
            <span className="category-label"><i style={{ background: CATEGORY_COLORS[index % CATEGORY_COLORS.length] }} />{label(item.category)}</span>
            <strong>{formatCurrency(item.amount, currency)}</strong>
          </div>
        )) : <p className="muted small-copy">No categorized debit transactions for this currency and date range.</p>}
      </div>
    </div>
  )
}

function App() {
  const [authenticated, setAuthenticated] = useState(false)
  const [loginName, setLoginName] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [loginError, setLoginError] = useState('')
  const [loggingIn, setLoggingIn] = useState(false)
  const [registering, setRegistering] = useState(false)
  const [range, setRange] = useState(initialRange)
  const [summary, setSummary] = useState(null)
  const [trends, setTrends] = useState([])
  const [categories, setCategories] = useState([])
  const [transactions, setTransactions] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 })
  const [currency, setCurrency] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [directionFilter, setDirectionFilter] = useState('')
  const [page, setPage] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')
  const formRef = useRef(null)
  const [file, setFile] = useState(null)
  const [dragging, setDragging] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadResult, setUploadResult] = useState(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')
  const [transactionError, setTransactionError] = useState('')
  const [uploadError, setUploadError] = useState('')
  const [savingTransaction, setSavingTransaction] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  const rangeParams = useMemo(() => new URLSearchParams({ from: range.from, to: range.to }), [range])
  const rangeKey = rangeParams.toString()
  const availableCurrencies = currencyList(summary)
  const activeCurrency = availableCurrencies.some((item) => item.currencyCode === currency)
    ? currency
    : availableCurrencies[0]?.currencyCode || ''
  const selectedSummary = availableCurrencies.find((item) => item.currencyCode === activeCurrency)
  const selectedTrends = trends.filter((item) => item.currencyCode === activeCurrency)

  useEffect(() => {
    if (!authenticated) return undefined
    let cancelled = false
    setDashboardLoading(true)
    setDashboardError('')
    Promise.all([getSummary(rangeKey), getTrends(rangeKey), getCategories(rangeKey)])
      .then(([summaryData, trendData, categoryData]) => {
        if (cancelled) return
        setSummary(summaryData)
        setTrends(trendData.trends || [])
        setCategories(categoryData.categories || [])
        setCurrency((previous) => summaryData.currencies?.some((item) => item.currencyCode === previous)
          ? previous
          : summaryData.currencies?.[0]?.currencyCode || '')
      })
      .catch((error) => { if (!cancelled) setDashboardError(error.message) })
      .finally(() => { if (!cancelled) setDashboardLoading(false) })
    return () => { cancelled = true }
  }, [authenticated, rangeKey, refreshKey])

  useEffect(() => {
    if (!authenticated) return undefined
    let cancelled = false
    setTransactionError('')
    getTransactions({ range: rangeParams, category: categoryFilter, direction: directionFilter, q: searchTerm, page, size: PAGE_SIZE })
      .then((result) => { if (!cancelled) setTransactions(result) })
      .catch((error) => { if (!cancelled) setTransactionError(error.message) })
    return () => { cancelled = true }
  }, [authenticated, rangeKey, categoryFilter, directionFilter, searchTerm, page, refreshKey])

  async function handleLogin(event) {
    event.preventDefault()
    if (!loginName || !loginPassword || loggingIn) return
    setLoggingIn(true)
    setLoginError('')
    setApiCredentials(loginName, loginPassword)
    try {
      if (registering) await registerAccount(loginName, loginPassword)
      await getSummary('')
      setAuthenticated(true)
      setLoginPassword('')
    } catch (error) {
      clearApiCredentials()
      setLoginError(registering ? error.message : 'Sign-in failed. Check your username and password.')
    } finally {
      setLoggingIn(false)
    }
  }

  function handleLogout() {
    clearApiCredentials()
    setAuthenticated(false)
    setSummary(null)
    setTransactions({ content: [], page: 0, totalPages: 0, totalElements: 0 })
  }

  const chooseFile = useCallback((candidate) => {
    setUploadError('')
    if (!candidate) return
    if (!candidate.name.toLowerCase().endsWith('.pdf')) {
      setFile(null)
      setUploadError('Choose a PDF file to continue.')
      return
    }
    if (candidate.size > 10 * 1024 * 1024) {
      setFile(null)
      setUploadError('The file must be 10 MB or smaller.')
      return
    }
    setFile(candidate)
  }, [])

  async function handleUpload(event) {
    event.preventDefault()
    if (!file || uploading) return
    setUploading(true)
    setUploadError('')
    setUploadResult(null)
    try {
      const result = await uploadStatement(file)
      setUploadResult(result)
      setFile(null)
      formRef.current?.reset()
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setUploadError(error.message)
    } finally {
      setUploading(false)
    }
  }

  async function handleCategoryChange(transaction, nextCategory) {
    setSavingTransaction(transaction.id)
    setTransactionError('')
    try {
      await updateCategory(transaction.id, nextCategory)
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setTransactionError(error.message)
    } finally {
      setSavingTransaction('')
    }
  }

  function updateRange(key, value) {
    setRange((previous) => ({ ...previous, [key]: value }))
    setPage(0)
  }

  if (!authenticated) {
    return (
      <main className="login-shell">
        <form className="login-card" onSubmit={handleLogin}>
          <span className="brand-mark"><Icon name="trend" size={20} strokeWidth={2.1} /></span>
          <p className="eyebrow">FINSIGHT WORKSPACE</p>
          <h1>Sign in to your finances</h1>
          <p className="muted">{registering ? 'Create a private account for your statements.' : 'Sign in with your analyzer account.'}</p>
          <label>Username<input autoComplete="username" value={loginName} onChange={(event) => setLoginName(event.target.value)} required /></label>
          <label>Password<input type="password" autoComplete="current-password" value={loginPassword} onChange={(event) => setLoginPassword(event.target.value)} required /></label>
          {loginError && <p className="form-error" role="alert">{loginError}</p>}
          {registering && <p className="muted small-copy">Use a unique username and a password with at least 12 characters.</p>}
          <button className="primary-button" type="submit" disabled={loggingIn}>{loggingIn ? 'Please wait…' : registering ? 'Create account' : 'Sign in'}</button>
          <button className="text-button" type="button" onClick={() => { setRegistering((value) => !value); setLoginError('') }}>
            {registering ? 'Already registered? Sign in' : 'New here? Create an account'}
          </button>
        </form>
      </main>
    )
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview" aria-label="FinSight home">
          <span className="brand-mark"><Icon name="trend" size={20} strokeWidth={2.1} /></span>
          <span>finsight<span className="brand-period">.</span></span>
        </a>
        <div className="side-caption">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <a className="side-link active" href="#overview"><Icon name="overview" />Overview</a>
          <a className="side-link" href="#transactions"><Icon name="transactions" />Transactions</a>
          <a className="side-link" href="#insights"><Icon name="insight" />Insights</a>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-card">
            <span className="help-orbit">?</span>
            <strong>Need a hand?</strong>
            <p>Learn how your statement insights work.</p>
            <a href="#how-it-works">Read the guide <Icon name="arrow" size={14} /></a>
          </div>
          <div className="profile-row">
            <div className="avatar">{initials(loginName)}</div>
            <div className="profile-copy"><strong>{loginName}</strong><span>Personal finance</span></div>
          </div>
        </div>
      </aside>

      <main className="main-content" id="overview">
        <header className="topbar">
          <div className="breadcrumb">Workspace <Icon name="chevron" size={14} /> <span>Overview</span></div>
          <div className="top-actions">
            <button className="icon-button search-button" aria-label="Search transactions" onClick={() => document.getElementById('transaction-search')?.focus()}><Icon name="search" /></button>
            <div className="top-avatar" aria-label={`Signed in as ${loginName}`}>{initials(loginName)}</div>
            <button className="logout-button" onClick={handleLogout}><Icon name="logout" size={15} /> Sign out</button>
          </div>
        </header>

        <div className="page-wrap">
          <section className="welcome-row">
            <div>
              <p className="eyebrow">YOUR FINANCIAL SNAPSHOT</p>
              <h1>Money, made clearer.</h1>
              <p className="subhead">A thoughtful view of where it goes and how it grows.</p>
            </div>
            <div className="range-controls">
              <label><span>From</span><input type="date" value={range.from} max={range.to} onChange={(event) => updateRange('from', event.target.value)} /></label>
              <span className="range-divider">—</span>
              <label><span>To</span><input type="date" value={range.to} min={range.from} onChange={(event) => updateRange('to', event.target.value)} /></label>
              {availableCurrencies.length > 1 && <label className="currency-select"><span>Currency</span><select value={activeCurrency} onChange={(event) => setCurrency(event.target.value)}>{availableCurrencies.map((item) => <option key={item.currencyCode} value={item.currencyCode}>{item.currencyCode}</option>)}</select></label>}
            </div>
          </section>

          {dashboardError && <div className="alert alert-error"><strong>Couldn’t load your insights.</strong> {dashboardError}</div>}
          {uploadResult && <div className={`alert ${uploadResult.status === 'COMPLETED' ? 'alert-success' : 'alert-review'}`}>
            <strong>{uploadResult.status === 'COMPLETED' ? 'Statement processed.' : `Statement ${label(uploadResult.status).toLowerCase()}.`}</strong>
            <span>{uploadResult.originalFilename}{uploadResult.currencyCode ? ` · ${uploadResult.currencyCode}` : ''}</span>
            {uploadResult.processingMessage && <p>{uploadResult.processingMessage}</p>}
            <button className="alert-close" onClick={() => setUploadResult(null)} aria-label="Dismiss message">×</button>
          </div>}

          <section className="metrics-grid" aria-label="Financial summary">
            <MetricCard title="Income" amount={selectedSummary?.income} currency={activeCurrency} footnote="Categorized income received" icon="wallet" tone="green" />
            <MetricCard title="Net expenses" amount={selectedSummary?.netExpenses} currency={activeCurrency} footnote="Spending after recognized refunds" icon="download" tone="peach" />
            <MetricCard title="Net savings" amount={selectedSummary?.netSavings} currency={activeCurrency} footnote="Income less net expenses" icon="trend" tone="lavender" />
            <MetricCard title="Savings rate" amount={selectedSummary?.savingsRatePercent} currency={activeCurrency} suffix="%" footnote="Of categorized income" icon="insight" tone="sand" />
          </section>
          {selectedSummary && selectedSummary.refunds > 0 && <p className="summary-note">Includes {formatCurrency(selectedSummary.refunds, activeCurrency)} in recognized refunds. Transfers are excluded from income and expenses.</p>}

          <section className="upload-panel" id="import">
            <div className="upload-intro">
              <div className="upload-icon"><Icon name="file" size={20} /></div>
              <div><p className="eyebrow">START WITH A STATEMENT</p><h2>Bring your money story in</h2><p>Upload a bank statement PDF to see your spending take shape.</p></div>
            </div>
            <form ref={formRef} onSubmit={handleUpload} className="upload-form">
              <label className={`drop-zone ${dragging ? 'is-dragging' : ''}`} onDragOver={(event) => { event.preventDefault(); setDragging(true) }} onDragLeave={() => setDragging(false)} onDrop={(event) => { event.preventDefault(); setDragging(false); chooseFile(event.dataTransfer.files?.[0]) }}>
                <input type="file" accept="application/pdf,.pdf" onChange={(event) => chooseFile(event.target.files?.[0])} />
                <span className="drop-icon"><Icon name="upload" size={18} /></span>
                <span className="drop-copy"><strong>{file ? file.name : 'Drop your PDF here'}</strong><small>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB · PDF` : 'or click to browse · up to 10 MB'}</small></span>
                {file && <span className="file-ready">Ready</span>}
              </label>
              <button className="primary-button upload-button" type="submit" disabled={!file || uploading}>{uploading ? <><span className="spinner" />Processing</> : <>Analyze statement <Icon name="arrow" size={16} /></>}</button>
              {(uploadError) && <p className="inline-error">{uploadError}</p>}
            </form>
            <div className="privacy-note"><span className="privacy-lock">⌑</span> Your statements stay in your workspace. We never connect to your bank.</div>
          </section>

          <section className="charts-grid" id="insights">
            <article className="panel cashflow-panel">
              <div className="panel-heading"><div><p className="eyebrow">CASH FLOW</p><h2>Income & expenses</h2></div><span className="period-pill">{activeCurrency || 'Choose currency'}</span></div>
              {dashboardLoading ? <div className="chart-skeleton" /> : <CashflowChart data={selectedTrends} currency={activeCurrency} />}
            </article>
            <article className="panel category-panel">
              <div className="panel-heading"><div><p className="eyebrow">SPENDING BREAKDOWN</p><h2>By category</h2></div><span className="panel-hint">Debits, excluding transfers</span></div>
              {dashboardLoading ? <div className="chart-skeleton donut-skeleton" /> : <CategoryChart categories={categories} currency={activeCurrency} />}
            </article>
          </section>

          <section className="transactions-panel panel" id="transactions">
            <div className="transactions-heading">
              <div><p className="eyebrow">YOUR ACTIVITY</p><h2>Transactions <span className="count-pill">{transactions.totalElements || 0}</span></h2></div>
              <button className="text-button" onClick={() => { setCategoryFilter(''); setDirectionFilter(''); setSearchTerm(''); setPage(0) }}>Clear filters <Icon name="refresh" size={15} /></button>
            </div>
            <div className="table-toolbar">
              <label className="search-field"><Icon name="search" size={16} /><input id="transaction-search" placeholder="Search by merchant or description" maxLength={100} onChange={(event) => { setSearchTerm(event.target.value); setPage(0) }} /></label>
              <div className="table-filters">
                <select value={categoryFilter} onChange={(event) => { setCategoryFilter(event.target.value); setPage(0) }} aria-label="Filter by category"><option value="">All categories</option>{CATEGORY_OPTIONS.map((category) => <option value={category} key={category}>{label(category)}</option>)}</select>
                <select value={directionFilter} onChange={(event) => { setDirectionFilter(event.target.value); setPage(0) }} aria-label="Filter by direction"><option value="">All types</option><option value="DEBIT">Expenses</option><option value="CREDIT">Credits</option></select>
              </div>
            </div>
            {transactionError && <div className="inline-error table-error">{transactionError}</div>}
            <div className="table-scroll"><table>
              <thead><tr><th>Transaction</th><th>Date</th><th>Amount</th><th>Type</th><th>Category</th><th aria-label="Category source" /></tr></thead>
              <tbody>
                {transactions.content?.map((transaction) => (
                  <tr key={transaction.id}>
                    <td><div className="transaction-title"><span className={`merchant-icon merchant-${transaction.direction.toLowerCase()}`}>{merchantInitial(transaction.description)}</span><span><strong>{transaction.description}</strong><small>Statement transaction</small></span></div></td>
                    <td className="date-cell">{new Date(`${transaction.transactionDate}T12:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                    <td className={`amount-cell ${transaction.direction === 'CREDIT' ? 'amount-credit' : ''}`}>{transaction.direction === 'DEBIT' ? '−' : '+'}{formatCurrency(transaction.amount, transaction.currencyCode)}</td>
                    <td><span className={`type-pill ${transaction.direction.toLowerCase()}`}>{transaction.direction === 'DEBIT' ? 'Expense' : transaction.category === 'INCOME' ? 'Income' : 'Credit'}</span></td>
                    <td><select className="category-edit" aria-label={`Category for ${transaction.description}`} value={transaction.category} disabled={savingTransaction === transaction.id} onChange={(event) => handleCategoryChange(transaction, event.target.value)}>{CATEGORY_OPTIONS.map((item) => <option key={item} value={item}>{label(item)}</option>)}</select></td>
                    <td>{transaction.categoryWasAssignedAutomatically && <span className="auto-tag" title={transaction.categoryExplanation || 'Automatically categorized'}>Auto</span>}</td>
                  </tr>
                ))}
                {!transactions.content?.length && <tr><td colSpan="6"><div className="table-empty"><span className="empty-file"><Icon name="file" size={22} /></span><strong>No transactions yet</strong><small>Upload a statement PDF to start your overview.</small><a href="#import">Upload a statement <Icon name="arrow" size={14} /></a></div></td></tr>}
              </tbody>
            </table></div>
            <div className="table-footer"><span>Showing {transactions.content?.length ? page * PAGE_SIZE + 1 : 0}–{Math.min((page + 1) * PAGE_SIZE, transactions.totalElements || 0)} of {transactions.totalElements || 0}</span><div className="pagination"><button disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))} aria-label="Previous page">‹</button><span>Page {transactions.totalPages ? page + 1 : 0} of {transactions.totalPages}</span><button disabled={!transactions.totalPages || page + 1 >= transactions.totalPages} onClick={() => setPage((value) => value + 1)} aria-label="Next page">›</button></div></div>
          </section>

          <footer className="footer-note" id="how-it-works"><span>FinSight</span> turns statements into a clearer picture. Review categories and check every insight against your source statement.</footer>
        </div>
      </main>
    </div>
  )
}

function merchantInitial(description = '') {
  const letters = description.trim().replace(/[^A-Za-z0-9 ]/g, '').split(/\s+/).filter(Boolean).slice(0, 2)
  return letters.map((word) => word[0]).join('').toUpperCase() || 'TX'
}

export default App


