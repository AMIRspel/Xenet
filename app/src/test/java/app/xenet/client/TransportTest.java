package app.xenet.client;
import org.junit.*;import static org.junit.Assert.*;import java.net.*;import java.util.concurrent.*;import java.io.*;
/** Actual host sockets test new OkHttp code, not Android VPN or TLS. */
public class TransportTest {
 ServerSocket server;ExecutorService network,closer;ScheduledExecutorService timer;int port;
 @Before public void init()throws Exception{server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));network=Executors.newCachedThreadPool();closer=Executors.newCachedThreadPool();timer=Executors.newSingleThreadScheduledExecutor();port=server.getLocalPort();}
 @After public void clean()throws Exception{server.close();network.shutdownNow();closer.shutdownNow();timer.shutdownNow();}
 interface Reply {void send(OutputStream out)throws Exception;}
 void serve(String headers,Reply reply){network.submit(()->{try(Socket socket=server.accept()){socket.setSoTimeout(3000);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));String l;while((l=in.readLine())!=null&&!l.isEmpty()){}OutputStream out=socket.getOutputStream();out.write((headers+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));out.flush();reply.send(out);}catch(Exception ignored){}});}
 Profile profile()throws Exception{return Parser.parse("vless://id@example.com:443","");}
 @Test public void actualBytesMeasured()throws Exception{serve("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: "+Net.BYTES,out->{byte[] b=new byte[16384];for(int n=0;n<Net.BYTES;n+=b.length)out.write(b);});Tasks t=new Tasks(closer);Net.Result r=Net.measureAt(profile(),port,t,t.begin("real"),timer,"http://sample.invalid/file");assertEquals(Net.BYTES,r.bytes);assertTrue(r.mbps>0);assertEquals("",r.error);}
 @Test public void httpErrorNotSuccess()throws Exception{serve("HTTP/1.1 503 Unavailable\r\nContent-Length: 0",out->{});Tasks t=new Tasks(closer);Net.Result r=Net.measureAt(profile(),port,t,t.begin("real"),timer,"http://sample.invalid/file");assertEquals("HTTP_503",r.error);assertEquals(-1,r.mbps,0);}
 @Test public void htmlNotSpeedSample()throws Exception{serve("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 0",out->{});Tasks t=new Tasks(closer);Net.Result r=Net.measureAt(profile(),port,t,t.begin("real"),timer,"http://sample.invalid/file");assertEquals("RESPONSE",r.error);}
 @Test public void cancelBlockedRead()throws Exception{CountDownLatch reading=new CountDownLatch(1);serve("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: "+Net.BYTES,out->{out.write(new byte[1024]);out.flush();reading.countDown();Thread.sleep(5000);});Tasks t=new Tasks(closer);Tasks.Job j=t.begin("real");Profile p=profile();Future<Boolean> f=network.submit(()->{try{Net.measureAt(p,port,t,j,timer,"http://sample.invalid/file");return false;}catch(InterruptedException expected){return true;}});assertTrue(reading.await(3,TimeUnit.SECONDS));t.stop();assertTrue(f.get(1,TimeUnit.SECONDS));}
}
