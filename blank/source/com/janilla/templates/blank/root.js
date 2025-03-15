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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class Root extends UpdatableHTMLElement {

	static get templateName() {
		return "root";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const m = location.pathname.match(/^\/admin(\/.*)?$/);
		this.appendChild(this.interpolateDom(m ? {
			$template: "admin",
			path: (() => {
				if (m[1]?.startsWith("/collections/") || m[1]?.startsWith("/globals/")) {
					const s = m[1].substring(1);
					return s.split("/").map(x => x.split("-").map((y, i) => i ? y.charAt(0).toUpperCase() + y.substring(1) : y).join("")).join(".");
				}
				return null;
			})()
		} : { $template: "" }));
	}
}
