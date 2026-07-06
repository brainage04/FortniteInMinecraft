#!/usr/bin/env bash
set -euo pipefail

for tool in ffmpeg ffprobe; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required tool: $tool" >&2
    exit 1
  fi
done

is_truthy() {
  case "${1,,}" in
    1 | true | yes | on)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

sanitize_path_component() {
  local value="${1:-client-gametest}"
  local sanitized="${value//[^[:alnum:]._-]/-}"

  while [[ "$sanitized" == *--* ]]; do
    sanitized="${sanitized//--/-}"
  done

  sanitized="${sanitized#-}"
  sanitized="${sanitized%-}"
  if [[ -z "$sanitized" ]]; then
    sanitized="client-gametest"
  fi

  printf '%s\n' "$sanitized"
}

json_escape() {
  local value="${1-}"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "$value"
}

json_string_or_null() {
  if [[ -z "${1-}" ]]; then
    printf 'null'
  else
    printf '"%s"' "$(json_escape "$1")"
  fi
}

write_metadata() {
  local metadata_path="$1"
  local gradle_status="$2"
  local video_saved="$3"
  local kept_run_directory="$4"
  local finished_at="$5"
  local args_json=""
  local arg

  for arg in "${gradle_args[@]}"; do
    if [[ -n "$args_json" ]]; then
      args_json+=", "
    fi
    args_json+="\"$(json_escape "$arg")\""
  done

  cat > "$metadata_path" <<EOF
{
  "name": "$(json_escape "$recording_name")",
  "profile": "$(json_escape "$recording_profile")",
  "trace": $(is_truthy "$recording_trace" && printf 'true' || printf 'false'),
  "startedAt": "$(json_escape "$recording_started_at")",
  "finishedAt": "$(json_escape "$finished_at")",
  "gradleStatus": ${gradle_status},
  "captureMode": "$(json_escape "$capture_mode")",
  "fps": "$(json_escape "$fps")",
  "xvfbScreen": "$(json_escape "$xvfb_screen")",
  "video": "$(json_escape "$output")",
  "videoSaved": ${video_saved},
  "audioRequested": $(is_truthy "$recording_audio" && printf 'true' || printf 'false'),
  "audioRoute": "$(json_escape "$audio_route")",
  "audioSource": $(json_string_or_null "$audio_source"),
  "audioSink": $(json_string_or_null "$audio_sink_name"),
  "audioSaved": ${audio_saved},
  "recordingDirectory": "$(json_escape "$recording_dir")",
  "runDirectory": $(json_string_or_null "$resolved_run_dir"),
  "keptRunDirectory": $(json_string_or_null "$kept_run_directory"),
  "selectors": {
    "FIM_TEST_PROFILE": $(json_string_or_null "${FIM_TEST_PROFILE:-}"),
    "FIM_TEST_ONLY": $(json_string_or_null "${FIM_TEST_ONLY:-}"),
    "FIM_TEST_SUITE": $(json_string_or_null "${FIM_TEST_SUITE:-}")
  },
  "gradleArgs": [${args_json}]
}
EOF
}


recording_dir="${FIM_RECORDING_DIR:-build/recordings}"
recording_name="${FIM_RECORDING_NAME:-client-gametest}"
recording_profile="${FIM_RECORDING_PROFILE:-${FIM_TEST_PROFILE:-showcase}}"
recording_trace="${FIM_RECORDING_TRACE:-false}"
keep_run_dir="${FIM_RECORDING_KEEP_RUN_DIR:-false}"
run_dir="${FIM_RECORDING_RUN_DIR:-build/run/clientGameTest}"
fps="${FIM_RECORDING_FPS:-30}"
recording_start_wait_seconds="${FIM_RECORDING_START_WAIT_SECONDS:-90}"
capture_mode="${FIM_RECORDING_CAPTURE_MODE:-xvfb}"
recording_audio="${FIM_RECORDING_AUDIO:-true}"
recording_audio_route="${FIM_RECORDING_AUDIO_ROUTE:-virtual}"
recording_audio_source="${FIM_RECORDING_AUDIO_SOURCE:-}"
recording_audio_set_default="${FIM_RECORDING_AUDIO_SET_DEFAULT:-false}"
recording_heartbeat_seconds="${FIM_RECORDING_HEARTBEAT_SECONDS:-60}"
if [[ "$capture_mode" != "xvfb" ]]; then
  echo "FIM_RECORDING_CAPTURE_MODE only supports xvfb; window and geometry capture were removed." >&2
  exit 1
