import { useEffect, useRef, useState } from 'react'
import './streaming-player.css'

type StreamSignaling = (offerSdp: string) => Promise<string>

type StreamingPlayerProps = {
    onSignalOffer?: StreamSignaling
}

function StreamingPlayer({ onSignalOffer }: StreamingPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const peerConnectionRef = useRef<RTCPeerConnection | null>(null)
    const [isConnecting, setIsConnecting] = useState(false)
    const [isConnected, setIsConnected] = useState(false)
    const [error, setError] = useState<string | null>(null)

    async function connect() {
        if (!window.RTCPeerConnection) {
            setError('Este navegador no soporta recepción de streaming WebRTC.')
            return
        }
        if (!onSignalOffer) {
            setError('La señalización del servidor todavía no está configurada.')
            return
        }

        setError(null)
        setIsConnecting(true)
        const peerConnection = new RTCPeerConnection()
        peerConnectionRef.current = peerConnection
        peerConnection.addTransceiver('video', { direction: 'recvonly' })
        peerConnection.addTransceiver('audio', { direction: 'recvonly' })
        peerConnection.ontrack = (event) => {
            const [stream] = event.streams
            if (videoRef.current && stream) {
                videoRef.current.srcObject = stream
            }
        }

        try {
            const offer = await peerConnection.createOffer()
            await peerConnection.setLocalDescription(offer)
            await waitForIceGathering(peerConnection)
            if (!peerConnection.localDescription) {
                throw new Error('No se pudo crear la oferta WebRTC.')
            }
            const answerSdp = await onSignalOffer(peerConnection.localDescription.sdp)
            await peerConnection.setRemoteDescription({ type: 'answer', sdp: answerSdp })
            setIsConnected(true)
        } catch (connectionError) {
            peerConnection.close()
            peerConnectionRef.current = null
            setError(connectionError instanceof Error ? connectionError.message : 'No se pudo iniciar la transmisión.')
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
                    <span className="streaming-eyebrow">Transmisión en vivo</span>
                    <h1>Streaming seguro</h1>
                    <p>Recibe una transmisión WebRTC sobre UDP con DTLS-SRTP.</p>
                </div>
                <span className={`streaming-status ${isConnected ? 'is-live' : ''}`}>
                    <span />
                    {isConnected ? 'En vivo' : 'Listo para conectar'}
                </span>
            </div>

            <div className="streaming-player-shell">
                <video ref={videoRef} autoPlay controls playsInline />
                {!isConnected && (
                    <div className="streaming-placeholder">
                        <div className="streaming-placeholder-icon">▶</div>
                        <strong>{isConnecting ? 'Conectando con el servidor...' : 'Tu reproductor está listo'}</strong>
                        <span>La señal de video aparecerá aquí cuando el backend de señalización esté disponible.</span>
                        <button type="button" onClick={() => void connect()} disabled={isConnecting}>
                            {isConnecting ? 'Conectando...' : 'Iniciar recepción'}
                        </button>
                    </div>
                )}
                {error && <p className="streaming-error" role="alert">{error}</p>}
            </div>

            <div className="streaming-security-grid">
                <div><strong>WebRTC sobre UDP</strong><span>Transporte de baja latencia</span></div>
                <div><strong>DTLS-SRTP</strong><span>Cifrado de medios extremo a extremo</span></div>
                <div><strong>Señalización TLS 1.3</strong><span>Preparado para ticket JWT ES256</span></div>
            </div>
        </section>
    )
}

function waitForIceGathering(peerConnection: RTCPeerConnection): Promise<void> {
    if (peerConnection.iceGatheringState === 'complete') return Promise.resolve()
    return new Promise((resolve, reject) => {
        const timeout = window.setTimeout(() => {
            cleanup()
            reject(new Error('La recopilación ICE tardó demasiado.'))
        }, 10000)
        function cleanup() {
            window.clearTimeout(timeout)
            peerConnection.removeEventListener('icegatheringstatechange', handleStateChange)
        }
        function handleStateChange() {
            if (peerConnection.iceGatheringState === 'complete') {
                cleanup()
                resolve()
            }
        }
        peerConnection.addEventListener('icegatheringstatechange', handleStateChange)
    })
}

export type { StreamSignaling }
export default StreamingPlayer
