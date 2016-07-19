package ru.flashsafe.api;

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

@Path("/delete")
public class Delete {

  @GET
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response listOrDelete(@Context HttpServletRequest requestContext, @QueryParam("dir_id") int dir_id, @QueryParam("delete") int del, @QueryParam("access_token") String access_token) {
	  if(DBManager.checkAuth(access_token)) {
		  return Response.ok("{\"meta\":{\"code\":\"200\",\"msg\":\"Item " + DBManager.delete(dir_id, del) + " was delete.\"}}", "application/json; charset=UTF-8").build();
	  } else {
		  return Response.ok("{\"meta\":{\"code\":\"423\",\"msg\":\"take_token\"},\"data\":[]}", "application/json; charset=UTF-8").build();
	  }
  }

}
