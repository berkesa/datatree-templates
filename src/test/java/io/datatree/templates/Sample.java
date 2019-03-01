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

import io.datatree.Tree;

/**
 * Usage of the template engine.
 */
public class Sample {

	public static final void main(String[] args) throws Exception {

		// Create JSON structure
		Tree data = new Tree();
		data.put("a", 1);
		data.put("b", true);
		data.put("c", "< & >");
		data.put("d.e", "abc");

		Tree table = data.putList("table");
		for (int i = 0; i < 10; i++) {
			Tree row = table.addMap();
			row.put("first", "12345");
			row.put("second", i % 2 == 0);
			row.put("third", i);
		}

		// Display JSON
		System.out.println(data.toString());

		// Create Template Engine
		TemplateEngine engine = new TemplateEngine();
		engine.setRootDirectory("/io/datatree/templates/html");
		engine.setReloadTemplates(false);
		engine.setTemplatePreProcessor(new SimpleHtmlMinifier());
		
		// Invoke Engine
		String result = engine.process("template.html", data);

		// Display result
		System.out.println(result);

	}
	
}