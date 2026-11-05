package app.xenet.client;
import okhttp3.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class Net {
 public static final int BYTES=2*1024*1024;public static final long BODY_MS=3000;
 public static final OkHttpClient CLIENT=new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).connectTimeout(7,TimeUnit.SECONDS).readTimeout(4,TimeUnit.SECONDS).callTimeout(10,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();
 public static String subscription(String url)throws Exception {return subscription(url,0);}
 public static String subscription(String url,int proxyPort)throws Exception {
  if(proxyPort<0||proxyPort>65535)throw new IllegalArgumentException("BAD_PORT");
  OkHttpClient transport=proxyPort>0?CLIENT.newBuilder().proxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",proxyPort))).build():CLIENT;
  HttpUrl current=subscriptionUrl(url);long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
  Set<String> visited=new HashSet<>();
  for(int hop=0;hop<=5;hop++){
   if(!visited.add(current.toString()))throw new IOException("REDIRECT_LOOP");
   long remaining=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(remaining<=0)throw new SocketTimeoutException();
   OkHttpClient fetcher=transport.newBuilder().callTimeout(remaining,TimeUnit.MILLISECONDS).build();
   Request req=new Request.Builder().url(current).header("User-Agent","Xenet/1.0").header("Accept","text/plain, application/json, */*").header("Cache-Control","no-cache").build();
   try(Response r=fetcher.newCall(req).execute()){
    if(Arrays.asList(301,302,303,307,308).contains(r.code())){String location=r.header("Location");HttpUrl next=redirectTarget(current,location);if(hop==5)throw new IOException("TOO_MANY_REDIRECTS");current=next;continue;}
    if(!r.isSuccessful()||r.body()==null)throw new IOException("HTTP_"+r.code());
    String value=new String(Store.read(r.body().byteStream(),2*1024*1024),"UTF-8");if(value.startsWith("\uFEFF"))value=value.substring(1);value=value.trim();
    if(value.toLowerCase(Locale.ROOT).startsWith("<!doctype html")||value.toLowerCase(Locale.ROOT).startsWith("<html"))throw new IOException("SUBSCRIPTION_HTML");return value;
   }
  }throw new IOException("TOO_MANY_REDIRECTS");
 }
 static HttpUrl redirectTarget(HttpUrl from,String location)throws IOException{HttpUrl next=location==null?null:from.resolve(location);if(next==null)throw new IOException("BAD_REDIRECT");next=subscriptionUrl(next.toString());if(from.isHttps()&&!next.isHttps())throw new IOException("HTTPS_DOWNGRADE");return next;}
 static HttpUrl subscriptionUrl(String url){HttpUrl u=HttpUrl.parse(url);if(u==null||!(u.scheme().equals("http")||u.scheme().equals("https"))||!u.username().isEmpty()||!u.password().isEmpty())throw new IllegalArgumentException("BAD_URL");return u;}
 public static String error(Throwable e){if(e instanceof javax.net.ssl.SSLException)return "TLS";if(e instanceof SocketTimeoutException||e instanceof InterruptedIOException)return "TIMEOUT";if(e instanceof UnknownHostException)return "DNS";String m=e.getMessage();if(m!=null&&m.matches("[A-Z0-9_]{1,48}"))return m;return "NETWORK";}
 public static final class Result {public final String id;public final long ping,bytes;public final double mbps;public final String error;Result(String id,long ping,long bytes,double mbps,String error){this.id=id;this.ping=ping;this.bytes=bytes;this.mbps=mbps;this.error=error;}}
 public static double mbps(long bytes,long nanos){return bytes<65536||nanos<=0?-1:bytes*8000.0/nanos;}
 public static Result measure(Profile p,int httpPort,Tasks tasks,Tasks.Job job,ScheduledExecutorService timer)throws Exception {
  return measureAt(p,httpPort,tasks,job,timer,"https://speed.cloudflare.com/__down?bytes="+BYTES+"&xenet="+System.nanoTime());
 }
 /** Package-private endpoint injection for host transport tests, never exposed in UI. */
 static Result measureAt(Profile p,int httpPort,Tasks tasks,Tasks.Job job,ScheduledExecutorService timer,String endpoint)throws Exception {
  job.check();OkHttpClient client=CLIENT.newBuilder().proxy(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",httpPort))).build();
  Request req=new Request.Builder().url(endpoint).header("Accept-Encoding","identity").header("Cache-Control","no-store").build();Call call=client.newCall(req);tasks.hook(job,call::cancel);long start=System.nanoTime(),bodyStart=0,count=0,ping=-1;AtomicBoolean deadline=new AtomicBoolean();ScheduledFuture<?> cutoff=null;
  try(Response r=call.execute()){
   job.check();ping=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);if(r.code()!=200)throw new IOException("HTTP_"+r.code());if(r.body()==null||r.header("Content-Type","").toLowerCase(Locale.ROOT).indexOf("application/octet-stream")!=0)throw new IOException("RESPONSE");
   bodyStart=System.nanoTime();cutoff=timer.schedule(()->{deadline.set(true);call.cancel();},BODY_MS,TimeUnit.MILLISECONDS);
   try(InputStream in=r.body().byteStream()){byte[] buf=new byte[16384];while(count<BYTES){job.check();int n=in.read(buf,0,(int)Math.min(buf.length,BYTES-count));if(n<0)break;count+=n;}}
   catch(IOException e){job.check();if(!deadline.get())throw e;}
   job.check();long elapsed=System.nanoTime()-bodyStart;double speed=mbps(count,elapsed);return new Result(p.id,ping,count,speed,speed<0?"SHORT_SAMPLE":"");
  }catch(InterruptedException e){throw e;}catch(Exception e){job.check();return new Result(p.id,ping,count,-1,error(e));}
  finally {if(cutoff!=null)cutoff.cancel(false);tasks.hook(job,null);call.cancel();client.connectionPool().evictAll();}
 }
}
