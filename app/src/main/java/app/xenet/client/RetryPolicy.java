package app.xenet.client;
public final class RetryPolicy {private RetryPolicy(){}public static int delayMillis(int attempt){return delayMillis(attempt,3,1000);}public static int delayMillis(int attempt,int maximum,int base){return attempt<1||attempt>maximum?-1:(int)Math.min(30000L,((long)base)<<(attempt-1));}public static boolean settled(long now,long lastChange){return now-lastChange>=1500;}}
