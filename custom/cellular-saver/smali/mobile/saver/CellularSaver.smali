.class public final Lmobile/saver/CellularSaver;
.super Landroid/net/ConnectivityManager$NetworkCallback;

.field private static instance:Lmobile/saver/CellularSaver;
.field private final manager:Lcom/twitter/app/settings/s1;

.method private constructor <init>(Lcom/twitter/app/settings/s1;)V
    .locals 0
    invoke-direct {p0}, Landroid/net/ConnectivityManager$NetworkCallback;-><init>()V
    iput-object p1, p0, Lmobile/saver/CellularSaver;->manager:Lcom/twitter/app/settings/s1;
    return-void
.end method

.method public static start(Landroid/content/Context;)V
    .locals 5
    sget-object v0, Lmobile/saver/CellularSaver;->instance:Lmobile/saver/CellularSaver;
    if-nez v0, :done
    :try_start
    new-instance v0, Lcom/twitter/app/settings/s1;
    invoke-direct {v0, p0}, Lcom/twitter/app/settings/s1;-><init>(Landroid/content/Context;)V
    new-instance v1, Lmobile/saver/CellularSaver;
    invoke-direct {v1, v0}, Lmobile/saver/CellularSaver;-><init>(Lcom/twitter/app/settings/s1;)V
    const-string v0, "connectivity"
    invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/net/ConnectivityManager;
    if-eqz v0, :done
    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
    move-result-object v2
    new-instance v3, Landroid/os/Handler;
    invoke-direct {v3, v2}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
    invoke-virtual {v0, v1, v3}, Landroid/net/ConnectivityManager;->registerDefaultNetworkCallback(Landroid/net/ConnectivityManager$NetworkCallback;Landroid/os/Handler;)V
    sput-object v1, Lmobile/saver/CellularSaver;->instance:Lmobile/saver/CellularSaver;
    invoke-virtual {v0}, Landroid/net/ConnectivityManager;->getActiveNetwork()Landroid/net/Network;
    move-result-object v2
    invoke-virtual {v0, v2}, Landroid/net/ConnectivityManager;->getNetworkCapabilities(Landroid/net/Network;)Landroid/net/NetworkCapabilities;
    move-result-object v2
    invoke-direct {v1, v2}, Lmobile/saver/CellularSaver;->update(Landroid/net/NetworkCapabilities;)V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :error
    :done
    return-void
    :error
    move-exception v0
    const-string v1, "CellularSaver"
    const-string v2, "Could not initialize cellular data saver"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method

.method private update(Landroid/net/NetworkCapabilities;)V
    .locals 5
    :try_start
    const/4 v0, 0x0
    if-eqz p1, :apply
    const/4 v1, 0x1
    invoke-virtual {p1, v1}, Landroid/net/NetworkCapabilities;->hasTransport(I)Z
    move-result v1
    if-nez v1, :apply
    invoke-virtual {p1, v0}, Landroid/net/NetworkCapabilities;->hasTransport(I)Z
    move-result v0
    :apply
    iget-object v1, p0, Lmobile/saver/CellularSaver;->manager:Lcom/twitter/app/settings/s1;
    iget-object v2, v1, Lcom/twitter/app/settings/s1;->a:Lcom/twitter/util/prefs/o;
    const-string v3, "pref_data_saver"
    const/4 v4, 0x0
    invoke-interface {v2, v3, v4}, Lcom/twitter/util/prefs/o;->getBoolean(Ljava/lang/String;Z)Z
    move-result v4
    if-eq v4, v0, :done
    invoke-virtual {v1, v0}, Lcom/twitter/app/settings/s1;->a(Z)V
    invoke-interface {v2}, Lcom/twitter/util/prefs/o;->edit()Lcom/twitter/util/prefs/o$c;
    move-result-object v2
    invoke-interface {v2, v3, v0}, Lcom/twitter/util/prefs/o$c;->d(Ljava/lang/String;Z)Lcom/twitter/util/prefs/o$c;
    move-result-object v2
    invoke-interface {v2}, Lcom/twitter/util/prefs/o$c;->g()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :error
    :done
    return-void
    :error
    move-exception v0
    const-string v1, "CellularSaver"
    const-string v2, "Could not update cellular data saver"
    invoke-static {v1, v2, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method

.method public onCapabilitiesChanged(Landroid/net/Network;Landroid/net/NetworkCapabilities;)V
    .locals 0
    invoke-direct {p0, p2}, Lmobile/saver/CellularSaver;->update(Landroid/net/NetworkCapabilities;)V
    return-void
.end method

.method public onLost(Landroid/net/Network;)V
    .locals 1
    const/4 v0, 0x0
    invoke-direct {p0, v0}, Lmobile/saver/CellularSaver;->update(Landroid/net/NetworkCapabilities;)V
    return-void
.end method
