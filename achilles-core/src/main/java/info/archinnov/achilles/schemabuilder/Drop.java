/*
 * Copyright (C) 2012-2014 DuyHai DOAN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package info.archinnov.achilles.schemabuilder;

import com.google.common.base.Optional;

/**
 * A built DROP TABLE statement
 */
public class Drop extends SchemaStatement {

    private Optional<String> keyspaceName = Optional.absent();
    private String tableName;
    private Optional<Boolean> ifExists = Optional.absent();

    Drop(String keyspaceName, String tableName) {
        validateNotEmpty(keyspaceName, "Keyspace name");
        validateNotEmpty(tableName, "Table name");
        validateNotKeyWord(keyspaceName, String.format("The keyspace name '%s' is not allowed because it is a reserved keyword", keyspaceName));
        validateNotKeyWord(tableName,String.format("The table name '%s' is not allowed because it is a reserved keyword",tableName));
        this.tableName = tableName;
        this.keyspaceName = Optional.fromNullable(keyspaceName);
    }

    Drop(String tableName) {
        validateNotEmpty(tableName, "Table name");
        validateNotKeyWord(tableName,String.format("The table name '%s' is not allowed because it is a reserved keyword",tableName));
        this.tableName = tableName;
    }

    /**
     * Use 'IF EXISTS' LWT condition for the table drop.
     *
     * @param ifExists whether to use the LWT condition.
     * @return a new {@link Drop} instance.
     */
    public Drop ifExists(Boolean ifExists) {
        this.ifExists = Optional.fromNullable(ifExists);
        return this;
    }

    @Override
    String buildInternal() {
        StringBuilder dropStatement = new StringBuilder(DROP_TABLE);
        if (ifExists.isPresent() && ifExists.get()) {
            dropStatement.append(SPACE).append(IF_EXISTS);
        }
        dropStatement.append(SPACE);
        if (keyspaceName.isPresent()) {
            dropStatement.append(keyspaceName.get()).append(DOT);
        }

        dropStatement.append(tableName);
        return dropStatement.toString();
    }

    /**
     * Generate a DROP TABLE statement
     * @return the final DROP TABLE statement
     */
    public String build() {
        return this.buildInternal();
    }
}
