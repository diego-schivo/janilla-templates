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
import Archive from "./archive.js";
import Banner from "./banner.js";
import CallToAction from "./call-to-action.js";
import Card from "./card.js";
import CmsArray from "./cms-array.js";
import CmsCheckbox from "./cms-checkbox.js";
import CmsCollection from "./cms-collection.js";
import CmsDashboard from "./cms-dashboard.js";
import CmsDatetime from "./cms-datetime.js";
import CmsDocument from "./cms-document.js";
import CmsDocumentReference from "./cms-document-reference.js";
import CmsEdit from "./cms-edit.js";
import CmsFile from "./cms-file.js";
import CmsFirstRegister from "./cms-first-register.js";
import CmsForgotPassword from "./cms-forgot-password.js";
import CmsLogin from "./cms-login.js";
import CmsObject from "./cms-object.js";
import CmsRadio from "./cms-radio.js";
import CmsReference from "./cms-reference.js";
import CmsReferenceArray from "./cms-reference-array.js";
import CmsResetPassword from "./cms-reset-password.js";
import CmsRichText from "./cms-rich-text.js";
import CmsSelect from "./cms-select.js";
import CmsSlug from "./cms-slug.js";
import CmsTabs from "./cms-tabs.js";
import CmsText from "./cms-text.js";
import CmsTextarea from "./cms-textarea.js";
import CmsToasts from "./cms-toasts.js";
import CmsVersion from "./cms-version.js";
import CmsVersions from "./cms-versions.js";
import Content from "./content.js";
import CustomCmsAdmin from "./custom-cms-admin.js";
import FormBlock from "./form-block.js";
import Hero from "./hero.js";
import LucideIcon from "./lucide-icon.js";
import MediaBlock from "./media-block.js";
import NotFound from "./not-found.js";
import Page from "./page.js";
import Post from "./post.js";
import Posts from "./posts.js";
import RichText from "./rich-text.js";
import Root from "./root.js";
import Search from "./search.js";

customElements.define("archive-element", Archive);
customElements.define("banner-element", Banner);
customElements.define("call-to-action", CallToAction);
customElements.define("card-element", Card);
customElements.define("cms-admin", CustomCmsAdmin);
customElements.define("cms-array", CmsArray);
customElements.define("cms-checkbox", CmsCheckbox);
customElements.define("cms-collection", CmsCollection);
customElements.define("cms-dashboard", CmsDashboard);
customElements.define("cms-datetime", CmsDatetime);
customElements.define("cms-document", CmsDocument);
customElements.define("cms-document-reference", CmsDocumentReference);
customElements.define("cms-edit", CmsEdit);
customElements.define("cms-file", CmsFile);
customElements.define("cms-first-register", CmsFirstRegister);
customElements.define("cms-forgot-password", CmsForgotPassword);
customElements.define("cms-login", CmsLogin);
customElements.define("cms-object", CmsObject);
customElements.define("cms-radio", CmsRadio);
customElements.define("cms-reference", CmsReference);
customElements.define("cms-reference-array", CmsReferenceArray);
customElements.define("cms-reset-password", CmsResetPassword);
customElements.define("cms-rich-text", CmsRichText);
customElements.define("cms-select", CmsSelect);
customElements.define("cms-slug", CmsSlug);
customElements.define("cms-tabs", CmsTabs);
customElements.define("cms-text", CmsText);
customElements.define("cms-textarea", CmsTextarea);
customElements.define("cms-toasts", CmsToasts);
customElements.define("cms-version", CmsVersion);
customElements.define("cms-versions", CmsVersions);
customElements.define("content-element", Content);
customElements.define("form-block", FormBlock);
customElements.define("hero-element", Hero);
customElements.define("lucide-icon", LucideIcon);
customElements.define("media-block", MediaBlock);
customElements.define("not-found", NotFound);
customElements.define("page-element", Page);
customElements.define("post-element", Post);
customElements.define("posts-element", Posts);
customElements.define("rich-text", RichText);
customElements.define("root-element", Root);
customElements.define("search-element", Search);
