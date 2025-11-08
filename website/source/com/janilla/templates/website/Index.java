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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.http.HttpServer;
import com.janilla.web.Render;

@Render(template = "index.html")
public record Index(String stylesheetHref, Meta meta, @Render(renderer = DataRenderer.class) Map<String, Object> data) {

	public static final String SITE_NAME = "Janilla Website Template";

	public String title() {
		return Stream.of(meta != null ? meta.title() : null, SITE_NAME).filter(x -> x != null && !x.isBlank())
				.collect(Collectors.joining(" | "));
	}

	public Stream<Map.@Render(template = "meta") Entry<String, String>> metaEntries() {
		var r = HttpServer.HTTP_EXCHANGE.get().request();
		var m = meta != null && meta.image() != null
				? WebsiteTemplate.INSTANCE.get().persistence.crud(Media.class).read(meta.image())
				: null;
		var ss = Stream
				.of("description", meta != null ? meta.description() : null, "og:title", title(), "og:description",
						meta != null ? meta.description() : null, "og:url",
						r.getScheme() + "://" + r.getAuthority() + r.getTarget(), "og:site_name", SITE_NAME, "og:image",
						m != null ? r.getScheme() + "://" + r.getAuthority() + m.uri() : null, "og:type", "website")
				.iterator();
		return Stream.<Map.Entry<String, String>>iterate(null,
				_ -> ss.hasNext() ? new AbstractMap.SimpleImmutableEntry<>(ss.next(), ss.next()) : null).skip(1)
				.takeWhile(Objects::nonNull);
	}
}
