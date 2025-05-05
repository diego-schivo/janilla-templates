package com.janilla.templates.ecommerce;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;

public class Foo {

	public static void main(String[] args) {
		try {
			var sc = SSLContext.getInstance("TLSv1.3");
			sc.init(null, null, null);
			var rq = new HttpRequest();
			rq.setMethod("GET");
			rq.setTarget("/v1/products?limit=3");
			rq.setScheme("https");
			rq.setAuthority("api.stripe.com");
			rq.setHeaderValue("authorization",
					"Basic *");
			new HttpClient(sc).send(rq, rs -> {
				try {
					System.out.println(
							new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
