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

const adminRegex = /^\/admin(\/.*)?$/;
const postsRegex = /^\/posts(\/.*)?$/;

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
		this.addEventListener("click", this.handleClick);
		addEventListener("popstate", this.handlePopState);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		removeEventListener("popstate", this.handlePopState);
	}

	handleClick = event => {
		const a = event.target.closest("a");
		if (!a?.href || event.defaultPrevented || a.target)
			return;
		const u = new URL(a.href);
		if (!u.pathname.match(adminRegex) !== !location.pathname.match(adminRegex))
			return;
		event.preventDefault();
		history.pushState(undefined, "", u.pathname + u.search);
		dispatchEvent(new CustomEvent("popstate"));
		window.scrollTo(0, 0);
	}

	handlePopState = () => {
		delete this.state.notFound;
		this.requestDisplay();
	}

	async computeState() {
		const s = this.state;
		const nn = ["header", "footer", "redirects"];
		nn.forEach(x => delete s[x]);
		for (const [k, v] of await Promise.all(nn.map(x => {
			const k = `/api/${x}`;
			return this.fetchData(k).then(y => ([x, y]));
		})))
			s[k] = v;
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
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
		s.computeState ??= this.computeState();
		if (s.redirects)
			for (const x of s.redirects)
				if (x.from === location.pathname) {
					history.pushState(undefined, "", x.to);
					dispatchEvent(new CustomEvent("popstate"));
					return;
				}
		const link = x => {
			let h;
			switch (x.type) {
				case "REFERENCE":
					switch (x.reference?.$type) {
						case "Page":
							h = `/${x.reference.slug}`;
							break;
						case "Post":
							h = `/posts/${x.reference.slug}`;
							break;
					}
					break;
				case "CUSTOM":
					h = x.url;
					break;
			}
			return {
				$template: "link",
				href: h,
				text: x.label,
				target: x.newTab ? "_blank" : null
			};
		};
		this.appendChild(this.interpolateDom({
			$template: "",
			header: s.header ? {
				$template: "header",
				navItems: s.header.navItems?.map(link)
			} : null,
			content: s.notFound ? { $template: "not-found" } : (() => {
				const m2 = location.pathname.match(postsRegex);
				if (m2)
					return m2[1] ? {
						$template: "post",
						slug: m2[1].substring(1)
					} : { $template: "posts" };
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
				navItems: s.footer.navItems?.map(link)
			} : null
		}));
	}

	updateSeo(meta) {
		const sn = "Janilla Website Template";
		const t = [meta?.title && meta.title !== sn ? meta.title : null, sn].filter(x => x).join(" | ");
		const d = meta?.description ?? "";
		for (const [k, v] of Object.entries({
			title: t,
			description: d,
			"og:title": t,
			"og:description": d,
			"og:url": location.href,
			"og:site_name": sn,
			"og:image": meta?.image?.url ? `${location.protocol}://${location.host}${meta.image.url}` : null,
			"og:type": "website"
		}))
			if (k === "title")
				document.title = v ?? "";
			else
				document.querySelector(`meta[name="${k}"]`).setAttribute("content", v ?? "");
	}

	async fetchData(key) {
		if (!Object.hasOwn(this.#serverData, key))
			return await (await fetch(key)).json();
		const v = this.#serverData[key];
		delete this.#serverData[key];
		return v;
	}

	notFound() {
		this.state.notFound = true;
		this.requestDisplay();
	}
}
