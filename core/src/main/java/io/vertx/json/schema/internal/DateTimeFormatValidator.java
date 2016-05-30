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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import io.vertx.json.schema.FormatValidator;

/**
 * Implementation of the "date-time" format value.
 */
public class DateTimeFormatValidator implements FormatValidator {

  private static final String DATETIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssXXX";

  private static final String DATETIME_FORMAT_STRING_SECFRAC = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  private SimpleDateFormat dateFormat(final String pattern) {
    SimpleDateFormat rval = new SimpleDateFormat(pattern);
    rval.setLenient(false);
    return rval;
  }

  @Override
  public Optional<String> validate(final String subject) {
    try {
      dateFormat(DATETIME_FORMAT_STRING).parse(subject);
      return Optional.empty();
    } catch (ParseException e) {
      try {
        dateFormat(DATETIME_FORMAT_STRING_SECFRAC).parse(subject);
        return Optional.empty();
      } catch (ParseException e1) {
        return Optional.of(String.format("[%s] is not a valid date-time", subject));
      }
    }
  }

}
