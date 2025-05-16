#!/system/bin/sh
#lock samba conf
chmod 444 /data/samba/etc/smb.conf
chattr +i /data/samba/etc/smb.conf
UNLOCK_SAMBA_CONF='#!/system/bin/sh

chattr -i /data/samba/etc/smb.conf

chmod 644 /data/samba/etc/smb.conf

rm -f /data/samba/etc/smb.conf

sync'
echo "$UNLOCK_SAMBA_CONF" > /cache/unlock_samba.sh
echo "$UNLOCK_SAMBA_CONF" > /sdcard/unlock_samba.sh

# check ttyd running
if ! top -n 1 | grep "ttyd --writable --port 1146 /system/bin/sh" | grep -v grep > /dev/null; then
    echo "start ttyd..." >> /sdcard/smb_log.log
    #Drop port for ipv6
    ip6tables -A INPUT -p tcp --dport 8080 -j DROP
    ip6tables -A INPUT -p tcp --dport 1146 -j DROP
    ip6tables -A INPUT -p tcp --dport 139 -j DROP
    ip6tables -A INPUT -p tcp --dport 445 -j DROP
    ip6tables -A INPUT -p tcp --dport 5555 -j DROP
    /data/data/com.minikano.f50_sms/files/ttyd --writable --port 1146 /system/bin/sh &
else
    echo "ttyd already running." >> /sdcard/smb_log.log
fi


