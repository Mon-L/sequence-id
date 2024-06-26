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

import com.zcn.sequence.id.model.IdSlot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 用于 Sequence ids 持久化和分配 ids。
 *
 * @author zicung
 */
public class IdSlotDao {

    private final DataSource dataSource;

    private static final String GET_ALL =
            "select id, type, max, step, max_step, step_duration, update_time from sequence_id";

    private static final String UPDATE_MAX = "update sequence_id set max = max + ? where type = ?";

    private static final String GET_ONE =
            "select id, type, max, step, update_time, max_step, step_duration from sequence_id where type = ?";

    public IdSlotDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取所有Sequence id
     *
     * @return List<IdSlot>
     */
    public List<IdSlot> loadAll() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<IdSlot> idSlotList = new ArrayList<>();
            while (resultSet.next()) {
                IdSlot idSlot = new IdSlot();
                fillModel(idSlot, resultSet);
                idSlotList.add(idSlot);
            }
            return idSlotList;
        }
    }

    public IdSlot updateIdAllocAndGet(int type, int step) throws SQLException {
        boolean autoCommit = false;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection.getAutoCommit()) {
                autoCommit = true;

                // 取消自动提交
                connection.setAutoCommit(false);
            }

            // update max
            PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_MAX);
            preparedStatement.setInt(1, step);
            preparedStatement.setInt(2, type);
            int count = preparedStatement.executeUpdate();
            if (count > 0) {
                // query
                preparedStatement = connection.prepareStatement(GET_ONE);
                preparedStatement.setInt(1, type);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    IdSlot idSlot = new IdSlot();
                    fillModel(idSlot, resultSet);
                    connection.commit();
                    return idSlot;
                }
            }
            return null;
        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (connection != null) {
                if (autoCommit) {
                    connection.setAutoCommit(true);
                }
                connection.close();
            }
        }
    }

    private void fillModel(IdSlot idSlot, ResultSet resultSet) throws SQLException {
        idSlot.setType(resultSet.getInt("type"));
        idSlot.setMax(resultSet.getLong("max"));
        idSlot.setStep(resultSet.getInt("step"));
        idSlot.setMaxStep(resultSet.getInt("max_step"));
        idSlot.setStepDuration(resultSet.getInt("step_duration"));
        idSlot.setUpdateTime(resultSet.getDate("update_time"));
    }
}
