import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { getApiErrorMessage } from '../../lib/api'
import { createReceiver, startPlayback } from '../../lib/webrtc'
import './streaming-player.css'

type StreamingPlayerProps = {
    initialVideoId?: string
}

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
            setError('Tu navegador no soporta WebRTC.')
            return
        }
        const id = videoId.match(UUID_PATTERN)?.[0]
        if (!id) {
            setError('Ingresa un ID o enlace válido.')
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
                setError('Error de conexión UDP. Verifica tu red o firewall.')
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
        <section className="sp-minimal">
            <header className="sp-header">
                <div className="sp-title-group">
                    <h1 className="sp-title">Streaming</h1>
                    <span className={`sp-status ${isConnected ? 'is-live' : ''}`}>
                        <span className="sp-status-dot" />
                        {isConnected ? 'En vivo' : connectionState === 'new' ? 'Listo' : connectionState}
                    </span>
                </div>

                <form className="sp-form" onSubmit={(event) => void connect(event)}>
                    <input
                        value={videoId}
                        onChange={(event) => setVideoId(event.target.value)}
                        placeholder="Enlace o ID del video"
                        aria-label="Enlace o ID del video"
                        spellCheck={false}
                    />
                    <button type="submit" disabled={isConnecting}>
                        {isConnecting ? 'Conectando...' : 'Reproducir'}
                    </button>
                    {connectionState !== 'new' && connectionState !== 'closed' && (
                        <button type="button" className="sp-btn-stop" onClick={disconnect}>Detener</button>
                    )}
                </form>
            </header>

            <div className="sp-stage">
                <video ref={videoRef} autoPlay controls playsInline />
                {!isConnected && (
                    <div className="sp-placeholder">
                        <p>{isConnecting ? 'Conectando de forma segura...' : 'Ingresa un enlace para comenzar la transmisión.'}</p>
                    </div>
                )}
                {error && <div className="sp-error">{error}</div>}
            </div>

            <div className="sp-footer">
                <span>WebRTC sobre UDP</span>
                <span className="sp-separator">·</span>
                <span>Cifrado DTLS-SRTP</span>
                <span className="sp-separator">·</span>
                <span>Ticket JWT de un solo uso</span>
            </div>
        </section>
    )
}

export default StreamingPlayer