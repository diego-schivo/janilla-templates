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

import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.cms.CollectionApi;
import com.janilla.http.HttpExchange;
import com.janilla.json.Jwt;
import com.janilla.web.Handle;

@Handle(path = "/api/users")
public class UserApi extends CollectionApi<User> {

	public Properties configuration;

	public UserApi() {
		super(User.class);
	}

	@Handle(method = "GET")
	public Stream<User> read() {
		return crud().read(crud().list());
	}

	@Handle(method = "POST", path = "login")
	public User login(User user, CustomHttpExchange exchange) {
		var u = crud().read(crud().find("email", user.email()));
		if (u == null || !u.password().equals(user.password()))
			return null;
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("blank-template.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Handle(method = "POST", path = "logout")
	public void logout(User user, CustomHttpExchange exchange) {
		exchange.setSessionCookie(null);
	}

	@Handle(method = "GET", path = "me")
	public User me(CustomHttpExchange exchange) {
		return exchange.sessionUser();
	}

	@Handle(method = "POST", path = "first-register")
	public User firstRegister(User user, CustomHttpExchange exchange) {
		var u = crud().create(user);
		var h = Map.of("alg", "HS256", "typ", "JWT");
		var p = Map.of("loggedInAs", u.email());
		var t = Jwt.generateToken(h, p, configuration.getProperty("blank-template.jwt.key"));
		exchange.setSessionCookie(t);
		return u;
	}

	@Override
	protected boolean drafts(HttpExchange exchange) {
		// TODO Auto-generated method stub
		return false;
	}
}
