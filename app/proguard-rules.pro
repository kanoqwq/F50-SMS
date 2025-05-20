# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class org.slf4j.Logger {
    public void info(...);
    public void debug(...);
    public void error(...);
    public void warn(...);
    public void trace(...);
    public boolean isDebugEnabled();
    public boolean isInfoEnabled();
    public boolean isWarnEnabled();
    public boolean isErrorEnabled();
    public boolean isTraceEnabled();
}

-assumenosideeffects class org.slf4j.LoggerFactory {
    public static org.slf4j.Logger getLogger(...);
}

# -------- 保留 JavaMail 的相关类 ----------
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class jakarta.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.activation.** { *; }
-keep class javax.activation.** { *; }

# 保留构造函数（因为邮件类经常通过反射创建）
-keepclassmembers class * {
    public <init>(...);
    public *;
}
-keep class com.sun.mail.util.MailLogger { *; }
-dontwarn org.slf4j.**

# 避免移除 mailcap 相关配置（部分 MIME 类型注册）
-dontoptimize
-dontshrink

# 忽略 junixsocket 所依赖但不会运行时使用的注解类
-dontwarn com.kohlschutter.annotations.compiletime.SuppressFBWarnings
-dontwarn com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport
-dontwarn org.eclipse.jdt.annotation.NonNullByDefault
-dontwarn java.rmi.server.RemoteServer

# 保留 junixsocket 相关类，避免反射或 native 调用丢失
-keep class org.newsclub.net.unix.** { *; }
-keep class com.kohlschutter.util.** { *; }

# 防止类名被混淆（可选，根据你是否希望混淆这些类名）
-keepnames class org.newsclub.net.unix.**


-dontwarn afu.org.checkerframework.dataflow.qual.Pure
-dontwarn afu.org.checkerframework.dataflow.qual.SideEffectFree
-dontwarn afu.org.checkerframework.framework.qual.EnsuresQualifierIf
-dontwarn afu.org.checkerframework.framework.qual.EnsuresQualifiersIf
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.checker.formatter.qual.ConversionCategory
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.checker.formatter.qual.ReturnsFormat
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.checker.nullness.qual.EnsuresNonNull
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.afu.org.checkerframework.checker.regex.qual.Regex
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.checker.formatter.qual.ConversionCategory
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.checker.formatter.qual.ReturnsFormat
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.checker.nullness.qual.EnsuresNonNull
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.org.checkerframework.checker.regex.qual.Regex
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn javax.security.auth.callback.NameCallback
-dontwarn javax.security.auth.kerberos.KerberosKey
-dontwarn javax.security.auth.kerberos.KerberosPrincipal
-dontwarn javax.security.auth.kerberos.KerberosTicket
-dontwarn javax.security.auth.login.AppConfigurationEntry$LoginModuleControlFlag
-dontwarn javax.security.auth.login.AppConfigurationEntry
-dontwarn javax.security.auth.login.Configuration
-dontwarn javax.security.auth.login.LoginContext
-dontwarn javax.servlet.Filter
-dontwarn javax.servlet.FilterChain
-dontwarn javax.servlet.FilterConfig
-dontwarn javax.servlet.ServletConfig
-dontwarn javax.servlet.ServletContextEvent
-dontwarn javax.servlet.ServletContextListener
-dontwarn javax.servlet.ServletException
-dontwarn javax.servlet.ServletOutputStream
-dontwarn javax.servlet.ServletRequest
-dontwarn javax.servlet.ServletResponse
-dontwarn javax.servlet.http.HttpServlet
-dontwarn javax.servlet.http.HttpServletRequest
-dontwarn javax.servlet.http.HttpServletRequestWrapper
-dontwarn javax.servlet.http.HttpServletResponse
-dontwarn javax.servlet.http.HttpSession
-dontwarn org.apache.avalon.framework.logger.Logger
-dontwarn org.apache.log.Hierarchy
-dontwarn org.apache.log.Logger
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.checkerframework.dataflow.qual.Pure
-dontwarn org.checkerframework.dataflow.qual.SideEffectFree
-dontwarn org.checkerframework.framework.qual.EnsuresQualifierIf
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.MessageProp
-dontwarn org.ietf.jgss.Oid