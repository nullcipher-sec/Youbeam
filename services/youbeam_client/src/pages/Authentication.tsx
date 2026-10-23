import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, getApiErrorMessage, login, register } from '../lib/api'
import './Authentication.css'

type AuthMode = 'login' | 'signup'

function Icon({ name }: { name: 'mail' | 'lock' | 'eye' | 'eye-off' | 'user' }) {
  if (name === 'mail') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <path d="m4 7 8 6 8-6" />
        </svg>
    )
  }
  if (name === 'user') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
    )
  }
  if (name === 'lock') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="5" y="11" width="14" height="10" rx="2" />
          <path d="M8 11V7a4 4 0 0 1 8 0v4" />
        </svg>
    )
  }
  if (name === 'eye-off') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" />
          <circle cx="12" cy="12" r="2.5" />
          <path d="m4 4 16 16" />
        </svg>
    )
  }
  return (
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" />
        <circle cx="12" cy="12" r="2.5" />
      </svg>
  )
}

function Authentication() {
  const [mode, setMode] = useState<AuthMode>('signup')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(true)
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [message, setMessage] = useState('')
  const [isError, setIsError] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [identifier, setIdentifier] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const isLogin = mode === 'login'

  function switchMode(nextMode: AuthMode) {
    setMessage('')
    setIsError(false)
    setMode(nextMode)
  }

  function showMessage(text: string, error: boolean) {
    setMessage(text)
    setIsError(error)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!isLogin && !acceptedTerms) {
      showMessage('Debes aceptar los términos y condiciones.', true)
      return
    }

    setSubmitting(true)
    try {
      if (!isLogin) {
        await register({ username: username.trim(), email: identifier.trim(), password })
      }
      await login({ login: isLogin ? identifier.trim() : username.trim(), password })
      showMessage(isLogin ? 'Sesión iniciada.' : 'Cuenta creada.', false)
      window.location.assign('/')
    } catch (authError) {
      if (authError instanceof ApiError && authError.status === 400 && !isLogin) {
        showMessage('Verifica tus datos: usuario válido y contraseña de 12 caracteres.', true)
      } else if (authError instanceof ApiError && authError.status === 401) {
        showMessage('Usuario o contraseña incorrectos.', true)
      } else {
        showMessage(getApiErrorMessage(authError), true)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
      <main className="auth-minimal-page">
        <button className="auth-minimal-back" type="button" onClick={() => window.location.assign('/')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7" /></svg>
          Volver
        </button>

        <section className="auth-minimal-card">
          <div className="auth-minimal-header">
            <div className="auth-minimal-logo">YOUBEAM</div>
            <h1>{isLogin ? 'Bienvenido de nuevo' : 'Crea tu cuenta'}</h1>
            <p>{isLogin ? 'Ingresa tus credenciales para continuar' : 'Únete y empieza a compartir tu contenido'}</p>
          </div>

          <div className="auth-minimal-toggle">
            <button type="button" className={isLogin ? 'is-active' : ''} onClick={() => switchMode('login')}>
              Iniciar sesión
            </button>
            <button type="button" className={!isLogin ? 'is-active' : ''} onClick={() => switchMode('signup')}>
              Registrarse
            </button>
          </div>

          <form className="auth-minimal-form" onSubmit={(event) => void handleSubmit(event)}>
            {!isLogin && (
                <label className="auth-minimal-field">
                  <div className="field-icon"><Icon name="user" /></div>
                  <input
                      type="text"
                      placeholder="Nombre de usuario"
                      autoComplete="username"
                      required
                      minLength={3}
                      maxLength={50}
                      pattern="[a-zA-Z0-9._\-]+"
                      value={username}
                      onChange={(event) => setUsername(event.target.value)}
                  />
                </label>
            )}

            <label className="auth-minimal-field">
              <div className="field-icon"><Icon name="mail" /></div>
              <input
                  type={isLogin ? 'text' : 'email'}
                  placeholder={isLogin ? 'Correo o usuario' : 'Correo electrónico'}
                  autoComplete={isLogin ? 'username' : 'email'}
                  required
                  maxLength={255}
                  value={identifier}
                  onChange={(event) => setIdentifier(event.target.value)}
              />
            </label>

            <label className="auth-minimal-field">
              <div className="field-icon"><Icon name="lock" /></div>
              <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Contraseña"
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  required
                  minLength={isLogin ? undefined : 12}
                  maxLength={128}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
              />
              <button
                  className="field-toggle-btn"
                  type="button"
                  onClick={() => setShowPassword((visible) => !visible)}
              >
                <Icon name={showPassword ? 'eye-off' : 'eye'} />
              </button>
            </label>

            {isLogin ? (
                <label className="auth-minimal-checkbox">
                  <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
                  <div className="checkbox-box"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"><polyline points="20 6 9 17 4 12"/></svg></div>
                  <span>Mantener sesión iniciada</span>
                </label>
            ) : (
                <label className="auth-minimal-checkbox">
                  <input type="checkbox" checked={acceptedTerms} onChange={(event) => setAcceptedTerms(event.target.checked)} />
                  <div className="checkbox-box"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"><polyline points="20 6 9 17 4 12"/></svg></div>
                  <span>Acepto los términos y condiciones</span>
                </label>
            )}

            {message && (
                <div className={`auth-minimal-message ${isError ? 'is-error' : 'is-success'}`}>
                  {message}
                </div>
            )}

            <button className="auth-minimal-submit" type="submit" disabled={submitting}>
              {submitting ? 'Procesando...' : isLogin ? 'Ingresar' : 'Crear cuenta'}
            </button>
          </form>
        </section>
      </main>
  )
}

export default Authentication