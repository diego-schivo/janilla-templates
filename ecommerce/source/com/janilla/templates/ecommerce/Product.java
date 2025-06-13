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
package com.janilla.templates.ecommerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.cms.Versions;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

@Store
@Index(sort = "title")
@Versions(drafts = true)
public record Product(Long id, String title, String description, List<@Types(Media.class) Long> gallery,
		Boolean enableVariants, List<@Types( {
				Fabric.class, Size.class, Color.class }) Object> variantOptions,
		List<Variant> variants, Long stock, BigDecimal price, @Index List<@Types(Category.class) Long> categories,
		Meta meta, @Index String slug, Instant createdAt, Instant updatedAt, Document.Status documentStatus,
		Instant publishedAt) implements Document{

	public Product withVariants(List<Variant> variants) {
		return new Product(id, title, description, gallery, enableVariants, variantOptions, variants, stock, price,
				categories, meta, slug, createdAt, updatedAt, documentStatus, publishedAt);
	}

	public Product withNonNullVariantIds() {
		return variants != null && variants.stream().anyMatch(x -> x.id() == null)
				? withVariants(variants.stream().map(x -> x.id() == null ? x.withId(UUID.randomUUID()) : x).toList())
				: this;
	}

	public record Variant(UUID id, Boolean active, Set<Object> options, BigDecimal price, Long stock) {

		public Variant withId(UUID id) {
			return new Variant(id, active, options, price, stock);
		}
	}
}
