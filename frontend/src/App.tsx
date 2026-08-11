import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Routes, Route, Link } from 'react-router'
import OverviewPage from './pages/OverviewPage'
import AlertSimulatorPage from './pages/AlertSimulatorPage'
import IncidentsPage from './pages/IncidentsPage'
import IncidentDetailPage from './pages/IncidentDetailPage'
import FailedEventsPage from './pages/FailedEventsPage'
import RoutingRulesPage from './pages/RoutingRulesPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchInterval: 5000,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="min-h-screen flex">
          <nav className="w-56 shrink-0 border-r border-gray-200 p-4 space-y-2">
            <h1 className="text-lg font-semibold mb-4">IncidentFlow</h1>
            <Link to="/" className="block text-sm text-gray-700 hover:text-black">
              Overview
            </Link>
            <Link to="/simulator" className="block text-sm text-gray-700 hover:text-black">
              Alert Simulator
            </Link>
            <Link to="/incidents" className="block text-sm text-gray-700 hover:text-black">
              Incidents
            </Link>
            <Link to="/failures" className="block text-sm text-gray-700 hover:text-black">
              Failed Events
            </Link>
            <Link to="/routing" className="block text-sm text-gray-700 hover:text-black">
              Routing Rules
            </Link>
          </nav>
          <main className="flex-1 p-6">
            <Routes>
              <Route path="/" element={<OverviewPage />} />
              <Route path="/simulator" element={<AlertSimulatorPage />} />
              <Route path="/incidents" element={<IncidentsPage />} />
              <Route path="/incidents/:id" element={<IncidentDetailPage />} />
              <Route path="/failures" element={<FailedEventsPage />} />
              <Route path="/routing" element={<RoutingRulesPage />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
