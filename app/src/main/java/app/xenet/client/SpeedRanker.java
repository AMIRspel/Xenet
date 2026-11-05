package app.xenet.client;
import java.util.*;import org.json.*;
/** Rank only within a group. Valid measured downloads first; never synthetic speed. */
final class SpeedRanker {
 static int category(JSONObject r){if(r==null)return 1;double s=r.optDouble("mbps",-1);return r.optString("error").isEmpty()&&s>=0&&!Double.isNaN(s)&&!Double.isInfinite(s)?0:2;}
 static List<Profile> sort(List<Profile> source,Map<String,JSONObject> results){List<Profile> copy=new ArrayList<>(source);Collections.sort(copy,(a,b)->{JSONObject x=results.get(a.id),y=results.get(b.id);int cx=category(x),cy=category(y);if(cx!=cy)return Integer.compare(cx,cy);if(cx!=0)return 0;int speed=Double.compare(y.optDouble("mbps"),x.optDouble("mbps"));if(speed!=0)return speed;long xp=x.optLong("ping",-1),yp=y.optLong("ping",-1);return Long.compare(xp<0?Long.MAX_VALUE:xp,yp<0?Long.MAX_VALUE:yp);});return copy;}
}
