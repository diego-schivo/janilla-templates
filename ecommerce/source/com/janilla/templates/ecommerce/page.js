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

	attributeChangedCallback(name, oldValue, newValue) {
		const s = this.state;
		if (newValue !== oldValue && s?.computeState)
			delete s.computeState;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	async computeState() {
		const s = this.state;
		delete s.page;
		const u = new URL("/api/pages", location.href);
		u.searchParams.append("slug", this.dataset.slug);
		const r = this.closest("root-element");
		s.page = (await r.fetchData(u.pathname + u.search))[0];
		if (!s.page && this.dataset.slug === "home")
			s.page = {
				layout: [
					{
						$type: "Content",
						columns: [
							{
								$type: "Column",
								size: "FULL",
								richText: `<h1>Janilla Website Template</h1>
<p>
  <a href="/admin">Visit the admin dashboard</a>
  to make your account and seed content for your website.
</p>`
							}
						]
					}
				]
			};
		if (s.page)
			this.requestDisplay();
		else
			r.notFound();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.closest("root-element").updateSeo(s.page?.meta);
		this.appendChild(this.interpolateDom({
			$template: "",
			hero: s.page?.hero && s.page.hero.type !== "NONE" ? {
				$template: "hero",
				path: "hero"
			} : null,
			layout: s.page?.layout?.map((x, i) => ({
				$template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
				path: `layout.${i}`
			}))
		}));
	}

	data(path) {
		// console.log("this.state.page", this.state.page, "path", path);
		return path.split(".").reduce((x, n) => Array.isArray(x)
			? x[parseInt(n)]
			: typeof x === "object" && x !== null
				? x[n]
				: null, this.state.page);
	}
}
