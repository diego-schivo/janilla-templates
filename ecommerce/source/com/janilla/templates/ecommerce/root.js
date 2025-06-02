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
const ordersRegex = /^\/orders(\/.*)?$/;
const productRegex = /^\/products(\/.*)$/;

export default class Root extends WebComponent {

	static get templateName() {
		return "root";
	}

	#serverData;

	constructor() {
		super();
	}

	connectedCallback() {
		const el = this.querySelector("#server-data");
		if (el) {
			el.remove();
			this.#serverData = JSON.parse(el.text);
		}
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
		addEventListener("popstate", this.handlePopState);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
		removeEventListener("popstate", this.handlePopState);
	}

	handleChange = event => {
		const el = event.target.closest("select");
		if (el?.closest("footer")) {
			if (el.value === "auto")
				localStorage.removeItem("janilla-templates.website.color-scheme");
			else
				localStorage.setItem("janilla-templates.website.color-scheme", el.value);
			this.requestDisplay();
		}
	}

	handleClick = event => {
		const a = event.target.closest("a");
		if (a?.href && !event.defaultPrevented && !a.target) {
			if (a.getAttribute("href") === "#") {
				event.preventDefault();
				this.querySelector("dialog").showModal();
			} else {
				const u = new URL(a.href);
				if (!u.pathname.match(adminRegex) !== !location.pathname.match(adminRegex))
					return;
				event.preventDefault();
				history.pushState(undefined, "", u.pathname + u.search);
				dispatchEvent(new CustomEvent("popstate"));
			}
		}
		const b = event.target.closest("header button");
		if (b) {
			event.preventDefault();
			b.nextElementSibling.showModal();
		}
	}

	handlePopState = () => {
		window.scrollTo(0, 0);
		document.querySelectorAll("dialog[open]").forEach(x => x.close());
		delete this.state.notFound;
		this.requestDisplay();
	}

	async computeState() {
		const s = this.state;
		const nn = location.pathname.match(adminRegex)
			? ["user"]
			: ["user", "header", "footer", "redirects", "config"];
		nn.forEach(x => delete s[x]);
		const kkvv = await Promise.all(nn.map(x => this.fetchData(`/api/${x === "user" ? "users/me" : x}`).then(y => ([x, y]))));
		for (const [k, v] of kkvv)
			s[k] = v;
		if (typeof Stripe !== "undefined")
			s.stripe = Stripe(s.config.publishableKey, { apiVersion: "2020-08-27" });
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();

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

		if (s.redirects)
			for (const x of s.redirects)
				if (x.from === location.pathname) {
					history.pushState(undefined, "", x.to);
					dispatchEvent(new CustomEvent("popstate"));
					return;
				}

		switch (location.pathname) {
			case "/account":
				if (!s.user) {
					history.pushState(undefined, "", "/login");
					dispatchEvent(new CustomEvent("popstate"));
					return;
				}
				break;
			case "/logout":
				await fetch("/api/users/logout", { method: "POST" });
				delete s.user;
				break;
		}

		const cs = localStorage.getItem("janilla-templates.website.color-scheme");
		const link = x => {
			let h;
			switch (x.type.name) {
				case "REFERENCE":
					switch (x.reference?.$type) {
						case "Page":
							h = `/${x.reference.slug}`;
							break;
						case "Product":
							h = `/products/${x.reference.slug}`;
							break;
					}
					break;
				case "CUSTOM":
					h = x.uri;
					break;
			}
			return {
				$template: "link",
				...x,
				href: h,
				target: x.newTab ? "_blank" : null
			};
		};
		const c = JSON.parse(localStorage.getItem("cart"));
		this.appendChild(this.interpolateDom({
			$template: "",
			style: `color-scheme: ${cs ?? "light dark"}`,
			header: s.header ? {
				$template: "header",
				navItems: s.header.navItems?.map(link),
				cartQuantity: c?.items?.reduce((x, y) => x + y.quantity, 0) ?? 0,
				cartItems: c?.items?.map(x => {
					const v = x.product.variants[x.variant];
					return {
						$template: "cart-item",
						...x,
						variant: v,
						option: v.options[0].$type.split(".")[0]
					};
				}),
				cartTotal: c?.items?.reduce((x, y) => x + y.quantity * y.product.variants[y.variant].price, 0) ?? 0
			} : null,
			content: s.notFound ? { $template: "not-found" } : (() => {
				switch (location.pathname) {
					case "/account":
						return { $template: "account" };
					case "/checkout":
						return { $template: "checkout" };
					case "/login":
						return { $template: "login" };
					case "/logout":
						return { $template: "logout" };
					case "/order-confirmation":
						return {
							$template: "order-confirmation",
							stripePaymentIntentId: new URLSearchParams(location.search).get("payment_intent")
						};
					case "/shop":
						return { $template: "shop" };
				}
				const m2 = location.pathname.match(productRegex);
				if (m2)
					return {
						$template: "product",
						slug: m2[1].substring(1)
					};
				const m3 = location.pathname.match(ordersRegex);
				if (m3)
					return m3[1] ? {
						$template: "order",
						id: m3[1].substring(1)
					} : { $template: "orders" };
				return location.pathname === "/search" ? {
					$template: "search",
					query: new URLSearchParams(location.search).get("query")
				} : {
					$template: "page",
					slug: (() => {
						const s2 = location.pathname.substring(1);
						return s2 ? s2 : "home";
					})()
				};
			})(),
			footer: s.footer ? {
				$template: "footer",
				navItems: s.footer.navItems?.map(link),
				options: ["auto", "light", "dark"].map(x => ({
					$template: "option",
					value: x,
					text: x.charAt(0).toUpperCase() + x.substring(1),
					selected: x === (cs ?? "auto")
				}))
			} : null
		}));
	}

	updateSeo(meta) {
		const sn = "Janilla Ecommerce Template";
		const t = [meta?.title && meta.title !== sn ? meta.title : null, sn].filter(x => x).join(" | ");
		const d = meta?.description ?? "";
		for (const [k, v] of Object.entries({
			title: t,
			description: d,
			"og:title": t,
			"og:description": d,
			"og:url": location.href,
			"og:site_name": sn,
			"og:image": meta?.image?.uri ? `${location.protocol}://${location.host}${meta.image.uri}` : null,
			"og:type": "website"
		}))
			if (k === "title")
				document.title = v ?? "";
			else
				document.querySelector(`meta[name="${k}"]`).setAttribute("content", v ?? "");
	}

	async fetchData(key) {
		if (Object.hasOwn(this.#serverData, key)) {
			const v = this.#serverData[key];
			delete this.#serverData[key];
			return v;
		}
		return await (await fetch(key)).json();
	}

	notFound() {
		this.state.notFound = true;
		this.requestDisplay();
	}
}
