package com.diyiliu.web.photo;

import com.diyiliu.support.util.DateUtil;
import com.diyiliu.support.util.ImageUtil;
import com.diyiliu.web.photo.dto.MemBody;
import com.diyiliu.web.photo.dto.MemPhoto;
import com.diyiliu.web.photo.facade.MemBodyJpa;
import com.diyiliu.web.photo.facade.MemPhotoJpa;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Description: PhotoController
 * Author: DIYILIU
 * Update: 2018-09-19 16:16
 */

@RestController
@RequestMapping("/photo")
public class PhotoController {

    @Resource
    private Environment environment;

    @Resource
    private MemPhotoJpa memPhotoJpa;

    @Resource
    private MemBodyJpa memBodyJpa;

    @PostMapping("/save")
    public Integer save(MemBody memBody, HttpSession session) {
        String username = (String) SecurityUtils.getSubject().getPrincipal();
        Date date = DateUtil.strToDate(memBody.getDayStr());

        memBody.setDay(date);
        memBody.setCreateTime(new Date());
        memBody.setCreateUser(username);

        String files = memBody.getPhotos();
        String[] fileArray = files.split(",");
        List<String> names = Arrays.asList(fileArray);

        List<MemPhoto> photos = (List<MemPhoto>) session.getAttribute("temp");
        try {
            String ids = doPhoto(names, photos);
            memBody.setPhotos(ids);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (memBody.getMemType() != null &&
                memBody.getMemType().getId() == null) {

            memBody.setMemType(null);
        }

        memBody = memBodyJpa.save(memBody);
        if (memBody == null) {

            return 0;
        }


        return 1;
    }

    @DeleteMapping("/img/{pid}/{id}")
    public Integer delete(@PathVariable Long pid, @PathVariable Long id) throws Exception {
        MemBody body = memBodyJpa.findById(pid).get();
        String photos = body.getPhotos();
        String idStr = String.valueOf(id);
        if (photos.contains(idStr)) {
            photos = photos.replaceAll(idStr + ",", "").replaceAll("," + idStr, "").replaceAll(idStr, "");
            if (StringUtils.isEmpty(photos)) {
                photos = null;
            }
            body.setPhotos(photos);
            memBodyJpa.save(body);
        }

        MemPhoto photo = memPhotoJpa.findById(id).get();
        String path = photo.getPath();
        org.springframework.core.io.Resource res = new UrlResource("file:" + path);
        if (res.exists()) {
            res.getFile().delete();
        }
        memPhotoJpa.delete(photo);

        return 1;
    }

    @PostMapping("/img")
    public Integer saveImg(MemPhoto photo) {
        MemPhoto temp = memPhotoJpa.findById(photo.getId()).get();

        photo.setThumb(temp.getThumb());
        photo.setPath(temp.getPath());
        temp = memPhotoJpa.save(photo);
        if (temp == null) {

            return 0;
        }

        return 1;
    }

    private String doPhoto(List<String> names, List<MemPhoto> photos) throws Exception {
        if (CollectionUtils.isEmpty(photos)) {

            return null;
        }

        String ids = "";
        for (Iterator<MemPhoto> iterator = photos.iterator(); iterator.hasNext(); ) {
            MemPhoto photo = iterator.next();
            String originalName = photo.getOriginalName();

            String path = photo.getPath();
            org.springframework.core.io.Resource res = new UrlResource("file:" + path);
            if (names.contains(originalName)) {
                String thumbDir = environment.getProperty("upload.photo") + File.separator + "thumb";
                org.springframework.core.io.Resource resDir = new UrlResource(thumbDir);
                // 创建缩略图
                File tempFile = File.createTempFile("small", getFileExtension(originalName), resDir.getFile());
                // 图片高度
                int height = 414;
                Thumbnails.of(res.getFile().getPath()).height(height).toFile(tempFile.getPath());

                photo.setThumb(tempFile.getPath());
                photo = memPhotoJpa.save(photo);
                if (photo != null) {
                    ids += photo.getId() + ",";
                }
            } else {
                if (res.exists()) {
                    res.getFile().delete();
                }
            }
            iterator.remove();
        }

        return ids.substring(0, ids.length() - 1);
    }

    @PostMapping("/upload")
    public Map upload(MultipartFile file, HttpSession session) throws Exception {
        String fileName = file.getOriginalFilename();

        String picDir = environment.getProperty("upload.photo");
        org.springframework.core.io.Resource resDir = new UrlResource(picDir);
        // 创建临时文件
        File tempFile = File.createTempFile("full", getFileExtension(fileName), resDir.getFile());
        FileCopyUtils.copy(file.getBytes(), tempFile);

        // 压缩图片质量
        BigDecimal size = new BigDecimal(file.getSize()).divide(new BigDecimal(1024 * 1024), 1, RoundingMode.HALF_UP);
        // 最大 1M
        int max = 1;
        if (size.doubleValue() > max) {
            ImageUtil.zipQuality(tempFile.getPath(), tempFile.getPath(), size.intValue(), max);
        }

        List<MemPhoto> photos = (List<MemPhoto>) session.getAttribute("temp");
        if (photos == null) {
            photos = new ArrayList();
            session.setAttribute("temp", photos);
        }
        MemPhoto photo = new MemPhoto();
        photo.setOriginalName(fileName);
        photo.setPath(tempFile.getPath());
        photos.add(photo);

        Map respMap = new HashMap();
        respMap.put("status", 1);

        return respMap;
    }

    /**
     * 获取文件后缀
     * .jpg .png
     *
     * @param name
     * @return
     */
    private static String getFileExtension(String name) {

        return name.substring(name.lastIndexOf(".")).toLowerCase();
    }
}
