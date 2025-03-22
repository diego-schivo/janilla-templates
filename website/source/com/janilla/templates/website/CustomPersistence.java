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

import java.lang.reflect.Modifier;

import com.janilla.cms.Entity;
import com.janilla.cms.EntityCrud;
import com.janilla.cms.Version;
import com.janilla.cms.Versions;
import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Index;
import com.janilla.database.KeyAndData;
import com.janilla.database.NameAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.persistence.Store;

public class CustomPersistence extends Persistence {

	public CustomPersistence(Database database, Iterable<Class<?>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	protected void createStoresAndIndexes() {
		super.createStoresAndIndexes();
		for (var t : configuration.cruds().keySet())
			if (t.isAnnotationPresent(Versions.class))
				database.perform((ss, ii) -> {
					var n = Version.class.getSimpleName() + "<" + t.getSimpleName() + ">";
					ss.create(n);
					ii.create(n + ".entity");
					return null;
				}, true);
	}

	@Override
	protected <E> Crud<E> newCrud(Class<E> type) {
		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class)) {
			@SuppressWarnings("unchecked")
			var t = (Class<? extends Entity>) type;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			var c = (Crud<E>) new EntityCrud(t, this, type.isAnnotationPresent(Versions.class));
			return c;
		}
		return null;
	}

	@Override
	public <K, V> Index<K, V> newIndex(NameAndData nameAndData) {
		if (nameAndData.name().startsWith(Version.class.getSimpleName() + "<")
				&& nameAndData.name().endsWith(">.entity")) {
			@SuppressWarnings("unchecked")
			var i = (Index<K, V>) new Index<Long, Object[]>(
					new BTree<>(database.bTreeOrder(), database.channel(), database.memory(),
							KeyAndData.getByteConverter(ByteConverter.LONG), nameAndData.bTree()),
					ByteConverter.of(Version.class, "-updatedAt", "id"));
			return i;
		}
		return super.newIndex(nameAndData);
	}
}
