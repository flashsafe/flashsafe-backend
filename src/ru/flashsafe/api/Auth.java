package ru.flashsafe.api;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ru.flashsafe.db.DBManager;


@Path("/auth")
public class Auth {

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response response(@Context HttpServletRequest requestContext, @FormParam("id") int device_id, @FormParam("access_token") String access_token) {
	  if(access_token == null || access_token.equals("")) {
		  HashMap<String,String> data = DBManager.getAuthToken(device_id, requestContext.getRemoteHost());
		  return Response.ok("{\"meta\":{\"code\":\"200\"},\"data\":{\"timestamp\":\"" + data.get("timestamp") + "\",\"token\":\"" + data.get("auth_token") + "\"}}",
				  "application/json; charset=UTF-8").build();
	  } else {
		  HashMap<String,String> data = DBManager.getAccessToken(device_id, access_token, requestContext.getRemoteHost());
		  if(data != null) {
			  return Response.ok("{\"meta\":{\"code\":\"200\"},\"data\":{\"timestamp\":\"" + data.get("timestamp") + "\",\"token\":\"" + data.get("access_token")
			  		+ "\",\"timeout\":" + Integer.parseInt(data.get("timeout")) + "}}", "application/json; charset=UTF-8").build();
		  } else {
			  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"Fail token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
		  }
	  }
  }
  
  

}
