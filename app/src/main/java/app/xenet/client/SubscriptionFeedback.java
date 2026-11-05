package app.xenet.client;
/** Text never contains the private subscription URL. */
final class SubscriptionFeedback {
 private SubscriptionFeedback(){}
 static String running(){return "Updating subscription… Please wait.";}
 static String success(int count,boolean connected){return "Subscription updated successfully · "+count+(count==1?" server.":" servers.")+(connected?" Stop, then Connect to use the refreshed config.":"");}
 static String failure(String detail,String code){return "Subscription update failed. "+detail+" Error code: "+code+". Your previous servers were not changed.";}
}
