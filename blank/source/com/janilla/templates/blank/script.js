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
import CmsAdmin from "./cms-admin.js";
import CmsCheckbox from "./cms-checkbox.js";
import CmsCollection from "./cms-collection.js";
import CmsFirstRegister from "./cms-first-register.js";
import CmsDashboard from "./cms-dashboard.js";
import CmsFile from "./cms-file.js";
import CmsArray from "./cms-array.js";
import CmsLogin from "./login.js";
import LucideIcon from "./lucide-icon.js";
import CmsObject from "./cms-object.js";
import CmsRadio from "./cms-radio.js";
import CmsReference from "./cms-reference.js";
import CmsReferenceArray from "./cms-reference-array.js";
import CmsRichText from "./cms-rich-text.js";
import Root from "./root.js";
import CmsSelect from "./cms-select.js";
import CmsTabsField from "./tabs-field.js";
import CmsTextarea from "./cms-textarea.js";
import CmsText from "./cms-text.js";

customElements.define("cms-admin", CmsAdmin);
customElements.define("cms-checkbox", CmsCheckbox);
customElements.define("cms-collection", CmsCollection);
customElements.define("cms-first-register", CmsFirstRegister);
customElements.define("cms-dashboard", CmsDashboard);
customElements.define("cms-file", CmsFile);
customElements.define("cms-array", CmsArray);
customElements.define("cms-login", CmsLogin);
customElements.define("lucide-icon", LucideIcon);
customElements.define("cms-object", CmsObject);
customElements.define("cms-radio", CmsRadio);
customElements.define("cms-reference", CmsReference);
customElements.define("cms-reference-array", CmsReferenceArray);
customElements.define("cms-rich-text", CmsRichText);
customElements.define("root-element", Root);
customElements.define("cms-select", CmsSelect);
customElements.define("tabs-field", CmsTabsField);
customElements.define("cms-textarea", CmsTextarea);
customElements.define("cms-text", CmsText);
