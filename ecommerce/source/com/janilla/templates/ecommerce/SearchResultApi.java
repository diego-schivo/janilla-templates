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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.cms.CollectionApi;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/search-results")
public class SearchResultApi extends CollectionApi<SearchResult> {

	public SearchResultApi() {
		super(SearchResult.class, EcommerceTemplate.DRAFTS);
	}

	@Override
	public List<SearchResult> read() {
		throw new UnsupportedOperationException();
	}

	@Handle(method = "GET")
	public List<SearchResult> read(@Bind("slug") String slug, @Bind("query") String query) {
		var pp = crud().read(slug != null && !slug.isBlank() ? crud().filter("slug", slug) : crud().list());
		return query != null && !query.isBlank() ? pp.stream().filter(x -> {
			var m = x.meta();
			var s = Stream.of(m != null ? m.title() : null, m != null ? m.description() : null)
					.filter(y -> y != null && !y.isBlank()).collect(Collectors.joining(" "));
			return s.toLowerCase().contains(query.toLowerCase());
		}).toList() : pp;
	}
}
