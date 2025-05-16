package com.minikano.f50_sms

import android.content.Context
import android.os.Build
import java.io.File
import java.io.IOException

object SMBConfig {
    fun writeConfig(context: Context,command:String = "/system/bin/sh /data/data/com.minikano.f50_sms/files/samba_exec.sh"): String? {
        val model = Build.MODEL
        val fileName = "smb.conf"
        val presetString = """[global]
            	workgroup = SAMBA
            	netbios name = Android
            	server string = Android Samber Server
            	security = user
            	passdb backend = smbpasswd:/data/samba/etc/smbpasswd
            	map to guest = bad user
                root preexec = $command

            [$model]
            	comment = Android Server
            	path = /storage
            	browseable = yes
            	writable = yes
            	public = yes
            	guest ok = yes

            [root]
            	comment = Android Server
            	path = /
            	browseable = yes
            	writable = yes
            	public = yes
            	guest ok = yes

            [internal_storage]
            	comment = Android Server
            	path = /sdcard
            	browseable = yes
            	writable = yes
            	public = yes
            	guest ok = yes

            [sdcard]
            	comment = Android Server
            	path = /storage/sdcard0
            	browseable = yes
            	writable = yes
            	public = yes
            	guest ok = yes
            """.trimIndent()
        return try {
            val dir = context.getExternalFilesDir(null)
            if (dir != null) {
                val file = File(dir, fileName)
                file.writeText(presetString, Charsets.UTF_8)
                file.absolutePath
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}