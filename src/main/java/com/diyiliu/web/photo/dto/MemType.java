package com.diyiliu.web.photo.dto;

import lombok.Data;

import javax.persistence.*;

/**
 * Description: MemType
 * Author: DIYILIU
 * Update: 2018-09-20 14:22
 */

@Data
@Entity
@Table(name = "mem_type")
public class MemType {

    public MemType() {

    }

    public MemType(Long id) {

        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String note;

    private Integer sort;
}
