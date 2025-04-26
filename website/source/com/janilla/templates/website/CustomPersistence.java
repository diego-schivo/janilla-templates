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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.janilla.cms.CmsPersistence;
import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.database.Database;
import com.janilla.io.IO;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;

public class CustomPersistence extends CmsPersistence {

	private Crud.Observer searchObserver;

	public Properties configuration;

	public Factory factory;

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

	public void seed() throws IOException {
		for (var t : new Class<?>[] { Page.class, Post.class, Media.class, Category.class, User.class, Redirect.class,
				Form.class, FormSubmission.class, SearchResult.class, Header.class, Footer.class }) {
			database.perform((ss, _) -> {
				var c = crud(t);
				c.delete(c.list()).forEach(_ -> {
				});
				ss.perform(t.getSimpleName(), s -> {
					s.getAttributes().clear();
					return null;
				});
				return null;
			}, true);
		}

		SeedData sd;
		try (var is = getClass().getResourceAsStream("seed-data.json")) {
			var s = new String(is.readAllBytes());
			var o = Json.parse(s);
			sd = (SeedData) factory.create(Converter.class).convert(o, SeedData.class);
		}
		for (var x : sd.pages())
			crud(Page.class).create(x);
		for (var x : sd.posts())
			crud(Post.class).create(x);
		for (var x : sd.media())
			crud(Media.class).create(x);
		for (var x : sd.categories())
			crud(Category.class).create(x);
		for (var x : sd.users())
			crud(User.class).create(x);
		for (var x : sd.redirects())
			crud(Redirect.class).create(x);
		for (var x : sd.forms())
			crud(Form.class).create(x);
		for (var x : sd.formSubmissions())
			crud(FormSubmission.class).create(x);
		for (var x : sd.searchResults())
			crud(SearchResult.class).create(x);
		crud(Header.class).create(sd.header());
		crud(Footer.class).create(sd.footer());

		var r = getClass().getResource("seed-data.zip");
		URI u;
		try {
			u = r.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		if (!u.toString().startsWith("jar:"))
			u = URI.create("jar:" + u);
		var s = IO.zipFileSystem(u).getPath("/");
//		var d = Files.createDirectories(databaseFile.getParent().resolve("website-template-upload"));
		var ud = configuration.getProperty("website-template.upload.directory");
		if (ud.startsWith("~"))
			ud = System.getProperty("user.home") + ud.substring(1);
		var d = Files.createDirectories(Path.of(ud));
		Files.walkFileTree(s, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var t = d.resolve(s.relativize(file).toString());
				Files.copy(file, t, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
