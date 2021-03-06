/**
 * This software is licensed under the Apache 2 license, quoted below.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at<br>
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0<br>
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datatree.templates;

import java.util.function.Function;

/**
 * Simple HTML minifier. For complex web sites, use a professional HTML-minifier
 * (eg. Google's HTMLCompressor). Minimization is run only once (per page),
 * after loading the template. Usage:
 * 
 * <pre>
 * TemplateEngine engine = new TemplateEngine();
 * engine.setRootDirectory("/www");
 * engine.setTemplatePreProcessor(new SimpleHtmlMinifier());
 * </pre>
 */
public class SimpleHtmlMinifier implements Function<String, String> {

	@Override
	public String apply(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		char[] in = text.toCharArray();
		char[] out = new char[in.length];
		boolean inScript = false;
		boolean wasWhitespace = false;
		char c;
		int len = 0;
		for (int i = 0; i < in.length; i++) {
			c = in[i];
			if (inScript) {
				out[len++] = c;
				if (c == '<' && i < in.length - 7 && text.substring(i, i + 7).equalsIgnoreCase("</scrip")) {
					inScript = false;
				}
			} else if (Character.isWhitespace(c)) {
				if (wasWhitespace) {
					continue;
				}
				wasWhitespace = true;
				out[len++] = ' ';
			} else if (c == '<' && i < in.length - 7 && text.substring(i, i + 7).equalsIgnoreCase("<script")) {
				inScript = true;
				out[len++] = c;
			} else if ((c == '<' || c == '#') && len > 2 && (out[len - 2] == '>' || out[len - 2] == '}')
					&& Character.isWhitespace(out[len - 1])) {
				out[len - 1] = c;
			} else {
				wasWhitespace = false;
				out[len++] = c;
			}
		}
		return new String(out, 0, len);
	}

}
