package app.xenet.client;
import org.json.*;import java.net.*;import java.util.*;
/** Independently authored URI-to-Xray-outbound parser. Never falls back to a different transport. */
public final class Parser {
 static JSONObject obj(Object... kv)throws Exception{JSONObject o=new JSONObject();for(int i=0;i<kv.length;i+=2)o.put((String)kv[i],kv[i+1]);return o;}
 static JSONArray arr(Object... values){JSONArray a=new JSONArray();for(Object v:values)a.put(v);return a;}
 static String val(Map<String,String> q,String k,String d){String v=q.get(k);return v==null||v.isEmpty()?d:v;}
 static int port(String s){try{int n=Integer.parseInt(s);if(n>0&&n<=65535)return n;}catch(Exception ignored){}throw new IllegalArgumentException("BAD_PORT");}
 public static List<Profile> batch(String input,String group)throws Exception{
  input=Codec.bounded(input.trim(),2*1024*1024);if(input.isEmpty())throw new IllegalArgumentException("EMPTY_INPUT");
  if(input.startsWith("{"))return Collections.singletonList(parse(input,group));
  if(!input.contains("://"))input=Codec.b64(input).trim();
  List<Profile> rows=new ArrayList<>();for(String line:input.split("\\r?\\n")){line=line.trim();if(line.isEmpty())continue;if(rows.size()>=100)throw new IllegalArgumentException("LIMIT_100");rows.add(parse(line,group));}
  if(rows.isEmpty())throw new IllegalArgumentException("EMPTY_INPUT");return rows;
 }
 public static Profile parse(String raw,String group)throws Exception {
  raw=Codec.bounded(raw.trim(),65536);String name=Codec.label(raw),host="",protocol;int p=0;JSONArray outs;JSONObject outbound;
  if(raw.startsWith("{")){
   JSONObject root=new JSONObject(raw);outs=root.optJSONArray("outbounds");if(outs==null)outs=arr(root);
   if(outs.length()==0)throw new IllegalArgumentException("NO_OUTBOUND");outbound=outs.getJSONObject(0);protocol=outbound.getString("protocol");allowed(protocol);
   if(root.has("inbounds")||root.has("routing")||root.has("api"))throw new IllegalArgumentException("OUTBOUND_JSON_ONLY");
   name=root.optString("remarks",root.optString("name","Xray JSON"));
   JSONObject settings=outbound.optJSONObject("settings");if(settings!=null){JSONArray servers=settings.optJSONArray("vnext");if(servers==null)servers=settings.optJSONArray("servers");if(servers!=null&&servers.length()>0){host=servers.getJSONObject(0).optString("address");p=servers.getJSONObject(0).optInt("port");}}
  } else if(raw.startsWith("vmess://")){
   String encoded=raw.substring(8).split("#",2)[0];JSONObject v=new JSONObject(Codec.b64(encoded));host=v.getString("add");p=port(v.get("port").toString());protocol="vmess";if(name.isEmpty())name=v.optString("ps","VMess");
   JSONObject user=obj("id",v.getString("id"),"alterId",v.optInt("aid",0),"security",v.optString("scy","auto"));
   outbound=obj("protocol",protocol,"settings",obj("vnext",arr(obj("address",host,"port",p,"users",arr(user)))));
   Map<String,String> q=new HashMap<>();q.put("type",v.optString("net","tcp"));q.put("security",v.optString("tls","none"));for(String k:new String[]{"host","path","sni","fp","alpn"})q.put(k,v.optString(k));outbound.put("streamSettings",stream(q));outs=arr(outbound);
  } else {
   int sc=raw.indexOf("://");if(sc<1)throw new IllegalArgumentException("BAD_SCHEME");String scheme=raw.substring(0,sc).toLowerCase(Locale.ROOT);String body=raw.substring(sc+3);Map<String,String> q=Codec.query(raw);
   String authority=body.split("[?#]",2)[0];if(scheme.equals("ss")&&!authority.contains("@"))authority=Codec.b64(authority);
   int at=authority.lastIndexOf('@');String user=at<0?"":authority.substring(0,at);String endpoint=at<0?authority:authority.substring(at+1);if(endpoint.endsWith("/"))endpoint=endpoint.substring(0,endpoint.length()-1);
   if(endpoint.startsWith("[")){int close=endpoint.indexOf(']');if(close<0||close+1>=endpoint.length()||endpoint.charAt(close+1)!=':')throw new IllegalArgumentException("BAD_HOST");host=endpoint.substring(1,close);p=port(endpoint.substring(close+2));}
   else{int colon=endpoint.lastIndexOf(':');if(colon<1)throw new IllegalArgumentException("BAD_HOST");host=endpoint.substring(0,colon);p=port(endpoint.substring(colon+1));}
   if(host.isEmpty()||host.contains("/")||host.contains(" "))throw new IllegalArgumentException("BAD_HOST");
   if(!host.contains(":"))host=IDN.toASCII(host);
   switch(scheme){
    case "vless":protocol="vless";JSONObject u=obj("id",Codec.decode(user),"encryption",val(q,"encryption","none"));if(q.containsKey("flow"))u.put("flow",q.get("flow"));outbound=obj("protocol",protocol,"settings",obj("vnext",arr(obj("address",host,"port",p,"users",arr(u)))));break;
    case "trojan":protocol="trojan";outbound=obj("protocol",protocol,"settings",obj("servers",arr(obj("address",host,"port",p,"password",Codec.decode(user)))));if(!q.containsKey("security"))q.put("security","tls");break;
    case "ss":protocol="shadowsocks";if(q.containsKey("plugin"))throw new IllegalArgumentException("SS_PLUGIN_UNSUPPORTED");String auth=Codec.decode(user);if(!auth.contains(":"))auth=Codec.b64(auth);int c=auth.indexOf(':');if(c<1)throw new IllegalArgumentException("BAD_SS_AUTH");outbound=obj("protocol",protocol,"settings",obj("servers",arr(obj("address",host,"port",p,"method",auth.substring(0,c),"password",auth.substring(c+1)))));break;
    case "socks":case "socks5":case "http":case "https":protocol=scheme.startsWith("socks")?"socks":"http";JSONObject server=obj("address",host,"port",p);if(!user.isEmpty()){String[] pair=Codec.decode(user).split(":",2);if(pair.length!=2)throw new IllegalArgumentException("BAD_PROXY_AUTH");server.put("users",arr(obj("user",pair[0],"pass",pair[1])));}outbound=obj("protocol",protocol,"settings",obj("servers",arr(server)));if(scheme.equals("https"))q.put("security","tls");break;
    default:throw new IllegalArgumentException("UNSUPPORTED_PROTOCOL");
   }
   outbound.put("streamSettings",stream(q));outs=arr(outbound);
  }
  if(name.isEmpty())name=protocol.toUpperCase(Locale.ROOT);if(name.length()>160)name=name.substring(0,160);
  outbound.put("tag","proxy");return new Profile(UUID.randomUUID().toString(),group,name,host,p,protocol,outs.toString());
 }
 static void allowed(String protocol){if(!Arrays.asList("vmess","vless","trojan","shadowsocks","socks","http","wireguard").contains(protocol))throw new IllegalArgumentException("UNSUPPORTED_PROTOCOL");}
 static JSONObject stream(Map<String,String> q)throws Exception {
  String type=val(q,"type","tcp"),security=val(q,"security","none");if(type.equals("raw"))type="tcp";JSONObject o=obj("network",type,"security",security);
  switch(type){
   case "tcp":if("http".equals(q.get("headerType")))o.put("tcpSettings",obj("header",obj("type","http","request",obj("path",arr(val(q,"path","/")),"headers",obj("Host",arr(val(q,"host","")))))));break;
   case "ws":o.put("wsSettings",obj("path",val(q,"path","/"),"headers",obj("Host",val(q,"host",""))));break;
   case "grpc":o.put("grpcSettings",obj("serviceName",val(q,"serviceName",val(q,"path","")),"multiMode","multi".equals(q.get("mode")),"authority",val(q,"authority","")));break;
   case "httpupgrade":o.put("httpupgradeSettings",obj("path",val(q,"path","/"),"host",val(q,"host","")));break;
   case "xhttp":JSONObject x=obj("path",val(q,"path","/"),"host",val(q,"host",""),"mode",val(q,"mode","auto"));if(q.containsKey("extra"))x.put("extra",new JSONObject(q.get("extra")));o.put("xhttpSettings",x);break;
   case "kcp":o.put("kcpSettings",obj("seed",val(q,"seed",""),"header",obj("type",val(q,"headerType","none"))));break;
   default:throw new IllegalArgumentException("UNSUPPORTED_TRANSPORT");
  }
  if(security.equals("tls")){JSONObject tls=obj("serverName",val(q,"sni",val(q,"serverName","")),"allowInsecure","1".equals(q.get("allowInsecure"))||"true".equals(q.get("allowInsecure")));if(q.containsKey("fp"))tls.put("fingerprint",q.get("fp"));if(q.containsKey("alpn"))tls.put("alpn",new JSONArray(Arrays.asList(q.get("alpn").split(","))));o.put("tlsSettings",tls);}
  else if(security.equals("reality")){if(!q.containsKey("pbk"))throw new IllegalArgumentException("REALITY_KEY_MISSING");o.put("realitySettings",obj("serverName",val(q,"sni",""),"fingerprint",val(q,"fp","chrome"),"publicKey",q.get("pbk"),"shortId",val(q,"sid",""),"spiderX",val(q,"spx","/")));}
  else if(!security.equals("none")&&!security.isEmpty())throw new IllegalArgumentException("UNSUPPORTED_SECURITY");return o;
 }
}
