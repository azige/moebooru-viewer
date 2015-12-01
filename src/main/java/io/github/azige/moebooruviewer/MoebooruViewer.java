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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Azige
 */
public class MoebooruViewer{

    private static final Logger logger = LoggerFactory.getLogger(MoebooruViewer.class);

    public static final String KONACHAN_URL = "https://konachan.com";
    public static final String KONACHAN_NAME = "konachan.com";

    public static final String YANDERE_URL = "https://yande.re";
    public static final String YANDERE_NAME = "yande.re";

    private static String siteName = KONACHAN_NAME;

    private static final int THREAD_POOL_SIZE = 10;

    private static ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static MoebooruAPI mapi = new MoebooruAPI(KONACHAN_URL);
    private static NetIO netIO = new NetIO(new File(siteName));
    private static ListPostFrame listPostFrame;

    private static void init(){
        logger.info("init");
        File tagFile = new File(siteName, "tags.json");
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

    public static void switchToKonachan(){
        ListPostFrame.disposeAllInstance();

        destroy();

        siteName = KONACHAN_NAME;
        mapi = new MoebooruAPI(KONACHAN_URL);
        netIO = new NetIO(new File(KONACHAN_NAME));
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        init();

        listPostFrame = new ListPostFrame(KONACHAN_NAME);
        configFrame(listPostFrame);
    }

    public static void switchToYandere(){
        ListPostFrame.disposeAllInstance();

        destroy();

        siteName = YANDERE_NAME;
        mapi = new MoebooruAPI(YANDERE_URL);
        netIO = new NetIO(new File(YANDERE_NAME));
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        init();

        listPostFrame = new ListPostFrame(YANDERE_NAME);
        configFrame(listPostFrame);
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

    public static MoebooruAPI getMAPI(){
        return mapi;
    }

    public static NetIO getNetIO(){
        return netIO;
    }

    public static void execute(Runnable task){
        executorService.execute(task);
    }

    public static String getSiteName(){
        return siteName;
    }

    private static void destroy(){
        logger.info("destroy");
        executorService.shutdownNow();
        ObjectMapper mapper = new ObjectMapper();
        File tagFile = new File(siteName, "tags.json");
        try{
            mapper.writeValue(tagFile, mapi.getTagMap());
        }catch (IOException ex){
            logger.warn("无法读取tag记录文件", ex);
        }
    }

    private static void configFrame(ListPostFrame frame){
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e){
                destroy();
            }

        });
        frame.setVisible(true);
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

        init();

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            listPostFrame = new ListPostFrame(siteName);
            configFrame(listPostFrame);
        });
    }
}
