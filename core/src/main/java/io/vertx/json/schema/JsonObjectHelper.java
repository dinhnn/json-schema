package io.vertx.json.schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dinhnn on 5/30/16.
 */
public class JsonObjectHelper {
  private static String loadString(InputStream in){
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buff = new byte[64];
      int r;
      while ((r=in.read(buff))!=-1){
        out.write(buff,0,r);
      }
      return out.toString("UTF-8");
    }catch(Throwable e){
      throw new RuntimeException(e);
    }finally {
      try {
        in.close();
      }catch (IOException e){

      }
    }
  }
  public static JsonObject load(InputStream in){
    return new JsonObject(loadString(in));
  }
  public static JsonArray loadJsonArray(InputStream in){
    return new JsonArray(loadString(in));
  }
}
