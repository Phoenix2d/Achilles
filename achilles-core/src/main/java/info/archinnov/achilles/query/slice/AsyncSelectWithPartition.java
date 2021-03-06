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

package info.archinnov.achilles.query.slice;

import info.archinnov.achilles.internal.metadata.holder.EntityMeta;
import info.archinnov.achilles.internal.persistence.operations.SliceQueryExecutor;

import static info.archinnov.achilles.query.slice.SliceQueryProperties.SliceType;

public class AsyncSelectWithPartition<TYPE> extends AsyncSelectPartitionRoot<TYPE,AsyncSelectWithPartition<TYPE>> {

    protected AsyncSelectWithPartition(SliceQueryExecutor sliceQueryExecutor, Class<TYPE> entityClass, EntityMeta meta, SliceType sliceType) {
        super(sliceQueryExecutor, entityClass, meta, sliceType);
    }

    @Override
    protected AsyncSelectWithPartition<TYPE> getThis() {
        return AsyncSelectWithPartition.this;
    }
}
