package io.datatree.templates;

import io.datatree.Tree;

public class TemplateEngineTest {

	// --- SAMPLE ---

	public static final void main(String[] args) throws Exception {

		// Create JSON
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
		engine.setReloadTemplates(true);
		
		// Invoke Engine
		byte[] bytes = engine.process("template.html", data);

		// Display result		
		System.out.println(new String(bytes, "UTF8"));

	}

}
