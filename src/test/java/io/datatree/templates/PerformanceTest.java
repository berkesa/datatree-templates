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

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.resolver.FileSystemResolver;
import com.mitchellbosecke.pebble.loader.FileLoader;

import de.neuland.jade4j.Jade4J.Mode;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.model.JadeModel;
import freemarker.template.Configuration;
import io.datatree.Tree;

public class PerformanceTest {

	@SuppressWarnings("unchecked")
	public static final void main(String[] args) throws Exception {

		// Create JSON structure
		Tree data = new Tree();
		for (int i = 0; i < 100; i++) {
			data.put("key" + i, "value" + i);	
		}
		Tree table = data.putList("table");
		for (int i = 0; i < 10; i++) {
			Tree row = table.addMap();
			for (int c = 0; c < 10; c++) {
				row.put("cell" + c, i * c);
			}
		}
		Map<String, Object> map = (Map<String, Object>) data.asObject();
		long max = 50000;
		System.out.println("Generating " + max + " pages (per engines), please wait...");
		
		// DataTree
		io.datatree.templates.TemplateEngine e1 = new io.datatree.templates.TemplateEngine();
		e1.setRootDirectory("/io/datatree/templates/html");
		byte[] rsp = null;
		for (int i = 0; i < 10; i++) {
			rsp = dataTreeGen(map, e1);
		}
		// System.out.println("DataTree Templates:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		long start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = dataTreeGen(map, e1);
		}
		long duration = System.currentTimeMillis() - start;
		System.out.println("DataTree Templates: " + duration + " msec");
		
		// Freemarker
		freemarker.template.Configuration e2 = new freemarker.template.Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		e2.setDefaultEncoding("UTF-8");
		e2.setClassForTemplateLoading(PerformanceTest.class, "/io/datatree/templates/html");
		e2.setCacheStorage(new freemarker.cache.StrongCacheStorage());
		for (int i = 0; i < 10; i++) {
			rsp = freeMarkerGen(map, e2);
		}
		// System.out.println("FreeMarker:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = freeMarkerGen(map, e2);
		}
		duration = System.currentTimeMillis() - start;
		System.out.println("FreeMarker: " + duration + " msec");
		
		// Jade
		JadeConfiguration e3 = new JadeConfiguration();
		e3.setCaching(true);
		e3.setPrettyPrint(false);
		URL url = PerformanceTest.class.getResource("/io/datatree/templates/html/all.html");
		e3.setMode(Mode.HTML);
		String dir = new File(url.getFile()).getParent();
		String name = dir + "/test.jade";
		for (int i = 0; i < 10; i++) {
			rsp = jadeGen(name, map, e3);
		}
		// System.out.println("Jade:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = jadeGen(name, map, e3);
		}
		duration = System.currentTimeMillis() - start;
		System.out.println("Jade: " + duration + " msec");
		
		// Mustache
		DefaultMustacheFactory e4 = new DefaultMustacheFactory(new FileSystemResolver());
		name = dir + "/test.mustache";
		for (int i = 0; i < 10; i++) {
			rsp = mustacheGen(name, map, e4);
		}
		// System.out.println("Mustache:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = mustacheGen(name, map, e4);
		}
		duration = System.currentTimeMillis() - start;
		System.out.println("Mustache: " + duration + " msec");
		
		// Pebble
		FileLoader loader = new FileLoader();
		loader.setPrefix(dir);
		com.mitchellbosecke.pebble.PebbleEngine e5 = new com.mitchellbosecke.pebble.PebbleEngine.Builder().cacheActive(true).loader(loader).build();
		for (int i = 0; i < 10; i++) {
			rsp = pebbleGen(map, e5);
		}
		//System.out.println("Pebble:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = pebbleGen(map, e5);
		}
		duration = System.currentTimeMillis() - start;
		System.out.println("Pebble: " + duration + " msec");
		
		// Thymeleaf
		org.thymeleaf.TemplateEngine e6 = new org.thymeleaf.TemplateEngine();
		FileTemplateResolver r = new FileTemplateResolver();
		r.setCacheable(true);
		r.setCharacterEncoding("UTF-8");
		r.setPrefix(dir + "/");
		e6.setTemplateResolver(r);
		for (int i = 0; i < 10; i++) {
			rsp = thymeleafGen(map, e6);
		}
		// System.out.println("Thymeleaf:\r\n"  + new String(rsp, StandardCharsets.UTF_8));
		start = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {
			rsp = thymeleafGen(map, e6);
		}
		duration = System.currentTimeMillis() - start;
		System.out.println("Thymeleaf: " + duration + " msec");
	}

	private static final byte[] thymeleafGen(Map<String, Object> map, org.thymeleaf.TemplateEngine e6) throws Exception {
		StringWriter out = new StringWriter(2048);
		e6.process("test.thymeleaf", new Context(Locale.ENGLISH, map), out);
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static final byte[] pebbleGen(Map<String, Object> map, com.mitchellbosecke.pebble.PebbleEngine e5) throws Exception {
		StringWriter out = new StringWriter(2048);
		e5.getTemplate("test.pebble").evaluate(out, map);
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}
	
	private static final byte[] mustacheGen(String name, Map<String, Object> map, DefaultMustacheFactory e4) throws Exception {
		StringWriter out = new StringWriter(2048);
		e4.compile(name).execute(out, map);
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}
	
	private static final byte[] dataTreeGen(Map<String, Object> map, io.datatree.templates.TemplateEngine e1) throws Exception {
		return e1.processToString("test.datatree", map).getBytes(StandardCharsets.UTF_8);
	}
	
	private static final byte[] freeMarkerGen(Map<String, Object> map, freemarker.template.Configuration e2) throws Exception {
		StringWriter out = new StringWriter(2048);
		e2.getTemplate("test.freemarker").process(map, out);
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}
	
	private static final byte[] jadeGen(String name, Map<String, Object> map, JadeConfiguration e3) throws Exception {
		StringWriter out = new StringWriter(2048);
		e3.getTemplate(name).process(new JadeModel(map), out);
		return out.toString().getBytes(StandardCharsets.UTF_8);
	}
}
