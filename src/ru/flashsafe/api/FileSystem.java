package ru.flashsafe.api;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import ru.flashsafe.db.DBManager;
import ru.flashsafe.fs.FSObject;

@Path("/dir")
public class FileSystem {
	private static final String SERVER_UPLOAD_LOCATION_FOLDER = "./cloud/1/";

  @GET
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response listOrDelete(@Context HttpServletRequest requestContext, @QueryParam("dir_id") int dir_id, @QueryParam("create") String create_dir,  @QueryParam("access_token") String access_token) {
	  if(DBManager.checkAuth(access_token)) {
		  List<FSObject> items = DBManager.getFoldersList(dir_id);
		  if(items == null) {
			  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"null\"},\"data\":[]}", "application/json; charset=UTF-8").build();
		  } else {
			  String response = "\"data\":[";
			  for(FSObject item : items) {
				  
				  response += "{\"id\":" + item.getId() + ",";
				  response += "\"type\":\"" + item.getType() + "\",";
				  response += "\"name\":\"" + item.getName() + "\",";
				  response += "\"format\":\"" + item.getFormat() + "\",";
				  response += "\"size\":" + item.getSize() + ",";
				  response += "\"pincode\":\"" + item.getPincode() + "\",";
				  response += "\"count\":" + item.getCount() + ",";
				  response += "\"create_time\":\"" + item.getCreate_time() + "\",";
				  response += "\"update_time\":\"" + item.getUpdate_time() + "\"}";
				  response += ",";
			  }
			  response = response.substring(0, response.length() - 1);
			  response += "]}";
			  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"ok\"}," + response, "application/json; charset=UTF-8").build();
		  }
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context HttpServletRequest requestContext, @QueryParam("dir_id") int dir_id, @FormParam("create") String create_dir,  @QueryParam("access_token") String access_token) {
	  if(DBManager.checkAuth(access_token)) {
		  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"create_dir\",\"dir_id\":" + DBManager.createDir(dir_id, create_dir) + "}}", "application/json; charset=UTF-8").build();
		  // TODO: need check request parameter - it must be not null and not empty string!
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }
  
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadFile(@Context HttpServletRequest requestContext, @QueryParam("access_token") String access_token, @FormDataParam("dir_id") int dir_id, @FormDataParam("file") InputStream fileInputStream, 
		  @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
	  if(DBManager.checkAuth(access_token)) {
		  int file_id = DBManager.addFile(dir_id, contentDispositionHeader.getFileName(), contentDispositionHeader.getType(), contentDispositionHeader.getSize());
		  String filePath = SERVER_UPLOAD_LOCATION_FOLDER + contentDispositionHeader.getFileName();
		  saveFile(fileInputStream, filePath);
		  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"upload_file\",\"file_id\":" + file_id + "}}", "application/json; charset=UTF-8").build();
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }
  
    private void saveFile(InputStream uploadedInputStream, String serverLocation) {
        try (OutputStream outpuStream = new FileOutputStream(serverLocation)) {
            int read = 0;
            byte[] buffer = new byte[1024];

            while ((read = uploadedInputStream.read(buffer)) != -1) {
                outpuStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

