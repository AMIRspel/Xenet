package app.xenet.client;
import org.json.*;import java.util.*;
public final class Profile {
 public final String id,group,name,host,protocol,outbounds;public final int port;
 public Profile(String id,String group,String name,String host,int port,String protocol,String outbounds){this.id=id;this.group=group;this.name=name;this.host=host;this.port=port;this.protocol=protocol;this.outbounds=outbounds;}
 public JSONObject json()throws Exception{return new JSONObject().put("id",id).put("group",group).put("name",name).put("host",host).put("port",port).put("protocol",protocol).put("outbounds",new JSONArray(outbounds));}
 public static Profile from(JSONObject o)throws Exception{return new Profile(o.getString("id"),o.optString("group"),o.getString("name"),o.optString("host"),o.optInt("port"),o.getString("protocol"),o.getJSONArray("outbounds").toString());}
}
