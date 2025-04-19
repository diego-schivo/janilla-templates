package com.janilla.templates.blank;

import com.janilla.cms.CmsReflectionJsonIterator;
import com.janilla.persistence.Persistence;

public class CustomReflectionJsonIterator extends CmsReflectionJsonIterator {

	public CustomReflectionJsonIterator(Persistence persistence) {
		super(persistence);
	}
}
