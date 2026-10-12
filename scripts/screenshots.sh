#!/usr/bin/env bash
# Captures README screenshots of the Planner sample on a running emulator.
# Swipes and taps can't be timed reliably through adb, so the sample opens each screen from the `scene` extra with
# the calendar already settled (pagers and lists start at their page, nothing animates in), and the app is fixed to
# Wednesday, 14 October 2026, so every capture is the same.
# Every capture is checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: month and agenda side by side
#   bash scripts/screenshots.sh phone    # pixel_7: month with event dots, range selection, week strip and agenda
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it.
# "6 nights" only appears once the range is complete, "Design review" is in the agenda of the 14th,
# "Upcoming" is the title of the tablet's agenda pane.
expected_text() {
  case "$1" in
    month) echo "October 2026" ;;
    range) echo "6 nights" ;;
    week) echo "Design review" ;;
    tablet) echo "Upcoming" ;;
  esac
}

install_sample
set_night_mode light
if [ "$device" = tablet ]; then
  ensure_landscape
  fresh_launch --es scene tablet
  capture "tablet-planner" "$(expected_text tablet)"
else
  for scene in month range week; do
    fresh_launch --es scene "$scene"
    capture "phone-$scene" "$(expected_text "$scene")"
  done
fi
