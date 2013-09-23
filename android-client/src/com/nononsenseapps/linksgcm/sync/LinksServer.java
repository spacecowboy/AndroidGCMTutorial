package com.nononsenseapps.linksgcm.sync;

import java.util.List;

import com.nononsenseapps.linksgcm.database.LinkItem;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface LinksServer {

	public static final String API_URL = "http://links.nononsenseapps.com";

	public static class LinkItems {
		String latestTimestamp;
		List<LinkItem> links;
	}

	public static class RegId {
		public String regid;
	}

	public static class Dummy {
		// Methods must have return type
	}

	@GET("/links")
	LinkItems listLinks(@Header("Bearer") String token,
			@Query("showDeleted") String showDeleted,
			@Query("timestampMin") String timestampMin);

	@GET("/links/{sha}")
	LinkItem getLink(@Header("Bearer") String token, @Path("sha") String sha);

	@DELETE("/links/{sha}")
	Dummy deleteLink(@Header("Bearer") String token, @Path("sha") String sha,
			@Query("regid") String regid);

	@POST("/links")
	LinkItem addLink(@Header("Bearer") String token, @Body LinkItem item,
			@Query("regid") String regid);

	@POST("/registergcm")
	Dummy registerGCM(@Header("Bearer") String token, @Body RegId regid);
}
