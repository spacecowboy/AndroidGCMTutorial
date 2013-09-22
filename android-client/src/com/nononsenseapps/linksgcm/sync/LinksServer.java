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

	/**
	 * Change the IP to the address of your server
	 */
	// Server-app uses no prefixes in the URL
	public static final String API_URL = "http://192.168.1.17:5500";
	// Server on App Engine will have a Base URL like this
	//public static final String API_URL = "http://192.168.1.17:8080/_ah/api/links/v1";

	public static class LinkItems {
		String latestTimestamp;
		List<LinkMSG> links;
	}
	
	/**
	 * We could have used LinkItem class directly instead.
	 * But to make it compatible with both servers, I chose
	 * to make this converter class to handle the deleted field.
	 * Converting the integer to boolean for the JSON message.
	 */
	public static class LinkMSG {
		String url;
		String sha;
		boolean deleted;
		String timestamp;
		
		public LinkMSG(LinkItem link) {
			url = link.url;
			sha = link.sha;
			deleted = (link.deleted == 1);
		}
		
		public LinkItem toDBItem() {
			final LinkItem item = new LinkItem();
			item.url = url;
			item.sha = sha;
			item.timestamp = timestamp;
			if (deleted) {
				item.deleted = 1;
			}
			return item;
		}
	}
	
	public static class Dummy {
		// Methods must have return type
	}

	@GET("/links")
	LinkItems listLinks(@Header("Authorization") String token,
			@Query("showDeleted") String showDeleted,
			@Query("timestampMin") String timestampMin);

	@GET("/links/{sha}")
	LinkMSG getLink(@Header("Authorization") String token, @Path("sha") String sha);

	@DELETE("/links/{sha}")
	Dummy deleteLink(@Header("Authorization") String token, @Path("sha") String sha);

	@POST("/links")
	LinkMSG addLink(@Header("Authorization") String token, @Body LinkMSG item);
}
