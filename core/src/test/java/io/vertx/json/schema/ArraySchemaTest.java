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
package io.vertx.json.schema;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class ArraySchemaTest {

  private static final JsonObject ARRAYS = JsonObjectHelper.load((
      ArraySchemaTest.class.getResourceAsStream("/io/vertx/jsonvalidator/arraytestcases.json")));

  @Test
  public void additionalItemsSchema() {
    ArraySchema.builder()
    .addItemSchema(BooleanSchema.INSTANCE)
    .schemaOfAdditionalItems(NullSchema.INSTANCE)
    .build().validate(ARRAYS.getValue("additionalItemsSchema"));
  }

  @Test
  public void additionalItemsSchemaFailure() {
    ArraySchema subject = ArraySchema.builder()
        .addItemSchema(BooleanSchema.INSTANCE)
        .schemaOfAdditionalItems(NullSchema.INSTANCE)
        .build();
    TestSupport.expectFailure(subject, NullSchema.INSTANCE, "#/2",
        ARRAYS.getValue("additionalItemsSchemaFailure"));
  }

  @Test
  public void booleanItems() {
    ArraySchema subject = ArraySchema.builder().allItemSchema(BooleanSchema.INSTANCE).build();
    TestSupport.expectFailure(subject, BooleanSchema.INSTANCE, "#/2", ARRAYS.getValue("boolArrFailure"));
  }

  @Test
  public void doesNotRequireExplicitArray() {
    ArraySchema.builder()
    .requiresArray(false)
    .uniqueItems(true)
    .build().validate(ARRAYS.getValue("doesNotRequireExplicitArray"));
  }

  @Test
  public void maxItems() {
    ArraySchema subject = ArraySchema.builder().maxItems(0).build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("onlyOneItem"));
  }

  @Test
  public void minItems() {
    ArraySchema subject = ArraySchema.builder().minItems(2).build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("onlyOneItem"));
  }

  @Test
  public void noAdditionalItems() {
    ArraySchema subject = ArraySchema.builder()
        .additionalItems(false)
        .addItemSchema(BooleanSchema.INSTANCE)
        .addItemSchema(NullSchema.INSTANCE)
        .build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("twoItemTupleWithAdditional"));
  }

  @Test
  public void noItemSchema() {
    ArraySchema.builder().build().validate(ARRAYS.getValue("noItemSchema"));
  }

  @Test
  public void nonUniqueArrayOfArrays() {
    ArraySchema subject = ArraySchema.builder().uniqueItems(true).build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("nonUniqueArrayOfArrays"));
  }

  @Test(expected = SchemaException.class)
  public void tupleAndListFailure() {
    ArraySchema.builder().addItemSchema(BooleanSchema.INSTANCE).allItemSchema(NullSchema.INSTANCE)
    .build();
  }

  @Test
  public void tupleWithOneItem() {
    ArraySchema subject = ArraySchema.builder().addItemSchema(BooleanSchema.INSTANCE).build();
    TestSupport.expectFailure(subject, BooleanSchema.INSTANCE, "#/0",
        ARRAYS.getValue("tupleWithOneItem"));
  }

  @Test
  public void typeFailure() {
    TestSupport.expectFailure(ArraySchema.builder().build(), true);
  }

  @Test
  public void uniqueItemsObjectViolation() {
    ArraySchema subject = ArraySchema.builder().uniqueItems(true).build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("nonUniqueObjects"));
  }

  @Test
  public void uniqueItemsViolation() {
    ArraySchema subject = ArraySchema.builder().uniqueItems(true).build();
    TestSupport.expectFailure(subject, "#", ARRAYS.getValue("nonUniqueItems"));
  }

  @Test
  public void uniqueItemsWithSameToString() {
    ArraySchema.builder().uniqueItems(true).build()
    .validate(ARRAYS.getValue("uniqueItemsWithSameToString"));
  }

  @Test
  public void uniqueObjectValues() {
    ArraySchema.builder().uniqueItems(true).build()
    .validate(ARRAYS.getValue("uniqueObjectValues"));
  }
}
