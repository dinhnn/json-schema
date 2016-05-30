/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.json.schema.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ArraySchema;
import io.vertx.json.schema.BooleanSchema;
import io.vertx.json.schema.CombinedSchema;
import io.vertx.json.schema.EmptySchema;
import io.vertx.json.schema.EnumSchema;
import io.vertx.json.schema.FormatValidator;
import io.vertx.json.schema.NotSchema;
import io.vertx.json.schema.NullSchema;
import io.vertx.json.schema.NumberSchema;
import io.vertx.json.schema.ObjectSchema;
import io.vertx.json.schema.ObjectSchema.Builder;
import io.vertx.json.schema.ReferenceSchema;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.StringSchema;
import io.vertx.json.schema.internal.DateTimeFormatValidator;
import io.vertx.json.schema.internal.EmailFormatValidator;
import io.vertx.json.schema.internal.HostnameFormatValidator;
import io.vertx.json.schema.internal.IPV4Validator;
import io.vertx.json.schema.internal.IPV6Validator;
import io.vertx.json.schema.internal.URIFormatValidator;
import io.vertx.json.schema.loader.internal.DefaultSchemaClient;
import io.vertx.json.schema.loader.internal.JSONPointer;
import io.vertx.json.schema.loader.internal.JSONPointer.QueryResult;
import io.vertx.json.schema.loader.internal.ReferenceResolver;
import io.vertx.json.schema.loader.internal.TypeBasedMultiplexer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Loads a JSON schema's JSON representation into schema validator instances.
 */
public class SchemaLoader {

  /**
   * Alias for {@code Function<Collection<Schema>, CombinedSchema.Builder>}.
   */
  @FunctionalInterface
  private interface CombinedSchemaProvider
      extends Function<Collection<Schema>, CombinedSchema.Builder> {

  }

  /**
   * Builder class for {@link SchemaLoader}.
   */
  public static class SchemaLoaderBuilder {

    SchemaClient httpClient = new DefaultSchemaClient();

    JsonObject schemaJson;

    JsonObject rootSchemaJson;

    Map<String, ReferenceSchema.Builder> pointerSchemas = new HashMap<>();

    URI id;

    Map<String, FormatValidator> formatValidators = new HashMap<>();

    {
      formatValidators.put("date-time", new DateTimeFormatValidator());
      formatValidators.put("uri", new URIFormatValidator());
      formatValidators.put("email", new EmailFormatValidator());
      formatValidators.put("ipv4", new IPV4Validator());
      formatValidators.put("ipv6", new IPV6Validator());
      formatValidators.put("hostname", new HostnameFormatValidator());
    }

    public SchemaLoaderBuilder addFormatValidator(final String formatName,
        final FormatValidator formatValidator) {
      formatValidators.put(formatName, formatValidator);
      return this;
    }

    public SchemaLoader build() {
      return new SchemaLoader(this);
    }

    public JsonObject getRootSchemaJson() {
      return rootSchemaJson == null ? schemaJson : rootSchemaJson;
    }

