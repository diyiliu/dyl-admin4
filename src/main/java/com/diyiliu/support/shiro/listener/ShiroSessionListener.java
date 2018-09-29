package com.diyiliu.support.shiro.listener;

import com.diyiliu.web.photo.dto.MemPhoto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Description: ShiroSessionListener
 * Author: DIYILIU
 * Update: 2018-09-20 09:42
 */

@Slf4j
@Component
public class ShiroSessionListener implements SessionListener {

    @Override
    public void onStart(Session session) {

        //log.info("会话创建 ... ");
    }

    @Override
    public void onStop(Session session) {
        //log.info("会话退出 ... ");

        try {
            clearTemp(session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExpiration(Session session) {
        //log.info("会话过期 ... ");

        try {
            clearTemp(session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearTemp(Session session) throws Exception{
        List<MemPhoto> photos = (List<MemPhoto>) session.getAttribute("temp");

        if (CollectionUtils.isNotEmpty(photos)){
            for (MemPhoto photo: photos){
                String path = photo.getPath();
                org.springframework.core.io.Resource res = new UrlResource("file:" + path);
                if (res.exists()){
                    res.getFile().delete();
                    log.info("删除临时文件: [{}]", path);
                }
            }
        }
    }
}
