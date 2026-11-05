package app.xenet.client;
import org.junit.*;import static org.junit.Assert.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
public class RewriteTest {
 String v="vless://11111111-1111-4111-8111-111111111111@example.com:443?security=tls&type=ws&path=%2Fx#Test";
 String b64(String s){return Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 Profile p(String s)throws Exception{return Parser.parse(s,"");}JSONObject store()throws Exception{return Parser.obj("profiles",new JSONArray(),"groups",new JSONArray(),"selected","");}
 interface Throwing {void run()throws Exception;}void fails(String code,Throwing f)throws Exception{try{f.run();fail("expected "+code);}catch(IllegalArgumentException e){assertEquals(code,e.getMessage());}}
 @Test public void literalPlusPreserved(){assertEquals("a+b",Codec.decode("a+b"));assertEquals("a+b",Codec.decode("a%2Bb"));}
 @Test public void shadowsocksPlusPassword()throws Exception{JSONObject out=new JSONArray(p("ss://aes-128-gcm:abc+def@example.com:8388").outbounds).getJSONObject(0);assertEquals("abc+def",out.getJSONObject("settings").getJSONArray("servers").getJSONObject(0).getString("password"));}
 @Test public void vlessTlsWs()throws Exception{Profile p=p(v);assertEquals("Test",p.name);assertEquals("/x",new JSONArray(p.outbounds).getJSONObject(0).getJSONObject("streamSettings").getJSONObject("wsSettings").getString("path"));}
 @Test public void vmess()throws Exception{assertEquals("vmess",p("vmess://"+b64("{\"add\":\"example.com\",\"port\":443,\"id\":\"id\",\"net\":\"tcp\"}")).protocol);}
 @Test public void trojan()throws Exception{assertEquals("trojan",p("trojan://secret@example.com:443").protocol);}
 @Test public void shadowsocksEncoded()throws Exception{assertEquals("shadowsocks",p("ss://"+b64("aes-128-gcm:secret")+"@example.com:8388").protocol);}
 @Test public void shadowsocksWhole()throws Exception{assertEquals(8388,p("ss://"+b64("aes-128-gcm:secret@example.com:8388")).port);}
 @Test public void socks()throws Exception{assertEquals("socks",p("socks5://u:p@example.com:1080").protocol);}
 @Test public void https()throws Exception{assertEquals("tls",new JSONArray(p("https://example.com:443").outbounds).getJSONObject(0).getJSONObject("streamSettings").getString("security"));}
 @Test public void ipv6()throws Exception{assertEquals("::1",p("vless://id@[::1]:443").host);}
 @Test public void reality()throws Exception{assertEquals("reality",new JSONArray(p("vless://id@example.com:443?security=reality&pbk=key").outbounds).getJSONObject(0).getJSONObject("streamSettings").getString("security"));}
 @Test public void realityNeedsKey()throws Exception{fails("REALITY_KEY_MISSING",()->p("vless://id@example.com:443?security=reality"));}
 @Test public void noTuicFallback()throws Exception{fails("UNSUPPORTED_PROTOCOL",()->p("tuic://id@example.com:443"));}
 @Test public void noHysteriaFallback()throws Exception{fails("UNSUPPORTED_PROTOCOL",()->p("hysteria2://id@example.com:443"));}
 @Test public void transportRejection()throws Exception{fails("UNSUPPORTED_TRANSPORT",()->p("vless://id@example.com:443?type=unrecognized"));}
 @Test public void badPort()throws Exception{fails("BAD_PORT",()->p("trojan://secret@example.com:70000"));}
 @Test public void ssPluginRejected()throws Exception{fails("SS_PLUGIN_UNSUPPORTED",()->p("ss://"+b64("aes-128-gcm:secret")+"@example.com:8388?plugin=x"));}
 @Test public void base64Subscription()throws Exception{assertEquals(2,Parser.batch(b64(v+"\n"+v),"g").size());}
 @Test public void exactly100()throws Exception{assertEquals(100,Parser.batch(String.join("\n",Collections.nCopies(100,v)),"g").size());}
 @Test public void reject101()throws Exception{fails("LIMIT_100",()->Parser.batch(String.join("\n",Collections.nCopies(101,v)),"g"));}
 @Test public void jsonWireguard()throws Exception{assertEquals("wireguard",p("{\"protocol\":\"wireguard\",\"settings\":{}}").protocol);}
 @Test public void jsonRoutesRejected()throws Exception{fails("OUTBOUND_JSON_ONLY",()->p("{\"outbounds\":[{\"protocol\":\"vless\"}],\"routing\":{}}"));}
 @Test public void jsonDirectRejected()throws Exception{fails("UNSUPPORTED_PROTOCOL",()->p("{\"protocol\":\"freedom\"}"));}
 @Test public void profileRoundtrip()throws Exception{Profile x=p(v);assertEquals(x.outbounds,Profile.from(x.json()).outbounds);}
 @Test public void forcedLoopbackRoute()throws Exception{JSONObject c=new JSONObject(CoreConfig.build(p(v),21001,21002));JSONArray a=c.getJSONArray("inbounds");assertEquals(2,a.length());for(int i=0;i<2;i++)assertEquals("127.0.0.1",a.getJSONObject(i).getString("listen"));assertEquals("proxy",c.getJSONObject("routing").getJSONArray("rules").getJSONObject(0).getString("outboundTag"));}
 @Test public void duplicatePortsRejected()throws Exception{fails("BAD_PORT",()->CoreConfig.build(p(v),21001,21001));}
 @Test public void appendCapAtomic()throws Exception{JSONObject d=store();List<Profile> a=Parser.batch(String.join("\n",Collections.nCopies(100,v)),"");Store.append(d,a);String before=d.toString();fails("LIMIT_100",()->Store.append(d,Collections.singletonList(p(v))));assertEquals(before,d.toString());}
 @Test public void replacementCapAtomic()throws Exception{JSONObject d=store();Store.append(d,Parser.batch(String.join("\n",Collections.nCopies(100,v)),""));String before=d.toString();fails("LIMIT_100",()->Store.replaceGroup(d,"g","n","url",Collections.singletonList(p(v))));assertEquals(before,d.toString());}
 @Test public void replacementReplaces()throws Exception{JSONObject d=store();Store.replaceGroup(d,"g","n","url",Parser.batch(v,"g"));Store.replaceGroup(d,"g","n","url",Parser.batch(v+"\n"+v,"g"));assertEquals(2,d.getJSONArray("profiles").length());assertEquals(1,d.getJSONArray("groups").length());}
 @Test public void selectionFallback()throws Exception{JSONObject d=store();Store.append(d,Collections.singletonList(p(v)));Store.validate(d);assertFalse(d.getString("selected").isEmpty());}
 @Test public void speedUnits(){assertEquals(8,Net.mbps(1000000,1000000000L),.00001);assertEquals(-1,Net.mbps(1,1),0);assertEquals(-1,Net.mbps(1000000,0),0);}
 @Test public void singleOwner(){Tasks t=new Tasks(Runnable::run);Tasks.Job j=t.begin("real");assertNull(t.begin("ping"));assertTrue(t.finish(j));assertNotNull(t.begin("vpn"));assertFalse(t.finish(j));}
 @Test public void stopDoesNotWait()throws Exception{ExecutorService e=Executors.newSingleThreadExecutor();CountDownLatch block=new CountDownLatch(1),started=new CountDownLatch(1);try{Tasks t=new Tasks(e);Tasks.Job j=t.begin("real");t.hook(j,()->{started.countDown();try{block.await();}catch(Exception ignored){}});long n=System.nanoTime();t.stop();assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-n)<100);assertTrue(started.await(1,TimeUnit.SECONDS));assertNull(t.begin("vpn"));assertTrue(t.requiresReset(j));}finally{block.countDown();e.shutdownNow();}}
 @Test public void lateHookCancelled(){Tasks t=new Tasks(Runnable::run);Tasks.Job j=t.begin("real");t.stop();java.util.concurrent.atomic.AtomicBoolean b=new java.util.concurrent.atomic.AtomicBoolean();t.hook(j,()->b.set(true));assertTrue(b.get());}
 @Test public void stressHundredStops(){Tasks t=new Tasks(Runnable::run);for(int i=0;i<100;i++){Tasks.Job j=t.begin("real");assertNotNull(j);t.stop();assertTrue(j.cancelled.get());assertTrue(t.finish(j));assertFalse(t.requiresReset(j));}}
}
