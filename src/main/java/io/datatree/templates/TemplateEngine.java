/**
 * This software is licensed under the Apache 2 license, quoted below.<br>
 * <br>
 * Copyright 2018 Andras Berkes [andras.berkes@programmer.net]<br>
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import io.datatree.Tree;
import io.datatree.dom.Cache;

/**
 * Server-side template engine. Combines a text template and data from a Tree
 * object. The Tree-based template engine capabilities are: <br>
 * <br>
 * 1.) Sub-templates (header / footer / etc) insertion<br>
 * 2.) Simple insertion (multiple levels, eg "user.address.city")<br>
 * 3.) Cycle on elements of a JSON array (eg "users" elements are repeated)<br>
 * 4.) Insert if a value exists or does not exist<br>
 * 5.) Insert if a value is the same or different from the specified value<br>
 * 6.) Can be used to generate txt, html, xhtml, xml files<br>
 * <br>
 * Sub-template insertion:<br>
 * <br>
 * #{include ../parts/header.html}<br>
 * <br>
 * Shorter syntax with "inc":<br>
 * <br>
 * #{in footer.txt}<br>
 * <br>
 * Simple variable insertion:<br>
 * <br>
 * Name of client: #{name}<br>
 * <br>
 * For a cycle of elements of a JSON array:<br>
 * <br>
 * #{for item : list}<br>
 * #{item.email}<br>
 * #{end}<br>
 * <br>
 * The use of a colon is optional:<br>
 * <br>
 * #{for item list}<br>
 * #{item.email}<br>
 * #{end}<br>
 * <br>
 * There may be multiple levels of the cycle:<br>
 * <br>
 * #{for row : rows}<br>
 * #{for cell : row}<br>
 * #{cell.email}<br>
 * #{end}<br>
 * #{end}<br>
 * <br>
 * Paste if there is a JSON parameter:<br>
 * <br>
 * #{exists email}<br>
 * Send mail to: {#email}<br>
 * #{end}<br>
 * <br>
 * Shorter syntax with "ex":<br>
 * <br>
 * #{ex email}<br>
 * Send mail to: {#email}<br>
 * #{end}<br>
 * <br>
 * Paste if NO value exists:<br>
 * <br>
 * #{!exists email}<br>
 * No email address provided!<br>
 * #{end}<br>
 * <br>
 * Shorter syntax with "!ex":<br>
 * <br>
 * #{!ex email}<br>
 * No email address provided!<br>
 * #{end}<br>
 * <br>
 * Paste if the value of the parameter is the same:<br>
 * <br>
 * #{equals email admin@foo.com}<br>
 * An administrator email address is provided.<br>
 * #{end}<br>
 * <br>
 * Shorter syntax with "eq":<br>
 * <br>
 * #{eq email admin@foo.com}<br>
 * An administrator email address is provided.<br>
 * #{end}<br>
 * <br>
 * Paste if NOT the same value as the given value:<br>
 * <br>
 * #{!equals email admin@foo.com}<br>
 * The administrator email address is not specified.<br>
 * #{end}<br>
 * <br>
 * Shorter syntax with "!eq":<br>
 * <br>
 * #{!eq email admin@foo.com}<br>
 * The administrator email address is not specified.<br>
 * #{end}<br>
 */
public class TemplateEngine {

	// --- VARIABLES ---

	/**
	 * Cached templates.
	 */
	protected final Cache<String, Fragment> cache;

	/**
	 * Interchangeable template loader.
	 */
	protected ResourceLoader loader = new DefaultLoader();

	/**
	 * Enables reload feature. Set to "false" in production mode!
	 */
	protected boolean reloadTemplates;

	/**
	 * Replaces special HTML characters (eg. "&lt;" to "&amp;lt;").
	 */
	protected boolean escapeSpecialCharacters = true;

	/**
	 * Charset of templates (default is "UTF-8").
	 */
	protected Charset charset = StandardCharsets.UTF_8;

	/**
	 * Initial size of write buffers.
	 */
	protected int writeBufferSize = 2048;

	/**
	 * Root directory of templates (eg. "/web/templates").
	 */
	protected String rootDirectory = "";

	/**
	 * Optional HTML pre-processor (eg. HTML minifier).
	 */
	protected Function<String, String> templatePreProcessor;
	
	// --- CONSTRUCTORS ---

	public TemplateEngine() {
		this(1024);
	}

	public TemplateEngine(int cacheSize) {
		cache = new Cache<>(cacheSize);
	}

	// --- TRANSFORM METHODS ---

	public byte[] process(String templatePath, Map<String, Object> data) throws Exception {
		return process(templatePath, new Tree(data));
	}

