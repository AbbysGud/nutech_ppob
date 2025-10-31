package com.nutech.ppob.repo;

import com.nutech.ppob.dto.content.BannerItem;
import com.nutech.ppob.dto.content.ServiceItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ContentRepo {

    private final JdbcTemplate jdbc;

    public ContentRepo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<BannerItem> BANNER_MAPPER = (rs, i) -> new BannerItem(
            rs.getString("banner_name"),
            rs.getString("banner_image"),
            rs.getString("description")
    );

    private static final RowMapper<ServiceItem> SERVICE_MAPPER = (rs, i) -> new ServiceItem(
            rs.getString("service_code"),
            rs.getString("service_name"),
            rs.getString("service_icon"),
            rs.getLong("service_tariff")
    );

    public List<BannerItem> findActiveBanners() {
        String sql = """
            SELECT banner_name,
                   banner_image_url AS banner_image,
                   description
            FROM banners
            WHERE is_active = ?
            ORDER BY sort_order ASC, id ASC
            """;
        return jdbc.query(sql, BANNER_MAPPER, 1);
    }

    public List<ServiceItem> findActiveServices() {
        String sql = """
            SELECT service_code,
                   service_name,
                   service_icon_url AS service_icon,
                   service_tariff
            FROM services
            WHERE is_active = ?
            ORDER BY service_name ASC
            """;
        return jdbc.query(sql, SERVICE_MAPPER, 1);
    }
}