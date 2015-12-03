/*
 * Created 2015-11-27 23:32:48
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Configuration
@ComponentScan
@Component
public class MoebooruViewer{

    private static final Logger logger = LoggerFactory.getLogger(MoebooruViewer.class);

    public static final String KONACHAN_URL = "https://konachan.com";
    public static final String KONACHAN_NAME = "konachan.com";

    public static final String YANDERE_URL = "https://yande.re";
    public static final String YANDERE_NAME = "yande.re";

    private static final int THREAD_POOL_SIZE = 10;

    @Autowired
    private AnnotationConfigApplicationContext context;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private ExecutorService executor;
    @Autowired
    private MoebooruAPI mapi;

    private Set<ListPostFrame> listPostFrames = new HashSet<>();

    public MoebooruViewer(){
    }

    @PostConstruct
    private void init(){
        logger.info("init");
        File tagFile = new File(siteConfig.getName(), "tags.json");
        if (tagFile.exists()){
            ObjectMapper mapper = new ObjectMapper();
            try{
                JsonNode root = mapper.readTree(tagFile);
                List<String> keys = new ArrayList<>();
                root.fieldNames().forEachRemaining(keys::add);
                Map<String, Tag> tagMap = keys.stream()
                    .collect(Collectors.toMap(Function.identity(), key -> mapper.convertValue(root.get(key), Tag.class)));
                mapi.setTagMap(tagMap);
            }catch (IOException ex){
                logger.warn("无法读取tag记录文件", ex);
            }
        }
    }

    public static Image resizeImage(Image image, double maxWidth, double maxHeight){
        double limitRatio = maxWidth / maxHeight;
        double width = image.getWidth(null);
        double height = image.getHeight(null);
        double ratio = width / height;
        if (ratio > limitRatio){
            width = maxWidth;
            height = maxWidth / ratio;
        }else{
            height = maxHeight;
            width = maxHeight * ratio;
        }
        return image.getScaledInstance((int)width, (int)height, Image.SCALE_SMOOTH);
    }

    public void listPosts(String... tags){
        ListPostFrame listPostFrame = context.getBean(ListPostFrame.class);
        listPostFrame.setTags(tags);
        listPostFrame.setVisible(true);
        listPostFrame.loadImages();

        listPostFrames.add(listPostFrame);
        listPostFrame.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e){
                listPostFrames.remove((ListPostFrame)e.getWindow());
                if (listPostFrames.isEmpty()){
                    context.close();
                }
            }

        });
    }

    public void switchSite(SiteConfig siteConfig){
        context.close();

        ApplicationContext context = buildContext(siteConfig);
        context.getBean(MoebooruViewer.class).listPosts();
    }

    @PreDestroy
    private void destroy(){
        logger.info("destroy");
        listPostFrames.forEach(ListPostFrame::dispose);
        executor.shutdownNow();
        ObjectMapper mapper = new ObjectMapper();
        File tagFile = new File(siteConfig.getName(), "tags.json");
        try{
            mapper.writeValue(tagFile, mapi.getTagMap());
        }catch (IOException ex){
            logger.warn("无法读取tag记录文件", ex);
        }
    }

    private static ApplicationContext buildContext(SiteConfig siteConfig){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerResolvableDependency(ExecutorService.class, Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        context.getBeanFactory().registerResolvableDependency(SiteConfig.class, siteConfig);
        context.register(MoebooruViewer.class);
        context.registerShutdownHook();
        context.refresh();
        return context;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]){
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try{
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()){
                if ("Nimbus".equals(info.getName())){
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex){
            java.util.logging.Logger.getLogger(ListPostFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        ApplicationContext context = buildContext(SiteConfig.KONACHAN);
        SwingUtilities.invokeLater(() -> {
            context.getBean(MoebooruViewer.class).listPosts();
        });

        /* Create and display the form */
    }
}
