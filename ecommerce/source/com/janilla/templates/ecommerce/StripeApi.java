/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.templates.ecommerce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.json.Json;
import com.janilla.net.Net;
import com.janilla.persistence.Persistence;
import com.janilla.web.Handle;
import com.janilla.web.UnauthorizedException;

@Handle(path = "/api/stripe")
public class StripeApi {

	public static Map<String, BlockingQueue<Order>> FOO = new ConcurrentHashMap<>();

	public Properties configuration;

	public Persistence persistence;

	@Handle(method = "GET", path = "create-payment-intent")
	public Map<String, Object> createPaymentIntent(String email, Long amount, CustomHttpExchange exchange)
			throws GeneralSecurityException, IOException {
		var u = exchange.sessionUser();
		if (u == null && (email == null || email.isBlank()))
			throw new UnauthorizedException("A user or an email is required for this transaction.");

		if (u != null)
			email = u.email();
		var c = u != null ? u.stripeCustomerId() : null;
		var sc = SSLContext.getInstance("TLSv1.3");
		sc.init(null, null, null);
		var a = "Basic " + Base64.getEncoder()
				.encodeToString((configuration.getProperty("ecommerce-template.stripe.secret-key") + ":").getBytes());

		if (c == null) {
			var rq = new HttpRequest();
			rq.setMethod("GET");
			rq.setTarget(Net.uriString("/v1/customers", Map.entry("email", email)));
			rq.setScheme("https");
			rq.setAuthority("api.stripe.com");
			rq.setHeaderValue("authorization", a);
			@SuppressWarnings("unchecked")
			var m1 = (Map<String, Object>) new HttpClient(sc).send(rq, rs -> {
				try {
					return Json.parse(
							new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			IO.println("m1=" + m1);
			@SuppressWarnings("unchecked")
			var l1 = (List<Object>) m1.get("data");
			@SuppressWarnings("unchecked")
			var m2 = !l1.isEmpty() ? (Map<String, Object>) l1.getFirst() : null;
			c = m2 != null ? (String) m2.get("id") : null;
		}

		if (c == null) {
			var rq = new HttpRequest();
			rq.setMethod("POST");
			rq.setTarget("/v1/customers");
			rq.setScheme("https");
			rq.setAuthority("api.stripe.com");
			rq.setHeaderValue("authorization", a);
			var bb = Net.uriString(null, Map.entry("email", email)).getBytes();
			rq.setHeaderValue("content-length", String.valueOf(bb.length));
			rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
			rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
			@SuppressWarnings("unchecked")
			var m3 = (Map<String, Object>) new HttpClient(sc).send(rq, rs -> {
				try {
					return Json.parse(
							new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			IO.println("m3=" + m3);
			c = (String) m3.get("id");
		}

		if (u == null)
			u = persistence.crud(User.class).create(new User(null, null, email, null, null, null, null,
					Set.of(User.Role.CUSTOMER), c, null, null, null, null, null));
		else if (u.stripeCustomerId() == null) {
			var c0 = c;
			u = persistence.crud(User.class).update(u.id(), x -> x.withStripeCustomerId(c0));
		}

		var rq = new HttpRequest();
		rq.setMethod("POST");
		rq.setTarget("/v1/payment_intents");
		rq.setScheme("https");
		rq.setAuthority("api.stripe.com");
		rq.setHeaderValue("authorization", a);
		var bb = Net
				.uriString(null, Map.entry("customer", c), Map.entry("amount", amount.toString()),
						Map.entry("currency", "usd"), Map.entry("automatic_payment_methods[enabled]", "true"))
				.getBytes();
		rq.setHeaderValue("content-length", String.valueOf(bb.length));
		rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
		@SuppressWarnings("unchecked")
		var m3 = (Map<String, Object>) new HttpClient(sc).send(rq, rs -> {
			try {
				return Json
						.parse(new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		IO.println("m3=" + m3);
		return m3;
	}

	@Handle(path = "webhooks")
	public void webhooks(HttpRequest request) throws IOException {
		var b = request.getBody();
		if (b != null) {
			var s = new String(Channels.newInputStream((ReadableByteChannel) b).readAllBytes());
			IO.println("s=" + s);
			var j = Json.parse(s);
			@SuppressWarnings("unchecked")
			var m = (Map<String, Object>) j;
			var t = m.get("type");
			if (t.equals("payment_intent.succeeded")) {
				@SuppressWarnings("unchecked")
				var m2 = (Map<String, Object>) ((Map<String, Object>) m.get("data")).get("object");
//				IO.println("m2=" + m2);
				var uc = persistence.crud(User.class);
				var u = uc.read(uc.filter("stripeCustomerId", (String) m2.get("customer")).getFirst());
				var o = persistence.crud(Order.class).create(new Order(null, u.id(), (String) m2.get("id"),
						(Long) m2.get("amount"), null, null, Order.Status.PROCESSING, null, null, null, null));
				var q = FOO.computeIfAbsent(o.stripePaymentIntentId(), _ -> new ArrayBlockingQueue<>(1));
				try {
					q.put(o);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
