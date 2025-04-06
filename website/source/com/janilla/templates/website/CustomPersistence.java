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

import java.util.Set;

import com.janilla.cms.Document;
import com.janilla.cms.CmsPersistence;
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

				@Override
				public <E> void afterCreate(E entity) {
					if (entity instanceof Post p && p.status() == Document.Status.PUBLISHED)
						crud(SearchResult.class)
								.create(Reflection.copy(p,
										new SearchResult(null, new Document.Reference<>(Post.class, p.id()), null, null,
												null, null, null, null, null),
										y -> !Set.of("id", "document").contains(y)));
				}

				@Override
				public <E> void afterUpdate(E entity1, E entity2) {
					if (entity1 instanceof Post p1) {
						var p2 = (Post) entity2;
						var c = crud(SearchResult.class);
						switch (p1.status()) {
						case DRAFT:
							if (p2.status() == p1.status())
								;
							else
								c.create(Reflection.copy(p2,
										new SearchResult(null, new Document.Reference<>(Post.class, p2.id()), null,
												null, null, null, null, null, null),
										y -> !Set.of("id", "document").contains(y)));
							break;
						case PUBLISHED:
							if (p2.status() == p1.status())
								c.update(c.find("document", p2.id()),
										x -> Reflection.copy(p2, x, y -> !Set.of("id", "document").contains(y)));
							else
								c.delete(c.find("document", p2.id()));
							break;
						}
					}
				}

				@Override
				public <E> void afterDelete(E entity) {
					if (entity instanceof Post p && p.status() == Document.Status.PUBLISHED) {
						var c = crud(SearchResult.class);
						c.delete(c.find("document", p.id()));
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
