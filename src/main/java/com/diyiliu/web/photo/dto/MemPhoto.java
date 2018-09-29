package com.diyiliu.web.photo.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.File;

/**
 * Description: MemPhoto
 * Author: DIYILIU
 * Update: 2018-09-20 14:24
 */

@Data
@Entity
@Table(name = "mem_photo")
public class MemPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String note;

    private String path;

    private String thumb;

    public String getFileName() {
        if (StringUtils.isEmpty(path)) {
            return null;
        }

        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    public String getThumbName() {
        if (StringUtils.isEmpty(thumb)) {
            return null;
        }

        return thumb.substring(thumb.lastIndexOf(File.separator) + 1);
    }

    /**
     * 1-3 随机数
     *
     * @return
     */
    public int getLuckyNo() {

        return Double.valueOf(Math.random() * 3 + 1).intValue();
    }

    @Transient
    private String originalName;
}
