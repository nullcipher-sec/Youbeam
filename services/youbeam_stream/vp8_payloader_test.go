package main

import (
	"bytes"
	"testing"

	"github.com/pion/rtp/codecs"
)

// Todos los paquetes llevan PictureID (también el primer keyframe), S solo en el primero y el
// fotograma se reconstruye íntegro con el depacketizador de Pion.
func TestVP8PayloaderAlwaysSendsPictureID(t *testing.T) {
	payloader := &vp8Payloader{}
	frame := bytes.Repeat([]byte{0xAB, 0xCD, 0xEF}, 1500) // 4500 bytes: varios paquetes

	for want := uint16(0); want < 3; want++ {
		packets := payloader.Payload(1200, frame)
		if len(packets) < 4 {
			t.Fatalf("expected the frame to be split, got %d packets", len(packets))
		}
		var rebuilt []byte
		for i, raw := range packets {
			if len(raw) > 1200 {
				t.Fatalf("packet %d exceeds MTU: %d bytes", i, len(raw))
			}
			var vp8 codecs.VP8Packet
			payload, err := vp8.Unmarshal(raw)
			if err != nil {
				t.Fatalf("unmarshal packet %d: %v", i, err)
			}
			if vp8.I != 1 || vp8.PictureID != want {
				t.Fatalf("frame %d packet %d: PictureID present=%d value=%d", want, i, vp8.I, vp8.PictureID)
			}
			if (vp8.S == 1) != (i == 0) {
				t.Fatalf("frame %d packet %d: unexpected S bit %d", want, i, vp8.S)
			}
			rebuilt = append(rebuilt, payload...)
		}
		if !bytes.Equal(rebuilt, frame) {
			t.Fatalf("frame %d was not rebuilt correctly", want)
		}
	}
}

// Al dar la vuelta (32767 → 0) el PictureID 0 también se envía.
func TestVP8PayloaderPictureIDWraps(t *testing.T) {
	payloader := &vp8Payloader{pictureID: 0x7FFF}
	payloader.Payload(1200, []byte{1, 2, 3})
	var vp8 codecs.VP8Packet
	if _, err := vp8.Unmarshal(payloader.Payload(1200, []byte{1, 2, 3})[0]); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if vp8.I != 1 || vp8.PictureID != 0 {
		t.Fatalf("after wrap: PictureID present=%d value=%d", vp8.I, vp8.PictureID)
	}
}
