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

	static get templateNames() {
		return ["cms-admin"];
	}

	constructor() {
		super();
	}

	field(path, parent) {
		const f = super.field(path, parent);
		if (f.parent?.type === "User" && f.name === "password")
			f.type = "String";
		return f;
	}

	label(path) {
		return ["hero", "hero.richText", "meta"].includes(path) ? null : super.label(path);
	}

	headers(entitySlug) {
		switch (entitySlug) {
			case "pages":
			case "posts":
				return ["title", "slug", "updatedAt"];
			case "redirects":
				return ["from"];
			case "users":
				return ["name", "email", "roles"];
		}
		return super.headers(entitySlug);
	}

	cell(object, key) {
		const x = super.cell(object, key);
		switch (key) {
			case "documentStatus":
				return x.name;
			case "roles":
				return x.map(y => y.name).join();
		}
		return x;
	}

	controlTemplate(field) {
		if (field.type === "String")
			switch (field.name) {
				case "confirmationMessage":
				case "content":
				case "message":
				case "richText":
					return "rich-text";
				case "type":
					return field.options.length <= 2 ? "radio" : "select";
			}
		return super.controlTemplate(field);
	}

	tabs(type) {
		switch (type) {
			case "Page":
				return {
					Hero: ["hero"],
					Content: ["layout"],
					SEO: ["meta"]
				};
			case "Post":
				return {
					Content: ["heroImage", "content"],
					Meta: ["relatedPosts", "categories"],
					SEO: ["meta"]
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
		switch (type) {
			case "Page":
				return ["publishedAt", "slug"];
			case "Post":
				return ["publishedAt", "slug", "authors"];
			case "SearchResult":
				return ["document"];
		}
		return super.sidebar(type);
	}

	isReadOnly(type) {
		return type === "SearchResult";
	}

	formProperties(field) {
		//console.log("f", f);
		const x = super.formProperties(field);
		if (field.type === "User")
			x.splice(x.findIndex(([k, _]) => k === "salt"), 4, ["password", null]);
		//console.log("x", x);
		return x;
	}
}
