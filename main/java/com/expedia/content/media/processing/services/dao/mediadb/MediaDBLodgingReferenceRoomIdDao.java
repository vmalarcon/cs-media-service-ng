package com.expedia.content.media.processing.services.dao.mediadb;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by sstannus on 5/11/17.
 */
public class MediaDBLodgingReferenceRoomIdDao {

    private final JdbcTemplate jdbcTemplate;

    public MediaDBLodgingReferenceRoomIdDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
