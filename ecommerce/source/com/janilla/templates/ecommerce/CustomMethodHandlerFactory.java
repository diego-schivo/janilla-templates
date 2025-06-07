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

import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.janilla.http.HttpExchange;
import com.janilla.json.MapAndType;
import com.janilla.web.ForbiddenException;
import com.janilla.web.MethodHandlerFactory;

public class CustomMethodHandlerFactory extends MethodHandlerFactory {

	protected static final Set<String> GUEST_POST = Set.of("/api/form-submissions", "/api/users/first-register",
			"/api/users/forgot-password", "/api/users/login", "/api/users/reset-password", "/api/stripe/webhooks");

	protected static final Set<String> USER_LOGIN_LOGOUT = Set.of("/api/users/login", "/api/users/logout");

	public Properties configuration;

	public MapAndType.DollarTypeResolver typeResolver;

	@Override
	protected void handle(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.getRequest();
		if (rq.getPath().startsWith("/api/") && !rq.getMethod().equals("GET")) {
			if (rq.getPath().startsWith("/api/search-results"))
				throw new ForbiddenException("Forbidden");
			else if (!GUEST_POST.contains(rq.getPath())) {
				var ex = (CustomHttpExchange) exchange;
				if (rq.getPath().equals("/api/users/logout"))
					ex.requireSessionEmail();
				else {
					var m = Pattern.compile("/api/users/(\\d+)").matcher(rq.getPath());
					var u = ex.sessionUser();
					if (m.matches() && u != null && u.id().equals(Long.parseLong(m.group(1))))
						;
					else
						ex.requireSessionRole(User.Role.ADMIN);
				}
			}
		}

//		if (Boolean.parseBoolean(configuration.getProperty("ecommerce-template.live-demo")))
//			if (!rq.getMethod().equals("GET") && !USER_LOGIN_LOGOUT.contains(rq.getPath()))
//				throw new HandleException(new MethodBlockedException());

//		if (rq.getPath().startsWith("/api/"))
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		super.handle(invocation, exchange);
	}

	@Override
	protected MapAndType.TypeResolver resolver(Class<? extends MapAndType.TypeResolver> class0) {
		if (class0 == MapAndType.DollarTypeResolver.class)
			return typeResolver;
		return super.resolver(class0);
	}
}
