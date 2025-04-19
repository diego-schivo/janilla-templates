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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.janilla.cms.CmsPersistence;
import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.database.Database;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.reflect.Reflection;

public class CustomPersistence extends CmsPersistence {

	private Crud.Observer searchObserver;

	public CustomPersistence(Database database, Iterable<Class<?>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	protected Crud.Observer searchObserver() {
		if (searchObserver == null)
			searchObserver = new Crud.Observer() {

				private Set<Class<?>> types = Arrays.stream(Reflection.property(SearchResult.class, "document")
						.annotatedType().getAnnotation(Types.class).value()).collect(Collectors.toSet());

				@Override
				public <E> void afterCreate(E entity) {
					var d = (Document) entity;
					var dc = d.getClass();
					if (types.contains(dc) && d.status() == Document.Status.PUBLISHED)
						crud(SearchResult.class)
								.create(Reflection.copy(d,
										new SearchResult(null, new Document.Reference<>(dc, d.id()), null, null, null,
												null, null, null, null, null),
										y -> !Set.of("id", "document").contains(y)));
				}

				@Override
				public <E> void afterUpdate(E entity1, E entity2) {
					var d1 = (Document) entity1;
					var d2 = (Document) entity2;
					var dc = d1.getClass();
					if (types.contains(dc)) {
						var c = crud(SearchResult.class);
						switch (d1.status()) {
						case DRAFT:
							if (d2.status() == d1.status())
								;
							else
								c.create(Reflection.copy(d2,
										new SearchResult(null, new Document.Reference<>(dc, d2.id()), null, null, null,
												null, null, null, null, null),
										y -> !Set.of("id", "document").contains(y)));
							break;
						case PUBLISHED:
							if (d2.status() == d1.status())
								c.update(c.find("document", new Document.Reference<>(dc, d2.id())),
										x -> Reflection.copy(d2, x, y -> !Set.of("id", "document").contains(y)));
							else
								c.delete(c.find("document", d2.id()));
							break;
						}
					}
				}

				@Override
				public <E> void afterDelete(E entity) {
					var d = (Document) entity;
					var dc = d.getClass();
					if (types.contains(dc) && d.status() == Document.Status.PUBLISHED) {
						var c = crud(SearchResult.class);
						c.delete(c.find("document", new Document.Reference<>(dc, d.id())));
					}
				}
			};
		return searchObserver;
	}

	@Override
	protected <E> Crud<E> newCrud(Class<E> type) {
		var x = super.newCrud(type);
		if (x != null)
			x.observers().add(searchObserver());
		return x;
	}
}
