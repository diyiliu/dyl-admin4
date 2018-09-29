package com.diyiliu.web;

import com.diyiliu.support.util.ImageUtil;
import com.diyiliu.support.util.JacksonUtil;
import com.diyiliu.web.guide.dto.SiteType;
import com.diyiliu.web.guide.facade.SiteTypeJpa;
import com.diyiliu.web.photo.dto.MemBody;
import com.diyiliu.web.photo.dto.MemPhoto;
import com.diyiliu.web.photo.dto.MemType;
import com.diyiliu.web.photo.facade.MemBodyJpa;
import com.diyiliu.web.photo.facade.MemPhotoJpa;
import com.diyiliu.web.photo.facade.MemTypeJpa;
import com.diyiliu.web.sys.dto.SysAsset;
import com.diyiliu.web.sys.dto.SysUser;
import com.diyiliu.web.sys.facade.SysAssetJpa;
import com.diyiliu.web.sys.facade.SysUserJpa;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: HomeController
 * Author: DIYILIU
 * Update: 2018-08-26 16:45
 */

@Slf4j
@Controller
public class HomeController {

    @Resource
    private Environment environment;

    @Resource
    private SysAssetJpa sysAssetJpa;

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

    @GetMapping("/")
    public String guide(Model model) {
        List<SiteType> siteTypes = siteTypeJpa.findAll(Sort.by("sort"));

        List<SiteType> typeList = siteTypes.stream().filter(t -> CollectionUtils.isNotEmpty(t.getSiteList())).collect(Collectors.toList());
        model.addAttribute("typeList", typeList);

        return "index";
    }

    @GetMapping("/login")
    public String login() {

        return "login";
    }

    @PostMapping("/login")
    public String login(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String exceptionClassName = (String) request.getAttribute("shiroLoginFailure");

        if (UnknownAccountException.class.getName().equals(exceptionClassName)
                || IncorrectCredentialsException.class.getName().equals(exceptionClassName)) {

            redirectAttributes.addFlashAttribute("error", "用户名或密码错误");
            return "redirect:/login";
        } else if (ExcessiveAttemptsException.class.getName().equals(exceptionClassName)) {

            redirectAttributes.addFlashAttribute("error", "登录错误次数超限，请稍后再试！");
            return "redirect:/login";

        } else if (exceptionClassName != null) {
            redirectAttributes.addFlashAttribute("error", "登录异常：" + exceptionClassName);

            return "redirect:/login";
        }

        return "home";
    }

    @GetMapping("/logout")
    public String logout() {
        SecurityUtils.getSubject().logout();

        return "redirect:/login";
    }

    @GetMapping("/home")
    public String index(HttpSession session) {
        SysAsset asset = sysAssetJpa.findByCode("index");
        session.setAttribute("active", asset);

        return "home";
    }


    @GetMapping("/home/{menu}")
    public String show(@PathVariable("menu") String menu, HttpServletRequest request,
                       @ModelAttribute("type") String type, @ModelAttribute("page") String page) {

        SysAsset asset = sysAssetJpa.findByController("home/" + menu);
        if (asset == null) {
            return "error/404";
        }
        String view = asset.getView();

        // 设置当前页
        request.getSession().setAttribute("active", asset);

        // 添加参数
        Map data = new HashMap();
        if (StringUtils.isNotEmpty(type)) {
            data.put("type", Integer.valueOf(type));
        }
        if (StringUtils.isNotEmpty(page)) {
            data.put("page", Integer.valueOf(page));
        }
        if (MapUtils.isNotEmpty(data)) {
            request.setAttribute("data", data);
        }

        return view;
    }


    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        session.removeAttribute("active");
        model.addAttribute("user", session.getAttribute("user"));

