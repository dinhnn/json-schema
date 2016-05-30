# JSON Schema Validator

* [When to use this library?](#when-to-use-this-library)
* [Maven installation](#maven-installation)
* [Quickstart](#quickstart)
* [Investigating failures](#investigating-failures)
* [Format validators](#format-validators)
  * [Example](#example)
* [Resolution scopes](#resolution-scopes)


This project is an implementation of the [JSON Schema Core Draft v4](http://json-schema.org/latest/json-schema-core.html) specification.
It uses the [org.json API](http://stleary.github.io/JSON-java/) (created by Douglas Crockford) for representing JSON data.

# When to use this library?

Lets assume that you already know what JSON Schema is, and you want to utilize it in a Java application to validate JSON data.
But - as you may have already discovered - there is also an [other Java implementation](https://github.com/fge/json-schema-validator)
of the JSON Schema specification. So here are some advices about which one to use:
 * if you use Jackson to handle JSON in Java code, then fge/json-schema-validator is obviously a better choice, since it
uses Jackson
 * if you want to use the [org.json API](http://www.json.org/java/) then this library is the better choice
 * if you want to use anything else for handling JSON (like GSON or javax.json), then you are in a little trouble, since
currently there is no schema validation library backed by these libraries. It means that you will have to parse the JSON
twice: once for the schema validator, and once for your own processing. In a case like that, this library is probably still
a better choice, since it seems to be [twice faster](https://github.com/erosb/json-schema-perftest) than the Jackson-based fge
library.


## Maven installation

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>org.everit.json</groupId>
    <artifactId>org.everit.json.schema</artifactId>
    <version>1.3.0</version>
</dependency>
```

## Quickstart


```java
import Schema;
import SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
// ...
try (InputStream inputStream = getClass().getResourceAsStream("/path/to/your/schema.json")) {
  JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
  Schema schema = SchemaLoader.load(rawSchema);
  schema.validate(new JSONObject("{\"hello\" : \"world\"}")); // throws a ValidationException if this object is invalid
}
```

## Investigating failures


Starting from version `1.1.0` the validator collects every schema violations (instead of failing immediately on the first
one). Each failure is denoted by a JSON pointer, pointing from the root of the document to the violating part. If  more
than one schema violations have been detected, then a `ValidationException` will be thrown at the most common parent
elements of the violations, and each separate violations can be obtained using the `ValidationException#getCausingExceptions()`
method.

To demonstrate the above concepts, lets see an example. Lets consider the following schema:

```json
{
	"type" : "object",
	"properties" : {
		"rectangle" : {"$ref" : "#/definitions/Rectangle" }
	},
	"definitions" : {
		"size" : {
			"type" : "number",
			"minimum" : 0
		},
		"Rectangle" : {
			"type" : "object",
			"properties" : {
				"a" : {"$ref" : "#/definitions/size"},
				"b" : {"$ref" : "#/definitions/size"}
			}
		}
	}
}
```

The following JSON document has only one violation against the schema (since "a" cannot be negative):

```json
{
	"rectangle" : {
		"a" : -5,
		"b" : 5
	}
}
```

In this case the thrown `ValidationException` will point to `#/rectangle/a` and it won't contain sub-exceptions:

```java
try {
  schema.validate(rectangleSingleFailure);
} catch (ValidationException e) {
  // prints #/rectangle/a: -5.0 is not higher or equal to 0
  System.out.println(e.getMessage());
}
```


Now - to illustrate the way how multiple violations are handled - lets consider the following JSON document, where both
the "a" and "b" properties violate the above schema:

```json
{
	"rectangle" : {
		"a" : -5,
		"b" : "asd"
	}
}
```

In this case the thrown `ValidationException` will point to `#/rectangle`, and it has 2 sub-exceptions, pointing to
`#/rectangle/a` and `#/rectangle/b` :

```java
try {
  schema.validate(rectangleMultipleFailures);
} catch (ValidationException e) {
  System.out.println(e.getMessage());
  e.getCausingExceptions().stream()
      .map(ValidationException::getMessage)
      .forEach(System.out::println);
}
```

This will print the following output:
```
#/rectangle: 2 schema violations found
#/rectangle/a: -5.0 is not higher or equal to 0
#/rectangle/b: expected type: Number, found: String
```



## Format validators


Starting from version `1.2.0` the library supports the [`"format"` keyword](http://json-schema.org/latest/json-schema-validation.html#anchor104)
(which is an optional part of the specification), so you can use the following formats in the schemas:

 * date-time
 * email
 * hostname
 * ipv4
 * ipv6
 * uri

The library also supports adding custom format validators. To use a custom validator basically you have to

 * create your own validation in a class implementing the `FormatValidator` interface
 * bind your validator to a name in a `SchemaLoader.SchemaLoaderBuilder` instance before loading the actual schema

### Example



Lets assume the task is to create a custom validator which accepts strings with an even number of characters.

The custom `FormatValidator` will look something like this:

```java
public class EvenCharNumValidator implements FormatValidator {

  @Override
  public Optional<String> validate(final String subject) {
    if (subject.length() % 2 == 0) {
      return Optional.empty();
    } else {
      return Optional.of(String.format("the length of srtring [%s] is odd", subject));
    }
  }

}
```

To bind the `EvenCharNumValidator` to a `"format"` value (for example `"evenlength"`) you have to bind a validator instance
to the keyword in the schema loader configuration:

```java
JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
SchemaLoader schemaLoader = SchemaLoader.builder()
	.schemaJson(rawSchema) // rawSchema is the JSON representation of the schema utilizing the "evenlength" non-standard format
	.addFormatValidator("evenlength", new EvenCharNumValidator()) // the EvenCharNumValidator gets bound to the "evenlength" keyword
	.build();
Schema schema = schemaLoader.load().build(); // the schema is created using the above created configuration
schema.validate(jsonDcoument);  // the document validation happens here
```


## Resolution scopes

In a JSON Schema document it is possible to use relative URIs to refer previously defined
types. Such references are expressed using the `"$ref"` and `"id"` keywords. While the specification describes resolution scope alteration and dereferencing in detail, it doesn't explain the expected behavior when the first occuring `"$ref"` or `"id"` is a relative URI.

In the case of this implementation it is possible to explicitly define an absolute URI serving as the base URI (resolution scope) using the appropriate builder method:

```java
SchemaLoader schemaLoader = SchemaLoader.builder()
        .schemaJson(jsonSchema)
        .resolutionScope("http://example.org/") // setting the default resolution scope
        .build();
```
