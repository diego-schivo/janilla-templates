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
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.cms.DocumentCrud;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DependencyInjector;
import com.janilla.java.Java;
import com.janilla.json.DollarTypeResolver;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.TypeResolver;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.smtp.SmtpClient;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.Renderer;

public class EcommerceTemplate {

	public static final AtomicReference<EcommerceTemplate> INSTANCE = new AtomicReference<>();

	protected static final Pattern PRODUCTS = Pattern.compile("/products(/.*)?");

	protected static final Pattern SHOP = Pattern.compile("/shop(/.*)?");

	public static void main(String[] args) {
		try {
			EcommerceTemplate a;
			{
				var f = new DependencyInjector(Java.getPackageClasses(EcommerceTemplate.class.getPackageName()),
						EcommerceTemplate.INSTANCE::get);
				a = f.create(EcommerceTemplate.class,
						Java.hashMap("factory", f, "configurationFile",
								args.length > 0 ? Path.of(
										args[0].startsWith("~") ? System.getProperty("user.home") + args[0].substring(1)
												: args[0])
										: null));
			}

			HttpServer s;
			{
				SSLContext c;
				try (var x = Net.class.getResourceAsStream("testkeys")) {
					c = Net.getSSLContext(Map.entry("JKS", x), "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty("ecommerce-template.server.port"));
				s = a.injector.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected final Properties configuration;

	protected final Path databaseFile;

	protected final Predicate<HttpExchange> drafts = x -> ((CustomHttpExchange) x).sessionUser() != null;

	protected final DependencyInjector injector;

	protected final HttpHandler handler;

	protected final Persistence persistence;

	protected final RenderableFactory renderableFactory;

	protected final SmtpClient smtpClient;

	protected final TypeResolver typeResolver;

	public EcommerceTemplate(DependencyInjector injector, Path configurationFile) {
		this.injector = injector;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		configuration = injector.create(Properties.class, Collections.singletonMap("file", configurationFile));
		typeResolver = injector.create(DollarTypeResolver.class);

		{
			var f = configuration.getProperty("ecommerce-template.database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			databaseFile = Path.of(f);
			var b = injector.create(ApplicationPersistenceBuilder.class);
			persistence = b.build();
		}

		renderableFactory = new RenderableFactory();
		smtpClient = injector.create(SmtpClient.class,
				Stream.of("host", "port", "username", "password").collect(Collectors.toMap(x -> x, x -> {
					var y = configuration.getProperty("ecommerce-template.mail." + x);
					return x.equals("port") ? Integer.parseInt(y) : y;
				})));

		{
			var f = injector.create(ApplicationHandlerFactory.class, Map.of("methods", types().stream()
					.flatMap(x -> Arrays.stream(x.getMethods()).filter(y -> !Modifier.isStatic(y.getModifiers()))
							.map(y -> new ClassAndMethod(x, y)))
					.toList(), "files",
					Stream.of("com.janilla.frontend", EcommerceTemplate.class.getPackageName())
							.flatMap(x -> Java.getPackagePaths(x).stream().filter(Files::isRegularFile)).toList()));
			handler = x -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			};
		}
	}

	public EcommerceTemplate application() {
		return this;
	}

	public Properties configuration() {
		return configuration;
	}

	public Path databaseFile() {
		return databaseFile;
	}

	public DependencyInjector injector() {
		return injector;
	}

	public HttpHandler handler() {
		return handler;
	}

	public Persistence persistence() {
		return persistence;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	public Collection<Class<?>> types() {
		return injector.types();
	}

	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
	public Index index(String path, CustomHttpExchange exchange) {
		if (path.equals("/admin") || path.startsWith("/admin/")) {
			switch (path) {
			case "/admin/login":
				if (persistence.crud(User.class).count() == 0) {
					var rs = exchange.response();
					rs.setStatus(307);
					rs.setHeaderValue("cache-control", "no-cache");
					rs.setHeaderValue("location", "/admin/create-first-user");
					return null;
				}
				break;
			case "/admin/create-first-user":
				break;
			default:
				if (exchange.sessionEmail() == null) {
					var rs = exchange.response();
					rs.setStatus(307);
					rs.setHeaderValue("cache-control", "no-cache");
					rs.setHeaderValue("location", "/admin/login");
					return null;
				} else if (!Set.of("/admin/logout", "/admin/unauthorized").contains(path)) {
					if (exchange.sessionUser() == null || !exchange.sessionUser().hasRole(User.Role.ADMIN)) {
						var rs = exchange.response();
						rs.setStatus(307);
						rs.setHeaderValue("cache-control", "no-cache");
						rs.setHeaderValue("location", "/admin/unauthorized");
						return null;
					}
				}
				break;
			}
			return new Index("/admin.css", null, Map.of());
		}

		if (path.equals("/account")) {
			if (exchange.sessionUser() == null) {
				var rs = exchange.response();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/login");
				return null;
			}
		}

		Meta m2;
		var m3 = new LinkedHashMap<String, Object>();
		m3.put("/api/header", persistence.crud(Header.class).read(1L));
		Matcher m;
		if ((m = PRODUCTS.matcher(path)).matches()) {
			var c = ((DocumentCrud<Long, Product>) persistence.crud(Product.class));
			var d = INSTANCE.get().drafts.test(exchange);
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
		} else if ((m = SHOP.matcher(path)).matches()) {
			m2 = null;
			var cc = persistence.crud(Category.class);
			var cc2 = cc.read(cc.list());
			m3.put("/api/categories", cc2);
			var s = m.groupCount() == 2 ? m.group(1).substring(1) : null;
			var c = s != null ? cc2.stream().filter(x -> x.slug().equals(s)).findFirst().orElseThrow() : null;
			var pp = ProductApi.INSTANCE.read(null, c != null ? new Long[] { c.id() } : null, null, exchange);
			m3.put(c != null ? "/api/products?category=" + c.id() : "/api/products", pp);
		} else
			switch (path) {
			case "/account":
				m2 = null;
				break;
			case "/orders":
				m2 = null;
				var oc = persistence.crud(Order.class);
				var oo = oc.read(oc.filter("orderedBy", exchange.sessionUser().id()));
				m3.put("/api/orders", oo);
				break;
//			case "/shop": {
//				m2 = null;
//				var c1 = persistence.crud(Product.class);
//				var pp = c1.read(c1.list());
//				m3.put("/api/products", pp);
//				var c2 = persistence.crud(Category.class);
//				var cc = c2.read(c2.list());
//				m3.put("/api/categories", cc);
//			}
//				break;
			default:
				var c = (DocumentCrud<Long, Page>) persistence.crud(Page.class);
				var d = drafts.test(exchange);
				var s = path.substring(1);
				if (s.isEmpty())
					s = "home";
				var pp = c.read(c.filter(d ? "slugDraft" : "slug", s), d);
				m2 = !pp.isEmpty() ? pp.get(0).meta() : null;
				m3.put("/api/pages?slug=" + s, pp);
				break;
			}
		m3.put("/api/footer", persistence.crud(Footer.class).read(1L));
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

	@Render(template = "index.html")
	public record Index(String href, Meta meta, @Render(renderer = DataRenderer.class) Map<String, Object> data) {

		public static final String SITE_NAME = "Janilla Ecommerce Template";

		public String title() {
			return Stream.of(meta != null ? meta.title() : null, SITE_NAME).filter(x -> x != null && !x.isBlank())
					.collect(Collectors.joining(" | "));
		}

		public Stream<Map.@Render(template = "meta") Entry<String, String>> metaEntries() {
			var r = HttpServer.HTTP_EXCHANGE.get().request();
			var m = meta != null && meta.image() != null
					? INSTANCE.get().persistence.crud(Media.class).read(meta.image())
					: null;
			var ss = Stream.of("description", meta != null ? meta.description() : null, "og:title", title(),
					"og:description", meta != null ? meta.description() : null, "og:url",
					r.getScheme() + "://" + r.getAuthority() + r.getTarget(), "og:site_name", SITE_NAME, "og:image",
					m != null ? r.getScheme() + "://" + r.getAuthority() + m.uri() : null, "og:type", "ecommerce")
					.iterator();
			return Stream.<Map.Entry<String, String>>iterate(null,
					_ -> ss.hasNext() ? new AbstractMap.SimpleImmutableEntry<>(ss.next(), ss.next()) : null).skip(1)
					.takeWhile(Objects::nonNull);
		}
	}

	public static class DataRenderer<T> extends Renderer<T> {

		@Override
		public String apply(T value) {
			return Json.format(INSTANCE.get().injector.create(ReflectionJsonIterator.class,
					Map.of("object", value, "includeType", true)));
		}
	}
}
