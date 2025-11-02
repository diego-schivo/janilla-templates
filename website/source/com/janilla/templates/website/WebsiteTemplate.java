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
package com.janilla.templates.website;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.cms.DocumentCrud;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.java.Java;
import com.janilla.json.DollarTypeResolver;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.TypeResolver;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.reflect.Factory;
import com.janilla.smtp.SmtpClient;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.Renderer;

public class WebsiteTemplate {

	public static final AtomicReference<WebsiteTemplate> INSTANCE = new AtomicReference<>();

	protected static final Pattern ADMIN = Pattern.compile("/admin(/.*)?");

	protected static final Pattern POSTS = Pattern.compile("/posts(/.*)?");

	public static void main(String[] args) {
		try {
			WebsiteTemplate a;
			{
				var f = new Factory(Java.getPackageClasses(WebsiteTemplate.class.getPackageName()),
						WebsiteTemplate.INSTANCE::get);
				a = f.create(WebsiteTemplate.class,
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
				var p = Integer.parseInt(a.configuration.getProperty("website-template.server.port"));
				s = a.factory.create(HttpServer.class,
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

	protected final Factory factory;

	protected final HttpHandler handler;

	protected final Persistence persistence;

	protected final RenderableFactory renderableFactory;

	protected final SmtpClient smtpClient;

	protected final TypeResolver typeResolver;

	public WebsiteTemplate(Factory factory, Path configurationFile) {
		this.factory = factory;
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		configuration = factory.create(Properties.class, Collections.singletonMap("file", configurationFile));
		typeResolver = factory.create(DollarTypeResolver.class);

		{
			var f = configuration.getProperty("website-template.database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			databaseFile = Path.of(f);
			var b = factory.create(ApplicationPersistenceBuilder.class);
			persistence = b.build();
		}

		renderableFactory = new RenderableFactory();
		smtpClient = factory.create(SmtpClient.class,
				Stream.of("host", "port", "username", "password").collect(Collectors.toMap(x -> x, x -> {
					var y = configuration.getProperty("website-template.mail." + x);
					return x.equals("port") ? Integer.parseInt(y) : y;
				})));

		{
			var f = factory.create(ApplicationHandlerFactory.class, Map.of("methods", types().stream()
					.flatMap(x -> Arrays.stream(x.getMethods()).filter(y -> !Modifier.isStatic(y.getModifiers()))
							.map(y -> new ClassAndMethod(x, y)))
					.toList(), "files",
					Stream.of("com.janilla.frontend", WebsiteTemplate.class.getPackageName())
							.flatMap(x -> Java.getPackagePaths(x).stream().filter(Files::isRegularFile)).toList()));
			handler = x -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			};
		}
	}

	public WebsiteTemplate application() {
		return this;
	}

	public Properties configuration() {
		return configuration;
	}

	public Path databaseFile() {
		return databaseFile;
	}

	public Factory factory() {
		return factory;
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
		return factory.types();
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

		Meta m2;
		var m3 = new LinkedHashMap<String, Object>();
		m3.put("/api/redirects", persistence.crud(Redirect.class).read(persistence.crud(Redirect.class).list()));
		m3.put("/api/header", persistence.crud(Header.class).read(1L));
		var m = POSTS.matcher(path);
		if (m.matches()) {
			var c = ((DocumentCrud<Long, Post>) persistence.crud(Post.class));
			var d = drafts.test(exchange);
			if (m.groupCount() == 1) {
				var pp = c.read(c.list(), d);
				m2 = null;
				m3.put("/api/posts", pp);
			} else {
				var s = m.group(1).substring(1);
				var pp = c.read(c.filter(d ? "slugDraft" : "slug", s), d);
				m2 = !pp.isEmpty() ? pp.get(0).meta() : null;
				m3.put("/api/posts?slug=" + s, pp);
			}
		} else
			switch (path) {
			case "/account":
				m2 = null;
				break;
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

	@Render(template = "index.html")
	public record Index(String href, Meta meta, @Render(renderer = DataRenderer.class) Map<String, Object> data) {

		public static final String SITE_NAME = "Janilla Website Template";

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
					m != null ? r.getScheme() + "://" + r.getAuthority() + m.uri() : null, "og:type", "website")
					.iterator();
			return Stream.<Map.Entry<String, String>>iterate(null,
					_ -> ss.hasNext() ? new AbstractMap.SimpleImmutableEntry<>(ss.next(), ss.next()) : null).skip(1)
					.takeWhile(Objects::nonNull);
		}
	}

	public static class DataRenderer<T> extends Renderer<T> {

		@Override
		public String apply(T value) {
			return Json.format(INSTANCE.get().factory.create(ReflectionJsonIterator.class,
					Map.of("object", value, "includeType", true)));
		}
	}
}
