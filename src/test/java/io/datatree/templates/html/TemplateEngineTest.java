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
package io.datatree.templates.html;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import io.datatree.Tree;
import io.datatree.templates.SimpleHtmlMinifier;
import io.datatree.templates.TemplateEngine;
import junit.framework.TestCase;

/**
 * Template Engine tests.
 * 
 * @author Andras Berkes [andras.berkes@programmer.net]
 */
public class TemplateEngineTest extends TestCase {

	TemplateEngine engine;

	@Test
	public void testDefine() throws Exception {
		engine.setRootDirectory("");
		engine.define("page.html", ">>>#{variable}<<<");
		
		assertTrue(engine.contains("page.html"));
		
		Tree data = new Tree();
		data.put("variable", 123);
		
		String html = process("page.html", data);
		assertEquals(html, ">>>123<<<");
		
		engine.remove("page.html");
		assertFalse(engine.contains("page.html"));
		try {
			html = process("page.html", data);
			fail();
		} catch (Exception e) {
			// Ok!
		}
		engine.define("page.html", ">>>#{variable}<<<");
		data.put("variable", "a");
		html = process("page.html", data);
		assertEquals(html, ">>>a<<<");
		
		assertTrue(engine.contains("page.html"));
		engine.clear();
		assertFalse(engine.contains("page.html"));
	}
	
	@Test
	public void testBase() throws Exception {
		Tree data = new Tree();			
		String html = process(data);
		
		assertEquals("static text var1::var1 var2::var2 ex::ex !ex::!ex eq::eq !eq::!eq for::for in:<h1>header</h1>:in", html);
		
		// var1:#{a}:var1
		data.put("a", 1.2);
		html = process(data);
		assertTrue(html.contains("text var1:1.2:var1 var2"));
		data.put("a", false);
		html = process(data);
		assertTrue(html.contains("text var1:false:var1 var2"));
		data.put("a", "abc");
		html = process(data);
		assertTrue(html.contains("text var1:abc:var1 var2"));
		data.putMap("a").put("x", 1).put("y", 2);
		html = process(data);
		assertTrue(html.contains("var1:{x=1, y=2}:var1"));
		data.put("a", "<>&'\"");
		html = process(data);
		assertTrue(html.contains("var1:&lt;&gt;&amp;&#x27;&quot;:var1"));
		
		// var2:#{b.c.d}:var2
		data.put("b.c.d", 1.2);
		html = process(data);
		assertTrue(html.contains("var2:1.2:var2"));
		data.put("b.c.d", false);
		html = process(data);
		assertTrue(html.contains("var2:false:var2"));
		data.put("b.c.d", "abc");
		html = process(data);
		assertTrue(html.contains("var2:abc:var2"));
		data.putMap("b.c.d").put("x", 1).put("y", 2);
		html = process(data);
		assertTrue(html.contains("var2:{x=1, y=2}:var2"));
		data.put("b.c.d", "<>&'\"");
		html = process(data);
		assertTrue(html.contains("var2:&lt;&gt;&amp;&#x27;&quot;:var2"));
		
		// ex:#{ex e}#{f}#{end}:ex
		data.put("f", "hello world!");
		html = process(data);
		assertFalse(html.contains("hello world!"));
		data.put("e", UUID.randomUUID());
		html = process(data);
		assertTrue(html.contains("ex:hello world!:ex"));
		
		// !ex:#{!ex g}#{h}#{end}:!ex
		data.put("h", "foo");
		html = process(data);
		assertTrue(html.contains("foo"));
		data.put("g", false);
		html = process(data);
		assertFalse(html.contains("foo"));
		
		// eq:#{eq i 1}#{j}#{end}:eq
		data.put("j", 1234567L);
		html = process(data);
		assertTrue(html.contains("!ex eq::eq !eq"));
		data.put("i", 1);
		html = process(data);
		assertTrue(html.contains("eq:1234567:eq"));
		data.put("i", "a");
		html = process(data);
		assertFalse(html.contains("eq:1234567:eq"));
		data.put("i", 1);
		data.put("j", true);
		html = process(data);
		assertTrue(html.contains("eq:true:eq"));
		
		// !eq:#{!eq k 2}#{l}#{end}:!eq
		data.put("l", 1234567L);
		html = process(data);
		assertTrue(html.contains("!eq:1234567:!eq"));
		data.put("k", 2);
		html = process(data);
		assertTrue(html.contains("!eq::!eq"));
		data.put("k", false);
		data.put("l", true);
		html = process(data);
		assertTrue(html.contains("!eq:true:!eq"));
		data.put("k", 2);
		html = process(data);
		assertTrue(html.contains("!eq::!eq"));

		// for:#{for m n}#{m}#{end}:for
		html = process(data);
		assertTrue(html.contains("for::for"));
		data.put("n", 2);
		html = process(data);
		assertTrue(html.contains("for:2:for"));
		data.putList("n").add(1).add(2).add(3).add(4).add("X");
		html = process(data);
		assertTrue(html.contains("for:1234X:for"));
	}
	
	public String process(Tree data) throws Exception {
		return process("all.html", data);
	}
	
	@Test
	public void testPage() throws Exception {
		
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
		
		String html = process("template.html", data);
		assertTrue(html.contains("A: 1"));
		assertTrue(html.contains("B: true"));
		assertTrue(html.contains("C: &lt; &amp; &gt;"));
		assertTrue(html.contains("<td>12345</td>"));
		assertTrue(html.contains("<body><h1>header</h1><p>"));
		assertTrue(html.contains("true<li>6<li>12345<li>"));
		assertTrue(html.contains("12345<li>false<li>9</body></html>"));
	}

	@SuppressWarnings("unchecked")
	protected String process(String templatePath, Tree data) throws Exception {
		byte[] bytes1 = engine.process(templatePath, data);
		byte[] bytes2 = engine.process(templatePath, (Map<String, Object>) data.asObject());		
		String html1 = new String(bytes1, StandardCharsets.UTF_8);
		String html2 = new String(bytes2, StandardCharsets.UTF_8);		
		assertEquals(html1, html2);
		
		String html3 = engine.processToString(templatePath, data);	
		String html4 = engine.processToString(templatePath, (Map<String, Object>) data.asObject());
		assertEquals(html2, html3);
		assertEquals(html3, html4);
		
		return html1;
	}

	@Override
	protected void setUp() throws Exception {
		engine = new TemplateEngine();
		engine.setRootDirectory("/io/datatree/templates/html");
		engine.setReloadTemplates(false);
		engine.setTemplatePreProcessor(new SimpleHtmlMinifier());
		engine.setCharset(StandardCharsets.UTF_8);
	}

	@Override
	protected void tearDown() throws Exception {
		engine = null;
	}

}
