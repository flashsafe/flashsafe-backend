package ru.flashsafe.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import ru.flashsafe.db.DBManager;

@Path("/download")
public class Download {

  @GET
  @Consumes(MediaType.APPLICATION_OCTET_STREAM)
  @Produces(MediaType.MULTIPART_FORM_DATA)
  public Response listOrDelete(@Context HttpServletRequest requestContext, @QueryParam("file_id") int file_id, @QueryParam("access_token") String access_token) {
	  if(DBManager.checkAuth(access_token)) {
		  final String file_name = DBManager.getFileNameByID(file_id);
		  StreamingOutput fileStream =  new StreamingOutput() {
	            @Override
	            public void write(java.io.OutputStream output) throws IOException, WebApplicationException {
	                try {
	                    java.nio.file.Path path = Paths.get("./cloud/1/" + file_name);
	                    byte[] data = Files.readAllBytes(path);
	                    output.write(data);
	                    output.flush();
	                } catch (Exception e) {
	                    throw new WebApplicationException("File not found");
	                }
	            }
	        };
	        return Response
	                .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
	                .header("content-disposition","attachment; filename=" + file_name)
	                .build();
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }

}

