package com.diyiliu.web.guide.facade;

import com.diyiliu.web.guide.dto.SiteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Description: SiteTypeJpa
 * Author: DIYILIU
 * Update: 2018-05-15 21:19
 */

public interface SiteTypeJpa extends JpaRepository<SiteType, Long> {

    @Transactional
    void deleteByNameIn(List<String> names);
}
