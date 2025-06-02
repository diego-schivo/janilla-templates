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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.janilla.http.HttpRequest;
import com.janilla.json.Json;
import com.janilla.persistence.Persistence;
import com.janilla.web.Handle;

@Handle(path = "/api/stripe")
public class StripeApi {

	public static Map<String, BlockingQueue<Order>> FOO = new ConcurrentHashMap<>();

	public Persistence persistence;

	@Handle(path = "webhooks")
	public void webhooks(HttpRequest request) throws IOException {
		var b = request.getBody();
		if (b != null) {
			var s = new String(Channels.newInputStream((ReadableByteChannel) b).readAllBytes());
			System.out.println("s=" + s);
			var j = Json.parse(s);
			var m = Json.asMap(j);
			var t = m.get("type");
			if (t.equals("payment_intent.succeeded")) {
				var m2 = Json.asMap(Json.asMap(m.get("data")).get("object"));
				var o = persistence.crud(Order.class).create(
						new Order(null, (String) m2.get("id"), (Long) m2.get("amount"), null, null, null, null));
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
