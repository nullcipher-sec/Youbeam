import { getApiErrorMessage } from '../../lib/api'
import './api-feedback.css'

type ApiFeedbackProps = {
  error: unknown
  onRetry?: () => void
}

function ApiFeedback({ error, onRetry }: ApiFeedbackProps) {
  return (
    <div className="api-feedback" role="alert">
      <strong>{getApiErrorMessage(error)}</strong>
      <span>La operación quedó preparada para reintentarse cuando el servicio esté disponible.</span>
      {onRetry && <button type="button" onClick={onRetry}>Reintentar</button>}
    </div>
  )
}

export default ApiFeedback
