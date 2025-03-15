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

export default class FormBlock extends UpdatableHTMLElement {

	static get templateName() {
		return "form-block";
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
		const ee = [...new FormData(event.target).entries()];
		this.state.submission = await (await fetch("/api/form-submissions", {
			method: "POST",
			headers: { "content-type": "application/json" },
			body: JSON.stringify({
				$type: "FormSubmission",
				form: 1,
				submissionData: ee.map(([k, v]) => ({
					field: k,
					value: v
				}))
			})
		})).json();
		this.requestUpdate();
	}

	async updateDisplay() {
		const s = this.state;
		const d = this.closest("page-element").data(this.dataset.path);
		if (s.submission && d.form.confirmationType === "REDIRECT") {
			history.pushState(undefined, "", d.form.redirect);
			dispatchEvent(new CustomEvent("popstate"));
			return;
		}
		this.appendChild(this.interpolateDom({
			$template: "",
			intro: !s.submission && d.enableIntro ? {
				$template: "intro",
				...d
			} : null,
			form: !s.submission && d.form ? {
				$template: "form",
				fields: d.form.fields.map(x => ({
					$template: "field",
					...x,
					control: (() => {
						const t = x.$type.split(".")[1].toLowerCase();
						return {
							$template: t === "textarea" ? "textarea-control" : "input-control",
							...x,
							type: t !== "text" ? t : null
						};
					})()
				}))
			} : null,
			confirmation: s.submission ? {
				$template: "confirmation",
				...d.form
			} : null
		}));
	}
}