fi
xvfb_screen="${FIM_RECORDING_XVFB_SCREEN:-1920x1080x24}"
xvfb_width="${xvfb_screen%%x*}"
xvfb_height_and_depth="${xvfb_screen#*x}"
xvfb_height="${xvfb_height_and_depth%%x*}"
timestamp="$(date +%Y%m%d-%H%M%S)"
recording_started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
recording_audio_sink_name="${FIM_RECORDING_AUDIO_SINK_NAME:-fim_recording_${timestamp}}"
safe_recording_name="$(sanitize_path_component "$recording_name")"
gradle_args=("$@")

if [[ -n "$recording_profile" && -z "${FIM_TEST_PROFILE:-}" ]]; then
  export FIM_TEST_PROFILE="$recording_profile"
fi
export FIM_RECORDING_NAME="$recording_name"
export FIM_RECORDING_PROFILE="$recording_profile"
export FIM_RECORDING_TRACE="$recording_trace"
export FIM_CLIENT_GAMETEST_RECORDING_NAME="$recording_name"
export FIM_CLIENT_GAMETEST_RECORDING_PROFILE="$recording_profile"
export FIM_CLIENT_GAMETEST_RECORDING_TRACE="$recording_trace"

gradle_pid=""
ffmpeg_pid=""
heartbeat_pid=""
xvfb_pid=""
xvfb_display=""
interrupted=false
resolved_run_dir=""
kept_run_dir=""
audio_source=""
audio_capture_enabled=false
audio_saved=false
audio_input_args=()
audio_output_args=()
audio_route="none"
audio_sink_name=""
audio_sink_id=""
alsoft_config=""
previous_default_sink_id=""
recording_audio_device_name=""

mkdir -p "$recording_dir"
recording_dir="$(cd "$recording_dir" && pwd -P)"
if [[ -d "$run_dir" ]]; then
  resolved_run_dir="$(cd "$run_dir" && pwd -P)"
fi
output="${recording_dir}/${safe_recording_name}-${timestamp}.mp4"
metadata="${recording_dir}/${safe_recording_name}-${timestamp}.json"
recording_start_signal="${recording_dir}/.${safe_recording_name}-${timestamp}.start"
recording_ready_signal="${recording_dir}/.${safe_recording_name}-${timestamp}.ready"
rm -f "$recording_start_signal"
rm -f "$recording_ready_signal"
export FIM_CLIENT_GAMETEST_RECORDING_START_SIGNAL="$recording_start_signal"
export FIM_CLIENT_GAMETEST_RECORDING_READY_SIGNAL="$recording_ready_signal"

for tool in Xvfb xdpyinfo; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required tool for xvfb recording: $tool" >&2
    exit 1
  fi
done

find_free_xvfb_display() {
  local number

  for number in $(seq 90 130); do
    if [[ ! -e "/tmp/.X${number}-lock" && ! -e "/tmp/.X11-unix/X${number}" ]]; then
      printf ':%s\n' "$number"
      return 0
    fi
  done

  return 1
}

filter_xvfb_stderr() {
  if [[ "${FIM_RECORDING_SUPPRESS_XKB_WARNINGS:-true}" != "true" ]]; then
    cat
    return
  fi

  awk '
    /The XKEYBOARD keymap compiler \(xkbcomp\) reports:/ {
      skip_xkb = 1
      next
    }
    skip_xkb && /^> Warning:/ {
      next
    }
    skip_xkb && /^Errors from xkbcomp are not fatal to the X server$/ {
      skip_xkb = 0
      next
    }
    {
      print
    }
  '
}

