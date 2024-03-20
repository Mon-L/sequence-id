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
