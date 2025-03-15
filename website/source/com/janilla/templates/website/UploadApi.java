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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.IntStream;

import com.janilla.http.HttpRequest;
import com.janilla.net.Net;
import com.janilla.util.Util;
import com.janilla.web.Handle;

@Handle(path = "/api/upload")
public class UploadApi {

	public Properties configuration;

	@Handle(method = "POST")
	public Path create(HttpRequest request) throws IOException {
		byte[] bb;
		{
			var b = request.getHeaders().stream().filter(x -> x.name().equals("content-type"))
					.map(x -> x.value().split(";")[1].trim().substring("boundary=".length())).findFirst().orElse(null);
			var ch = (ReadableByteChannel) request.getBody();
			var cc = Channels.newInputStream(ch).readAllBytes();
			var s = ("--" + b).getBytes();
			var ii = Util.findIndexes(cc, s);
			bb = IntStream.range(0, ii.length - 1)
					.mapToObj(i -> Arrays.copyOfRange(cc, ii[i] + s.length + 2, ii[i + 1] - 2)).findFirst()
					.orElse(null);
		}
		var i = Util.findIndexes(bb, "\r\n\r\n".getBytes(), 1)[0];
		var hh = Net.parseEntryList(new String(bb, 0, i), "\r\n", ":");
		var n = Arrays.stream(hh.get("Content-Disposition").split(";")).map(String::trim)
				.filter(x -> x.startsWith("filename=")).map(x -> x.substring(x.indexOf('=') + 1))
				.map(x -> x.startsWith("\"") && x.endsWith("\"") ? x.substring(1, x.length() - 1) : x).findFirst()
				.orElseThrow();
		bb = Arrays.copyOfRange(bb, i + 4, bb.length);
		var ud = configuration.getProperty("website-template.upload.directory");
		if (ud.startsWith("~"))
			ud = System.getProperty("user.home") + ud.substring(1);
		var p = Path.of(ud);
		if (!Files.exists(p))
			Files.createDirectories(p);
		return Files.write(p.resolve(n), bb);
	}
}
