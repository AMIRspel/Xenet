package app.xenet.client;
import java.util.*;
/** Never accidentally turn an empty allowlist into an unrestricted tunnel. */
public final class AppRoutePolicy {
 public final boolean allow;public final Set<String> apps;
 private AppRoutePolicy(boolean a,Set<String> p){allow=a;apps=Collections.unmodifiableSet(p);}
 public static AppRoutePolicy resolve(ConnectionOptions o,String own,Set<String> installed){Set<String> p=new TreeSet<>(o.packages);p.retainAll(installed);p.remove(own);if(o.appMode.equals("include")){if(p.isEmpty())throw new IllegalArgumentException("NO_APPS_SELECTED");return new AppRoutePolicy(true,p);}if(o.appMode.equals("all"))p.clear();p.add(own);return new AppRoutePolicy(false,p);}
}
