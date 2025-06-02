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
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.cms.DocumentCrud;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpServer;
import com.janilla.json.Json;
import com.janilla.json.MapAndType;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Factory;
import com.janilla.smtp.SmtpClient;
import com.janilla.util.EntryList;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.Renderer;

public class EcommerceTemplate {

	public static final Predicate<HttpExchange> DRAFTS = x -> ((CustomHttpExchange) x).sessionUser() != null;

	public static EcommerceTemplate INSTANCE;

	protected static final Pattern PRODUCTS = Pattern.compile("/products(/.*)?");

	public static void main(String[] args) {
		try {
			var pp = new Properties();
			try (var is = EcommerceTemplate.class.getResourceAsStream("configuration.properties")) {
				pp.load(is);
				if (args.length > 0) {
					var p = args[0];
					if (p.startsWith("~"))
						p = System.getProperty("user.home") + p.substring(1);
					pp.load(Files.newInputStream(Path.of(p)));
				}
			}
			new EcommerceTemplate(pp);
			HttpServer s;
			{
				SSLContext sc;
				try (var is = Net.class.getResourceAsStream("testkeys")) {
					sc = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
				}
				s = INSTANCE.factory.create(HttpServer.class, Map.of("sslContext", sc, "handler", INSTANCE.handler));
			}
			var p = Integer.parseInt(INSTANCE.configuration.getProperty("ecommerce-template.server.port"));
			s.serve(new InetSocketAddress(p));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Path databaseFile;

	public Factory factory;

	public HttpHandler handler;

	public Persistence persistence;

	public RenderableFactory renderableFactory;

	public SmtpClient smtpClient;

	public MapAndType.TypeResolver typeResolver;

	public Iterable<Class<?>> types;

	public EcommerceTemplate(Properties configuration) {
		INSTANCE = this;
		this.configuration = configuration;
		types = Util.getPackageClasses(getClass().getPackageName()).toList();
		factory = new Factory(types, this);
		typeResolver = factory.create(MapAndType.DollarTypeResolver.class);
		{
			var p = configuration.getProperty("ecommerce-template.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			databaseFile = Path.of(p);
			var pb = factory.create(ApplicationPersistenceBuilder.class);
			persistence = pb.build();
		}
		renderableFactory = new RenderableFactory();
		smtpClient = factory.create(SmtpClient.class,
				Map.of("host", configuration.getProperty("ecommerce-template.mail.host"), "port",
						Integer.parseInt(configuration.getProperty("ecommerce-template.mail.port")), "username",
						configuration.getProperty("ecommerce-template.mail.username"), "password",
						configuration.getProperty("ecommerce-template.mail.password")));
		handler = factory.create(ApplicationHandlerBuilder.class).build();
	}

	public EcommerceTemplate application() {
		return this;
	}

	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
	public Index index(String path, CustomHttpExchange exchange) {
		if (path.equals("/admin") || path.startsWith("/admin/")) {
			if (path.equals("/admin/login")) {
				if (persistence.crud(User.class).count() == 0) {
					var rs = exchange.getResponse();
					rs.setStatus(307);
					rs.setHeaderValue("cache-control", "no-cache");
					rs.setHeaderValue("location", "/admin/create-first-user");
					return null;
				}
			} else if (exchange.sessionEmail() == null) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/login");
				return null;
			} else if (!Set.of("/admin/logout", "/admin/unauthorized").contains(path)) {
				if (exchange.sessionUser() == null || !exchange.sessionUser().hasRole(User.Role.ADMIN)) {
					var rs = exchange.getResponse();
					rs.setStatus(307);
					rs.setHeaderValue("cache-control", "no-cache");
					rs.setHeaderValue("location", "/admin/unauthorized");
					return null;
				}
			}
			return new Index("/admin.css", null, Map.of());
		}

		if (path.equals("/account")) {
			if (exchange.sessionUser() == null) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/login");
				return null;
			}
		}

		var m = PRODUCTS.matcher(path);
		Meta m2;
		Map<String, Object> m3 = new LinkedHashMap<>();
		m3.put("/api/redirects", persistence.crud(Redirect.class).read(persistence.crud(Redirect.class).list()));
		m3.put("/api/header", persistence.crud(Header.class).read(1));
		if (m.matches()) {
			var c = ((DocumentCrud<Product>) persistence.crud(Product.class));
			var d = DRAFTS.test(exchange);
			if (m.groupCount() == 1) {
				var pp = c.read(c.list(), d);
				m2 = null;
				m3.put("/api/products", pp);
			} else {
				var s = m.group(1).substring(1);
				var pp = c.read(c.filter(d ? "slugDraft" : "slug", s), d);
				m2 = !pp.isEmpty() ? pp.get(0).meta() : null;
				m3.put("/api/products?slug=" + s, pp);
			}
		} else {
			var c = ((DocumentCrud<Page>) persistence.crud(Page.class));
			var d = DRAFTS.test(exchange);
			var s = path.substring(1);
			if (s.isEmpty())
				s = "home";
			var pp = c.read(c.filter(d ? "slugDraft" : "slug", s), d);
			m2 = !pp.isEmpty() ? pp.get(0).meta() : null;
			m3.put("/api/pages?slug=" + s, pp);
		}
		m3.put("/api/footer", persistence.crud(Footer.class).read(1));
		return new Index("/style.css", m2, m3);
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		return Cms.schema(Data.class);
	}

	@Handle(method = "POST", path = "/api/seed")
	public void seed() throws IOException {
		((CustomPersistence) persistence).seed();
	}

	@Handle(method = "GET", path = "/api/config")
	public Map<String, String> config() {
		return Map.of("publishableKey", configuration.getProperty("ecommerce-template.stripe.publishable-key"));
	}

	@Handle(method = "GET", path = "/api/create-payment-intent")
	public Map<String, Object> createPaymentIntent(String email, Long amount)
			throws GeneralSecurityException, IOException {
		var sc = SSLContext.getInstance("TLSv1.3");
		sc.init(null, null, null);
		var a = "Basic " + Base64.getEncoder()
				.encodeToString((configuration.getProperty("ecommerce-template.stripe.secret-key") + ":").getBytes());

		var rq = new HttpRequest();
		rq.setMethod("GET");
		var el = new EntryList<String, String>();
		el.add("email", email);
		rq.setTarget("/v1/customers?" + Net.formatQueryString(el));
		rq.setScheme("https");
		rq.setAuthority("api.stripe.com");
		rq.setHeaderValue("authorization", a);
		var oo1 = new Object[1];
		new HttpClient(sc).send(rq, rs -> {
			try {
				oo1[0] = Json
						.parse(new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		@SuppressWarnings("unchecked")
		var m1 = (Map<String, Object>) oo1[0];
		System.out.println("m1=" + m1);
		@SuppressWarnings("unchecked")
		var l1 = (List<Object>) m1.get("data");
		@SuppressWarnings("unchecked")
		var m2 = !l1.isEmpty() ? (Map<String, Object>) l1.getFirst() : null;
		var c = m2 != null ? (String) m2.get("id") : null;

		if (c == null) {
			rq = new HttpRequest();
			rq.setMethod("POST");
			rq.setTarget("/v1/customers");
			rq.setScheme("https");
			rq.setAuthority("api.stripe.com");
			rq.setHeaderValue("authorization", a);
			el = new EntryList<String, String>();
			el.add("email", email);
			var bb = Net.formatQueryString(el).getBytes();
			rq.setHeaderValue("content-length", String.valueOf(bb.length));
			rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
			rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
			var oo2 = new Object[1];
			new HttpClient(sc).send(rq, rs -> {
				try {
					oo2[0] = Json.parse(
							new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			@SuppressWarnings("unchecked")
			var m3 = (Map<String, Object>) oo2[0];
			System.out.println("m3=" + m3);
			c = (String) m3.get("id");
		}

		rq = new HttpRequest();
		rq.setMethod("POST");
		rq.setTarget("/v1/payment_intents");
		rq.setScheme("https");
		rq.setAuthority("api.stripe.com");
		rq.setHeaderValue("authorization", a);
		el = new EntryList<String, String>();
		el.add("customer", c);
		el.add("amount", amount.toString());
		el.add("currency", "usd");
		el.add("automatic_payment_methods[enabled]", "true");
		var bb = Net.formatQueryString(el).getBytes();
		rq.setHeaderValue("content-length", String.valueOf(bb.length));
		rq.setHeaderValue("content-type", "application/x-www-form-urlencoded");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
		var oo2 = new Object[1];
		new HttpClient(sc).send(rq, rs -> {
			try {
				oo2[0] = Json
						.parse(new String(Channels.newInputStream((ReadableByteChannel) rs.getBody()).readAllBytes()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		@SuppressWarnings("unchecked")
		var m3 = (Map<String, Object>) oo2[0];
		System.out.println("m3=" + m3);
		return m3;
	}

	@Render(template = "index.html")
	public record Index(String href, Meta meta, @Render(renderer = DataRenderer.class) Map<String, Object> data) {

		public static final String SITE_NAME = "Janilla Ecommerce Template";

		public String title() {
			return Stream.of(meta != null ? meta.title() : null, SITE_NAME).filter(x -> x != null && !x.isBlank())
					.collect(Collectors.joining(" | "));
		}

		public Stream<Map.@Render(template = "meta") Entry<String, String>> metaEntries() {
			var r = HttpServer.HTTP_EXCHANGE.get().getRequest();
			var m = meta != null && meta.image() != null ? INSTANCE.persistence.crud(Media.class).read(meta.image())
					: null;
			var ss = Stream.of("description", meta != null ? meta.description() : null, "og:title", title(),
					"og:description", meta != null ? meta.description() : null, "og:url",
					r.getScheme() + "://" + r.getAuthority() + r.getTarget(), "og:site_name", SITE_NAME, "og:image",
					m != null ? r.getScheme() + "://" + r.getAuthority() + m.uri() : null, "og:type", "ecommerce")
					.iterator();
			return Stream.<Map.Entry<String, String>>iterate(null,
					_ -> ss.hasNext() ? new AbstractMap.SimpleEntry<>(ss.next(), ss.next()) : null).skip(1)
					.takeWhile(Objects::nonNull);
		}
	}

	public static class DataRenderer<T> extends Renderer<T> {

		@Override
		public String apply(T value) {
			var tt = INSTANCE.factory.create(ReflectionJsonIterator.class);
			tt.setObject(value);
			tt.setIncludeType(true);
			return Json.format(tt);
		}
	}
}
