/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright 2018 Nextdoor.com, Inc
 *
 */

package com.nextdoor.bender.operation.substitution;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.text.ExtendedMessageFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

@JsonTypeName("StringSubstitution")
@JsonSchemaDescription("Creates a new string using variable replacement.")
public class StringSubSpecConfig extends SubSpecConfig<StringSubSpecConfig> {
  public StringSubSpecConfig() {}

  public StringSubSpecConfig(String key, String format, List<VariableConfig<?>> variables,
      boolean failDstNotFound) {
    super(key, failDstNotFound);
    setFormat(format);
    this.variables = variables;
  }

  @JsonSchemaDescription("String with variable indices to substitue. See "
      + "https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html")
  @JsonProperty(required = true)
  private String format;

  @JsonSchemaDescription("List of variables used in string substitution. Index of variable "
      + "relates to index in 'format' string.")
  @JsonProperty(required = true)
  private List<VariableConfig<?>> variables = Collections.emptyList();

  @JsonIgnore
  private ExtendedMessageFormat mf;

  public String getFormat() {
    return this.format;
  }

  public void setFormat(String format) {
    this.format = format;
    if (format != null) {
      this.mf = new ExtendedMessageFormat(format);
    }
  }

  public void setVariables(List<VariableConfig<?>> variables) {
    this.variables = variables;
  }

  public List<VariableConfig<?>> getVariables() {
    return this.variables;
  }

  public ExtendedMessageFormat getMessageFormat() {
    return this.mf;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.format, this.variables);
  }

  @Override
  public String toString() {
    return super.getKey() + ":" + this.format;
  }
}
