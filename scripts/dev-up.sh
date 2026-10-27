#!/usr/bin/env bash
# Levanta el stack de desarrollo anunciando como candidato ICE la IP de red actual del equipo.
#
# El contenedor del Streaming Server no puede ver la red del host, así que PUBLIC_IP se calcula
# aquí: la IP de la interfaz física (Wi-Fi/Ethernet) de la ruta por defecto, ignorando VPNs
# (utun, wg, tun, tailscale...). Firefox no usa loopback para WebRTC, por eso no sirve 127.0.0.1.
# Al cambiar de red basta con volver a ejecutarlo: solo se recrea el contenedor del stream.
#
# Uso:  scripts/dev-up.sh [argumentos extra para docker compose up]
#       PUBLIC_IP=1.2.3.4 scripts/dev-up.sh   (fuerza una IP concreta)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Nombres de interfaces virtuales que no deben anunciarse (VPNs, túneles, puentes)
is_virtual() {
  [[ "$1" =~ ^(utun|ipsec|ppp|wg|tun|tap|tailscale|zt|docker|br-|veth|vboxnet|bridge) ]]
}

detect_macos() {
  local iface ip
  for iface in $(netstat -rn -f inet | awk '$1 == "default" { print $NF }'); do
    is_virtual "$iface" && continue
    ip="$(ipconfig getifaddr "$iface" 2>/dev/null || true)"
    [[ -n "$ip" ]] && { echo "$ip"; return; }
  done
}

detect_linux() {
  local iface ip
  for iface in $(ip -4 route show default | awk '{ for (i = 1; i < NF; i++) if ($i == "dev") print $(i + 1) }'); do
    is_virtual "$iface" && continue
    ip="$(ip -4 -o addr show dev "$iface" | awk '{ split($4, a, "/"); print a[1]; exit }')"
    [[ -n "$ip" ]] && { echo "$ip"; return; }
  done
}

detect_windows() {
  # Git Bash: adaptador activo con puerta de enlace que no sea VPN
  powershell.exe -NoProfile -Command "(Get-NetIPConfiguration | Where-Object { \$_.IPv4DefaultGateway -and \$_.NetAdapter.Status -eq 'Up' -and \$_.InterfaceDescription -notmatch 'WireGuard|Wintun|TAP|VPN|Tailscale|Hyper-V|VirtualBox' } | Select-Object -First 1).IPv4Address.IPAddress" 2>/dev/null | tr -d '\r'
}

if [[ -z "${PUBLIC_IP:-}" ]]; then
  case "$(uname -s)" in
    Darwin) PUBLIC_IP="$(detect_macos)" ;;
    Linux) PUBLIC_IP="$(detect_linux)" ;;
    MINGW* | MSYS* | CYGWIN*) PUBLIC_IP="$(detect_windows)" ;;
  esac
fi

if [[ -z "${PUBLIC_IP:-}" ]]; then
  # Sin red física (offline): solo funcionará en navegadores que usen loopback (Chrome)
  PUBLIC_IP=127.0.0.1
  echo "WARN: no physical network interface found; falling back to $PUBLIC_IP (Firefox will not connect)" >&2
fi

echo ">> PUBLIC_IP=$PUBLIC_IP"
export PUBLIC_IP
cd "$ROOT/deploy"
docker compose up -d "$@"
