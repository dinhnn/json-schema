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
package io.vertx.json.schema.internal;

import java.util.Optional;

import io.vertx.json.schema.FormatValidator;

import com.google.common.net.InternetDomainName;

/**
 * Implementation of the "hostname" format value.
 */
public class HostnameFormatValidator implements FormatValidator {

  @Override
  public Optional<String> validate(final String subject) {
    try {
      InternetDomainName.from(subject);
      return Optional.empty();
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.of(String.format("[%s] is not a valid hostname", subject));
    }
  }

}
