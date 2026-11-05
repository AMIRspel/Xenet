package app.xenet.client;

import java.util.List;
import org.json.JSONObject;

/** Parse and validate a response before committing the new group. */
final class SubscriptionOperation {
 interface Fetch { String get(String url,int proxyPort)throws Exception; }
 interface Commit { JSONObject save(List<Profile> profiles)throws Exception; }
 static final class Result { final int count;final JSONObject data;Result(int count,JSONObject data){this.count=count;this.data=data;} }
 static Result execute(String id,String url,int proxyPort,Fetch fetch,Commit commit)throws Exception {
  String raw=fetch.get(url,proxyPort);List<Profile> profiles=Parser.batch(raw,id);
  JSONObject data=commit.save(profiles);return new Result(profiles.size(),data);
 }
}
