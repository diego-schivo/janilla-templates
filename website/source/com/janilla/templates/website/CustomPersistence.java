package com.janilla.templates.website;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Index;
import com.janilla.database.KeyAndData;
import com.janilla.database.NameAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;

public class CustomPersistence extends Persistence {

	public CustomPersistence(Database database, Iterable<Class<?>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	protected void createStoresAndIndexes() {
		super.createStoresAndIndexes();
		database.perform((ss, ii) -> {
			var n = Version.class.getSimpleName() + "<" + Page.class.getSimpleName() + ">";
			ss.create(n);
			ii.create(n + ".entity");
			return null;
		}, true);
	}

	@Override
	protected <E> Crud<E> newCrud(Class<E> type) {
		if (type == Page.class) {
			@SuppressWarnings("unchecked")
			var c = (Crud<E>) new PageCrud(this);
			return c;
		}
		return super.newCrud(type);
	}

	@Override
	public <K, V> Index<K, V> newIndex(NameAndData nameAndData) {
		if (nameAndData.name().equals(Version.class.getSimpleName() + "<" + Page.class.getSimpleName() + ">.entity")) {
			@SuppressWarnings("unchecked")
			var z = (Index<K, V>) new Index<Long, Long>(new BTree<>(database.bTreeOrder(), database.channel(),
					database.memory(), KeyAndData.getByteConverter(ByteConverter.LONG), nameAndData.bTree()),
					ByteConverter.LONG);
			return z;
		}
		return super.newIndex(nameAndData);
	}
}
