package app.xenet.client;
import android.app.*;import android.content.*;import android.net.*;import android.os.*;import android.content.pm.ServiceInfo;
import go.Seq;import libv2ray.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
/** All native/network work lives in :engine; UI never waits on a Future or native stop. */
public final class EngineService extends VpnService {
 public static final String CONTROL="app.xenet.client.CONTROL",VPN="vpn",REAL="real",PING="ping",STOP="stop",RECONNECT="reconnect";public static final int REGISTER=1,STATE=2,UNREGISTER=3;
 private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService io=Executors.newSingleThreadExecutor(),closer=Executors.newCachedThreadPool();private final ScheduledExecutorService timer=Executors.newScheduledThreadPool(1);
 private final ReconnectGate reconnectGate=new ReconnectGate();
 private final Tasks tasks=new Tasks(closer);private final List<Messenger> clients=new ArrayList<>();private final Map<String,Net.Result> results=new LinkedHashMap<>();
 private final Messenger binder=new Messenger(new Handler(Looper.getMainLooper(),msg->{if(msg.what==REGISTER&&msg.replyTo!=null){if(!clients.contains(msg.replyTo))clients.add(msg.replyTo);send(msg.replyTo);}else if(msg.what==UNREGISTER)clients.remove(msg.replyTo);return true;}));
 private volatile String activeProfileId="",activeProfileName="";private ConnectionOptions options;private int vpnHttp,vpnSocks;private ConnectivityManager.NetworkCallback watcher;private volatile long networkVersion,lastNetworkChange;private boolean engineReady;private volatile int subscriptionProxyPort;private volatile String state="idle",kind="",error="";private volatile int done=0,total=0;private volatile ParcelFileDescriptor tun;private volatile java.lang.Process tunnelProcess;private volatile CoreController core;
 @Override public IBinder onBind(Intent i){return CONTROL.equals(i.getAction())?binder.getBinder():super.onBind(i);}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null)return START_NOT_STICKY;String action=intent.getAction();if(STOP.equals(action)){cancel();return START_NOT_STICKY;}if(RECONNECT.equals(action)){if(reconnectGate.request(kind,state)){state="reconnecting";publish();}else if(tasks.current()==null)stopSelf();return START_NOT_STICKY;}
  if(!Arrays.asList(VPN,REAL,PING).contains(action))return START_NOT_STICKY;
  Tasks.Job job=tasks.begin(action);if(job==null){error="BUSY";publish();return START_NOT_STICKY;}
  reconnectGate.clear();kind=action;state="starting";error="";done=0;results.clear();foreground();publish();String profile=intent.getStringExtra("profile"),scope=intent.getStringExtra("scope");
  io.execute(()->run(job,profile,scope==null?"":scope));return START_NOT_STICKY;
 }
 private void run(Tasks.Job job,String selected,String scope){
  try {
   JSONObject data=new Store(this).transaction(null);options=new ConnectionOptions(data.optJSONObject("options"));List<Profile> all=Store.profiles(data);List<Profile> target=new ArrayList<>();for(Profile p:all)if(scope.isEmpty()||p.group.equals(scope)||(scope.equals("manual")&&p.group.isEmpty()))target.add(p);
   total=target.size();job.check();if(VPN.equals(job.kind)){
    Profile p=null;for(Profile row:all)if(row.id.equals(selected))p=row;if(p==null)throw new IOException("NO_PROFILE");activeProfileId=p.id;activeProfileName=p.name;watchNetworks();startVpn(job,p);job.check();state="connected";publish();long seen=networkVersion,stable=SystemClock.elapsedRealtime();int attempts=0;
    while(!job.cancelled.get()){job.check();boolean manual=reconnectGate.take();boolean failed=tunnelProcess==null||!alive(tunnelProcess)||core==null||!core.getIsRunning();long now=SystemClock.elapsedRealtime();boolean changed=networkVersion!=seen&&RetryPolicy.settled(now,lastNetworkChange);
     if(manual||failed||(options.reconnect&&changed)){if(!options.reconnect&&!manual)throw new IOException("TUN_EXIT");state="reconnecting";publish();if(manual||now-stable>60000)attempts=0;
      boolean restored=false;while(!restored){job.check();while(!physicalNetwork()){pause(job,1000);}int delay=RetryPolicy.delayMillis(++attempts,options.retryCount,options.retryDelay);if(delay<0)throw new IOException("RECONNECT_FAILED");if(!(manual&&attempts==1))pause(job,delay);job.check();try{stopNativeOnly();startCore(p,vpnHttp,vpnSocks);job.check();launchTunnel(job,vpnSocks);restored=true;}catch(InterruptedException stop){throw stop;}catch(Exception retry){if(attempts>=options.retryCount)throw new IOException("RECONNECT_FAILED");}}
      job.check();seen=networkVersion;stable=SystemClock.elapsedRealtime();state="connected";publish();}
     pause(job,500);
    }
   } else {
    if(hasVpn())throw new IOException("OTHER_VPN");if(target.isEmpty())throw new IOException("NO_PROFILE");state="testing";publish();
    for(Profile p:target){job.check();if(hasVpn())throw new IOException("OTHER_VPN");Net.Result result;
     if(PING.equals(job.kind)){long ping=-1;String fail="";if(p.host.isEmpty()||p.port<1)fail="NO_TCP_ENDPOINT";else{
       Socket socket=new Socket();tasks.hook(job,()->{try{socket.close();}catch(Exception ignored){}});try{long begin=System.nanoTime();socket.connect(new InetSocketAddress(p.host,p.port),options.pingTimeout);ping=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);}catch(Exception e){job.check();fail=Net.error(e);}finally{socket.close();tasks.hook(job,null);}}
      result=new Net.Result(p.id,ping,0,-1,fail);
     }else{
      try{int http=port(),socks=port();while(socks==http)socks=port();startCore(p,http,socks);job.check();result=Net.measure(p,http,tasks,job,timer);}catch(InterruptedException e){throw e;}catch(Exception e){result=new Net.Result(p.id,-1,0,-1,Net.error(e));}finally{stopCore();}
     }
     job.check();synchronized(results){results.put(p.id,result);}done++;publish();
    }
   }
  } catch(InterruptedException e){error="CANCELLED";}catch(Throwable e){error=e instanceof LinkageError?"CORE_UNAVAILABLE":Net.error(e);}
  finally {state="stopping";publish();cleanup();main.post(()->{tasks.finish(job);kind="";state="idle";publish();stopForeground(true);stopSelf();});}
 }
 private int port()throws Exception {try(ServerSocket s=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){return s.getLocalPort();}}
 private void startCore(Profile p,int http,int socks)throws Exception {
  if(!engineReady){Seq.setContext(getApplicationContext());Libv2ray.initCoreEnv(getFilesDir().getAbsolutePath(),"");engineReady=true;}
  core=Libv2ray.newCoreController(new CoreCallbackHandler(){public long startup(){return 0;}public long shutdown(){return 0;}public long onEmitStatus(long l,String s){return 0;}});
  core.startLoop(CoreConfig.build(p,http,socks,options));if(!core.getIsRunning())throw new IOException("CORE_START");subscriptionProxyPort=http;
 }
 private void stopCore(){subscriptionProxyPort=0;CoreController c=core;core=null;if(c!=null)try{c.stopLoop();}catch(Exception ignored){}}
 private void startVpn(Tasks.Job job,Profile p)throws Exception {
  vpnHttp=port();vpnSocks=port();while(vpnSocks==vpnHttp)vpnSocks=port();
  Builder b=new Builder().setSession("Xenet").setMtu(options.mtu).addAddress("172.19.0.2",30).addAddress("fdfe:dcba:9876::2",126).addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer(options.dns);
  Set<String> installed=new HashSet<>();installed.add(getPackageName());for(String name:options.packages)try{getPackageManager().getApplicationInfo(name,0);installed.add(name);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
  AppRoutePolicy route=AppRoutePolicy.resolve(options,getPackageName(),installed);for(String name:route.apps){if(route.allow)b.addAllowedApplication(name);else b.addDisallowedApplication(name);}
  tun=b.establish();if(tun==null)throw new IOException("VPN_PERMISSION");job.check();startCore(p,vpnHttp,vpnSocks);job.check();launchTunnel(job,vpnSocks);
 }
 private void launchTunnel(Tasks.Job job,int socks)throws Exception {
  String path=new File(getFilesDir(),"tunnel.sock").getAbsolutePath();new File(path).delete();
  List<String> cmd=Arrays.asList(new File(getApplicationInfo().nativeLibraryDir,"libtun2socks.so").getAbsolutePath(),"--netif-ipaddr","172.19.0.1","--netif-netmask","255.255.255.252","--netif-ip6addr","fdfe:dcba:9876::1","--socks-server-addr","127.0.0.1:"+socks,"--tunmtu",Integer.toString(options.mtu),"--sock-path",path,"--enable-udprelay","--loglevel","error");
  tunnelProcess=new ProcessBuilder(cmd).directory(getFilesDir()).redirectErrorStream(true).start();final java.lang.Process proc=tunnelProcess;closer.execute(()->{try(InputStream in=proc.getInputStream()){byte[] buf=new byte[512];while(in.read(buf)!=-1){}}catch(Exception ignored){}});
  boolean sent=false;for(int retry=0;retry<30;retry++){job.check();try(LocalSocket socket=new LocalSocket()){socket.connect(new LocalSocketAddress(path,LocalSocketAddress.Namespace.FILESYSTEM));socket.setFileDescriptorsForSend(new java.io.FileDescriptor[]{tun.getFileDescriptor()});socket.getOutputStream().write(42);sent=true;break;}catch(IOException e){Thread.sleep(100);}}
  if(!sent)throw new IOException("TUN_NOT_READY");job.check();
 }
 private static boolean alive(java.lang.Process p){try{p.exitValue();return false;}catch(IllegalThreadStateException e){return true;}}
 private boolean hasVpn(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return true;}return false;}
 private void cancel(){Tasks.Job j=tasks.current();if(j==null){state="idle";kind="";error="CANCELLED";publish();stopForeground(true);stopSelf();return;}tasks.stop();state="stopping";publish();
  // Last resort: kill only :engine if native cleanup remains stuck. UI process survives.
  main.postDelayed(()->{if(tasks.requiresReset(j)){java.lang.Process p=tunnelProcess;if(p!=null)p.destroy();try{if(tun!=null)tun.close();}catch(Exception ignored){}stopForeground(true);android.os.Process.killProcess(android.os.Process.myPid());}},3000);
 }
 private void stopNativeOnly(){java.lang.Process p=tunnelProcess;tunnelProcess=null;if(p!=null)p.destroy();stopCore();}
 private void cleanup(){reconnectGate.clear();if(watcher!=null)try{((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(watcher);}catch(Exception ignored){}watcher=null;stopNativeOnly();try{if(tun!=null)tun.close();}catch(Exception ignored){}tun=null;activeProfileId="";activeProfileName="";}
 private void pause(Tasks.Job job,int ms)throws InterruptedException{long end=SystemClock.elapsedRealtime()+ms;while(SystemClock.elapsedRealtime()<end){job.check();Thread.sleep(Math.min(100,Math.max(1,end-SystemClock.elapsedRealtime())));}job.check();}
 private boolean physicalNetwork(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&!c.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return true;}return false;}
 private void watchNetworks(){if(!options.reconnect)return;ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);watcher=new ConnectivityManager.NetworkCallback(){private void changed(){lastNetworkChange=SystemClock.elapsedRealtime();networkVersion++;}public void onAvailable(Network n){changed();}public void onLost(Network n){changed();}};try{cm.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),watcher);}catch(Exception ignored){watcher=null;}}
 private void foreground(){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("xenet_engine","Xenet",NotificationManager.IMPORTANCE_LOW);ch.setSound(null,null);nm.createNotificationChannel(ch);}Intent open=new Intent(this,MainActivity.class);int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
  PendingIntent content=PendingIntent.getActivity(this,1,open,flags),stop=PendingIntent.getService(this,2,new Intent(this,EngineService.class).setAction(STOP),flags),reconnect=PendingIntent.getService(this,3,new Intent(this,EngineService.class).setAction(RECONNECT),flags);
  boolean connected=state.equals("connected")&&VPN.equals(kind);String label=state.equals("stopping")?"Stopping…":state.equals("reconnecting")?"Reconnecting…":state.equals("starting")?"Connecting…":connected?"Connected":"Testing "+done+" / "+total;
  String description=VPN.equals(kind)&&!activeProfileName.isEmpty()?activeProfileName:label;
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"xenet_engine"):new Notification.Builder(this);
  builder.setSmallIcon(R.drawable.notice).setContentTitle("Xenet · "+label).setContentText(description).setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false).setUsesChronometer(false).addAction(R.drawable.ic_power,"Stop",stop).setVisibility(Notification.VISIBILITY_PRIVATE);
  if(connected){builder.setStyle(new Notification.BigTextStyle().bigText("Connected to: "+activeProfileName));builder.addAction(R.drawable.ic_refresh,"Reconnect",reconnect);}
  Notification.Builder publicBuilder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"xenet_engine"):new Notification.Builder(this);builder.setPublicVersion(publicBuilder.setShowWhen(false).setUsesChronometer(false).setSmallIcon(R.drawable.notice).setContentTitle("Xenet").setContentText("VPN or connection test is running").setVisibility(Notification.VISIBILITY_PUBLIC).build());
  Notification n=builder.build();if(Build.VERSION.SDK_INT>=34)startForeground(17,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(17,n);
 }
 private Bundle snapshot(){Bundle b=new Bundle();b.putString("state",state);b.putString("kind",kind);b.putString("error",error);b.putString("activeProfileId",activeProfileId);b.putString("activeProfileName",activeProfileName);b.putInt("done",done);b.putInt("total",total);b.putInt("subscriptionProxyPort",subscriptionProxyPort);JSONArray a=new JSONArray();synchronized(results){for(Net.Result r:results.values())try{a.put(Parser.obj("id",r.id,"ping",r.ping,"mbps",r.mbps,"bytes",r.bytes,"error",r.error));}catch(Exception ignored){}}b.putString("results",a.toString());return b;}
 private void send(Messenger m){try{Message msg=Message.obtain(null,STATE);msg.setData(snapshot());m.send(msg);}catch(RemoteException e){clients.remove(m);}}
 private void publish(){main.post(()->{if(!state.equals("idle"))foreground();for(Messenger m:new ArrayList<>(clients))send(m);});}
 @Override public void onRevoke(){cancel();}
 @Override public void onDestroy(){tasks.stop();io.shutdown();timer.shutdown();if(watcher!=null)try{((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(watcher);}catch(Exception ignored){}super.onDestroy();}
}
