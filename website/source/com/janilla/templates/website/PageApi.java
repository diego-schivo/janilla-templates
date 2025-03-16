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

import java.util.stream.Stream;

import com.janilla.cms.CollectionApi;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.MapAndType;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

@Handle(path = "/api/pages")
public class PageApi extends CollectionApi<Page> {

	public MapAndType.TypeResolver typeResolver;

	public PageApi() {
		super(Page.class);
	}

	@Handle(method = "GET")
	public Stream<Page> read(@Bind("slug") String slug) {
		return crud().read(slug != null && !slug.isBlank() ? crud().filter("slug", slug) : crud().list());
	}

	@Handle(method = "GET", path = "(\\d+)/versions")
	public Stream<Version<Page>> versions(long id) {
		var n = Version.class.getSimpleName() + "<" + Page.class.getSimpleName() + ">";
		return persistence.database().perform((ss, ii) -> {
			var ll = ii.perform(n + ".entity", i -> i.list(id).mapToLong(x -> (long) x));
			var c = new Converter(typeResolver);
			var vv = ss.perform(n, s -> ll.mapToObj(x -> {
				var o = s.read(x);
				var s2 = Json.parse((String) o);
				@SuppressWarnings("unchecked")
				var v = (Version<Page>) c.convert(s2, Version.class);
				return v;
			}));
			return vv;
		}, false);
	}
}
