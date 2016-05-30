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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class EnumSchemaTest {

  private Set<Object> possibleValues;

  @Before
  public void before() {
    possibleValues = new HashSet<>();
    possibleValues.add(true);
    possibleValues.add("foo");
    possibleValues.add(new JsonArray());
    possibleValues.add(new JsonObject("{\"a\" : 0}"));
  }

  @Test
  public void failure() {
    EnumSchema subject = subject();
    TestSupport.expectFailure(subject, new JsonArray("[1]"));
  }

  private EnumSchema subject() {
    return EnumSchema.builder().possibleValues(possibleValues).build();
  }

  @Test
  public void success() {
    EnumSchema subject = subject();
    subject.validate(true);
    subject.validate("foo");
    subject.validate(new JsonArray());
    subject.validate(new JsonObject("{\"a\" : 0}"));
  }

}
