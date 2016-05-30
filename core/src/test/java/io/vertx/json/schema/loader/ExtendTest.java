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

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonObjectHelper;
import io.vertx.json.schema.ObjectComparator;
import org.junit.Assert;
import org.junit.Test;

public class ExtendTest {

  private static JsonObject OBJECTS;

  static {
    OBJECTS = JsonObjectHelper.load(
        ExtendTest.class.getResourceAsStream("/org/everit/jsonvalidator/merge-testcases.json"));
  }

  @Test
  public void additionalHasMoreProps() {
    JsonObject actual = subject().extend(get("propIsTrue"), get("empty"));
    assertEquals(get("propIsTrue"), actual);
  }

  @Test
  public void additionalOverridesOriginal() {
    JsonObject actual = subject().extend(get("propIsTrue"), get("propIsFalse"));
    assertEquals(get("propIsTrue"), actual);
  }

  @Test
  public void additionalPropsAreMerged() {
    JsonObject actual = subject().extend(get("propIsTrue"), get("prop2IsFalse"));
    assertEquals(actual, get("propTrueProp2False"));
  }

  private void assertEquals(final JsonObject expected, final JsonObject actual) {
    Assert.assertTrue(ObjectComparator.deepEquals(expected, actual));
  }

  @Test
  public void bothEmpty() {
    JsonObject actual = subject().extend(get("empty"), get("empty"));
    assertEquals(new JsonObject(), actual);
  }

  private JsonObject get(final String objectName) {
    return OBJECTS.getJsonObject(objectName);
  }

  @Test
  public void multiplePropsAreMerged() {
    JsonObject actual = subject().extend(get("multipleWithPropTrue"), get("multipleWithPropFalse"));
    assertEquals(get("mergedMultiple"), actual);
  }

  @Test
  public void originalPropertyRemainsUnchanged() {
    JsonObject actual = subject().extend(get("empty"), get("propIsTrue"));
    assertEquals(get("propIsTrue"), actual);
  }

  private SchemaLoader subject() {
    return SchemaLoader.builder().schemaJson(new JsonObject()).build();
  }
}
