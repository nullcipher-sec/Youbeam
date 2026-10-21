import Home from '../pages/Home'
import Authentication from '../pages/Authentication'
import Watch from '../pages/Watch'
import './App.css'

function App() {
  return window.location.pathname.startsWith('/watch/')
    ? <Watch />
    : window.location.pathname === '/auth'
      ? <Authentication />
    : <Home />
}

export default App
