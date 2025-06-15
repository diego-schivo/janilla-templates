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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.cms.CollectionApi;
import com.janilla.http.HttpExchange;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/products")
public class ProductApi extends CollectionApi<Long, Product> {

	public static ProductApi INSTANCE;

	public ProductApi() {
		super(Product.class, EcommerceTemplate.DRAFTS);
		if (INSTANCE != null)
			throw new RuntimeException();
		INSTANCE = this;
	}

	@Override
	public List<Product> read() {
		throw new UnsupportedOperationException();
	}

	@Handle(method = "GET")
	public List<Product> read(@Bind("slug") String slug, @Bind("categories") Long[] categories,
			@Bind("query") String query, HttpExchange exchange) {
		List<Product> pp;
		{
			var d = drafts.test(exchange);
			var m = new LinkedHashMap<String, Object[]>();
			if (slug != null && !slug.isBlank())
				m.put(d ? "slugDraft" : "slug", new Object[] { slug });
			if (categories != null && categories.length > 0)
				m.put("categories", categories);
			pp = crud().read(!m.isEmpty() ? crud().filter(m, 0, -1).ids() : crud().list(), d);
		}
		return query != null && !query.isBlank() ? pp.stream().filter(x -> {
			var m = x.meta();
			var s = Stream.of(m != null ? m.title() : null, m != null ? m.description() : null)
					.filter(y -> y != null && !y.isBlank()).collect(Collectors.joining(" "));
			return s.toLowerCase().contains(query.toLowerCase());
		}).toList() : pp;
	}
}
