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

export default class Checkout extends WebComponent {

	static get templateName() {
		return "checkout";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("input", this.handleInput);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("input", this.handleInput);
		this.removeEventListener("submit", this.handleSubmit);
		while (this.firstChild)
			this.removeChild(this.lastChild);
	}

	handleInput = event => {
		if (event.target.matches('[name="email"]')) {
			this.state.email = event.target.value;
			this.requestDisplay();
		}
	}

	handleSubmit = event => {
		if (!event.target.matches("#payment-form")) {
			event.preventDefault();
			const fd = new FormData(event.target);
			this.state.email = fd.get("email");
			this.requestDisplay();
		}
	}

	async updateDisplay() {
		const s = this.state;
		const r = this.closest("root-element");
		const u = r.state.user;
		const c = u ? u.cart : JSON.parse(localStorage.getItem("cart"));
		s.email ??= u?.email;
		r.updateSeo(null);
		this.appendChild(this.interpolateDom({
			$template: "",
			user: u ? {
				$template: "user",
				...u
			} : {
				$template: "guest",
				value: s.email,
				disabled: !s.email
			},
			payment: s.email ? {
				$template: "payment",
				email: s.email,
				amount: 1234
			} : null,
			items: c?.items?.map(x => {
				const v = x.product.variants.find(y => y.id === x.variantId);
				return {
					$template: "item",
					...x,
					variant: v,
					option: v.options[0].name
				};
			}),
			total: c?.items?.reduce((x, y) => x + y.quantity * y.unitPrice, 0) ?? 0
		}));
	}
}
