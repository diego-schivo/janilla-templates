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

export default class Cart extends WebComponent {

	static get templateNames() {
		return ["cart"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		const r = this.closest("root-element");
		r.addEventListener("cart-change", this.handleCartChange);
		this.addEventListener("submit", this.handleSubmit);
		r.addEventListener("user-change", this.handleUserChange);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		const r = this.closest("root-element");
		r.removeEventListener("cart-change", this.handleCartChange);
		this.removeEventListener("submit", this.handleSubmit);
		r.removeEventListener("user-change", this.handleUserChange);
	}

	handleCartChange = () => {
		this.requestDisplay();
	}

	handleSubmit = async event => {
		event.preventDefault();
		const fd = new FormData(event.target);
		const p = parseInt(fd.get("product"));
		const v = fd.get("variant");
		const q = parseInt(fd.get("quantity"));
		const u = this.closest("root-element").state.user;
		const c = u ? u.cart : JSON.parse(localStorage.getItem("cart"));
		if (!q) {
			const i = c.items.findIndex(x => x.product.id === p && x.variantId === v);
			c.items.splice(i, 1);
		} else {
			const ci = c.items.find(x => x.product.id === p && x.variantId === v);
			ci.quantity = q;
		}
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

	handleUserChange = () => {
		this.requestDisplay();
	}

	async updateDisplay() {
		const u = this.closest("root-element").state.user;
		const c = u ? u.cart : JSON.parse(localStorage.getItem("cart"));
		const cq = c?.items?.reduce((x, y) => x + y.quantity, 0) ?? 0;
		this.appendChild(this.interpolateDom({
			$template: "",
			quantity: cq > 0 ? {
				$template: "quantity",
				value: cq
			} : null,
			content: cq > 0 ? {
				$template: "content",
				items: c?.items?.map(x => {
					const v = x.product.variants.find(y => y.id === x.variantId);
					return {
						$template: "item",
						...x,
						variant: v,
						option: v.options[0].$type.split(".")[0],
						quantityMinus1: x.quantity - 1,
						quantityPlus1: x.quantity + 1,
					};
				}),
				total: c?.items?.reduce((x, y) => x + y.quantity * y.unitPrice, 0) ?? 0
			} : { $template: "empty" }
		}));
	}
}
