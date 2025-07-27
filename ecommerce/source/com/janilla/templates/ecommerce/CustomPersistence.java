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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.janilla.cms.CmsPersistence;
import com.janilla.database.Database;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Entity;
import com.janilla.reflect.Factory;
import com.janilla.zip.Zip;

public class CustomPersistence extends CmsPersistence {

	protected static final Crud.Observer PRODUCT_OBSERVER = new Crud.Observer() {

		@Override
		public <E> E beforeCreate(E entity) {
			@SuppressWarnings("unchecked")
			var e = (E) ((Product) entity).withNonNullVariantIds();
			return e;
		}

		@Override
		public <E> E beforeUpdate(E entity) {
			return beforeCreate(entity);
		}
	};

	protected static final Crud.Observer USER_OBSERVER = new Crud.Observer() {

		@Override
		public <E> E beforeCreate(E entity) {
			var u = (User) entity;
			var c = u.cart();
			if (c != null)
				c = c.withNonNullItemIds();
			@SuppressWarnings("unchecked")
			var e = (E) (c != u.cart() ? u.withCart(c) : u);
			return e;
		}

		@Override
		public <E> E beforeUpdate(E entity) {
			return beforeCreate(entity);
		}
	};

	public Properties configuration;

	public Factory factory;

	public CustomPersistence(Database database, Collection<Class<? extends Entity<?>>> types,
			TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var x = super.newCrud(type);
		if (x != null) {
			if (type == Product.class)
				x.observers().add(PRODUCT_OBSERVER);
			else if (type == User.class)
				x.observers().add(USER_OBSERVER);
		}
		return x;
	}

	public void seed() throws IOException {
		for (var t : List.of(Page.class, Product.class, Media.class, Category.class, User.class, Header.class,
				Footer.class)) {
			database.perform((ss, _) -> {
				var c = crud(t);
				c.delete(c.list());
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
//			IO.println("o=" + o);
			sd = (SeedData) factory.create(Converter.class).convert(o, SeedData.class);
//			IO.println("sd=" + sd);
		}
		for (var x : sd.pages())
			crud(Page.class).create(x);
		for (var x : sd.products())
			crud(Product.class).create(x);
		for (var x : sd.media())
			crud(Media.class).create(x);
		for (var x : sd.categories())
			crud(Category.class).create(x);
		for (var x : sd.users())
			crud(User.class).create(x);
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
		var s = Zip.zipFileSystem(u).getPath("/");
//		var d = Files.createDirectories(databaseFile.getParent().resolve("ecommerce-template-upload"));
		var ud = configuration.getProperty("ecommerce-template.upload.directory");
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
