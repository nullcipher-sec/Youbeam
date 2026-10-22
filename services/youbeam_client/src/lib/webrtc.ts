import { negotiateStreamOffer, requestPlaybackTicket } from './api'

/**
 * Crea una conexión WebRTC solo de recepción (video + audio) y muestra las pistas en el elemento
 * de video. El cifrado del medio (DTLS-SRTP) lo negocia el navegador con el Streaming Server.
 */
export function createReceiver(videoElement: () => HTMLVideoElement | null): RTCPeerConnection {
  const peerConnection = new RTCPeerConnection()
  peerConnection.addTransceiver('video', { direction: 'recvonly' })
  peerConnection.addTransceiver('audio', { direction: 'recvonly' })
  peerConnection.ontrack = (event) => {
    const element = videoElement()
    if (!element) return
    // Si el servidor no agrupa las pistas en un stream, se arma uno con cada pista recibida
    const [stream] = event.streams
    if (stream) {
      element.srcObject = stream
    } else {
      const current = element.srcObject instanceof MediaStream ? element.srcObject : new MediaStream()
      current.addTrack(event.track)
      element.srcObject = current
    }
  }
  return peerConnection
}

// Crea la oferta SDP y espera a tener todos los candidatos ICE (el servidor no usa trickle ICE)
export async function createCompleteOffer(peerConnection: RTCPeerConnection): Promise<string> {
  await peerConnection.setLocalDescription(await peerConnection.createOffer())
  await waitForIceGathering(peerConnection)
  if (!peerConnection.localDescription) throw new Error('No se pudo crear la oferta WebRTC.')
  return peerConnection.localDescription.sdp
}

/**
 * Flujo completo de reproducción: ticket JWT de un solo uso → oferta → respuesta del Streaming
 * Server. Cada llamada pide un ticket nuevo, porque el servidor rechaza los ya usados.
 */
export async function startPlayback(peerConnection: RTCPeerConnection, videoId: string): Promise<void> {
  const ticket = await requestPlaybackTicket(videoId)
  const offer = await createCompleteOffer(peerConnection)
  const answer = await negotiateStreamOffer(offer, ticket)
  await peerConnection.setRemoteDescription({ type: 'answer', sdp: answer })
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
