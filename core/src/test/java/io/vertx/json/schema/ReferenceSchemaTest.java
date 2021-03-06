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

import org.junit.Assert;
import org.junit.Test;

public class ReferenceSchemaTest {

  @Test
  public void constructorMustRunOnlyOnce() {
    ReferenceSchema.Builder builder = ReferenceSchema.builder();
    Assert.assertSame(builder.build(), builder.build());
  }

  @Test(expected = IllegalStateException.class)
  public void setterShouldWorkOnlyOnce() {
    ReferenceSchema subject = ReferenceSchema.builder().build();
    subject.setReferredSchema(BooleanSchema.INSTANCE);
    subject.setReferredSchema(BooleanSchema.INSTANCE);
  }

}
