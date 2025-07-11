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
import WebComponent from "./web-component.js";

export default class Post extends WebComponent {

	static get observedAttributes() {
		return ["data-slug"];
	}

	static get templateNames() {
		return ["post"];
	}

	constructor() {
		super();
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		while (this.firstChild)
			this.removeChild(this.lastChild);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		const s = this.state;
		if (newValue !== oldValue && s?.computeState)
			delete s.computeState;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	async computeState() {
		const s = this.state;
		delete s.post;
		const u = new URL("/api/posts", location.href);
		u.searchParams.append("slug", this.dataset.slug);
		s.post = (await this.closest("root-element").fetchData(u.pathname + u.search))[0];
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.closest("root-element").updateSeo(s.post?.meta);
		this.appendChild(this.interpolateDom({
			$template: "",
			...s.post,
			content: s.post?.content?.map((x, i) => ({
				$template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
				path: `content.${i}`
			})),
			cards: s.post?.relatedPosts?.map(x => ({
				$template: "card",
				...x
			}))
		}));
	}

	data(path) {
		return path.split(".").reduce((x, n) => x[Array.isArray(x) ? parseInt(n) : n], this.state.post);
	}
}
