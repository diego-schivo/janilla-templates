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
package com.janilla.templates.blank;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.cms.Cms;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.java.Java;
import com.janilla.json.DollarTypeResolver;
import com.janilla.json.TypeResolver;
import com.janilla.net.Net;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.reflect.Factory;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;

public class BlankTemplate {

	public static final AtomicReference<BlankTemplate> INSTANCE = new AtomicReference<>();

	protected static final Pattern ADMIN = Pattern.compile("/admin(/.*)?");

	public static void main(String[] args) {
		try {
			BlankTemplate a;
			{
				var c = new Properties();
				try (var x = BlankTemplate.class.getResourceAsStream("configuration.properties")) {
					c.load(x);
				}
				if (args.length > 0) {
					var f = args[0];
					if (f.startsWith("~"))
						f = System.getProperty("user.home") + f.substring(1);
					try (var x = Files.newInputStream(Path.of(f))) {
						c.load(x);
					}
				}
				a = new BlankTemplate(c);
			}

			HttpServer s;
			{
				SSLContext c;
				try (var x = Net.class.getResourceAsStream("testkeys")) {
					c = Net.getSSLContext(Map.entry("JKS", x), "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty("blank-template.server.port"));
				s = a.factory.create(HttpServer.class,
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public final Properties configuration;

	public final Path databaseFile;

	public final Predicate<HttpExchange> drafts = x -> ((CustomHttpExchange) x).sessionUser() != null;

	public final Factory factory;

	public final HttpHandler handler;

	public final Persistence persistence;

	public final RenderableFactory renderableFactory;

	public final TypeResolver typeResolver;

	public final List<Class<?>> types;

	public BlankTemplate(Properties configuration) {
		if (!INSTANCE.compareAndSet(null, this))
			throw new IllegalStateException();
		this.configuration = configuration;
		types = Java.getPackageClasses(BlankTemplate.class.getPackageName());
		factory = new Factory(types, INSTANCE::get);
		typeResolver = factory.create(DollarTypeResolver.class);

		{
			var f = configuration.getProperty("blank-template.database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			databaseFile = Path.of(f);
			var b = factory.create(ApplicationPersistenceBuilder.class);
			persistence = b.build();
		}

		renderableFactory = new RenderableFactory();

		{
			var f = factory.create(ApplicationHandlerFactory.class, Map.of("methods", types.stream()
					.flatMap(x -> Arrays.stream(x.getMethods()).filter(y -> !Modifier.isStatic(y.getModifiers()))
							.map(y -> new ClassAndMethod(x, y)))
					.toList(), "files",
					Stream.of("com.janilla.frontend", BlankTemplate.class.getPackageName())
							.flatMap(x -> Java.getPackagePaths(x).stream().filter(Files::isRegularFile)).toList()));
			handler = x -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			};
		}
	}

	public BlankTemplate application() {
		return this;
	}

	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
	public Index index(String path, CustomHttpExchange exchange) {
		switch (path) {
		case "/admin":
			if (exchange.sessionEmail() == null) {
				var rs = exchange.response();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/login");
				return null;
			}
		case "/admin/login":
			if (persistence.crud(User.class).count() == 0) {
				var rs = exchange.response();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/create-first-user");
				return null;
			}
		}
		return new Index(ADMIN.matcher(path).matches() ? "/admin.css" : "/style.css");
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		return Cms.schema(Data.class);
	}

	@Render(template = "index.html")
	public record Index(String href) {
	}
}
