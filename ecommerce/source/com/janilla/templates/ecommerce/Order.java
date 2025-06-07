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

import java.time.Instant;
import java.util.List;

import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

@Store
@Index(sort = "-createdAt")
public record Order(Long id, @Index(sort = "-createdAt") @Types(User.class) Long orderedBy,
		String stripePaymentIntentId, Long total, String currency, List<Item> items, Status status, Instant createdAt,
		Instant updatedAt, Document.Status documentStatus, Instant publishedAt) implements Document {

	public record Item(String id, Long product, String variant, Integer quantity) {
	}

	public enum Status {

		PROCESSING, COMPLETED
	}
}