	public byte[] process(String templatePath, Tree data) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(writeBufferSize);
		String path = getAbsolutePath(templatePath);
		transform(path, stream, getTemplate(path), data, null);
		return stream.toByteArray();
	}

	public String processToString(String templatePath, Map<String, Object> data) throws Exception {
		return processToString(templatePath, new Tree(data));
	}

	public String processToString(String templatePath, Tree data) throws Exception {
		StringBuilder builder = new StringBuilder(writeBufferSize);
		String path = getAbsolutePath(templatePath);
		transform(path, builder, getTemplate(path), data, null);
		return builder.toString();
	}

	// --- PROTECTED METHODS ---
	
	protected Fragment getTemplate(String templatePath) throws IOException {
		Fragment template = cache.get(templatePath);
		boolean loadable = template == null;
		if (reloadTemplates && !loadable) {
			long modified = Long.parseLong(template.content);
			long lastModified = loader.lastModified(templatePath);
			loadable = lastModified < 1 || lastModified > modified;
		}
		if (loadable) {
			String source = loader.loadTemplate(templatePath, charset);
			if (templatePreProcessor != null) {
				source = templatePreProcessor.apply(source);
			}
			long lastModified = loader.lastModified(templatePath);
			template = FragmentBuilder.compile(source, templatePath, lastModified, charset);
			cache.put(templatePath, template);			
		}
		return template;
	}

	protected void transform(String basePath, StringBuilder builder, Fragment command, Tree root,
			HashMap<String, Tree> variables) throws IOException {
		String path = command.arg;
		Tree current = root;
		if (variables != null && path != null) {
			for (String key : variables.keySet()) {
				if (path.startsWith(key)) {
					if (path.length() == key.length()) {
						path = "";
					} else {
						path = path.substring(key.length() + 1);
					}
					current = variables.get(key);
					break;
				}
			}
		}	
		switch (command.type) {
		case (Fragment.STATIC_TEXT):
			builder.append(command.content);
			break;

		case (Fragment.INSERTABLE_VARIABLE):
			String value = current.get(path, "");
			if (value != null && !value.isEmpty()) {
				if (escapeSpecialCharacters) {
					writeXMLContent(builder, value);
				} else {
					builder.append(value);
				}
			}
			break;

		case (Fragment.FOR_CYCLE):
			if (variables == null) {
				variables = new HashMap<String, Tree>();
			}
			for (Tree child : current.get(path)) {
				variables.put(command.content, child);
				transformChildren(basePath, builder, command, root, variables);
			}
			variables = null;
			return;

		case (Fragment.CONDITION_TAG_EXISTS):
			if (current.get(path) == null) {
				return;
			}
			break;

		case (Fragment.CONDITION_TAG_NOT_EXISTS):
			if (current.get(path) == null) {
				break;
			}
			return;

		case (Fragment.CONDITION_TAG_VALUE_EQUALS):
			value = current.get(path, "");
			if (value.equals(command.content)) {
				break;
			}
			return;

		case (Fragment.CONDITION_TAG_VALUE_NOT_EQUALS):
			value = current.get(path, "");
			if (value.equals(command.content)) {
				return;
			}
			break;

		case (Fragment.INSERTABLE_TEMPLATE_FILE):
			String subTemplatePath = getAbsolutePath(basePath, command.arg);
			Fragment include = getTemplate(subTemplatePath);
			if (include == null) {
				return;
			}
			transform(subTemplatePath, builder, include, root, variables);
			break;
		}
		transformChildren(basePath, builder, command, root, variables);
	}

	protected void transform(String basePath, ByteArrayOutputStream stream, Fragment command, Tree root,
			HashMap<String, Tree> variables) throws IOException {
		String path = command.arg;
		Tree current = root;
		if (variables != null && path != null) {
			for (String key : variables.keySet()) {
				if (path.startsWith(key)) {
					if (path.length() == key.length()) {
						path = "";
					} else {
						path = path.substring(key.length() + 1);
					}
					current = variables.get(key);
					break;
				}
			}
		}		
		switch (command.type) {
		case (Fragment.STATIC_TEXT):
			stream.write(command.body);
			break;

		case (Fragment.INSERTABLE_VARIABLE):
			String value = current.get(path, "");
			if (value != null && !value.isEmpty()) {
				if (escapeSpecialCharacters) {
					writeXMLContent(stream, value);
				} else {
					stream.write(value.getBytes(charset));
				}
			}
			break;

		case (Fragment.FOR_CYCLE):
			if (variables == null) {
				variables = new HashMap<String, Tree>();
			}
			for (Tree child : current.get(path)) {
				variables.put(command.content, child);
				transformChildren(basePath, stream, command, root, variables);
			}
			variables = null;
			return;

		case (Fragment.CONDITION_TAG_EXISTS):
			if (current.get(path) == null) {
				return;
			}
			break;

		case (Fragment.CONDITION_TAG_NOT_EXISTS):
			if (current.get(path) == null) {
				break;
			}
			return;

		case (Fragment.CONDITION_TAG_VALUE_EQUALS):
			value = current.get(path, "");
			if (value.equals(command.content)) {
				break;
			}
			return;

		case (Fragment.CONDITION_TAG_VALUE_NOT_EQUALS):
			value = current.get(path, "");
			if (value.equals(command.content)) {
				return;
			}
			break;

		case (Fragment.INSERTABLE_TEMPLATE_FILE):
			String subTemplatePath = getAbsolutePath(basePath, command.arg);
			Fragment include = getTemplate(subTemplatePath);
			if (include == null) {
				return;
			}
			transform(subTemplatePath, stream, include, root, variables);
			break;
		}
		transformChildren(basePath, stream, command, root, variables);
	}

	protected void transformChildren(String basePath, ByteArrayOutputStream stream, Fragment command, Tree root,
			HashMap<String, Tree> variables) throws IOException {
		if (command.children != null) {
			for (Fragment child : command.children) {
				transform(basePath, stream, child, root, variables);
			}
		}
	}

	protected void transformChildren(String basePath, StringBuilder builder, Fragment command, Tree root,
			HashMap<String, Tree> variables) throws IOException {
		if (command.children != null) {
			for (Fragment child : command.children) {
				transform(basePath, builder, child, root, variables);
			}
		}
	}

	protected void writeXMLContent(ByteArrayOutputStream out, String fromString) throws IOException {
		if (fromString == null || fromString.isEmpty()) {
			return;
		}
		StringBuilder builder = new StringBuilder(fromString.length() * 2);
		writeXMLContent(builder, fromString);
		out.write(builder.toString().getBytes(charset));
	}

	protected void writeXMLContent(StringBuilder builder, String fromString) {
		char[] chars = fromString.trim().toCharArray();
		if (chars.length != 0) {
			char c;
			for (int n = 0; n < chars.length; n++) {
				c = chars[n];
				switch (c) {
				case '<':
					builder.append("&lt;");
					break;
				case '>':
					builder.append("&gt;");
					break;
				case '&':
					builder.append("&amp;");
					break;
				case '"':
					builder.append("&quot;");
					break;
				case '\'':
					builder.append("&#x27;");
					break;
				default:
					builder.append(c);
				}
			}
		}
	}

	protected String getAbsolutePath(String relativePath) {
		String path = relativePath.replace('\\', '/');
		if (rootDirectory.isEmpty()) {
			return relativePath;
		}
		if (path.startsWith("/")) {
			return rootDirectory + path;
		}
		return rootDirectory + '/' + path;
	}
	
	protected String getAbsolutePath(String basePath, String relativePath) {
		try {
			if (relativePath != null) {
				if (relativePath.startsWith(".")) {

					// '../file'
					int i = 0;
					String tmpURL = basePath.substring(0, basePath.lastIndexOf('/'));
					while (relativePath.indexOf("..", i) != -1) {
						i += 3;
						tmpURL = tmpURL.substring(0, tmpURL.lastIndexOf('/'));
					}
					return tmpURL + '/' + relativePath.substring(i, relativePath.length());
				}
				if (relativePath.startsWith("/") || relativePath.indexOf(":/") != -1) {

					// '/directory/file'
					// 'c:\windows\file'
					return relativePath;
				}

				// 'directory/file'
				int i = basePath.lastIndexOf('/');
				if (i == -1) {
					return relativePath;
				}
				return basePath.substring(0, basePath.lastIndexOf('/')) + '/' + relativePath;
			}
			return basePath;
		} catch (Throwable t) {
			throw new IllegalArgumentException(t.getMessage());
		}
	}

	// --- GETTERS AND SETTERS ---

	public boolean isReloadTemplates() {
		return reloadTemplates;
	}

	public void setReloadTemplates(boolean reloadTemplates) {
		this.reloadTemplates = reloadTemplates;
	}

	public ResourceLoader getLoader() {
		return loader;
	}

	public void setLoader(ResourceLoader loader) {
		this.loader = Objects.requireNonNull(loader);
	}

	public boolean isEscapeSpecialCharacters() {
		return escapeSpecialCharacters;
	}

	public void setEscapeSpecialCharacters(boolean escapeSpecialCharacters) {
		this.escapeSpecialCharacters = escapeSpecialCharacters;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		if (this.charset != charset) {
			this.charset = Objects.requireNonNull(charset);
			cache.clear();
		}
	}

	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		String path = Objects.requireNonNull(rootDirectory);
		path = path.replace('\\', '/');
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		this.rootDirectory = path;
	}

	public Function<String, String> getTemplatePreProcessor() {
		return templatePreProcessor;
	}

	public void setTemplatePreProcessor(Function<String, String> templatePreProcessor) {
		this.templatePreProcessor = templatePreProcessor;
	}

}