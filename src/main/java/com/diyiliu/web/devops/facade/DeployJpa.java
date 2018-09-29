package com.diyiliu.web.devops.facade;

import com.diyiliu.web.devops.dto.Deploy;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: DeployJpa
 * Author: DIYILIU
 * Update: 2018-09-10 10:02
 */
public interface DeployJpa extends JpaRepository<Deploy, Long> {

}
