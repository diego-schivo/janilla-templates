/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import WebComponent from "./web-component.js";

export default class CmsFoo extends WebComponent {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateName() {
		return "cms-foo";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const s = a.state;
		const p = this.dataset.path;
		const f = a.field(p);
		this.appendChild(this.interpolateDom({
			$template: "",
			selects: s.document.variantOptions.map(x => {
				const t = `${x.$type}.Option`;
				const n = f.data?.find(y => y.$type === t)?.name;
				return {
					$template: "select",
					name: p,
					options: [{
						$type: t,
						name: ""
					}, ...x.options].map(y => ({
						$template: "option",
						value: `${x.$type}:${y.name}`,
						selected: y.name == (n ?? ""),
						text: y.name
					}))
				};
			})
		}));
	}
}
