#!/system/bin/sh
awk '{print $1}' /sys/class/thermal/thermal_zone*/temp | sort -nr | head -n1