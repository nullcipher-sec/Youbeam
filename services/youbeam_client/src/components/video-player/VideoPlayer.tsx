import { useEffect, useRef, useState } from 'react'
import { getApiErrorMessage } from '../../lib/api'
import { createReceiver, startPlayback } from '../../lib/webrtc'
import './video-player.css'

type VideoPlayerProps = {
    videoId: string
    title: string
    description?: string | null
    onClose: () => void
}

function VideoPlayer({ videoId, title, description, onClose }: VideoPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const [status, setStatus] = useState(() => window.RTCPeerConnection ? 'Conectando con el servidor...' : '')
    const [error, setError] = useState<string | null>(() => window.RTCPeerConnection ? null : 'Este navegador no soporta reproducción WebRTC.')

    useEffect(() => {
        let cancelled = false
        if (!window.RTCPeerConnection) {
            return
        }

        const peerConnection = createReceiver(() => (cancelled ? null : videoRef.current))
        peerConnection.onconnectionstatechange = () => {
            if (cancelled) return
            if (peerConnection.connectionState === 'connected') setStatus('')
            if (peerConnection.connectionState === 'failed') {
                // ICE no encontró ruta UDP hasta el Streaming Server: suele ser una VPN o un firewall
                setError('No se pudo abrir la conexión WebRTC (UDP) con el servidor de streaming. Revisa la VPN, el firewall o el permiso de «Red local» del navegador.')
                setStatus('')
            }
        }

        startPlayback(peerConnection, videoId)
            .then(() => {
                if (!cancelled && peerConnection.connectionState !== 'connected') {
                    setStatus('Estableciendo canal cifrado DTLS-SRTP...')
                }
            })
            .catch((connectionError: unknown) => {
                if (!cancelled) {
                    setError(getApiErrorMessage(connectionError))
                    setStatus('')
                }
            })

        return () => {
            cancelled = true
            peerConnection.close()
        }
    }, [videoId])

    return (
        <div className="video-player-backdrop" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
                onClose()
            }
        }}>
            <section className="video-player-dialog" role="dialog" aria-modal="true" aria-label={title}>
                <div className="video-player-header">
                    <h2>{title}</h2>
                    <button type="button" className="video-player-close" onClick={onClose} aria-label="Cerrar reproductor">
                        ×
                    </button>
                </div>
                <div className="video-player-stage">
                    <video ref={videoRef} autoPlay controls playsInline />
                    {(status || error) && (
                        <div className={`video-player-feedback ${error ? 'is-error' : ''}`}>
                            {error ?? status}
                        </div>
                    )}
                </div>
                {description && <p className="video-player-description">{description}</p>}
            </section>
        </div>
    )
}

export default VideoPlayer
