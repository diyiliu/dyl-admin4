package com.diyiliu.web.photo.dto;

import lombok.Data;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Description: MemBody
 * Author: DIYILIU
 * Update: 2018-09-20 14:27
 */

@Data
@Entity
@Table(name = "mem_body")
public class MemBody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type")
    @NotFound(action = NotFoundAction.IGNORE)
    private MemType memType;

    private Date day;

    @Transient
    private String dayStr;

    private String subject;

    private String content;

    private String photos;

    @Transient
    private List<MemPhoto> photoList;

    private Date createTime;

    private String createUser;
}
