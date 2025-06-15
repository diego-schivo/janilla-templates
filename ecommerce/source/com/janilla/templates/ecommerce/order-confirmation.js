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

export default class OrderConfirmation extends WebComponent {

	static get templateNames() {
		return ["order-confirmation"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		const u = new URL("/api/orders/poll", location.href);
		u.searchParams.append("stripePaymentIntentId", this.dataset.stripePaymentIntentId);
		this.eventSource = new EventSource(u);
		this.eventSource.onopen = () => {
			console.log("Connection to server opened.");
		};
		this.eventSource.onmessage = async e => {
			console.log(e.data);
			//this.querySelector("textarea").value += e.data + "\n";
		};
		this.eventSource.onerror = () => {
			console.log("EventSource failed.");
			this.eventSource.close();
		};
		/*
		this.eventSource.addEventListener("ping", e => {
		  const o = JSON.parse(e.data);
		  console.log("ping at " + o.time);
		});
		*/
		this.eventSource.addEventListener("order", e => {
			const o = JSON.parse(e.data);
			console.log("order", o);
			history.pushState(undefined, "", `/orders/${o.id}`);
			dispatchEvent(new CustomEvent("popstate"));
		});
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.eventSource.close();
	}

	async updateDisplay() {
		this.appendChild(this.interpolateDom({
			$template: ""
		}));
	}
}
