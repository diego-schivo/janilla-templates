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

import com.janilla.http.HttpExchange;
import com.janilla.web.Error;
import com.janilla.web.ExceptionHandlerFactory;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebHandlerFactory;

public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

	public WebHandlerFactory mainFactory;

	public RenderableFactory renderableFactory;

	@Override
	protected boolean handle(Error error, HttpExchange exchange) {
		super.handle(error, exchange);
		var m = exchange.getException().getMessage();
		var r = renderableFactory.createRenderable(null, m);
		var h = mainFactory.createHandler(r, exchange);
		h.handle(exchange);
		return true;
	}
}
