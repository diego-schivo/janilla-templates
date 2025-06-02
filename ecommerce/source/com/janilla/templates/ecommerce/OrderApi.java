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
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.janilla.cms.CollectionApi;
import com.janilla.http.HttpResponse;
import com.janilla.json.Json;
import com.janilla.web.Handle;

@Handle(path = "/api/orders")
public class OrderApi extends CollectionApi<Order> {

	public OrderApi() {
		super(Order.class, EcommerceTemplate.DRAFTS);
	}

	@Handle(method = "GET", path = "poll")
	public void poll(String stripePaymentIntentId, HttpResponse response) throws IOException, InterruptedException {
		response.setHeaderValue(":status", "200");
		response.setHeaderValue("content-type", "text/event-stream");
		response.setHeaderValue("cache-control", "no-cache");
		var ch = (WritableByteChannel) response.getBody();
		var q = StripeApi.FOO.computeIfAbsent(stripePaymentIntentId, _ -> new ArrayBlockingQueue<>(1));
		for (;;) {
			var o = q.poll(5, TimeUnit.SECONDS);
			if (o != null) {
//				System.out.println("OutputApi.read, e=" + e);
				var s = format(new Event("order", o));
//				System.out.println("OutputApi.read, s=" + s);
				ch.write(ByteBuffer.wrap(s.getBytes()));
			} else
				ch.write(ByteBuffer.wrap(format(new Event("ping", Map.of("time", new Date()))).getBytes()));
		}
	}

	protected static String format(Event event) {
//		System.out.println("event=" + event);
		var sb = new StringBuilder();
		if (event.type() != null)
			sb.append("event: ").append(event.type()).append("\n");
		sb.append("data: ").append(event.type() != null ? Json.format(event.data(), true) : event.data())
				.append("\n\n");
		return sb.toString();
	}
}
