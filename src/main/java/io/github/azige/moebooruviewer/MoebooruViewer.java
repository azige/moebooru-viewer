/*
 * Created 2015-11-27 23:32:48
 */
package io.github.azige.moebooruviewer;

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

/**
 *
 * @author Azige
 */
public class MoebooruViewer{

    public static final String KONACHAN_URL = "https://konachan.com";
    public static final String YANDERE_URL = "https://yande.re";

    private static ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static MoebooruAPI mapi = new MoebooruAPI(KONACHAN_URL);
    private static NetIO netIO = new NetIO(new File("konacha.com"));

    private static void init(){
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

    private static void destroy(){
        executorService.shutdownNow();
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
            JFrame frame = new ListPostFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter(){

                @Override
                public void windowClosed(WindowEvent e){
                    destroy();
                }

            });
            frame.setVisible(true);
        });
    }
}
