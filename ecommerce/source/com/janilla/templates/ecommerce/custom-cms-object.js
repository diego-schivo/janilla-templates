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
import CmsObject from "./cms-object.js";

export default class CustomCmsObject extends CmsObject {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateName() {
		return "custom-cms-object";
	}

	constructor() {
		super();
	}

	foo(f) {
		//console.log("f", f);
		const x = super.foo(f).filter(([k, _]) => k !== "id");
		if (f.type === "User")
			x.splice(x.findIndex(([k, _]) => k === "salt"), 4, ["password", null]);
		//console.log("x", x);
		return x;
	}
}
