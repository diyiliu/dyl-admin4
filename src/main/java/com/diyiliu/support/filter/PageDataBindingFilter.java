package com.diyiliu.support.filter;

import com.diyiliu.web.guide.dto.SiteType;
import com.diyiliu.web.guide.facade.SiteTypeJpa;
import com.diyiliu.web.photo.dto.MemBody;
import com.diyiliu.web.photo.dto.MemPhoto;
import com.diyiliu.web.photo.dto.MemType;
import com.diyiliu.web.photo.facade.MemBodyJpa;
import com.diyiliu.web.photo.facade.MemPhotoJpa;
import com.diyiliu.web.photo.facade.MemTypeJpa;
import com.diyiliu.web.sys.dto.SysRole;
import com.diyiliu.web.sys.dto.SysUser;
import com.diyiliu.web.sys.facade.SysRoleJpa;
import com.diyiliu.web.sys.facade.SysUserJpa;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: PageDataBindingFilter
 * Author: DIYILIU
 * Update: 2018-05-11 10:14
 */

@Aspect
@Component
public class PageDataBindingFilter {

    @Resource
    private SysRoleJpa sysRoleJpa;

    @Resource
    private SysUserJpa sysUserJpa;

    @Resource
    private MemTypeJpa memTypeJpa;

    @Resource
    private MemPhotoJpa memPhotoJpa;

    @Resource
    private MemBodyJpa memBodyJpa;

    @Resource
    private SiteTypeJpa siteTypeJpa;

    @After("execution(* com.diyiliu.web.HomeController.show(..))")
    public void doAfter(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        Object[] args = joinPoint.getArgs();
        String menu = (String) args[0];

        if ("user".equals(menu)) {
            List<SysRole> roleList = sysRoleJpa.findAll();
            request.setAttribute("roles", roleList);

            List<SysUser> userList = sysUserJpa.findAll();
            List<String> usernameList = userList.stream().map(SysUser::getUsername).collect(Collectors.toList());
            List<String> nameList = userList.stream().map(SysUser::getName).collect(Collectors.toList());
            nameList.addAll(usernameList);
            request.setAttribute("names", nameList);

            return;
        }


        if (menu.startsWith("guide")) {
            List<SiteType> siteTypes = siteTypeJpa.findAll(Sort.by("sort"));
            request.setAttribute("types", siteTypes);

            // 主页面 添加chosen
            if (menu.contains(".1")) {
                List<String> names = siteTypes.stream().map(SiteType::getName).collect(Collectors.toList());
                request.setAttribute("tNames", names);
            }

            return;
        }

        if ("upload".equals(menu) || "preview".equals(menu)) {
            List<MemType> typeList = memTypeJpa.findAll(Sort.by("sort"));
            request.setAttribute("types", typeList);

            if ("preview".equals(menu)) {
                int page = 0;
                if (request.getAttribute("data") != null) {
                    Map data = (Map) request.getAttribute("data");
                    if (data.containsKey("page")) {
                        page = (int) data.get("page");
                    }
                }

                Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, new String[]{"day", "createTime"}));
                Page<MemBody> bodyPage = memBodyJpa.findAll(
                        (Root<MemBody> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                            Path<String> photosExp = root.get("photos");
                            Path<MemType> typeExp = root.get("memType");

                            List<Predicate> list = new ArrayList();
                            Predicate predicate = cb.and(photosExp.isNotNull());
                            list.add(predicate);

                            if (request.getAttribute("data") != null) {
                                Map data = (Map) request.getAttribute("data");
                                if (data != null && data.containsKey("type")) {
                                    long typeId = (int) data.get("type");
                                    if (typeId > 0){
                                        predicate = cb.equal(typeExp, new MemType(typeId));
                                        list.add(predicate);
                                        request.setAttribute("typeId", typeId);
                                    }
                                }
                            }

                            Predicate[] predicates = list.toArray(new Predicate[]{});
                            return cb.and(predicates);
                        }, pageable);


                List<MemBody> list = bodyPage.getContent();
                list.forEach(e -> {
                    String children = e.getPhotos();
                    String[] strArray = children.split(",");
                    if (strArray.length > 0) {
                        Long[] ids = (Long[]) ConvertUtils.convert(strArray, Long.class);
                        e.setPhotoList(memPhotoJpa.findByIdIn(ids));
                    }
                });

                request.setAttribute("bodyList", list);
                request.setAttribute("total", bodyPage.getTotalPages());
                request.setAttribute("index", page);
            }
        }
    }
}
