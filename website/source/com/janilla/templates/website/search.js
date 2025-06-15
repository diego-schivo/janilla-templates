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

export default class Search extends WebComponent {

	static get observedAttributes() {
		return ["data-query"];
	}

	static get templateNames() {
		return ["search"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("input", this.handleInput);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("input", this.handleInput);
		while (this.firstChild)
			this.removeChild(this.lastChild);
	}

	handleInput = async event => {
		const el = event.target.closest("input");
		if (el?.name !== "query")
			return;
		event.stopPropagation();
		const u = new URL(location.pathname, location.href);
		if (el.value)
			u.searchParams.append("query", el.value);
		history.pushState(undefined, "", u.pathname + u.search);
		dispatchEvent(new CustomEvent("popstate"));
	}

	async updateDisplay() {
		const u = new URL("/api/search-results", location.href);
		if (this.dataset.query)
			u.searchParams.append("query", this.dataset.query);
		const s = this.state;
		s.results = await (await fetch(u)).json();
		this.appendChild(this.interpolateDom({
			$template: "",
			cards: s.results.map(x => ({
				$template: "card",
				...x
			}))
		}));
	}
}
