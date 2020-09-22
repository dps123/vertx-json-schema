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
package io.vertx.ext.json.schema.common;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.ext.json.schema.ValidationException.createException;
import java.util.Map;

public class OneOfValidatorFactory extends BaseCombinatorsValidatorFactory {

    @Override
    public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
      OneOfValidator validator = (OneOfValidator) super.createValidator(schema, scope, parser, parent);
      if (schema.containsKey("discriminator") && schema.getValue("discriminator") instanceof JsonObject) {
        JsonObject discriminator = schema.getJsonObject("discriminator");
        validator.setPropertyName(discriminator.getString("propertyName"));
        if (discriminator.containsKey("mapping")) {
          validator.setMapping(discriminator.getJsonObject("mapping").getMap().entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().toString()))
                );
        }
      }
      return validator;
    }

  @Override
  BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new OneOfValidator(parent);
  }

  @Override
  String getKeyword() {
    return "oneOf";
  }

  class OneOfValidator extends BaseCombinatorsValidator {

    private String propertyName;
    private Map<String, String> mapping;

    public OneOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    private boolean isValidSync(SchemaInternal schema, ValidatorContext context, Object in) {
      try {
        schema.validateSync(context, in);
        return true;
      } catch (ValidationException e) {
        return false;
      }
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      long validCount = Arrays.stream(schemas).filter(s -> checkDiscriminator(s, in)).map(s -> isValidSync(s, context, in)).filter(b -> b.equals(true)).count();
      if (validCount > 1) throw createException("More than one schema valid", "oneOf", in);
      else if (validCount == 0) throw createException("No schema matches", "oneOf", in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return FutureUtils
        .oneOf(Arrays.stream(schemas).filter(s -> checkDiscriminator(s, in)).map(s -> s.validateAsync(context, in)).collect(Collectors.toList()))
        .recover(err -> Future.failedFuture(createException("No schema matches", "oneOf", in, err)));
    }

    private boolean checkDiscriminator(SchemaInternal schema, Object in) {
      if (propertyName != null) {
        if (in instanceof JsonObject) {
          String discriminator = ((JsonObject) in).getString(propertyName);
          if (mapping != null) discriminator = mapping.get(discriminator);

          JsonObject jsonSchema = ((JsonObject) schema.getJson());
          String schemaName = (jsonSchema.containsKey("name")) ? jsonSchema.getString("name") : jsonSchema.getString("$ref");
          return discriminator != null && discriminator.equals(schemaName);
        }
      }
      return true;
    }

    public void setPropertyName(String propertyName) {
      this.propertyName = propertyName;
    }

    public void setMapping(Map<String, String> mapping) {
      this.mapping = mapping;
    }
  }
}
