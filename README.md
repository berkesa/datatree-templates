[![Build Status](https://travis-ci.org/berkesa/datatree-templates.svg?branch=master)](https://travis-ci.org/berkesa/datatree-templates)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ce3625975fd940829e3f9cfb163b7a9f)](https://www.codacy.com/app/berkesa/datatree-templates?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=berkesa/datatree-templates&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/berkesa/datatree-templates/branch/master/graph/badge.svg)](https://codecov.io/gh/berkesa/datatree-templates)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/berkesa/datatree/master/LICENSE)

# Template Engine based on DataTree API

Small and fast template engine capable of producing html, xml, and plain text files. The template engine works with hierarchical collection structures - similar to the Mustache Engine but with expandable features. Its operating logic is very simple which makes it pretty fast:

<p align="center">
<a href="https://github.com/berkesa/datatree-templates/blob/master/src/test/java/io/datatree/templates/PerformanceTest.java">
<img src="https://raw.githubusercontent.com/berkesa/datatree-templates/master/docs/chart.png">
</a>
</p>

## Capabilities
 
- Sub-templates (header/footer) insertion
- Simple insertion (multiple levels with JSON-path "user.address[2].city")
- Loop on elements of a JSON array/map (for creating tables and lists)
- Insert if a value exists or does not exist
- Insert if a value is the same or different from the specified value
- Can be used to generate TXT, HTML, XHTML or XML files (character escaping)
- User-defined functions/macros (special HTML-formatters and renderers)

## Limitations

Data must **NOT** contain POJO objects, only Collections (Maps, Lists, object arrays) with primitive types and Strings (or any object that can be easily converted to String). The contents of a POJO object can only be inserted into the templates with user-defined functions. No built-in multilingual support. Syntax isn't flexible; complicated logic conditions cannot be specified in the templates, only a few simpler condition types can be used.

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>datatree-templates</artifactId>
		<version>1.1.1</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'datatree-templates', version: '1.1.1' 
}
```

## Usage

### Creating templates directly

```java
TemplateEngine engine = new TemplateEngine();
engine.define("page.html", "Hello #{name}!");

Tree data = new Tree();
data.put("name", "Tom");

// The "out" contains "Hello Tom!"
String out = engine.process("page.html", data);
```

### Working from a directory

```java
TemplateEngine engine = new TemplateEngine();
engine.setRootDirectory("/www");
engine.setReloadTemplates(false);
engine.setCharset(StandardCharsets.UTF_8);

// The "data" can be Tree but can also be Map:
Map<String, Object> data = new HashMap<>();
data.put("key", "value");

// The "out" contains the merged "index.html"
String out = engine.process("index.html", data);
```

### Using custom loader

The default template-loader loads from classpath and file system. You can create your own template-loader by implementing the "io.datatree.templates.ResourceLoader" interface.

```java
TemplateEngine engine = new TemplateEngine();
engine.setLoader(new CustomResourceLoader());
```

### Using custom preprocessor

Template Preprocessor runs after the loader loads a template. If the cache is enabled (~= engine.setReloadable(false)), it will only run once per template. For example, this feature can be used to minimize HTML-pages.

```java
import io.datatree.templates.SimpleHtmlMinifier;

// ...

TemplateEngine engine = new TemplateEngine();
engine.setTemplatePreProcessor(new SimpleHtmlMinifier());
```

The following example shows how to embed Google's HtmlCompressor as a preprocessor:

```java
public class GoogleMinifier extends HtmlCompressor implements Function<String, String> {

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

// Use the HtmlCompressor:
TemplateEngine engine = new TemplateEngine();
engine.setTemplatePreProcessor(new GoogleMinifier());
```

The following two dependency is required for the example above:

```java
compile group: 'com.googlecode.htmlcompressor', name: 'htmlcompressor', version: '1.5.2'
compile group: 'com.yahoo.platform.yui', name: 'yuicompressor', version: '2.4.8'
```

### Template syntax

Sub-template insertion:

```html
#{include ../parts/header.html}
```

Shorter syntax with "in":

```html
#{in parts/footer.txt}
```

Simple variable insertion:

```html
<p> Name: #{name} </p>
<p> Age:  #{age} </p>
<p> Can be used for JSON-path syntax:  #{users[0].address.zipcode} </p>
```

For a cycle of elements of a JSON array:

```html
#{for item : list}
	#{item.email}
#{end}
```

The use of a colon is optional:

```html
#{for item list}
	<tr>
		<td> #{item.id} </td>
		<td> #{item.name} </td>
		<td> #{item.description} </td>
	</tr>
#{end}
```

There may be multiple levels of the cycle:

```html
#{for row : rows}
	#{for cell : row}
		#{cell.email}
	#{end}
#{end}
```

Paste if there is a JSON parameter:

```html
#{exists email}
	<!-- appears if the "email" parameter exists -->
	Send mail to: {#email}
#{end}
```

Shorter syntax with "ex":

```html
#{ex email}
	Send mail to: {#email}
#{end}
```

Paste if NO value exists:

```html
#{!exists email}
	<!-- appears only when the parameter is missing -->
	No email address provided!
#{end}
```

Shorter syntax with "!ex":

```html
#{!ex email}
	No email address provided!
#{end}
```

Paste if the value of the parameter is the same:

```html
#{equals email admin@foo.com}
	<!-- appears when the "email" parameter is "email admin@foo.com" -->
	An administrator email address is provided.
#{end}
```

Shorter syntax with "eq":

```html
#{eq email admin@foo.com}
	An administrator email address is provided.
#{end}
```

Paste if NOT the same value as the given value:

```html
#{!equals email admin@foo.com}
	<!-- appears when the "email" parameter is NOT "email admin@foo.com" -->
	The administrator email address is not specified.
#{end}
```

Shorter syntax with "!eq":

```html
#{!eq email admin@foo.com}
	The administrator email address is not specified.
#{end}
```

Invoke user-defined renderer / function:

```html
#{function formatEmail email}
```

Create the "formatEmail" function:

```java
engine.addFunction("formatEmail", (out, data) -> {
  if (data != null) {
    out.append(data.asString().replace("@", "[at]");
  }
});
```

Shorter syntax with "fn", parameter is optional:

```html
#{fn time}
```

Create the "time" function:

```java
engine.addFunction("time", (out, data) -> {
  out.append(new Date());
});
```

## License

DataTree-Templates is licensed under the Apache License V2, you can use it in your commercial products for free.
