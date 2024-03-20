/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zcn.sequence.id;

import javax.sql.DataSource;

/**
 * SequenceId生成器
 *
 * @author zicung
 */
public class SequenceIdGenerator {

    private final GenerationService generationService;

    public SequenceIdGenerator(DataSource dataSource) {
        this.generationService = new GenerationService(dataSource);
    }

    public void init() {
        this.generationService.init();
    }

    public void destroy() {
        this.generationService.destroy();
    }

    public long generate(int type) throws SequenceIdException {
        return generationService.generate(type);
    }
}
