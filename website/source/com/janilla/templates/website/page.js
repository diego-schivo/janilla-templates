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
import { WebComponent } from "./web-component.js";

export default class Page extends WebComponent {

	static get observedAttributes() {
		return ["data-slug"];
	}

	static get templateName() {
		return "page";
	}

	constructor() {
		super();
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		while (this.firstChild)
			this.removeChild(this.lastChild);
	}

	async updateDisplay() {
		const u = new URL("/api/pages", location.href);
		u.searchParams.append("slug", this.dataset.slug);
		const s = this.state;
		s.page = (await (await fetch(u)).json())[0];
		console.log("s.page", s.page);
		this.appendChild(this.interpolateDom({
			$template: "",
			hero: s.page.hero.type === "NONE" ? null : {
				$template: "hero",
				path: "hero"
			},
			layout: s.page.layout.map((x, i) => ({
				$template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
				path: `layout.${i}`
			}))
		}));
	}

	data(path) {
		console.log("this.state.page", this.state.page, "path", path);
		return path.split(".").reduce((x, n) => Array.isArray(x)
			? x[parseInt(n)]
			: typeof x === "object" && x !== null
				? x[n]
				: null, this.state.page);
	}
}
