[![Build Status](https://travis-ci.org/berkesa/datatree-templates.svg?branch=master)](https://travis-ci.org/berkesa/datatree-templates)
[![codecov](https://codecov.io/gh/berkesa/datatree-templates/branch/master/graph/badge.svg)](https://codecov.io/gh/berkesa/datatree-templates)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/berkesa/datatree/master/LICENSE)

# Template Engine based on DataTree API

Small and fast template engine capable of producing html, xml, and plain text files.
The template engine works with hierarchical collection structures (cannot be used with POJO classes).
Its operating logic is very simple - similar to the Mustache Engine - which makes it pretty fast.

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>datatree-templates</artifactId>
		<version>1.0.1</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'datatree-templates', version: '1.0.1' 
}
```

## Usage

### Creating templates directly

```java
TemplateEngine engine = new TemplateEngine();
engine.define("page.html", "Hello #{name} !");

Tree data = new Tree();
data.put("name", "Tom");

// The "out" contains "Hello Tom !" in UTF-8
byte[] out = engine.process("page.html", data);
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

// The "out" contains the "index.html" in UTF-8
byte[] out = engine.process("index.html", data);
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
TemplateEngine engine = new TemplateEngine();
engine.setTemplatePreProcessor(new SimpleHtmlMinifier());
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

## License

DataTree-Templates is licensed under the Apache License V2, you can use it in your commercial products for free.
