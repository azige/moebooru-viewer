/*
 * Created 2015-11-27 23:32:48
 */
package io.github.azige.moebooruviewer;

import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.bind.JAXB;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.azige.moebooruviewer.config.SiteConfig;
import io.github.azige.moebooruviewer.config.UserSetting;
import io.github.azige.moebooruviewer.io.NetIO;
import io.github.azige.moebooruviewer.model.Post;
import io.github.azige.moebooruviewer.model.Tag;
import io.github.azige.moebooruviewer.ui.DownloadManagerFrame;
import io.github.azige.moebooruviewer.ui.DownloadTaskPanel;
import io.github.azige.moebooruviewer.ui.ListPostFrame;
import io.github.azige.moebooruviewer.ui.ShowPostFrame;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
public class MoebooruViewer implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MoebooruViewer.class);

    @Autowired
    private AnnotationConfigApplicationContext context;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private MoebooruAPI mapi;
    @Autowired
    private UserSetting userSetting;
    @Autowired
    private NetIO netIO;
    @Autowired
    private ShowPostFrame showPostFrame;
    @Autowired
    private DownloadManagerFrame downloadFrame;

    private Set<ListPostFrame> listPostFrames = new HashSet<>();
    private SiteConfig siteConfigToSwitch = null;

    public MoebooruViewer() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("init");

        {
            boolean success = false;
            if (userSetting.getLookAndFeel() != null) {
                try {
                    UIManager.setLookAndFeel(userSetting.getLookAndFeel());
                    success = true;
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    logger.warn("设置 L&F 时出错", ex);
                }
            }
            if (!success) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    userSetting.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    logger.warn("设置系统默认 L&F 时出错", ex);
                }
            }
        }

        File tagFile = new File(siteConfig.getName(), MoebooruViewerConstants.TAG_FILE_NAME);
        if (tagFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode root = mapper.readTree(tagFile);
                List<String> keys = new ArrayList<>();
                root.fieldNames().forEachRemaining(keys::add);
                Map<String, Tag> tagMap = keys.stream()
                    .collect(Collectors.toMap(Function.identity(), key -> mapper.convertValue(root.get(key), Tag.class)));
                mapi.setTagMap(tagMap);
            } catch (IOException ex) {
                logger.warn("无法读取tag记录文件", ex);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("destroy");
        listPostFrames.forEach(ListPostFrame::dispose);
        ObjectMapper mapper = new ObjectMapper();
        File tagFile = new File(siteConfig.getName(), MoebooruViewerConstants.TAG_FILE_NAME);
        try {
            mapper.writeValue(tagFile, mapi.getTagMap());
        } catch (IOException ex) {
            logger.warn("无法读取tag记录文件", ex);
        }
        JAXB.marshal(userSetting, new File(MoebooruViewerConstants.SETTING_FILE_NAME));
    }

    public void searchByTags(String tags) {
        listPosts(tags.split(" "));
        userSetting.addSearchHistory(tags);
    }

    public void listPosts(String... tags) {
        ListPostFrame listPostFrame = context.getBean(ListPostFrame.class);
        if (userSetting.isSafeMode()) {
            tags = Arrays.copyOf(tags, tags.length + 1);
            tags[tags.length - 1] = "rating:s";
        }
        listPostFrame.setTags(tags);
        listPostFrame.setVisible(true);
        listPostFrame.loadImages();

        listPostFrames.add(listPostFrame);
        listPostFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                listPostFrames.remove((ListPostFrame) e.getWindow());
                if (listPostFrames.isEmpty()) {
                    context.close();
                }
            }

        });
    }

    public void showPost(Post post) {
        showPostFrame.showPost(post);
    }

    public void showPostById(int id, java.awt.Component dialogParent) {
        JOptionPane optionPane = new JOptionPane(Localization.format("retrieval_format", String.valueOf(id)), JOptionPane.INFORMATION_MESSAGE);
        JButton button = new JButton(Localization.getString("cancel"));
        optionPane.setOptions(new Object[]{button});
        JDialog dialog = optionPane.createDialog(dialogParent, Localization.getString("retrieving"));
        button.addActionListener(event -> dialog.dispose());
        dialog.setModalityType(ModalityType.MODELESS);
        dialog.setVisible(true);
        mapi.listPosts(1, 1, "id:" + id)
            .observeOn(Schedulers.from(SwingUtilities::invokeLater))
            .subscribe(searchPosts -> {
                if (dialog.isDisplayable()) {
                    dialog.dispose();
                    if (!searchPosts.isEmpty()) {
                        showPostFrame.showPost(searchPosts.get(0));
                    } else {
                        JOptionPane.showMessageDialog(null, Localization.getString("id_doesnot_exists"),
                            Localization.getString("error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
    }

    public void switchSite(SiteConfig siteConfig) {
        userSetting.setSiteConfig(siteConfig);

        context.close();

        ApplicationContext context = buildContext();
        context.getBean(MoebooruViewer.class).listPosts();
    }

    /**
     * 提交一个下载任务
     *
     * @param localFile 本地保存位置
     * @param url       资源URL
     * @param taskName  任务的名字，可以为 null，此时使用保存文件名替代
     */
    public void downloadFile(File localFile, String url, String taskName) {
        if (taskName == null) {
            taskName = localFile.getName();
        }
        DownloadTaskPanel taskPanel = context.getBean(DownloadTaskPanel.class);
        taskPanel.setTaskName(taskName);
        taskPanel.setDownloadFile(localFile);
        downloadFrame.addTaskPanel(taskPanel);

        if (!downloadFrame.isVisible()) {
            downloadFrame.setVisible(true);
        }

        // TODO: Refactor again
        netIO.downloadFileAsync(url, localFile, taskPanel::onProgress)
            .doOnComplete(() -> taskPanel.onComplete(localFile))
            .doOnError(taskPanel::onFail)
            .subscribe();
    }

    public void exit() {
        context.close();
    }

    private static ApplicationContext buildContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MoebooruViewerConfig.class);
        context.registerShutdownHook();
        return context;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        UIManager.put("OptionPane.yesButtonText", Localization.getString("yes"));
        UIManager.put("OptionPane.noButtonText", Localization.getString("no"));
        UIManager.put("OptionPane.cancelButtonText", Localization.getString("cancel"));

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.warn("未捕获的异常", e);
            }
        });

        ApplicationContext context = buildContext();
        SwingUtilities.invokeLater(() -> {
            context.getBean(MoebooruViewer.class).listPosts();
            System.out.println(UIManager.getLookAndFeel().getName());
        });
    }
}
