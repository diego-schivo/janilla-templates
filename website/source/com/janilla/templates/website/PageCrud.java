package com.janilla.templates.website;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class PageCrud extends Crud<Page> {

	public PageCrud(Persistence persistence) {
		super(Page.class, persistence);
	}

	@Override
	public Page create(Page entity) {
		return createVersion(() -> super.create(entity));
	}

	@Override
	public Page update(long id, UnaryOperator<Page> operator) {
		return createVersion(() -> super.update(id, operator));
	}

	@Override
	public Page delete(long id) {
		return deleteVersions(() -> super.delete(id));
	}

	public Page createVersion(Supplier<Page> supplier) {
		return persistence.database().perform((ss, ii) -> {
			var n = Version.class.getSimpleName() + "<" + Page.class.getSimpleName() + ">";
			var p = supplier.get();
			var l = ss.perform(n, s -> s.create(x -> {
				var v = Reflection.copy(new Entity(x), new Version<>(null, p));
				System.out.println("PageCrud.update, v=" + v);
				return format(v);
			}));
			ii.perform(n + ".entity", i -> {
				i.add(p.id(), new Object[] { l });
				return null;
			});
			return p;
		}, true);
	}

	public Page deleteVersions(Supplier<Page> supplier) {
		return persistence.database().perform((ss, ii) -> {
			var n = Version.class.getSimpleName() + "<" + Page.class.getSimpleName() + ">";
			var p = supplier.get();
			var ll = ii.perform(n + ".entity", i -> {
				var oo = i.list(p.id()).toArray();
				i.remove(p.id(), (Object[]) oo);
				return oo;
			});
			ss.perform(n, s -> {
				for (var l : ll)
					s.delete((long) l);
				return null;
			});
			return p;
		}, true);
	}
}
