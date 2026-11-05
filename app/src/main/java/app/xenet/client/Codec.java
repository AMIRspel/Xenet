package app.xenet.client;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class Codec {
 public static String decode(String s){try{return URLDecoder.decode(s.replace("+","%2B"),"UTF-8");}catch(Exception e){throw new IllegalArgumentException("BAD_ESCAPE");}}
 public static String b64(String s){String x=s.trim().replace('-','+').replace('_','/').replaceAll("\\s","");ByteArrayOutputStream out=new ByteArrayOutputStream();int acc=0,bits=0;String chars="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";for(int i=0;i<x.length();i++){char ch=x.charAt(i);if(ch=='=')break;int v=chars.indexOf(ch);if(v<0)throw new IllegalArgumentException("BAD_BASE64");acc=(acc<<6)|v;bits+=6;if(bits>=8){bits-=8;out.write((acc>>bits)&255);}}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
 public static String label(String raw){int n=raw.indexOf('#');return n<0?"":decode(raw.substring(n+1));}
 public static Map<String,String> query(String raw){Map<String,String> m=new HashMap<>();int a=raw.indexOf('?');if(a<0)return m;int b=raw.indexOf('#',a);String q=raw.substring(a+1,b<0?raw.length():b);for(String part:q.split("&")){String[] kv=part.split("=",2);m.put(decode(kv[0]),kv.length==2?decode(kv[1]):"");}return m;}
 public static String bounded(String s,int n){if(s.length()>n)throw new IllegalArgumentException("INPUT_TOO_LARGE");return s;}
}
