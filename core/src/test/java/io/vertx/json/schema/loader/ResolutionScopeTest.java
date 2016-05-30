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
import io.vertx.json.schema.loader.internal.DefaultSchemaClient;
import io.vertx.json.schema.JsonObjectHelper;
import org.junit.Test;

import java.io.InputStream;

public class ResolutionScopeTest {

  private static JsonObject ALL_SCHEMAS;

  static {
    InputStream stream = SchemaLoaderTest.class.getResourceAsStream(
        "/org/everit/jsonvalidator/testschemas.json");
    ALL_SCHEMAS = JsonObjectHelper.load(stream);
  }

  private JsonObject get(final String schemaName) {
    return ALL_SCHEMAS.getJsonObject(schemaName);
  }

  @Test
  public void resolutionScopeTest() {
    SchemaLoader.load(get("resolutionScopeTest"), new SchemaClient() {

      @Override
      public InputStream get(final String url) {
        System.out.println("GET " + url);
        return new DefaultSchemaClient().get(url);
      }
    });
  }

}
