package ru.flashsafe.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ru.flashsafe.db.DBManager;
import ru.flashsafe.fs.FSObject;

@Path("/trash")
public class Trash {

  @GET
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@Context HttpServletRequest requestContext, @QueryParam("dir_id") int dir_id, @QueryParam("access_token") String access_token) {
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
				  response += item + ",";
			  }
			  response = response.substring(0, response.length() - 1);
			  response += "]}";
			  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"ok\"}}," + response, "application/json; charset=UTF-8").build();
		  }
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }

}


