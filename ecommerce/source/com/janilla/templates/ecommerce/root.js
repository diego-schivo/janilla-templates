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
const shopRegex = /^\/shop(\/.*)?$/;

export default class Root extends WebComponent {

	static get templateNames() {
		return ["root"];
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
		this.addEventListener("submit", this.handleSubmit);
		this.addEventListener("user-change", this.handleUserChange);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
		removeEventListener("popstate", this.handlePopState);
		this.removeEventListener("submit", this.handleSubmit);
		this.removeEventListener("user-change", this.handleUserChange);
	}

	handleChange = event => {
		const el = event.target.closest("select");
		if (el?.closest("footer")) {
			if (el.value === "auto")
				localStorage.removeItem("janilla-templates.ecommerce.color-scheme");
			else
				localStorage.setItem("janilla-templates.ecommerce.color-scheme", el.value);
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
		const b = event.target.closest("button");
		if (b?.nextElementSibling?.matches("dialog"))
			b.nextElementSibling.showModal();
		else if (b?.parentElement?.matches("dialog"))
			b.parentElement.close();
	}

	handlePopState = () => {
		window.scrollTo(0, 0);
		document.querySelectorAll("dialog[open]").forEach(x => x.close());
		delete this.state.notFound;
		this.requestDisplay();
	}

	handleSubmit = event => {
		if (false) {
			event.preventDefault();
			const usp = new URLSearchParams(new FormData(event.target));
			history.pushState(undefined, "", `/search?${usp}`);
			dispatchEvent(new CustomEvent("popstate"));
		}
	}

	handleUserChange = event => {
		if (event.detail?.user)
			this.state.user = event.detail.user;
		else
			delete this.state.user;
	}

	async computeState() {
		const s = this.state;
		const nn = location.pathname.match(adminRegex)
			? ["user"]
			: ["user", "header", "footer", "config"];
		nn.forEach(x => delete s[x]);
		const kkvv = await Promise.all(nn.map(x => this.fetchData(`/api/${x === "user" ? "users/me" : x}`).then(y => ([x, y]))));
		for (const [k, v] of kkvv)
			s[k] = v;
		if (typeof Stripe !== "undefined" && s.config)
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
					email: this.state?.user?.email,
					path: m[1] ?? "/"
				}
			}));
			return;
		}

		switch (location.pathname) {
			case "/account":
				if (s.user === null) {
					history.pushState(undefined, "", "/login");
					dispatchEvent(new CustomEvent("popstate"));
					return;
				}
				break;
			case "/logout":
				await fetch("/api/users/logout", { method: "POST" });
				this.dispatchEvent(new CustomEvent("user-change"));
				break;
		}

		const cs = localStorage.getItem("janilla-templates.ecommerce.color-scheme");
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
		this.appendChild(this.interpolateDom({
			$template: "",
			style: `color-scheme: ${cs ?? "light dark"}`,
			header: s.header ? {
				$template: "header",
				navItems: s.header.navItems?.map(x => ({
					$template: "list-item",
					content: link(x)
				})),
				navItems2: (s.user ? [{
					href: "/orders",
					text: "Orders"
				}, {
					href: "/account",
					text: "Manage account"
				}, {
					href: "/logout",
					class: "button",
					text: "Log out"
				}] : [{
					href: "/login",
					class: "button",
					text: "Log in"
				}]).map(x => ({
					$template: "list-item",
					content: {
						$template: "link",
						...x
					}
				}))
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
				const m4 = location.pathname.match(shopRegex);
				if (m4)
					return {
						$template: "shop",
						category: m4[1]?.substring(1)
					};
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
