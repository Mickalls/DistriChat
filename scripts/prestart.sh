#!/usr/bin/env bash
set -euo pipefail

ports=(9000 28080)

kill_by_port() {
  local port="$1"
  # 仅匹配监听态，避免误杀
  local pids
  pids=$(lsof -ti tcp:"$port" -sTCP:LISTEN || true)

  if [[ -n "${pids}" ]]; then
    echo "端口 ${port} 被占用，PID: ${pids}"
    kill ${pids} || true
    sleep 2
    # 若仍占用则强制杀
    pids=$(lsof -ti tcp:"$port" -sTCP:LISTEN || true)
    if [[ -n "${pids}" ]]; then
      echo "端口 ${port} 仍占用，发送 -9: ${pids}"
      kill -9 ${pids} || true
    fi
  else
    echo "端口 ${port} 空闲"
  fi
}

for p in "${ports[@]}"; do
  kill_by_port "$p"
done