        return "profile";
    }

    @GetMapping("/image/{dir}/{name:.+}")
    public void uploadedImage(@PathVariable("name") String name, @PathVariable("dir") String dir,
                              HttpServletResponse response) {
        String imagePath = "";
        if (dir.equalsIgnoreCase("user")) {
            imagePath = environment.getProperty("upload.user");
        } else if (dir.equalsIgnoreCase("photo")) {
            if (name.startsWith("full")){
                imagePath = environment.getProperty("upload.photo");
            }else {
                imagePath = environment.getProperty("upload.photo") + "thumb" + File.separator;
            }
        }else if (dir.equalsIgnoreCase("guide")){
            if ("unknown.png".equals(name)) {
                imagePath = environment.getProperty("upload.guide.unknown");
            } else {
                imagePath = environment.getProperty("upload.guide.icon");
            }
        }

        try {
            org.springframework.core.io.Resource imgRes = new UrlResource(imagePath + name);
            if (imgRes != null && imgRes.exists()) {
                response.setHeader("Content-Type", URLConnection.guessContentTypeFromName(imgRes.getFilename()));
                FileCopyUtils.copy(imgRes.getInputStream(), response.getOutputStream());
                response.flushBuffer();
            }
        } catch (IOException e) {
            log.info("Error writing file to output stream. Filename was '{}'", name, e);
            throw new RuntimeException("IOError writing file to output stream");
        }
    }

    @ResponseBody
    @PostMapping("/image/user")
    public Map userIcon(MultipartFile file, HttpServletRequest request) throws Exception {
        Map respMap = new HashMap();
        if (file.isEmpty()) {
            respMap.put("status", 0);

            return respMap;
        }

        String data = request.getParameter("data");
        Map dataMap = JacksonUtil.toObject(data, HashMap.class);
        String fileName = (String) dataMap.get("name");

        String picDir = environment.getProperty("upload.user");
        org.springframework.core.io.Resource resDir = new UrlResource(picDir);
        // 创建临时文件
        File tempFile = File.createTempFile("icon", fileName.substring(fileName.lastIndexOf(".")).toLowerCase(), resDir.getFile());
        FileCopyUtils.copy(file.getBytes(), tempFile);
        // 剪切图片
        cutPic(tempFile.getPath(), dataMap);

        String username = (String) SecurityUtils.getSubject().getPrincipal();
        SysUser user = sysUserJpa.findByUsername(username);
        // 历史图片
        String oldIcon = user.getUserIcon();

        user.setUserIcon(tempFile.getName());
        user = sysUserJpa.save(user);
        if (user == null) {
            respMap.put("status", 0);

            return respMap;
        }

        // 删除文件
        org.springframework.core.io.Resource localRes = new UrlResource(picDir + oldIcon);
        if (localRes.exists()) {
            if (!localRes.getFile().delete()) {
                System.err.println("删除文件[{}]失败!");
            }
        }
        // 更新session
        request.getSession().setAttribute("user", user);

        respMap.put("status", 1);
        respMap.put("path", user.getUserIcon());

        return respMap;
    }


    @PostMapping("/home/preview/{type}/{page}")
    public String preview(@PathVariable int type, @PathVariable int page,
                          RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("type", type);
        redirectAttributes.addFlashAttribute("page", page);

        return "redirect:/home/preview";
    }


    @GetMapping("/guest")
    public String guest(Model model) {
        MemType memType = memTypeJpa.findByName("公开");

        List<MemBody> bodyPage = memBodyJpa.findAll(
                (Root<MemBody> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
                    Path<String> photosExp = root.get("photos");
                    Path<MemType> typeExp = root.get("memType");

                    List<Predicate> list = new ArrayList();
                    Predicate predicate = cb.and(photosExp.isNotNull());
                    list.add(predicate);

                    predicate = cb.equal(typeExp, memType);
                    list.add(predicate);

                    Predicate[] predicates = list.toArray(new Predicate[]{});
                    return cb.and(predicates);
                }, Sort.by(Sort.Direction.DESC, new String[]{"day", "createTime"}));

        List<MemPhoto> photoList = new ArrayList();
        bodyPage.forEach(e -> {
            String children = e.getPhotos();
            String[] strArray = children.split(",");
            if (strArray.length > 0) {
                Long[] ids = (Long[]) ConvertUtils.convert(strArray, Long.class);
                photoList.addAll(memPhotoJpa.findByIdIn(ids));
            }
        });

        int half = photoList.size() / 2;
        List<MemPhoto> photos1 = photoList.subList(0, half);
        List<MemPhoto> photos2 = photoList.subList(half, photoList.size());
        model.addAttribute("photos1", photos1);
        model.addAttribute("photos2", photos2);

        return "guest/picWall";
    }


    /**
     * 图片裁切, 缩放
     *
     * @param imagePath
     * @param data
     */
    private void cutPic(String imagePath, Map data) throws Exception {
        int x = new BigDecimal(String.valueOf(data.get("x"))).intValue();
        int y = new BigDecimal(String.valueOf(data.get("y"))).intValue();
        int w = new BigDecimal(String.valueOf(data.get("width"))).intValue();
        int h = new BigDecimal(String.valueOf(data.get("height"))).intValue();

        // 裁切
        ImageUtil.crop(imagePath, x, y, w, h, imagePath);
        // 缩放
        ImageUtil.scale(imagePath, 256, 256, imagePath);
    }
}
