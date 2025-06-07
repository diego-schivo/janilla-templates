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

export default class Payment extends WebComponent {

	static get observedAttributes() {
		return ["data-email", "data-amount"];
	}

	static get templateName() {
		return "payment";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleSubmit = async event => {
		event.preventDefault();
		const s = this.closest("root-element").state.stripe;
		const { error: stripeError, paymentIntent } = await s.confirmPayment({
			elements: this.state.elements,
			confirmParams: { return_url: `${location.origin}/order-confirmation` }
		});
	}

	async updateDisplay() {
		this.appendChild(this.interpolateDom({ $template: "" }));
		if (this.dataset.email && this.dataset.amount) {
			const u = new URL("/api/stripe/create-payment-intent", location.href);
			u.searchParams.append("email", this.dataset.email);
			u.searchParams.append("amount", this.dataset.amount);
			const s = this.closest("root-element").state.stripe;
			const pi = await (await fetch(u)).json();
			this.state.elements = s.elements({
				clientSecret: pi["client_secret"],
				loader: "auto"
			});
			const pe = this.state.elements.create("payment");
			pe.mount("#payment-element");
		}
	}
}
