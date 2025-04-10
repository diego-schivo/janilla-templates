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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.janilla.cms.Document;
import com.janilla.cms.Version;
import com.janilla.cms.Versions;
import com.janilla.http.HttpExchange;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.ReflectionValueIterator;
import com.janilla.json.TokenIterationContext;
import com.janilla.persistence.Persistence;
import com.janilla.web.JsonHandlerFactory;

public class CustomJsonHandlerFactory extends JsonHandlerFactory {

	public Persistence persistence;

	@Override
	protected Iterator<JsonToken<?>> buildJsonIterator(Object object, HttpExchange exchange) {
		var x = new CustomReflectionJsonIterator();
		x.setObject(object);
		x.setIncludeType(true);
		return x;
	}

	protected class CustomReflectionJsonIterator extends ReflectionJsonIterator {

		@Override
		public Iterator<JsonToken<?>> newValueIterator(Object object) {
			var o = stack().peek();
			if (o instanceof Map.Entry<?, ?> kv) {
				var n = (String) kv.getKey();
				if (object instanceof Long l)
					switch (n) {
					case "form":
						object = persistence.crud(Form.class).read(l);
						break;
					case "heroImage", "image", "media":
						object = persistence.crud(Media.class).read(l);
						break;
					case "document":
						object = persistence.crud(Post.class).read(l);
						break;
					}
				else if (object instanceof Document.Reference<?> dr) {
					object = persistence.crud(dr.type()).read(dr.id());
				}
				switch (n) {
				case "relatedPosts":
					if (object instanceof List<?> oo && !oo.isEmpty() && oo.getFirst() instanceof Long) {
						var ll = oo.stream().mapToLong(x -> (long) x).toArray();
						object = persistence.crud(Post.class).read(ll).stream().map(x -> x.withRelatedPosts(null))
								.toList();
					}
					break;
				case "categories":
					if (object instanceof List<?> l && !l.isEmpty() && l.getFirst() instanceof Long)
						object = persistence.crud(Category.class).read(l.stream().mapToLong(x -> (long) x).toArray());
					break;
				}
			}
			return new CustomReflectionValueIterator(this, object);
		}
	}

	protected class CustomReflectionValueIterator extends ReflectionValueIterator {

		public CustomReflectionValueIterator(TokenIterationContext context, Object object) {
			super(context, object);
		}

		@Override
		protected Iterator<JsonToken<?>> newIterator() {
			return object instanceof Class<?> c
					? context.newStringIterator(c.getName().substring(c.getPackageName().length() + 1))
					: super.newIterator();
		}

		@Override
		protected Stream<Entry<String, Object>> entries(Class<?> class0) {
			var kkvv = super.entries(class0);
			var v = class0.getAnnotation(Versions.class);
			if (v == null || !v.drafts())
				kkvv = kkvv.filter(kv -> kv.getKey() != "status");
			if (v != null) {
				var n = Version.class.getSimpleName() + "<" + class0.getSimpleName() + ">.document";
				var vc = persistence.database()
						.perform((_, ii) -> ii.perform(n, i -> i.count(((Document) object).id())), false);
				var kv = Map.entry("versionCount", (Object) vc);
				kkvv = Stream.concat(kkvv, Stream.of(kv));
			}
			return kkvv;
		}
	}
}