    public SchemaLoaderBuilder httpClient(final SchemaClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /**
     * Sets the initial resolution scope of the schema. {@code id} and {@code $ref} attributes
     * accuring in the schema will be resolved against this value.
     *
     * @param id
     *          the initial (absolute) URI, used as the resolution scope.
     * @return {@code this}
     */
    public SchemaLoaderBuilder resolutionScope(final String id) {
      try {
        return resolutionScope(new URI(id));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    public SchemaLoaderBuilder resolutionScope(final URI id) {
      this.id = id;
      return this;
    }

    SchemaLoaderBuilder pointerSchemas(final Map<String, ReferenceSchema.Builder> pointerSchemas) {
      this.pointerSchemas = pointerSchemas;
      return this;
    }

    SchemaLoaderBuilder rootSchemaJson(final JsonObject rootSchemaJson) {
      this.rootSchemaJson = rootSchemaJson;
      return this;
    }

    public SchemaLoaderBuilder schemaJson(final JsonObject schemaJson) {
      this.schemaJson = schemaJson;
      return this;
    }

    SchemaLoaderBuilder formatValidators(final Map<String, FormatValidator> formatValidators) {
      this.formatValidators = formatValidators;
      return this;
    }

  }

  private static final List<String> ARRAY_SCHEMA_PROPS = Arrays.asList("items", "additionalItems",
      "minItems",
      "maxItems",
      "uniqueItems");

  private static final Map<String, CombinedSchemaProvider> COMB_SCHEMA_PROVIDERS = new HashMap<>(3);

  private static final List<String> NUMBER_SCHEMA_PROPS = Arrays.asList("minimum", "maximum",
      "minimumExclusive", "maximumExclusive", "multipleOf");

  private static final List<String> OBJECT_SCHEMA_PROPS = Arrays.asList("properties", "required",
      "minProperties",
      "maxProperties",
      "dependencies",
      "patternProperties",
      "additionalProperties");

  private static final List<String> STRING_SCHEMA_PROPS = Arrays.asList("minLength", "maxLength",
      "pattern", "format");

  static {
    COMB_SCHEMA_PROVIDERS.put("allOf", CombinedSchema::allOf);
    COMB_SCHEMA_PROVIDERS.put("anyOf", CombinedSchema::anyOf);
    COMB_SCHEMA_PROVIDERS.put("oneOf", CombinedSchema::oneOf);
  }

  public static SchemaLoaderBuilder builder() {
    return new SchemaLoaderBuilder();
  }

  /**
   * Loads a JSON schema to a schema validator using a {@link DefaultSchemaClient default HTTP
   * client}.
   *
   * @param schemaJson
   *          the JSON representation of the schema.
   * @return the schema validator object
   */
  public static Schema load(final JsonObject schemaJson) {
    return SchemaLoader.load(schemaJson, new DefaultSchemaClient());
  }

  /**
   * Creates Schema instance from its JSON representation.
   *
   * @param schemaJson
   *          the JSON representation of the schema.
   * @param httpClient
   *          the HTTP client to be used for resolving remote JSON references.
   * @return the created schema
   */
  public static Schema load(final JsonObject schemaJson, final SchemaClient httpClient) {
    SchemaLoader loader = builder()
        .schemaJson(schemaJson)
        .httpClient(httpClient)
        .build();
    return loader.load().build();
  }

  /**
   * Returns the absolute URI without its fragment part.
   *
   * @param fullUri
   *          the abslute URI
   * @return the URI without the fragment part
   */
  static URI withoutFragment(final String fullUri) {
    int containsKeyhmarkIdx = fullUri.indexOf('#');
    String rval;
    if (containsKeyhmarkIdx == -1) {
      rval = fullUri;
    } else {
      rval = fullUri.substring(0, containsKeyhmarkIdx);
    }
    try {
      return new URI(rval);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private final SchemaClient httpClient;

  private URI id = null;

  private final Map<String, ReferenceSchema.Builder> pointerSchemas;

  private final JsonObject rootSchemaJson;

  private final JsonObject schemaJson;

  private final Map<String, FormatValidator> formatValidators;

  /**
   * Constructor.
   *
   * @param builder
   *          the builder containing the properties. Only {@link SchemaLoaderBuilder#id} is
   *          nullable.
   * @throws NullPointerException
   *           if any of the builder properties except {@link SchemaLoaderBuilder#id id} is
   *           {@code null}.
   */
  public SchemaLoader(final SchemaLoaderBuilder builder) {
    this.schemaJson = Objects.requireNonNull(builder.schemaJson, "schemaJson cannot be null");
    this.rootSchemaJson = Objects.requireNonNull(builder.getRootSchemaJson(),
        "rootSchemaJson cannot be null");
    URI id = builder.id;
    if (id == null && builder.schemaJson.containsKey("id")) {
      try {
        id = new URI(builder.schemaJson.getString("id"));
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    this.id = id;
    this.httpClient = Objects.requireNonNull(builder.httpClient, "httpClient cannot be null");
    this.pointerSchemas = Objects.requireNonNull(builder.pointerSchemas,
        "pointerSchemas cannot be null");
    this.formatValidators = Objects.requireNonNull(builder.formatValidators,
        "formatValidators cannot be null");
  }

  /**
   * Constructor.
   *
   * @deprecated use {@link SchemaLoader#SchemaLoader(SchemaLoaderBuilder)} instead.
   */
  @Deprecated
  SchemaLoader(final String id, final JsonObject schemaJson,
      final JsonObject rootSchemaJson, final Map<String, ReferenceSchema.Builder> pointerSchemas,
      final SchemaClient httpClient) {
    this(builder().schemaJson(schemaJson)
        .rootSchemaJson(rootSchemaJson)
        .resolutionScope(id)
        .httpClient(httpClient)
        .pointerSchemas(pointerSchemas));
  }

  private void addDependencies(final Builder builder, final JsonObject deps) {
    deps.fieldNames().stream()
        .forEach(ifPresent -> addDependency(builder, ifPresent, deps.getValue(ifPresent)));
  }

  private void addDependency(final Builder builder, final String ifPresent, final Object deps) {
    typeMultiplexer(deps)
        .ifObject().then(obj -> {
          builder.schemaDependency(ifPresent, loadChild(obj).build());
        })
        .ifIs(JsonArray.class).then(propNames -> {
          IntStream.range(0, propNames.size())
              .mapToObj(i -> propNames.getString(i))
              .forEach(dependency -> builder.propertyDependency(ifPresent, dependency));
        }).requireAny();
  }

  private void addFormatValidator(final StringSchema.Builder builder, final String formatName) {
    getFormatValidator(formatName).ifPresent(builder::formatValidator);
  }

  private void addPropertySchemaDefinition(final String keyOfObj, final Object definition,
      final ObjectSchema.Builder builder) {
    typeMultiplexer(definition)
        .ifObject()
        .then(obj -> {
          builder.addPropertySchema(keyOfObj, loadChild(obj).build());
        })
        .requireAny();
  }

  private CombinedSchema.Builder buildAnyOfSchemaForMultipleTypes() {
    JsonArray subtypeJsons = schemaJson.getJsonArray("type");
    Map<String, Object> dummyJson = new HashMap<String, Object>();
    Collection<Schema> subschemas = new ArrayList<Schema>(subtypeJsons.size());
    for (int i = 0; i < subtypeJsons.size(); ++i) {
      Object subtypeJson = subtypeJsons.getValue(i);
      dummyJson.put("type", subtypeJson);
      JsonObject child = new JsonObject(dummyJson);
      subschemas.add(loadChild(child).build());
    }
    return CombinedSchema.anyOf(subschemas);
  }

  private ArraySchema.Builder buildArraySchema() {
    ArraySchema.Builder builder = ArraySchema.builder();
    ifPresent("minItems", Integer.class, builder::minItems);
    ifPresent("maxItems", Integer.class, builder::maxItems);
    ifPresent("uniqueItems", Boolean.class, builder::uniqueItems);
    if (schemaJson.containsKey("additionalItems")) {
      typeMultiplexer("additionalItems", schemaJson.getValue("additionalItems"))
          .ifIs(Boolean.class).then(builder::additionalItems)
          .ifObject().then(jsonObj -> builder.schemaOfAdditionalItems(loadChild(jsonObj).build()))
          .requireAny();
    }
    if (schemaJson.containsKey("items")) {
      typeMultiplexer("items", schemaJson.getValue("items"))
          .ifObject().then(itemSchema -> builder.allItemSchema(loadChild(itemSchema).build()))
          .ifIs(JsonArray.class).then(arr -> buildTupleSchema(builder, arr))
          .requireAny();
    }
    return builder;
  }

  private EnumSchema.Builder buildEnumSchema() {
    Set<Object> possibleValues = new HashSet<>();
    JsonArray arr = schemaJson.getJsonArray("enum");
    IntStream.range(0, arr.size())
        .mapToObj(arr::getValue)
        .forEach(possibleValues::add);
    return EnumSchema.builder().possibleValues(possibleValues);
  }

  private NotSchema.Builder buildNotSchema() {
    Schema mustNotMatch = loadChild(schemaJson.getJsonObject("not")).build();
    return NotSchema.builder().mustNotMatch(mustNotMatch);
  }

  private NumberSchema.Builder buildNumberSchema() {
    NumberSchema.Builder builder = NumberSchema.builder();
    ifPresent("minimum", Number.class, builder::minimum);
    ifPresent("maximum", Number.class, builder::maximum);
    ifPresent("multipleOf", Number.class, builder::multipleOf);
    ifPresent("exclusiveMinimum", Boolean.class, builder::exclusiveMinimum);
    ifPresent("exclusiveMaximum", Boolean.class, builder::exclusiveMaximum);
    return builder;
  }

  private ObjectSchema.Builder buildObjectSchema() {
    ObjectSchema.Builder builder = ObjectSchema.builder();
    ifPresent("minProperties", Integer.class, builder::minProperties);
    ifPresent("maxProperties", Integer.class, builder::maxProperties);
    if (schemaJson.containsKey("properties")) {
      typeMultiplexer(schemaJson.getValue("properties"))
          .ifObject().then(propertyDefs -> {
            populatePropertySchemas(propertyDefs, builder);
          }).requireAny();
    }
    if (schemaJson.containsKey("additionalProperties")) {
      typeMultiplexer("additionalProperties", schemaJson.getValue("additionalProperties"))
          .ifIs(Boolean.class).then(builder::additionalProperties)
          .ifObject().then(def -> builder.schemaOfAdditionalProperties(loadChild(def).build()))
          .requireAny();
    }
    if (schemaJson.containsKey("required")) {
      JsonArray requiredJson = schemaJson.getJsonArray("required");
      IntStream.range(0, requiredJson.size())
          .mapToObj(requiredJson::getString)
          .forEach(builder::addRequiredProperty);
    }
    if (schemaJson.containsKey("patternProperties")) {
      JsonObject patternPropsJson = schemaJson.getJsonObject("patternProperties");
      for (String pattern : patternPropsJson.fieldNames()) {
        builder.patternProperty(pattern, loadChild(patternPropsJson.getJsonObject(pattern))
            .build());
      }
    }
    ifPresent("dependencies", JsonObject.class, deps -> addDependencies(builder, deps));
    return builder;
  }

  private Schema.Builder buildSchemaWithoutExplicitType() {
    if (schemaJson==null||schemaJson.isEmpty()) {
      return EmptySchema.builder();
    }
    if (schemaJson.containsKey("$ref")) {
      return lookupReference(schemaJson.getString("$ref"), schemaJson);
    }
    Schema.Builder rval = sniffSchemaByProps();
    if (rval != null) {
      return rval;
    }
    if (schemaJson.containsKey("not")) {
      return buildNotSchema();
    }
    return EmptySchema.builder();
  }

  private StringSchema.Builder buildStringSchema() {
    StringSchema.Builder builder = StringSchema.builder();
    ifPresent("minLength", Integer.class, builder::minLength);
    ifPresent("maxLength", Integer.class, builder::maxLength);
    ifPresent("pattern", String.class, builder::pattern);
    ifPresent("format", String.class, format -> addFormatValidator(builder, format));
    return builder;
  }

  private void buildTupleSchema(final ArraySchema.Builder builder, final JsonArray itemSchema) {
    for (int i = 0; i < itemSchema.size(); ++i) {
      typeMultiplexer(itemSchema.getValue(i))
          .ifObject().then(schema -> builder.addItemSchema(loadChild(schema).build()))
          .requireAny();
    }
  }

  /**
   * Underscore-like extend function. Merges the properties of {@code additional} and
   * {@code original}. Neither {@code additional} nor {@code original} will be modified, but the
   * returned object may be referentially the same as one of the parameters (in case the other
   * parameter is an empty object).
   */
  JsonObject extend(final JsonObject additional, final JsonObject original) {
    if (additional==null||additional.isEmpty()) {
      return original;
    }
    if (original==null||original.isEmpty()) {
      return additional;
    }
    JsonObject rval = new JsonObject();
    original.fieldNames().forEach(name -> rval.put(name, original.getValue(name)));
    additional.fieldNames().forEach(name -> rval.put(name, additional.getValue(name)));
    return rval;
  }

  Optional<FormatValidator> getFormatValidator(final String format) {
    return Optional.ofNullable(formatValidators.get(format));
  }

  private <E> void ifPresent(final String key, final Class<E> expectedType,
      final Consumer<E> consumer) {
    if (schemaJson.containsKey(key)) {
      @SuppressWarnings("unchecked")
      E value = (E) schemaJson.getValue(key);
      try {
        consumer.accept(value);
      } catch (ClassCastException e) {
        throw new SchemaException(key, expectedType, value);
      }
    }
  }

  /**
   * Populates a {@code Schema.Builder} instance from the {@code schemaJson} schema definition.
   *
   * @return the builder which already contains the validation criteria of the schema, therefore
   *         {@link Schema.Builder#build()} can be immediately used to acquire the {@link Schema}
   *         instance to be used for validation
   */
  public Schema.Builder load() {
    Schema.Builder builder;
    if (schemaJson.containsKey("enum")) {
      builder = buildEnumSchema();
    } else {
      builder = tryCombinedSchema();
      if (builder == null) {
        if (!schemaJson.containsKey("type")) {
          builder = buildSchemaWithoutExplicitType();
        } else {
          builder = loadForType(schemaJson.getValue("type"));
        }
      }
    }
    ifPresent("id", String.class, builder::id);
    ifPresent("title", String.class, builder::title);
    ifPresent("description", String.class, builder::description);
    return builder;
  }

  private Schema.Builder loadChild(final JsonObject childJson) {
    return selfBuilder().schemaJson(childJson).build().load();
  }

  private Schema.Builder loadForExplicitType(final String typeString) {
    try {
      switch (typeString) {
        case "string":
          return buildStringSchema();
        case "integer":
          return buildNumberSchema().requiresInteger(true);
        case "number":
          return buildNumberSchema();
        case "boolean":
          return BooleanSchema.builder();
        case "null":
          return NullSchema.builder();
        case "array":
          return buildArraySchema();
        case "object":
          return buildObjectSchema();
        default:
          throw new SchemaException(String.format("unknown type: [%s]", typeString));
      }
    }catch(NullPointerException e){
      throw new SchemaException("Invalid schema",e);
    }
  }

  private Schema.Builder loadForType(final Object type) {
    if (type instanceof JsonArray) {
      return buildAnyOfSchemaForMultipleTypes();
    } else if (type instanceof String) {
      return loadForExplicitType((String) type);
    } else {
      throw new SchemaException("type", Arrays.asList(JsonArray.class, String.class), type);
    }
  }

  /**
   * Returns a schema builder instance after looking up the JSON pointer.
   */
  private Schema.Builder lookupReference(final String relPointerString, final JsonObject ctx) {
    String absPointerString = ReferenceResolver.resolve(id, relPointerString).toString();
    if (pointerSchemas.containsKey(absPointerString)) {
      return pointerSchemas.get(absPointerString);
    }
    boolean isExternal = !absPointerString.startsWith("#");
    JSONPointer pointer = isExternal
        ? JSONPointer.forURL(httpClient, absPointerString)
        : JSONPointer.forDocument(rootSchemaJson, absPointerString);
    ReferenceSchema.Builder refBuilder = ReferenceSchema.builder();
    pointerSchemas.put(absPointerString, refBuilder);
    QueryResult result = pointer.query();
    JsonObject resultObject = extend(withoutRef(ctx), result.getQueryResult());
    SchemaLoader childLoader =
            selfBuilder().resolutionScope(isExternal ? withoutFragment(absPointerString) : id)
            .schemaJson(resultObject)
            .rootSchemaJson(result.getContainingDocument()).build();
    Schema referredSchema = childLoader.load().build();
    refBuilder.build().setReferredSchema(referredSchema);
    return refBuilder;
  }

  private void populatePropertySchemas(final JsonObject propertyDefs,
      final ObjectSchema.Builder builder) {
    if (propertyDefs==null||propertyDefs.isEmpty()) {
      return;
    }
    propertyDefs.fieldNames().forEach(key -> {
      addPropertySchemaDefinition(key, propertyDefs.getValue(key), builder);
    });
  }

  private boolean schemaHasAnyOf(final Collection<String> propNames) {
    return propNames.stream().filter(schemaJson::containsKey).findAny().isPresent();
  }

  private SchemaLoaderBuilder selfBuilder() {
    SchemaLoaderBuilder rval = builder().resolutionScope(id).schemaJson(schemaJson)
        .rootSchemaJson(rootSchemaJson)
        .pointerSchemas(pointerSchemas)
        .httpClient(httpClient)
        .formatValidators(this.formatValidators);
    return rval;
  }

  private Schema.Builder sniffSchemaByProps() {
    if (schemaHasAnyOf(ARRAY_SCHEMA_PROPS)) {
      return buildArraySchema().requiresArray(false);
    } else if (schemaHasAnyOf(OBJECT_SCHEMA_PROPS)) {
      return buildObjectSchema().requiresObject(false);
    } else if (schemaHasAnyOf(NUMBER_SCHEMA_PROPS)) {
      return buildNumberSchema().requiresNumber(false);
    } else if (schemaHasAnyOf(STRING_SCHEMA_PROPS)) {
      return buildStringSchema().requiresString(false);
    }
    return null;
  }

  private CombinedSchema.Builder tryCombinedSchema() {
    List<String> presentKeys = COMB_SCHEMA_PROVIDERS.keySet().stream()
        .filter(schemaJson::containsKey)
        .collect(Collectors.toList());
    if (presentKeys.size() > 1) {
      throw new SchemaException(String.format(
          "expected at most 1 of 'allOf', 'anyOf', 'oneOf', %d found", presentKeys.size()));
    } else if (presentKeys.size() == 1) {
      String key = presentKeys.get(0);
      JsonArray subschemaDefs = schemaJson.getJsonArray(key);
      Collection<Schema> subschemas = IntStream.range(0, subschemaDefs.size())
          .mapToObj(subschemaDefs::getJsonObject)
          .map(this::loadChild)
          .map(Schema.Builder::build)
          .collect(Collectors.toList());
      CombinedSchema.Builder combinedSchema = COMB_SCHEMA_PROVIDERS.get(key).apply(
          subschemas);
      Schema.Builder baseSchema;
      if (schemaJson.containsKey("type")) {
        baseSchema = loadForType(schemaJson.getValue("type"));
      } else {
        baseSchema = sniffSchemaByProps();
      }
      if (baseSchema == null) {
        return combinedSchema;
      } else {
        return CombinedSchema.allOf(Arrays.asList(baseSchema.build(), combinedSchema.build()));
      }
    } else {
      return null;
    }
  }

  private TypeBasedMultiplexer typeMultiplexer(final Object obj) {
    return typeMultiplexer(null, obj);
  }

  private TypeBasedMultiplexer typeMultiplexer(final String keyOfObj, final Object obj) {
    TypeBasedMultiplexer multiplexer = new TypeBasedMultiplexer(keyOfObj, obj, id);
    multiplexer.addResolutionScopeChangeListener(scope -> {
      this.id = scope;
    });
    return multiplexer;
  }

  /**
   * Rerurns a shallow copy of the {@code original} object, but it does not copy the {@code $ref}
   * key, in case it is present in {@code original}.
   */
  JsonObject withoutRef(final JsonObject original) {
    if (original==null||original.isEmpty()) {
      return original;
    }
    JsonObject rval = new JsonObject();
    original.fieldNames().stream().filter(name -> !"$ref".equals(name))
        .forEach(name -> rval.put(name, original.getValue(name)));
    return rval;
  }
}
