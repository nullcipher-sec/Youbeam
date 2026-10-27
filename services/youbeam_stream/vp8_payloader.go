package main

import (
	"fmt"

	"github.com/pion/rtp"
	"github.com/pion/webrtc/v4"
)

// vp8DescriptorSize: byte obligatorio (X|S), byte de extensión (I) y PictureID de 15 bits (2 bytes).
const vp8DescriptorSize = 4

// vp8Payloader empaqueta fotogramas VP8 en RTP (RFC 7741) incluyendo SIEMPRE el PictureID.
//
// El VP8Payloader de Pion omite el PictureID cuando vale 0, es decir, en el primer fotograma
// (el keyframe) y cada vez que el contador da la vuelta. El navegador cambia entonces de método
// para resolver las referencias entre fotogramas, pierde el keyframe ya recibido y descarta todo
// hasta el siguiente: el video se congela ~4 s (intervalo de keyframes de libvpx) y los videos
// más cortos que eso no llegan a reproducirse.
type vp8Payloader struct {
	pictureID uint16
}

// Payload divide un fotograma en paquetes que caben en la MTU; S marca el inicio del fotograma.
func (p *vp8Payloader) Payload(mtu uint16, frame []byte) [][]byte {
	maxFragment := int(mtu) - vp8DescriptorSize
	if maxFragment <= 0 || len(frame) == 0 {
		return nil
	}

	var packets [][]byte
	for offset := 0; offset < len(frame); offset += maxFragment {
		end := min(offset+maxFragment, len(frame))
		packet := make([]byte, vp8DescriptorSize+end-offset)
		packet[0] = 0x80 // X: hay byte de extensión
		if offset == 0 {
			packet[0] |= 0x10 // S: primer paquete del fotograma
		}
		packet[1] = 0x80                        // I: hay PictureID
		packet[2] = 0x80 | byte(p.pictureID>>8) // M: PictureID de 15 bits
		packet[3] = byte(p.pictureID)
		copy(packet[vp8DescriptorSize:], frame[offset:end])
		packets = append(packets, packet)
	}
	p.pictureID = (p.pictureID + 1) & 0x7FFF
	return packets
}

// newVideoPayloader se pasa a webrtc.WithPayloader; la pista de video solo admite VP8.
func newVideoPayloader(codec webrtc.RTPCodecCapability) (rtp.Payloader, error) {
	if codec.MimeType != webrtc.MimeTypeVP8 {
		return nil, fmt.Errorf("unsupported video codec %s", codec.MimeType)
	}
	return &vp8Payloader{}, nil
}
