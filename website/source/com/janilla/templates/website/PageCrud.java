package com.janilla.templates.website;

import com.janilla.cms.VersionCrud;
import com.janilla.persistence.Persistence;

public class PageCrud extends VersionCrud<Page> {

	public PageCrud(Persistence persistence) {
		super(Page.class, persistence);
	}
}
