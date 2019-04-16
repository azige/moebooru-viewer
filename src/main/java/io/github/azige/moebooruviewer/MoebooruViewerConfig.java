/*
 * Copyright (C) 2017 Azige
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.azige.moebooruviewer;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.bind.JAXB;

import io.github.azige.moebooruviewer.config.SiteConfig;
import io.github.azige.moebooruviewer.config.UserSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

/**
 *
 * @author Azige
 */
@Configuration
@ComponentScan
public class MoebooruViewerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MoebooruViewerConfig.class);

    private UserSetting userSetting = null;

    public MoebooruViewerConfig() {
        File settingFile = new File(MoebooruViewerConstants.SETTING_FILE_NAME);
        if (settingFile.exists()) {
            try {
                userSetting = JAXB.unmarshal(settingFile, UserSetting.class);
                userSetting.verifyAndRepair();
            } catch (RuntimeException ex) {
                LOG.warn("读取用户配置文件出错", ex);
            }
        }
        if (userSetting == null) {
            userSetting = UserSetting.createDefaultSetting();
        }

        {
            boolean success = false;
            if (userSetting.getLookAndFeel() != null) {
                try {
                    UIManager.setLookAndFeel(userSetting.getLookAndFeel());
                    success = true;
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    LOG.warn("设置 L&F 时出错", ex);
                }
            }
            if (!success) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    userSetting.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    LOG.warn("设置系统默认 L&F 时出错", ex);
                }
            }
        }
    }

    @Bean
    public ThreadPoolExecutorFactoryBean generalDownloadExecutor() {
        ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
        factory.setCorePoolSize(MoebooruViewerConstants.DEFAULT_THREAD_POOL_SIZE);
        factory.setMaxPoolSize(MoebooruViewerConstants.DEFAULT_THREAD_POOL_SIZE);
        return factory;
    }

    @Bean
    public ThreadPoolExecutorFactoryBean imageFileDownloadExecutor() {
        ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
        factory.setCorePoolSize(MoebooruViewerConstants.DEFAULT_THREAD_POOL_SIZE);
        factory.setMaxPoolSize(MoebooruViewerConstants.DEFAULT_THREAD_POOL_SIZE);
        return factory;
    }

    @Bean
    public SiteConfig siteConfig() {
        return userSetting.getSiteConfig();
    }

    @Bean
    public UserSetting userSetting() {
        return userSetting;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30_000);
        requestFactory.setReadTimeout(30_000);
        return requestFactory;
    }
}
