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

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

/**
 * HTML minifier based on the Google's HtmlCompressor API. Usage:
 * 
 * <pre>
 * TemplateEngine engine = new TemplateEngine();
 * engine.setRootDirectory("/www");
 * engine.setTemplatePreProcessor(new GoogleHtmlMinifier());
 * </pre>
 * 
 * <b>Required dependencies:</b><br>
 * <br>
 * // https://mvnrepository.com/artifact/com.yahoo.platform.yui/yuicompressor
 * <br>
 * compile group: 'com.yahoo.platform.yui', name: 'yuicompressor', version:
 * '2.4.8'<br>
 * <br>
 * // https://mvnrepository.com/artifact/com.googlecode.htmlcompressor/
 * htmlcompressor<br>
 * compile group: 'com.googlecode.htmlcompressor', name: 'htmlcompressor',
 * version: '1.5.2'
 */
public class GoogleHtmlMinifier extends HtmlCompressor implements Function<String, String> {

	public GoogleHtmlMinifier() {
		setCompressCss(true);
		setCompressJavaScript(true);
	}

	@Override
	public String apply(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		return compress(text);
	}

}
