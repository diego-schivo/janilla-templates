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

export default class Shop extends WebComponent {

	static get observedAttributes() {
		return ["data-category"];
	}

	static get templateNames() {
		return ["shop"];
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
		const r = this.closest("root-element");
		s.categories ??= await r.fetchData("/api/categories");
		const c = this.dataset.category ? s.categories.find(x => x.slug === this.dataset.category) : null;
		const usp = c ? new URLSearchParams({ categories: c.id }) : null;
		s.products = await r.fetchData(usp ? `/api/products?${usp}` : "/api/products");
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.closest("root-element").updateSeo(null);
		this.appendChild(this.interpolateDom({
			$template: "",
			categories: [{
				href: "/shop",
				text: "All"
			}, ...(s.categories?.map(x => ({
				href: `/shop/${x.slug}`,
				text: x.title
			})) ?? [])].map(x => ({
				$template: "category",
				...x,
				class: x.href.split("/")[2] == this.dataset.category ? "active" : null
			})),
			products: s.products?.map(x => ({
				$template: "product",
				...x
			}))
		}));
	}
}
