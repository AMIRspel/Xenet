package app.xenet.client;
import java.util.*;import org.json.*;
/** Storage edits are small, atomic and have no network calls under the store lock. */
final class Catalog {
 static List<Profile> retainIds(JSONArray old,String group,List<Profile> incoming)throws Exception {
  Map<String,ArrayDeque<String>> ids=new HashMap<>();
  for(int i=0;i<old.length();i++){Profile p=Profile.from(old.getJSONObject(i));if(!p.group.equals(group))continue;String key=p.protocol+"\n"+canonical(new JSONArray(p.outbounds));ArrayDeque<String> q=ids.get(key);if(q==null){q=new ArrayDeque<>();ids.put(key,q);}q.add(p.id);}
  List<Profile> result=new ArrayList<>();for(Profile p:incoming){ArrayDeque<String> q=ids.get(p.protocol+"\n"+canonical(new JSONArray(p.outbounds)));String id=q!=null&&!q.isEmpty()?q.remove():p.id;result.add(new Profile(id,group,p.name,p.host,p.port,p.protocol,p.outbounds));}return result;
 }
 static String canonical(Object value)throws Exception {if(value instanceof JSONObject){JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();Iterator<String> i=o.keys();while(i.hasNext())keys.add(i.next());Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String k:keys)b.append(JSONObject.quote(k)).append(':').append(canonical(o.get(k))).append(',');return b.append('}').toString();}if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++)b.append(canonical(a.get(i))).append(',');return b.append(']').toString();}return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);}
 static void deleteGroup(JSONObject data,String id)throws Exception{JSONArray kept=new JSONArray(),old=data.getJSONArray("profiles");for(int i=0;i<old.length();i++){JSONObject p=old.getJSONObject(i);if(!p.optString("group").equals(id))kept.put(p);}JSONArray groups=new JSONArray(),g=data.getJSONArray("groups");for(int i=0;i<g.length();i++)if(!g.getJSONObject(i).getString("id").equals(id))groups.put(g.get(i));data.put("profiles",kept).put("groups",groups);}
 static void deleteProfile(JSONObject data,String id)throws Exception{JSONArray kept=new JSONArray(),a=data.getJSONArray("profiles");for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).getString("id").equals(id))kept.put(a.get(i));data.put("profiles",kept);}
}
