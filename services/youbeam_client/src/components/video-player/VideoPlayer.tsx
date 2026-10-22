import { useEffect, useRef, useState } from 'react'
import { getApiErrorMessage, negotiateStreamOffer, requestPlaybackTicket } from '../../lib/api'
import './video-player.css'

type VideoPlayerProps = {
    videoId: string
    title: string
    onClose: () => void
}

function VideoPlayer({ videoId, title, onClose }: VideoPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const peerConnectionRef = useRef<RTCPeerConnection | null>(null)
    const [status, setStatus] = useState(() => window.RTCPeerConnection ? 'Conectando con el servidor...' : '')
    const [error, setError] = useState<string | null>(() => window.RTCPeerConnection ? null : 'Este navegador no soporta reproducción WebRTC.')

    useEffect(() => {
        let cancelled = false
        if (!window.RTCPeerConnection) {
            return
        }

        const peerConnection = new RTCPeerConnection()
        peerConnectionRef.current = peerConnection

        peerConnection.addTransceiver('video', { direction: 'recvonly' })
        peerConnection.addTransceiver('audio', { direction: 'recvonly' })

        peerConnection.ontrack = (event) => {
            if (!videoRef.current || cancelled) {
                return
            }

            const [stream] = event.streams
            if (stream) {
                videoRef.current.srcObject = stream
            }
        }

        async function connect() {
            try {
                const ticket = await requestPlaybackTicket(videoId)
                if (cancelled) {
                    return
                }

                const offer = await peerConnection.createOffer()
                await peerConnection.setLocalDescription(offer)
                await waitForIceGathering(peerConnection)

                if (!peerConnection.localDescription || cancelled) {
                    return
                }

                const answerSdp = await negotiateStreamOffer(peerConnection.localDescription.sdp, ticket)
                await peerConnection.setRemoteDescription({
                    type: 'answer',
                    sdp: answerSdp,
                })

                if (!cancelled) {
                    setStatus('Reproduciendo')
                }
            } catch (connectionError) {
                if (!cancelled) {
                    setError(getApiErrorMessage(connectionError))
                    setStatus('')
                }
            }
        }

        void connect()

        return () => {
            cancelled = true
            peerConnection.close()
            peerConnectionRef.current = null
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
            </section>
        </div>
    )
}

function waitForIceGathering(peerConnection: RTCPeerConnection): Promise<void> {
    if (peerConnection.iceGatheringState === 'complete') {
        return Promise.resolve()
    }

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

export default VideoPlayer
