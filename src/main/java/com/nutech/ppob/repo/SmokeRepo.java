package com.nutech.ppob.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SmokeRepo {
    private final JdbcTemplate jdbc;

    public SmokeRepo(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public int countServices() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM services", Integer.class);
    }
}
