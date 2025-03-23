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
import CheckboxControl from "./checkbox-control.js";
import CmsCollection from "./cms-collection.js";
import CreateFirstUser from "./create-first-user.js";
import CmsDashboard from "./cms-dashboard.js";
import CmsFile from "./cms-file.js";
import CmsArray from "./cms-array.js";
import Login from "./login.js";
import LucideIcon from "./lucide-icon.js";
import CmsObject from "./cms-object.js";
import RadioGroupControl from "./radio-group-control.js";
import CmsReference from "./cms-reference.js";
import ReferenceListControl from "./reference-list-control.js";
import CmsRichText from "./cms-rich-text.js";
import Root from "./root.js";
import CmsSelect from "./cms-select.js";
import CmsTabsField from "./tabs-field.js";
import TextareaControl from "./textarea-control.js";
import CmsText from "./cms-text.js";

customElements.define("cms-admin", CmsAdmin);
customElements.define("checkbox-control", CheckboxControl);
customElements.define("cms-collection", CmsCollection);
customElements.define("create-first-user", CreateFirstUser);
customElements.define("cms-dashboard", CmsDashboard);
customElements.define("cms-file", CmsFile);
customElements.define("cms-array", CmsArray);
customElements.define("login-element", Login);
customElements.define("lucide-icon", LucideIcon);
customElements.define("cms-object", CmsObject);
customElements.define("radio-group-control", RadioGroupControl);
customElements.define("cms-reference", CmsReference);
customElements.define("reference-list-control", ReferenceListControl);
customElements.define("cms-rich-text", CmsRichText);
customElements.define("root-element", Root);
customElements.define("cms-select", CmsSelect);
customElements.define("tabs-field", CmsTabsField);
customElements.define("textarea-control", TextareaControl);
customElements.define("cms-text", CmsText);
