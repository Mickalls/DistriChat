#!/usr/bin/env bash
set -euo pipefail

# 用法:
#   ./prestart.sh           # 默认清理 9000 和 28080
#   ./prestart.sh 8080 9000 # 清理传入端口
#   USE_SUDO=1 ./prestart.sh  # 无权限时用 sudo

ports=("$@")
if [[ ${#ports[@]} -eq 0 ]]; then
  ports=(9000 28080 9001 28081 9092 28082)
fi

SUDO=""
if [[ "${USE_SUDO:-0}" = "1" ]]; then
  SUDO="sudo"
fi

kill_by_port() {
  local port="$1"
  local pids
  pids=$(lsof -ti tcp:"$port" -sTCP:LISTEN || true)
  if [[ -z "${pids}" ]]; then
    echo "端口 ${port} 空闲"
    return 0
  fi

  echo "端口 ${port} 被占用，PID: ${pids}，尝试关闭..."
  $SUDO kill ${pids} || true
  sleep 2

  pids=$(lsof -ti tcp:"$port" -sTCP:LISTEN || true)
  if [[ -n "${pids}" ]]; then
    echo "端口 ${port} 仍占用，发送 -9: ${pids}"
    $SUDO kill -9 ${pids} || true
  fi

  if lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "警告：端口 ${port} 仍被占用，可能需要手动处理。"
  else
    echo "端口 ${port} 已清理完成。"
  fi
}

for p in "${ports[@]}"; do
  kill_by_port "$p"
done

echo "端口清理完成。"