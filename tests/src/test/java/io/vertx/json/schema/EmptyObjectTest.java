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
import io.vertx.json.schema.loader.SchemaLoader;
import org.junit.Test;

public class EmptyObjectTest {
    @Test
    public void validateEmptyObject() {

        JsonObject jsonSchema = JsonObjectHelper.load(
                MetaSchemaTest.class
                        .getResourceAsStream("/io/vertx/json/schema/json-schema-draft-04.json"));

        JsonObject jsonSubject = new JsonObject("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {}\n" +
                "}");

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
    }
}
