import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, getApiErrorMessage, login, register } from '../lib/api'
import './Authentication.css'

type AuthMode = 'login' | 'signup'

function Icon({ name }: { name: 'mail' | 'lock' | 'eye' | 'eye-off' }) {
  if (name === 'mail') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <path d="m4 7 8 6 8-6" />
        </svg>
    )
  }

  if (name === 'lock') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <rect x="5" y="10" width="14" height="10" rx="2" />
          <path d="M8 10V7a4 4 0 0 1 8 0v3" />
        </svg>
    )
  }

  if (name === 'eye-off') {
    return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" />
          <circle cx="12" cy="12" r="2.5" />
          <path d="m4 4 16 16" />
        </svg>
    )
  }

  return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
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
      showMessage('Debes aceptar los términos y condiciones de la comunidad para continuar.', true)
      return
    }

    setSubmitting(true)
    try {
      // Al registrarse se inicia sesión directamente: el Core devuelve la cookie HttpOnly en el login
      if (!isLogin) {
        await register({ username: username.trim(), email: identifier.trim(), password })
      }
      await login({ login: isLogin ? identifier.trim() : username.trim(), password })
      showMessage(isLogin ? 'Sesión iniciada. Entrando a YouBeam...' : 'Cuenta creada. Bienvenido a YouBeam.', false)
      window.location.assign('/')
    } catch (authError) {
      // El Core responde igual si el usuario no existe o la contraseña falla, para no revelar cuentas
      if (authError instanceof ApiError && authError.status === 400 && !isLogin) {
        showMessage('Revisa los datos: usuario de 3 a 50 caracteres (letras, números, . _ -), correo válido y contraseña de al menos 12 caracteres.', true)
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
      <main className="authentication-page">
        <button className="auth-back-button" type="button" onClick={() => window.location.assign('/')}>
          <span aria-hidden="true">←</span>
          Volver al inicio
        </button>
        <section className={`auth-card ${isLogin ? 'is-login' : 'is-signup'}`}>
          <div className="auth-panel">
            <div className="auth-panel-content" key={`panel-${mode}`}>
              <h1>{isLogin ? '¡Hola de nuevo!' : 'Transmitir es conectar'}</h1>
              <p>
                {isLogin
                    ? 'Accede a tu cuenta para gestionar tus canales, revisar tus métricas de reproducción y continuar disfrutando de tu contenido preferido.'
                    : 'Únete a YouBeam para publicar tus vídeos, interactuar con tus creadores favoritos y personalizar tus listas de reproducción.'}
              </p>
              <button
                  className="panel-button"
                  type="button"
                  onClick={() => switchMode(isLogin ? 'signup' : 'login')}
              >
                {isLogin ? 'Crear una cuenta' : 'Iniciar sesión'}
              </button>
            </div>
          </div>

          <form className="auth-form" key={`form-${mode}`} onSubmit={(event) => void handleSubmit(event)}>
              <div className="auth-heading">
                <span className="auth-kicker">YOU<span>BEAM</span></span>
                <h2>{isLogin ? 'Iniciar sesión' : 'Crear tu cuenta'}</h2>
                <p>{isLogin ? 'Vuelve a tu espacio creativo.' : 'Empieza a compartir lo que te inspira.'}</p>
              </div>

            {!isLogin && (
                <label className="input-field">
                  <Icon name="mail" />
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

            <label className="input-field">
              <Icon name="mail" />
              <input
                  type={isLogin ? 'text' : 'email'}
                  placeholder={isLogin ? 'Correo electrónico o usuario' : 'Correo electrónico'}
                  autoComplete={isLogin ? 'username' : 'email'}
                  required
                  maxLength={255}
                  value={identifier}
                  onChange={(event) => setIdentifier(event.target.value)}
              />
            </label>

            <label className="input-field">
              <Icon name="lock" />
              <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder={isLogin ? 'Contraseña de acceso' : 'Contraseña (mínimo 12 caracteres)'}
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  required
                  minLength={isLogin ? undefined : 12}
                  maxLength={128}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
              />
              <button
                  className="icon-button"
                  type="button"
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  onClick={() => setShowPassword((visible) => !visible)}
              >
                <Icon name={showPassword ? 'eye-off' : 'eye'} />
              </button>
            </label>

            {isLogin ? (
                <label className="check-row">
                  <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(event) => setRememberMe(event.target.checked)}
                  />
                  <span>Mantener la sesión iniciada</span>
                </label>
            ) : (
                <label className="check-row">
                  <input
                      type="checkbox"
                      checked={acceptedTerms}
                      onChange={(event) => setAcceptedTerms(event.target.checked)}
                  />
                  <span>Acepto los Términos del servicio y Normas de la comunidad</span>
                </label>
            )}

            <button className="submit-button" type="submit" disabled={submitting}>
              {submitting ? 'Procesando...' : isLogin ? 'Ingresar a la plataforma' : 'Unirme a YouBeam'}
            </button>

            <div className="or-divider">
              <span>O continuar con</span>
            </div>

            <button
                className="google-button"
                type="button"
                onClick={() => showMessage('El inicio de sesión con Google no está disponible.', true)}
            >
              <span className="google-mark">G</span>
              Continuar con Google
            </button>

            <p className="legal-copy">
              Al {isLogin ? 'acceder a' : 'registrarte en'} <strong>YouBeam</strong>, confirmas tu conformidad con nuestras{' '}
              <a href="#terms">Condiciones del servicio</a> y la{' '}
              <a href="#privacy">Política de privacidad</a>.
            </p>

            <p className={`form-message ${message ? 'visible' : ''} ${isError ? 'is-error' : ''}`} role="status">
              {message}
            </p>
          </form>
        </section>
      </main>
  )
}

export default Authentication