package com.janilla.templates.website;

import com.janilla.cms.CollectionApi;
import com.janilla.cms.Document;
import com.janilla.http.HttpExchange;

public abstract class CustomCollectionApi<E extends Document> extends CollectionApi<E> {

	protected CustomCollectionApi(Class<E> type) {
		super(type);
	}

	@Override
	protected boolean drafts(HttpExchange exchange) {
		var e = (CustomHttpExchange) exchange;
		var u = e.sessionUser();
		return u != null;
	}
}
