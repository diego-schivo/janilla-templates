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

export default class Product extends WebComponent {

	static get observedAttributes() {
		return ["data-slug"];
	}

	static get templateName() {
		return "product";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("submit", this.handleSubmit);
		while (this.firstChild)
			this.removeChild(this.lastChild);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		const s = this.state;
		if (newValue !== oldValue && s?.computeState)
			delete s.computeState;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	handleChange = async event => {
		const s = this.state;
		const p = s.product;
		const fd = new FormData(event.target.form);
		s.variant = p.variants.find(x => x.active && x.options.every(y => y.name === fd.get(y.$type.split(".")[0])));
		this.requestDisplay();
	}

	handleClick = async event => {
		const b = event.target.closest("button");
		const ul1 = b?.closest("#gallery-arrows ul");
		const s = this.state;
		if (ul1) {
			let i = s.galleryIndex;
			i += [-1, 1][Array.prototype.findIndex.call(ul1.children, x => x.contains(b))];
			s.galleryIndex = (s.product.gallery.length + i) % s.product.gallery.length;
			this.requestDisplay();
		}
		const ul2 = b?.closest("#gallery-thumbnails ul");
		if (ul2) {
			s.galleryIndex = Array.prototype.findIndex.call(ul2.children, x => x.contains(b));
			this.requestDisplay();
		}
	}

	handleSubmit = async event => {
		event.preventDefault();
		const s = this.state;
		const p = s.product;
		const v = s.variant;
		const u = this.closest("root-element").state.user;
		const c = (u ? u.cart : JSON.parse(localStorage.getItem("cart") ?? "null")) ?? { "items": [] };
		const ci = c.items.find(x => x.product.id === p.id && x.variant === v.id);
		if (ci)
			ci.quantity++;
		else
			c.items.push({
				product: p,
				variantId: v.id,
				unitPrice: v.price,
				quantity: 1
			});
		if (u) {
			const r = await fetch(`/api/users/${u.id}`, {
				method: "PATCH",
				headers: { "content-type": "application/json" },
				body: JSON.stringify({
					$type: "User",
					cart: {
						$type: "User.Cart",
						items: c.items.map(x => ({
							$type: "User.Cart.Item",
							...x,
							product: x.product.id
						}))
					}
				})
			});
			if (r.ok)
				this.dispatchEvent(new CustomEvent("user-change", {
					bubbles: true,
					detail: { user: await r.json() }
				}));
		} else
			localStorage.setItem("cart", JSON.stringify(c));
		this.dispatchEvent(new CustomEvent("cart-change", { bubbles: true }));
	}

	async computeState() {
		const s = this.state;
		const usp = new URLSearchParams({ slug: this.dataset.slug });
		s.product = (await this.closest("root-element").fetchData(`/api/products?${usp}`))[0];
		s.galleryIndex = 0;
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.closest("root-element").updateSeo(s.product?.meta);
		const pp = s.product?.variants?.filter(x => x.active)?.map(x => x.price);
		this.appendChild(this.interpolateDom({
			$template: "",
			...s.product,
			image: s.product?.gallery[s.galleryIndex],
			galleryThumbnails: s.product?.gallery.map((x, i) => ({
				$template: "gallery-thumbnail",
				active: i === s.galleryIndex ? "active" : null,
				image: x
			})),
			lowestAmount: pp ? Math.min(...pp) : null,
			highestAmount: pp ? Math.max(...pp) : null,
			variantSelector: s.product?.enableVariants ? { $template: "variant-selector" } : null,
			disabled: !s.variant
		}));
	}

	data(path) {
		return path.split(".").reduce((x, n) => x[Array.isArray(x) ? parseInt(n) : n], this.state.product);
	}
}
