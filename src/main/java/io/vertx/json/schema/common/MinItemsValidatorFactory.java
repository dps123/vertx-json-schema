/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;

import java.util.List;

import static io.vertx.json.schema.common.JsonUtil.unwrap;

public class MinItemsValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number minimum = (Number) schema.getValue("minItems");
      if (minimum.intValue() < 0)
        throw new SchemaException(schema, "minItems must be >= 0");
      return new MinItemsValidator(minimum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minItems keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minItems keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minItems");
  }

  public static class MinItemsValidator extends BaseSyncValidator {
    private final int minimum;

    public MinItemsValidator(int minimum) {
      this.minimum = minimum;
    }

    @Override
    public void validateSync(ValidatorContext context, final Object in) throws ValidationException {
      Object o = unwrap(in);
      if (o instanceof List<?>) {
        List<?> arr = (List<?>) o;
        if (arr.size() < minimum) {
          throw ValidationException.create("provided array should have size >= " + minimum, "minItems", in);
        }
      }
    }
  }

}
