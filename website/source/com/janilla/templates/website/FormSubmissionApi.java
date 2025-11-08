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
package com.janilla.templates.website;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.janilla.cms.CollectionApi;
import com.janilla.http.HttpExchange;
import com.janilla.persistence.Persistence;
import com.janilla.smtp.SmtpClient;
import com.janilla.web.Handle;

@Handle(path = "/api/form-submissions")
public class FormSubmissionApi extends CollectionApi<Long, FormSubmission> {

	public SmtpClient smtpClient;

	public FormSubmissionApi(Predicate<HttpExchange> drafts, Persistence persistence, SmtpClient smtpClient) {
		super(FormSubmission.class, drafts, persistence);
	}

	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	@Handle(method = "POST")
	public FormSubmission create(FormSubmission entity) {
		var e = super.create(entity);
		var m = entity.submissionData().stream()
				.collect(Collectors.toMap(SubmissionDatum::field, SubmissionDatum::value));
		var f = persistence.crud(Form.class).read(entity.form());
		for (var x : f.emails()) {
			var ss = List.of(x.emailFrom(), x.emailTo(), x.subject(), x.message()).stream()
					.map(y -> PLACEHOLDER.matcher(y).replaceAll(z -> m.get(z.group(1)))).toList();
			smtpClient.sendMail(OffsetDateTime.now(), ss.get(0), ss.get(1), ss.get(2), ss.get(3));
		}
		return e;
	}
}
