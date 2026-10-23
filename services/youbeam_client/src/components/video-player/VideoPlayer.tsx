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
    const [status, setStatus] = useState(() => window.RTCPeerConnection ? 'Conectando...' : '')
    const [error, setError] = useState<string | null>(() => window.RTCPeerConnection ? null : 'Navegador sin soporte WebRTC.')

    useEffect(() => {
        let cancelled = false
        if (!window.RTCPeerConnection) return

        const peerConnection = createReceiver(() => (cancelled ? null : videoRef.current))
        peerConnection.onconnectionstatechange = () => {
            if (cancelled) return
            if (peerConnection.connectionState === 'connected') setStatus('')
            if (peerConnection.connectionState === 'failed') {
                setError('Error de conexión UDP. Revisa tu red o VPN.')
                setStatus('')
            }
        }

        startPlayback(peerConnection, videoId)
            .then(() => {
                if (!cancelled && peerConnection.connectionState !== 'connected') {
                    setStatus('Estableciendo conexión segura...')
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
        <section className="vp-minimal" aria-label={`Reproduciendo ${title}`}>
            <header className="vp-header">
                <button type="button" className="vp-back" onClick={onClose} aria-label="Volver a videos">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M19 12H5M12 19l-7-7 7-7" />
                    </svg>
                    Volver
                </button>
                <h2 className="vp-title">{title}</h2>
            </header>

            <div className="vp-stage">
                <video ref={videoRef} autoPlay controls playsInline />
                {(status || error) && (
                    <div className={`vp-feedback ${error ? 'is-error' : ''}`}>
                        {error ?? status}
                    </div>
                )}
            </div>

            {description && (
                <div className="vp-details">
                    <p>{description}</p>
                </div>
            )}
        </section>
    )
}

export default VideoPlayer