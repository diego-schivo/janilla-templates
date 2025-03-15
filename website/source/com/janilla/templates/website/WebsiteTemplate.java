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

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpProtocol;
import com.janilla.json.MapAndType;
import com.janilla.net.Net;
import com.janilla.net.Server;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;
import com.janilla.util.Util;
import com.janilla.web.ApplicationHandlerBuilder;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class WebsiteTemplate {

	public static void main(String[] args) {
		try {
			var pp = new Properties();
			try (var is = WebsiteTemplate.class.getResourceAsStream("configuration.properties")) {
				pp.load(is);
				if (args.length > 0) {
					var p = args[0];
					if (p.startsWith("~"))
						p = System.getProperty("user.home") + p.substring(1);
					pp.load(Files.newInputStream(Path.of(p)));
				}
			}
			var wt = new WebsiteTemplate(pp);
			Server s;
			{
				var a = new InetSocketAddress(
						Integer.parseInt(wt.configuration.getProperty("website-template.server.port")));
				SSLContext sc;
				try (var is = Net.class.getResourceAsStream("testkeys")) {
					sc = Net.getSSLContext("JKS", is, "passphrase".toCharArray());
				}
				var p = wt.factory.create(HttpProtocol.class,
						Map.of("handler", wt.handler, "sslContext", sc, "useClientMode", false));
				s = new Server(a, p);
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Properties configuration;

	public Iterable<Class<?>> types;

	public Factory factory;

	public MapAndType.TypeResolver typeResolver;

	public Persistence persistence;

	public HttpHandler handler;

	public WebsiteTemplate(Properties configuration) {
		this.configuration = configuration;
		types = Util.getPackageClasses(getClass().getPackageName()).toList();
		factory = new Factory(types, this);
		typeResolver = factory.create(MapAndType.DollarTypeResolver.class);
		{
			var p = configuration.getProperty("website-template.database.file");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			var pb = factory.create(ApplicationPersistenceBuilder.class, Map.of("databaseFile", Path.of(p)));
			persistence = pb.build();
		}
		handler = factory.create(ApplicationHandlerBuilder.class).build();
	}

	public WebsiteTemplate application() {
		return this;
	}

	@Handle(method = "GET", path = "((?!/api/)/[\\w\\d/-]*)")
	public Index index(String path, CustomHttpExchange exchange) {
		switch (path) {
		case "/admin":
			if (exchange.sessionEmail() == null) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/login");
				return null;
			}
		case "/admin/login":
			if (persistence.crud(User.class).count() == 0) {
				var rs = exchange.getResponse();
				rs.setStatus(307);
				rs.setHeaderValue("cache-control", "no-cache");
				rs.setHeaderValue("location", "/admin/create-first-user");
				return null;
			}
		}
		return new Index();
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Map<String, Map<String, Object>>> schema() {
		var m1 = new LinkedHashMap<String, Map<String, Map<String, Object>>>();
		var q = new ArrayDeque<Class<?>>();
		q.add(Data.class);
		Function<Class<?>, String> f = x -> x.getName().substring(x.getPackageName().length() + 1).replace('$', '.');
		do {
			var c = q.remove();
//			System.out.println("WebsiteTemplate.schema, c=" + c);
			var m2 = new LinkedHashMap<String, Map<String, Object>>();
			Reflection.properties(c).forEach(x -> {
//				System.out.println("WebsiteTemplate.schema, x=" + x);
				var m3 = new LinkedHashMap<String, Object>();
				m3.put("type", f.apply(x.type().isEnum() ? String.class : x.type()));
				List<Class<?>> cc;
				if (x.type() == List.class) {
					var c2 = (Class<?>) ((ParameterizedType) x.genericType()).getActualTypeArguments()[0];
					var apt = x.annotatedType() instanceof AnnotatedParameterizedType y ? y : null;
					var ta = apt != null ? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class) : null;
					if (c2 == Long.class) {
						cc = List.of();
						m3.put("elementTypes", List.of(f.apply(c2)));
						if (ta != null)
							m3.put("referenceType", f.apply(ta.value()[0]));
					} else {
						cc = ta != null ? Arrays.asList(ta.value())
								: c2.isInterface() ? Arrays.asList(c2.getPermittedSubclasses()) : List.of(c2);
						m3.put("elementTypes", cc.stream().map(f).toList());
					}
				} else if (x.type().getPackageName().startsWith("java.")) {
					if (x.type() == Long.class) {
						var ta = x.annotatedType().getAnnotation(Types.class);
						if (ta != null)
							m3.put("referenceType", f.apply(ta.value()[0]));
					}
					cc = List.of();
				} else if (x.type().isEnum()) {
					m3.put("options",
							Arrays.stream(x.type().getEnumConstants()).map(y -> ((Enum<?>) y).name()).toList());
					cc = List.of();
				} else if (!m1.containsKey(f.apply(x.type())))
					cc = List.of(x.type());
				else
					cc = List.of();
				m2.put(x.name(), m3);
				q.addAll(cc);
			});
			m1.put(f.apply(c), m2);
		} while (!q.isEmpty());
		return m1;
	}

	@Render(template = "index.html")
	public record Index() {
	}
}
