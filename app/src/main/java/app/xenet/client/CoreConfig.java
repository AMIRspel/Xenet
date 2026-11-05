package app.xenet.client;
import org.json.*;
public final class CoreConfig {
 public static String build(Profile p,int httpPort,int socksPort)throws Exception {
  return build(p,httpPort,socksPort,ConnectionOptions.defaults());
 }
 public static String build(Profile p,int httpPort,int socksPort,ConnectionOptions options)throws Exception {
  if(httpPort<1024||httpPort>65535||socksPort<1024||socksPort>65535||httpPort==socksPort)throw new IllegalArgumentException("BAD_PORT");
  JSONArray out=options.apply(new JSONArray(p.outbounds));Parser.allowed(out.getJSONObject(0).getString("protocol"));out.getJSONObject(0).put("tag","proxy");
  return Parser.obj("log",Parser.obj("loglevel","none"),"outbounds",out,"inbounds",Parser.arr(
   Parser.obj("tag","http-local","listen","127.0.0.1","port",httpPort,"protocol","http","settings",new JSONObject()),
   Parser.obj("tag","socks-local","listen","127.0.0.1","port",socksPort,"protocol","socks","settings",Parser.obj("auth","noauth","udp",true))),
   "routing",Parser.obj("domainStrategy","AsIs","rules",Parser.arr(Parser.obj("type","field","inboundTag",Parser.arr("http-local","socks-local"),"outboundTag","proxy")))).toString();
 }
}
