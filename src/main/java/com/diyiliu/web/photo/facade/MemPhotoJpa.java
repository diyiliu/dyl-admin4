package com.diyiliu.web.photo.facade;

import com.diyiliu.web.photo.dto.MemPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: MemPhotoJpa
 * Author: DIYILIU
 * Update: 2018-09-20 14:44
 */
public interface MemPhotoJpa extends JpaRepository<MemPhoto, Long> {

    List<MemPhoto> findByIdIn(Long[] ids);
}
