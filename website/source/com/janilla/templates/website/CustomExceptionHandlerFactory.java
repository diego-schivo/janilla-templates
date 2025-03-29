package com.janilla.templates.website;

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
