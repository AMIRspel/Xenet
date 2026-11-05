package app.xenet.client;
import org.junit.*;import static org.junit.Assert.*;import org.json.*;import java.util.*;
public class OptionsTest {
 JSONObject tls()throws Exception{JSONObject user=Parser.obj("id","00000000-0000-0000-0000-000000000001");JSONObject server=Parser.obj("users",Parser.arr(user));return Parser.obj("protocol","vless","settings",Parser.obj("vnext",Parser.arr(server)),"streamSettings",Parser.obj("network","tcp","security","tls"));}
 ConnectionOptions opts(String key,Object value)throws Exception{return new ConnectionOptions(Parser.obj(key,value));}
 Set<String> set(String... v){return new HashSet<>(Arrays.asList(v));}
 @Test public void defaults()throws Exception{ConnectionOptions o=ConnectionOptions.defaults();assertEquals("config",o.mux);assertEquals("config",o.fragment);assertEquals(1400,o.mtu);assertTrue(o.reconnect);}
 @Test public void defaultPreservesProvider()throws Exception{JSONObject p=tls().put("mux",Parser.obj("enabled",true,"concurrency",8));JSONArray a=Parser.arr(p);assertEquals(a.toString(),ConnectionOptions.defaults().apply(a).toString());}
 @Test public void enableMux()throws Exception{JSONObject p=opts("mux","on").apply(Parser.arr(tls())).getJSONObject(0);assertTrue(p.getJSONObject("mux").getBoolean("enabled"));assertEquals(4,p.getJSONObject("mux").getInt("concurrency"));}
 @Test public void disableMux()throws Exception{JSONObject p=tls().put("mux",Parser.obj("enabled",true));assertFalse(opts("mux","off").apply(Parser.arr(p)).getJSONObject(0).getJSONObject("mux").getBoolean("enabled"));}
 @Test public void visionNotForced()throws Exception{JSONObject p=tls();p.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getJSONArray("users").getJSONObject(0).put("flow","xtls-rprx-vision");assertFalse(opts("mux","on").apply(Parser.arr(p)).getJSONObject(0).has("mux"));}
 @Test public void wireguardNotForced()throws Exception{assertFalse(opts("mux","on").apply(Parser.arr(Parser.obj("protocol","wireguard"))).getJSONObject(0).has("mux"));}
 @Test public void grpcMuxNotForced()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("network","grpc");assertFalse(ConnectionOptions.muxCompatible(p));}
 @Test public void fragmentAppended()throws Exception{JSONArray a=opts("fragment","tlshello").apply(Parser.arr(tls()));assertEquals(2,a.length());assertEquals("xenet-fragment",a.getJSONObject(0).getJSONObject("streamSettings").getJSONObject("sockopt").getString("dialerProxy"));assertEquals("tlshello",a.getJSONObject(1).getJSONObject("settings").getJSONObject("fragment").getString("packets"));}
 @Test public void fragmentDoesNotMutateSource()throws Exception{JSONArray source=Parser.arr(tls());String before=source.toString();opts("fragment","tlshello").apply(source);assertEquals(before,source.toString());}
 @Test public void realityUntouched()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("security","reality");assertEquals(1,opts("fragment","tlshello").apply(Parser.arr(p)).length());}
 @Test public void existingDialerPreserved()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("sockopt",Parser.obj("dialerProxy","provider"));assertEquals(1,opts("fragment","tlshello").apply(Parser.arr(p)).length());}
 @Test public void providerChainPreserved()throws Exception{JSONObject p=tls().put("proxySettings",Parser.obj("tag","chain"));assertFalse(ConnectionOptions.fragmentCompatible(p));}
 @Test public void uniqueFragmentTag()throws Exception{JSONArray a=opts("fragment","tlshello").apply(Parser.arr(tls(),Parser.obj("tag","xenet-fragment","protocol","freedom")));assertEquals("xenet-fragment-1",a.getJSONObject(2).getString("tag"));}
 @Test public void websocketFragment()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("network","ws");assertTrue(ConnectionOptions.fragmentCompatible(p));}
 @Test public void plainTcpNoFragment()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("security","none");assertFalse(ConnectionOptions.fragmentCompatible(p));}
 @Test public void grpcNoFragment()throws Exception{JSONObject p=tls();p.getJSONObject("streamSettings").put("network","grpc");assertFalse(ConnectionOptions.fragmentCompatible(p));}
 @Test public void muxAndFragmentIndependent()throws Exception{JSONArray a=new ConnectionOptions(Parser.obj("mux","on","fragment","tlshello")).apply(Parser.arr(tls()));assertEquals(2,a.length());assertTrue(a.getJSONObject(0).getJSONObject("mux").getBoolean("enabled"));}
 @Test public void invalidMtu()throws Exception{try{opts("mtu",9000);fail();}catch(IllegalArgumentException expected){}}
 @Test public void invalidConcurrency()throws Exception{try{opts("concurrency",17);fail();}catch(IllegalArgumentException expected){}}
 @Test public void invalidMode()throws Exception{try{opts("appMode","bogus");fail();}catch(IllegalArgumentException expected){}}
 @Test public void invalidDns()throws Exception{try{opts("dns","http://example.com");fail();}catch(IllegalArgumentException expected){}}
 @Test public void invalidPackage()throws Exception{try{opts("packages",Parser.arr("not a package"));fail();}catch(IllegalArgumentException expected){}}
 @Test public void roundTrip()throws Exception{ConnectionOptions o=new ConnectionOptions(Parser.obj("mux","on","appMode","include","packages",Parser.arr("a.b","c.d")));assertEquals(o.json().toString(),new ConnectionOptions(o.json()).json().toString());}
 @Test public void includeOnlyInstalled()throws Exception{ConnectionOptions o=new ConnectionOptions(Parser.obj("appMode","include","packages",Parser.arr("a.b","gone.app","self.app")));AppRoutePolicy r=AppRoutePolicy.resolve(o,"self.app",set("a.b","self.app"));assertTrue(r.allow);assertEquals(set("a.b"),r.apps);}
 @Test public void emptyIncludeFailsClosed()throws Exception{try{AppRoutePolicy.resolve(opts("appMode","include"),"self.app",set("a.b"));fail();}catch(IllegalArgumentException e){assertEquals("NO_APPS_SELECTED",e.getMessage());}}
 @Test public void onlySelfIncludeFails()throws Exception{try{AppRoutePolicy.resolve(new ConnectionOptions(Parser.obj("appMode","include","packages",Parser.arr("self.app"))),"self.app",set("self.app"));fail();}catch(IllegalArgumentException expected){}}
 @Test public void uninstalledIncludeFails()throws Exception{try{AppRoutePolicy.resolve(new ConnectionOptions(Parser.obj("appMode","include","packages",Parser.arr("gone.app"))),"self.app",set("a.b"));fail();}catch(IllegalArgumentException expected){}}
 @Test public void excludeAlwaysBypassesOwn()throws Exception{AppRoutePolicy r=AppRoutePolicy.resolve(new ConnectionOptions(Parser.obj("appMode","exclude","packages",Parser.arr("a.b"))),"self.app",set("a.b"));assertFalse(r.allow);assertEquals(set("self.app","a.b"),r.apps);}
 @Test public void allIgnoresChosen()throws Exception{AppRoutePolicy r=AppRoutePolicy.resolve(opts("packages",Parser.arr("a.b")),"self.app",set("a.b"));assertEquals(set("self.app"),r.apps);}
 @Test public void boundedRetry(){assertEquals(1000,RetryPolicy.delayMillis(1));assertEquals(2000,RetryPolicy.delayMillis(2));assertEquals(4000,RetryPolicy.delayMillis(3));assertEquals(-1,RetryPolicy.delayMillis(4));assertEquals(-1,RetryPolicy.delayMillis(0));}
 @Test public void debounce(){assertFalse(RetryPolicy.settled(2499,1000));assertTrue(RetryPolicy.settled(2500,1000));}
 @Test public void reconnectMotion(){assertTrue(MotionPolicy.animate("reconnecting",true,true));assertFalse(MotionPolicy.animate("reconnecting",false,true));}
 @Test public void coreUsesOptionsWithoutStorageMutation()throws Exception{JSONObject p=tls();Profile row=new Profile("id","","name","example.invalid",443,"vless",Parser.arr(p).toString());JSONObject c=new JSONObject(CoreConfig.build(row,10080,10081,opts("fragment","tlshello")));assertEquals(2,c.getJSONArray("outbounds").length());assertEquals(1,new JSONArray(row.outbounds).length());assertEquals("proxy",c.getJSONArray("outbounds").getJSONObject(0).getString("tag"));}
}