detect_pulse_monitor_source() {
  ffmpeg -hide_banner -sources pulse 2>/dev/null | awk '
    /monitor \[/ {
      if ($1 == "*") {
        print $2
      } else {
        print $1
      }
      exit
    }
  '
}

find_pipewire_node_id_by_name() {
  local node_name="$1"

  pw-cli list-objects Node 2>/dev/null | awk -v name="$node_name" '
    /^[[:space:]]*id [0-9]+,/ {
      id = $2
      sub(/,/, "", id)
      next
    }

    $1 == "node.name" && $3 == "\"" name "\"" {
      print id
      exit
    }
  '
}

detect_default_monitor_source() {
  if command -v wpctl >/dev/null 2>&1; then
    local node_name
    node_name="$(wpctl inspect @DEFAULT_AUDIO_SINK@ 2>/dev/null | awk -F' = ' '
      $1 ~ /node.name$/ {
        gsub(/"/, "", $2)
        print $2
        exit
      }
    ')"
    if [[ -n "$node_name" ]]; then
      printf '%s.monitor\n' "$node_name"
      return
    fi
  fi

  detect_pulse_monitor_source
}

create_virtual_audio_sink() {
  if ! command -v pw-cli >/dev/null 2>&1; then
    return 1
  fi

  if [[ -z "$previous_default_sink_id" ]]; then
    previous_default_sink_id="$(current_default_audio_sink_id || true)"
  fi

  audio_sink_name="$recording_audio_sink_name"
  pw-cli create-node adapter "{ factory.name = support.null-audio-sink node.name = \"$audio_sink_name\" node.description = \"FIM Recording Null Sink\" media.class = Audio/Sink object.linger = true priority.driver = 1 priority.session = 1 node.autoconnect = false audio.position = [ FL FR ] }" >/dev/null

  for _ in {1..20}; do
    audio_sink_id="$(find_pipewire_node_id_by_name "$audio_sink_name")"
    if [[ -n "$audio_sink_id" ]]; then
      return 0
    fi

    sleep 0.1
  done

  audio_sink_name=""
  return 1
}

current_default_audio_sink_id() {
  if ! command -v wpctl >/dev/null 2>&1; then
    return 1
  fi

  wpctl inspect @DEFAULT_AUDIO_SINK@ 2>/dev/null | awk '
    /^id [0-9]+,/ {
      sub(/,/, "", $2)
      print $2
      exit
    }
  '
}

route_default_audio_to_virtual_sink() {
  if ! command -v wpctl >/dev/null 2>&1; then
    return
  fi

  if [[ -z "$previous_default_sink_id" ]]; then
    previous_default_sink_id="$(current_default_audio_sink_id || true)"
  fi
  wpctl set-default "$audio_sink_id" >/dev/null 2>&1 || true
}

restore_default_audio_sink() {
  if [[ -n "$previous_default_sink_id" ]] && command -v wpctl >/dev/null 2>&1; then
    wpctl set-default "$previous_default_sink_id" >/dev/null 2>&1 || true
    previous_default_sink_id=""
  fi
}

destroy_virtual_audio_sink() {
  if [[ -n "$audio_sink_id" ]] && command -v pw-cli >/dev/null 2>&1; then
    pw-cli destroy "$audio_sink_id" >/dev/null 2>&1 || true
    audio_sink_id=""
  fi
}

configure_audio_capture() {
  if ! is_truthy "$recording_audio"; then
    return
  fi

  if [[ -z "${ALSOFT_DRIVERS:-}" || "${ALSOFT_DRIVERS,,}" == "null" ]]; then
    export ALSOFT_DRIVERS="${FIM_RECORDING_ALSOFT_DRIVERS:-pulse,pipewire,alsa}"
  fi

  if [[ -n "$recording_audio_source" ]]; then
    audio_route="manual"
    audio_source="$recording_audio_source"
  elif [[ "${recording_audio_route,,}" == "virtual" ]] && create_virtual_audio_sink; then
    audio_route="virtual"
    audio_source="${audio_sink_name}.monitor"
    alsoft_config="${recording_dir}/.${safe_recording_name}-${timestamp}-alsoft.conf"
    cat > "$alsoft_config" <<EOF
[general]
drivers = pulse

[pulse]
device = ${audio_sink_name}
allow-moves = false
EOF
    export ALSOFT_CONF="$alsoft_config"
    export ALSOFT_DRIVERS="pulse"
    export PULSE_SINK="$audio_sink_name"
    recording_audio_device_name="FIM Recording Null Sink"
    export FIM_RECORDING_AUDIO_DEVICE="$recording_audio_device_name"
    if is_truthy "$recording_audio_set_default"; then
      route_default_audio_to_virtual_sink
    fi
    echo "Routing GameTest audio to virtual PipeWire sink: ${audio_sink_name}"
  else
    audio_route="system"
    audio_source="$(detect_default_monitor_source || true)"
  fi

  if [[ -z "$audio_source" ]]; then
    audio_route="none"
    echo "No PulseAudio monitor source detected; recording video without audio." >&2
    return
  fi

  audio_capture_enabled=true
  audio_input_args=(-f pulse -thread_queue_size 1024 -i "$audio_source")
  audio_output_args=(-map 0:v:0 -map 1:a:0 -c:a aac -b:a 160k -shortest)
  echo "Recording audio from PulseAudio monitor source: ${audio_source}"
}

start_xvfb() {
  xvfb_display="${FIM_RECORDING_XVFB_DISPLAY:-$(find_free_xvfb_display)}"

  echo "Starting virtual X display ${xvfb_display} (${xvfb_screen})..."
  Xvfb "$xvfb_display" -screen 0 "$xvfb_screen" -nolisten tcp 2> >(filter_xvfb_stderr >&2) &
  xvfb_pid=$!

  for _ in $(seq 1 50); do
    if DISPLAY="$xvfb_display" xdpyinfo >/dev/null 2>&1; then
      return 0
    fi

    if ! kill -0 "$xvfb_pid" >/dev/null 2>&1; then
      echo "Xvfb exited before becoming ready." >&2
      return 1
    fi

    sleep 0.1
  done

  echo "Timed out waiting for Xvfb display ${xvfb_display}." >&2
  return 1
}


wait_for_recording_start_signal() {
  local deadline=$((SECONDS + recording_start_wait_seconds))

  while (( SECONDS < deadline )); do
    if [[ -e "$recording_start_signal" ]]; then
      return 0
    fi

    if [[ -n "$gradle_pid" ]] && ! kill -0 "$gradle_pid" >/dev/null 2>&1; then
      return 1
    fi

    sleep 0.1
  done

  return 1
}

monitor_gradle_output() {
  local line

  while IFS= read -r line; do
    printf '%s\n' "$line"
  done
}

start_recording_heartbeat() {
  if [[ ! "$recording_heartbeat_seconds" =~ ^[0-9]+$ ]] || (( recording_heartbeat_seconds == 0 )); then
    return
  fi

  (
    while true; do
      sleep "$recording_heartbeat_seconds" || exit 0
      echo "Client GameTest still running; waiting for Gradle to finish..."
    done
  ) &
  heartbeat_pid=$!
}

stop_recording_heartbeat() {
  if [[ -n "$heartbeat_pid" ]] && kill -0 "$heartbeat_pid" >/dev/null 2>&1; then
    kill -TERM "$heartbeat_pid" >/dev/null 2>&1 || true
    wait "$heartbeat_pid" >/dev/null 2>&1 || true
  fi
  heartbeat_pid=""
}



stop_recorder() {
  stop_recording_heartbeat
  if [[ -n "$ffmpeg_pid" ]] && kill -0 "$ffmpeg_pid" >/dev/null 2>&1; then
    kill -INT "$ffmpeg_pid" >/dev/null 2>&1 || true
    wait "$ffmpeg_pid" >/dev/null 2>&1 || true
  fi

  restore_default_audio_sink
  destroy_virtual_audio_sink
  if [[ -n "$alsoft_config" ]]; then
    rm -f "$alsoft_config"
    alsoft_config=""
  fi


  if [[ -n "$xvfb_pid" ]] && kill -0 "$xvfb_pid" >/dev/null 2>&1; then
    kill -TERM "$xvfb_pid" >/dev/null 2>&1 || true
    wait "$xvfb_pid" >/dev/null 2>&1 || true
  fi

  rm -f "$recording_start_signal"
  rm -f "$recording_ready_signal"
}

handle_interrupt() {
  interrupted=true
  stop_recorder

  if [[ -n "$gradle_pid" ]] && kill -0 "$gradle_pid" >/dev/null 2>&1; then
    kill -TERM "$gradle_pid" >/dev/null 2>&1 || true
    wait "$gradle_pid" >/dev/null 2>&1 || true
  fi
}

trap handle_interrupt INT TERM
trap stop_recorder EXIT

start_xvfb
export DISPLAY="$xvfb_display"
export FIM_RECORDING_MANAGED_XVFB=true
configure_audio_capture

echo "Starting client GameTest..."
gradle_command=(./gradlew --no-daemon)
if [[ -n "$recording_audio_device_name" ]]; then
  gradle_command+=("-PfimRecordingAudioDevice=$recording_audio_device_name")
fi
gradle_command+=(runClientGameTest "$@")
"${gradle_command[@]}" > >(monitor_gradle_output) 2>&1 &
gradle_pid=$!

echo "Waiting for client GameTest recording-start signal..."
if ! wait_for_recording_start_signal; then
  echo "Client GameTest recording-start signal was not seen before the GameTest finished or timed out." >&2
else
  echo "Recording virtual display ${DISPLAY} to ${output}"
  ffmpeg \
    -hide_banner \
    -loglevel warning \
    -y \
    -f x11grab \
    -framerate "$fps" \
    -video_size "${xvfb_width}x${xvfb_height}" \
    -draw_mouse 0 \
    -i "${DISPLAY}.0" \
    "${audio_input_args[@]}" \
    -c:v libx264 \
    -preset veryfast \
    -crf 23 \
    -pix_fmt yuv420p \
    "${audio_output_args[@]}" \
    "$output" &
  ffmpeg_pid=$!
  : > "$recording_ready_signal"
fi

set +e
start_recording_heartbeat
wait "$gradle_pid"
gradle_status=$?
stop_recording_heartbeat
set -e

stop_recorder
if [[ -z "$resolved_run_dir" && -d "$run_dir" ]]; then
  resolved_run_dir="$(cd "$run_dir" && pwd -P)"
fi


video_saved=false
if [[ -f "$output" ]] && ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$output" >/dev/null 2>&1; then
  video_saved=true
  echo "Recording saved: ${output}"
fi
if [[ "$video_saved" == true ]] && [[ "$audio_capture_enabled" == true ]]; then
  if [[ -n "$(ffprobe -v error -select_streams a -show_entries stream=codec_type -of csv=p=0 "$output")" ]]; then
    audio_saved=true
    echo "Recording audio stream saved: ${audio_source}"
  else
    echo "Recording was saved without an audio stream." >&2
  fi
fi

if is_truthy "$keep_run_dir"; then
  if [[ -n "$resolved_run_dir" ]]; then
    kept_run_dir="${recording_dir}/${safe_recording_name}-${timestamp}-run"
    cp -a "$resolved_run_dir" "$kept_run_dir"
    echo "Client GameTest run directory saved: ${kept_run_dir}"
  else
    echo "Client GameTest run directory was not found: ${run_dir}" >&2
  fi
fi

write_metadata "$metadata" "$gradle_status" "$video_saved" "$kept_run_dir" "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "Recording metadata saved: ${metadata}"

if [[ "$interrupted" == true ]]; then
  exit 130
fi

exit "$gradle_status"
