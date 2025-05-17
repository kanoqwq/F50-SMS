#!/system/bin/sh
LOG_FILE="/sdcard/smb_log.log"

MAX_SIZE=$((4 * 1024 * 1024))  # 4MB = 4 * 1024 * 1024 bytes

# check log file
if [ -f "$LOG_FILE" ]; then
    # get file size
    FILE_SIZE=$(wc -c < "$LOG_FILE")

    if [ "$FILE_SIZE" -ge "$MAX_SIZE" ]; then
        echo "Log file exceeded 4MB, clearing..." > "$LOG_FILE"
    fi
fi

#lock samba conf
chmod 444 /data/samba/etc/smb.conf
chattr +i /data/samba/etc/smb.conf

UNLOCK_SAMBA_CONF='#!/system/bin/sh
chattr -i /data/samba/etc/smb.conf
chmod 644 /data/samba/etc/smb.conf
rm -f /data/samba/etc/smb.conf
sync'

BOOTUP_SH='#!/system/bin/sh
# you script here...
sync'

echo "$UNLOCK_SAMBA_CONF" > /cache/unlock_samba.sh
echo "$UNLOCK_SAMBA_CONF" > /sdcard/unlock_samba.sh

TARGET_SCRIPT="/sdcard/ufi_tools_boot.sh"

if [ -f "$TARGET_SCRIPT" ]; then
    echo "[`date`] exec $TARGET_SCRIPT ..." >> "$LOG_FILE"
    sh "$TARGET_SCRIPT"
else
    echo "$BOOTUP_SH" > /sdcard/ufi_tools_boot.sh
    echo "[`date`] $TARGET_SCRIPT not found，skip" >> "$LOG_FILE"
fi

TTYD_PATH="/data/data/com.minikano.f50_sms/files/ttyd"

#Drop port for ipv6
ip6tables -A INPUT -p tcp --dport 8080 -j DROP
ip6tables -A INPUT -p tcp --dport 1146 -j DROP
ip6tables -A INPUT -p tcp --dport 139 -j DROP
ip6tables -A INPUT -p tcp --dport 445 -j DROP
ip6tables -A INPUT -p tcp --dport 5555 -j DROP

# check ttyd running
if ! ps -ef | grep "ttyd --writable --port 1146 /system/bin/sh" | grep -v grep > /dev/null; then
    echo "[`date`] start ttyd..." >> "$LOG_FILE"

    "$TTYD_PATH" --writable --port 1146 /system/bin/sh &
else
    echo "[`date`] ttyd already running." >> "$LOG_FILE"
fi

SOCKET_DIR="/data/data/com.minikano.f50_sms/files"
SOCKET_FILE="$SOCKET_DIR/kano_root_shell.sock"
SOCAT_PATH="/data/data/com.minikano.f50_sms/files/socat"

# check socat
mkdir -p "$SOCKET_DIR"

# check socat running
if ! ps -ef | grep "$SOCKET_FILE" | grep -v grep > /dev/null; then
    echo "[`date`] start socat..." >> "$LOG_FILE"
    # run socat unix socket，exec /system/bin/sh
    "$SOCAT_PATH" -d -d UNIX-LISTEN:"$SOCKET_FILE",fork,reuseaddr,unlink-early EXEC:/system/bin/sh &
else
    echo "[`date`] socat already running." >> "$LOG_FILE"
fi

