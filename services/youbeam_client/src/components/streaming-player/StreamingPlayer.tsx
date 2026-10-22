import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { getApiErrorMessage } from '../../lib/api'
import { createReceiver, startPlayback } from '../../lib/webrtc'
import './streaming-player.css'

type StreamingPlayerProps = {
    initialVideoId?: string
}

// Se acepta el enlace compartido (/watch/{id}) o el id; solo se envía al Core un UUID válido
const UUID_PATTERN = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i

function StreamingPlayer({ initialVideoId = '' }: StreamingPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const peerConnectionRef = useRef<RTCPeerConnection | null>(null)
    const [videoId, setVideoId] = useState(initialVideoId)
    const [isConnecting, setIsConnecting] = useState(false)
    const [connectionState, setConnectionState] = useState<RTCPeerConnectionState>('new')
    const [error, setError] = useState<string | null>(null)

    const isConnected = connectionState === 'connected'

    function disconnect() {
        peerConnectionRef.current?.close()
        peerConnectionRef.current = null
        if (videoRef.current) videoRef.current.srcObject = null
        setConnectionState('closed')
    }

    async function connect(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (!window.RTCPeerConnection) {
            setError('Este navegador no soporta recepción de streaming WebRTC.')
            return
        }
        const id = videoId.match(UUID_PATTERN)?.[0]
        if (!id) {
            setError('Pega el enlace o el id de un video.')
            return
        }

        disconnect()
        setError(null)
        setIsConnecting(true)
        const peerConnection = createReceiver(() => videoRef.current)
        peerConnectionRef.current = peerConnection
        peerConnection.onconnectionstatechange = () => {
            setConnectionState(peerConnection.connectionState)
            if (peerConnection.connectionState === 'failed') {
                setError('No se pudo abrir la conexión WebRTC (UDP) con el servidor de streaming. Revisa la VPN, el firewall o el permiso de «Red local» del navegador.')
            }
        }

        try {
            await startPlayback(peerConnection, id)
        } catch (connectionError) {
            disconnect()
            setError(getApiErrorMessage(connectionError))
        } finally {
            setIsConnecting(false)
        }
    }

    useEffect(() => () => {
        peerConnectionRef.current?.close()
    }, [])

    return (
        <section className="streaming-panel">
            <div className="streaming-panel-heading">
                <div>
                    <span className="streaming-eyebrow">Transmisión cifrada</span>
                    <h1>Streaming seguro</h1>
                    <p>Recibe un video por WebRTC sobre UDP con DTLS-SRTP, usando un ticket JWT de un solo uso.</p>
                </div>
                <span className={`streaming-status ${isConnected ? 'is-live' : ''}`}>
                    <span />
                    {isConnected ? 'En vivo' : `Estado: ${connectionState}`}
                </span>
            </div>

            <form className="streaming-form" onSubmit={(event) => void connect(event)}>
                <input
                    value={videoId}
                    onChange={(event) => setVideoId(event.target.value)}
                    placeholder="Enlace o id del video"
                    aria-label="Enlace o id del video"
                    spellCheck={false}
                />
                <button type="submit" disabled={isConnecting}>
                    {isConnecting ? 'Conectando...' : 'Reproducir'}
                </button>
                {connectionState !== 'new' && connectionState !== 'closed' && (
                    <button type="button" className="streaming-stop" onClick={disconnect}>Detener</button>
                )}
            </form>

            <div className="streaming-player-shell">
                <video ref={videoRef} autoPlay controls playsInline />
                {!isConnected && (
                    <div className="streaming-placeholder">
                        <div className="streaming-placeholder-icon">▶</div>
                        <strong>{isConnecting ? 'Conectando con el servidor...' : 'Tu reproductor está listo'}</strong>
                        <span>Pega el enlace de un video (en «Mis videos» → Copiar enlace) y pulsa Reproducir.</span>
                    </div>
                )}
                {error && <p className="streaming-error" role="alert">{error}</p>}
            </div>

            <div className="streaming-security-grid">
                <div><strong>WebRTC sobre UDP</strong><span>Transporte de baja latencia</span></div>
                <div><strong>DTLS-SRTP</strong><span>Medio cifrado con AES-GCM</span></div>
                <div><strong>Ticket JWT ES256</strong><span>Un solo uso, caduca en 60 s</span></div>
            </div>
        </section>
    )
}

export default StreamingPlayer
