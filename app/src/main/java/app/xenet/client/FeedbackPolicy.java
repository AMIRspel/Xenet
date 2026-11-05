package app.xenet.client;
/** Show only messages that require action or convey a non-obvious consequence. */
public final class FeedbackPolicy {
 private FeedbackPolicy(){}
 public enum Event {SELECTED,THEME,COLLAPSED,IMPORTED,DELETED,SUB_UPDATED,ROUTING_SAVED,CONNECTION_SAVED,DEFAULTS_RESTORED,PRIVATE_COPIED}
 public static String message(Event event){if(event==null)return "";switch(event){
 case ROUTING_SAVED:return "App choices saved. Stop, then Connect to apply them.";
 case CONNECTION_SAVED:return "Settings saved. Stop, then Connect to apply them.";
 case DEFAULTS_RESTORED:return "Connection defaults restored. App choices kept. Stop, then Connect to apply.";
 case PRIVATE_COPIED:return "Config copied. Keep it private.";
 default:return "";
 }}
}
