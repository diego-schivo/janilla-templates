package com.janilla.templates.website;

import java.util.List;

import com.janilla.cms.CmsReflectionJsonIterator;
import com.janilla.persistence.Persistence;

public class CustomReflectionJsonIterator extends CmsReflectionJsonIterator {

	public CustomReflectionJsonIterator(Persistence persistence) {
		super(persistence);
	}

	@Override
	protected List<?> list(List<?> list) {
		return super.list(list).stream()
				.map(x -> x instanceof Post y && y.relatedPosts() != null ? y.withRelatedPosts(null) : x).toList();
	}
}
