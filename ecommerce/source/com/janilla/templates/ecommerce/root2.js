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

const adminRegex = /^\/admin(\/.*)?$/;

export default class Root extends WebComponent {

	static get templateNames() {
		return ["root"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		addEventListener("popstate", this.handlePopState);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		removeEventListener("popstate", this.handlePopState);
	}

	handlePopState = () => {
		this.requestDisplay();
	}

	async updateDisplay() {
		const m = location.pathname.match(adminRegex);
		if (m) {
			this.appendChild(this.interpolateDom({
				$template: "",
				admin: {
					$template: "admin",
					email: this.querySelector("cms-admin")?.state?.me?.email,
					path: m[1] ?? "/"
				}
			}));
			return;
		}
		const s = this.state;
		const c = await (await fetch("/api/config")).json();
		s.stripe = Stripe(c.publishableKey, { apiVersion: "2020-08-27" });
		this.appendChild(this.interpolateDom({
			$template: "",
			content: { $template: location.pathname === "/return" ? "return" : "payment" },
			messages: s.messages?.length ? {
				$template: "messages",
				items: s.messages.map(x => ({
					$template: "message",
					text: x
				}))
			} : null
		}));
	}

	addMessage(text) {
		const mm = (this.state.messages ??= []);
		mm.push(text);
		this.requestDisplay();
	}
}
