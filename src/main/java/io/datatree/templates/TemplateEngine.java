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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
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
 * 7.) User-defined functions (special HTML-formatters and renderers)<br>
 * <br>
 * Sub-template insertion:<br>
 * <br>
 * #{include ../parts/header.html}<br>
 * <br>
 * Shorter syntax with "in":<br>
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
 * <br>
 * Invoke custom function:<br>
 * <br>
 * #{function myFunction email}<br>
 * <br>
 * Shorter syntax with "fn", parameter is optional:<br>
 * <br>
 * #{fn myFunction}<br>
 */
public class TemplateEngine implements FragmentTypes {

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

	/**
	 * Custom, user-defined functions (special HTML renderers or formatters).
	 */
	protected Map<String, BiConsumer<StringBuilder, Tree>> functions = new HashMap<>();

	/**
	 * Cached StringBuilders.
	 */
	protected ThreadLocal<StringBuilder> builders = new ThreadLocal<>();
	
	// --- CONSTRUCTORS ---

	public TemplateEngine() {
		this(1024);
	}

	public TemplateEngine(int cacheSize) {
		cache = new Cache<>(cacheSize);
	}

	// --- PUBLIC PAGE-GENERATOR METHODS ---

	/**
	 * Executes template, using the Map-based data model provided, then
	 * returning the result as String.
	 * 
	 * @param templatePath
	 *            relative path to template with extension (eg. "index.html" or
	 *            "admin/login.html")
	 * @param data
	 *            data model as Map
	 * 
	 * @return rendered template as String
	 * 
	 * @throws IOException
	 *             any I/O or syntax exteption
	 */
	public String process(String templatePath, Map<String, Object> data) throws IOException {
		return process(templatePath, new Tree(data));
	}

	/**
	 * Executes template, using the Tree-based data model provided, then
	 * returning the result as String.
	 * 
	 * @param templatePath
	 *            relative path to template with extension (eg. "index.html" or
	 *            "admin/login.html")
	 * @param data
	 *            data model as Tree
	 * 
	 * @return rendered template as String
	 * 
	 * @throws IOException
	 *             any I/O or syntax exteption
	 */
	public String process(String templatePath, Tree data) throws IOException {
		StringBuilder builder = builders.get();
		if (builder == null) {
			builder = new StringBuilder(writeBufferSize);
			builders.set(builder);
		} else {
			builder.setLength(0);
		}
		String path = getAbsolutePath(templatePath);
		transform(path, builder, getTemplate(path), data, null);
		return builder.toString();
	}

	// --- DEFINE TEMPLATE BY SOURCE ---

	/**
	 * Adds a template based on its (HTML/TEXT/XML) source.
	 * 
	 * @param templatePath
	 *            "virtual" path of template (eg. "index.html" or
	 *            "admin/login.html")
	 * @param templateSource
	 *            source (~= HTML source and tags)
	 */
	public void define(String templatePath, String templateSource) {
		Fragment template = FragmentBuilder.compile(templateSource, templatePath, 1, functions);
		cache.put(templatePath, template);
	}

	/**
	 * Removes a template from the memory cache.
	 * 
	 * @param templatePath
	 *            path of template
	 */
	public void remove(String templatePath) {
		cache.remove(templatePath);
	}

	/**
	 * Returns true if the memory cache contains a mapping for the specified
	 * template.
	 * 
	 * @param templatePath
	 *            path of template
	 * 
	 * @return Returns true if the cache contains a mapping for the specified
	 *         template.
	 */
	public boolean contains(String templatePath) {
		return cache.get(templatePath) != null;
	}

	/**
	 * Removes all templates from the memory.
	 */
	public void clear() {
		cache.clear();
	}

	// --- CUSTOM FUNCTIONS / HTML RENDERERS ---

	public void addFunction(String name, BiConsumer<StringBuilder, Tree> function) {
		if (name == null || name.isEmpty() || name.contains(" ")) {
			throw new IllegalArgumentException("Invalid function name:" + name);
		}
		functions.put(name, Objects.requireNonNull(function));
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
			template = FragmentBuilder.compile(source, templatePath, lastModified, functions);
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
		case STATIC_TEXT:
			builder.append(command.content);
			break;

		case INSERTABLE_VARIABLE:
			String value = current.get(path, "");
			if (value != null && !value.isEmpty()) {
				if (escapeSpecialCharacters) {
					writeXMLContent(builder, value);
				} else {
					builder.append(value);
				}
			}
			break;

		case FOR_CYCLE:
			if (variables == null) {
				variables = new HashMap<String, Tree>();
			}
			Tree parent = current.get(path);
			if (parent != null) {
				for (Tree child : parent) {
					variables.put(command.content, child);
					transformChildren(basePath, builder, command, root, variables);
				}
			}
			variables = null;
			return;

		case CONDITION_TAG_EXISTS:
			if (current.get(path) == null) {
				return;
			}
			break;

		case CONDITION_TAG_NOT_EXISTS:
			if (current.get(path) == null) {
				break;
			}
			return;

		case CONDITION_TAG_VALUE_EQUALS:
			value = current.get(path, "");
			if (value.equals(command.content)) {
				break;
			}
			return;

		case CONDITION_TAG_VALUE_NOT_EQUALS:
			value = current.get(path, "");
			if (value.equals(command.content)) {
				return;
			}
			break;

		case INSERTABLE_TEMPLATE_FILE:
			String subTemplatePath = getAbsolutePath(basePath, command.arg);
			Fragment include = getTemplate(subTemplatePath);
			if (include == null) {
				throw new IllegalArgumentException("Missing template:" + subTemplatePath);
			}
			transform(subTemplatePath, builder, include, root, variables);
			break;

		case FUNCTION:
			if (command.content == null) {
				command.function.accept(builder, current);
			} else {
				command.function.accept(builder, current.get(command.content));
			}
			break;

		default:
			break;
		}
		transformChildren(basePath, builder, command, root, variables);
	}

	protected void transformChildren(String basePath, StringBuilder builder, Fragment command, Tree root,
			HashMap<String, Tree> variables) throws IOException {
		if (command.children != null) {
			for (Fragment child : command.children) {
				transform(basePath, builder, child, root, variables);
			}
		}
	}

	protected void writeXMLContent(StringBuilder builder, String str) {
		if (str.indexOf('<') == -1 && str.indexOf('>') == -1 && str.indexOf('&') == -1 && str.indexOf('"') == -1
				&& str.indexOf('\'') == -1) {
			builder.append(str);
			return;
		}
		char[] chars = str.trim().toCharArray();
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
					break;
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

	public int getWriteBufferSize() {
		return writeBufferSize;
	}

	public void setWriteBufferSize(int writeBufferSize) {
		this.writeBufferSize = writeBufferSize;
	}

}