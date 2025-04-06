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
import CmsAdmin from "./cms-admin.js";

export default class CustomCmsAdmin extends CmsAdmin {

	static get observedAttributes() {
		return ["data-email", "data-path"];
	}

	static get templateName() {
		return "cms-admin";
	}

	constructor() {
		super();
	}

	label(path) {
		switch (path) {
			case "hero":
			case "hero.richText":
				return null;
		}
		return super.label(path);
	}

	headers(entitySlug) {
		switch (entitySlug) {
			case "pages":
			case "posts":
				return ["title", "slug", "updatedAt"];
			case "redirects":
				return ["from"];
		}
		return super.headers(entitySlug);
	}

	controlTemplate(field) {
		if (field.type === "String")
			switch (field.name) {
				case "confirmationMessage":
				case "content":
				case "message":
				case "richText":
					return "cms-rich-text";
			}
		return super.controlTemplate(field);
	}

	tabs(type) {
		switch (type) {
			case "Page":
				return {
					hero: ["hero"],
					content: ["layout"]
				};
			case "Post":
				return {
					content: ["heroImage", "content"],
					meta: ["relatedPosts", "categories"],
					seo: ["meta"]
				};
		}
		return super.tabs(type);
	}

	preview(entity) {
		switch (entity.$type) {
			case "Page":
				return `/${entity.slug}`;
			case "Post":
				return `/posts/${entity.slug}`;
		}
		return super.preview(entity);
	}

	sidebar(type) {
		if (["Page", "Post"].includes(type))
			return ["publishedAt", "slug"];
		if (type === "SearchResult")
			return ["document"];
		return super.sidebar(type);
	}

	isReadOnly(type) {
		return type === "SearchResult";
	}
}
