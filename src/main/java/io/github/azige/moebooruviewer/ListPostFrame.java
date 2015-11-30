/*
 * Created 2015-11-27 23:32:57
 */
package io.github.azige.moebooruviewer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author Azige
 */
public class ListPostFrame extends javax.swing.JFrame{

    private static final int SAMPLE_WIDTH = 150;
    private static final int SAMPLE_HEIGHT = 100;
    private int pageCount = 1;
    private Set<Post> posts = new HashSet<>();
    private ShowPostFrame postFrame = new ShowPostFrame();
//    private Map<Post, ShowPostFrame> postFrameMap = new HashMap<>();
    private final JLabel loadMoreLabel;
    private String[] tags;

    /**
     * Creates new form MainFrame
     */
    public ListPostFrame(String... tags){
        initComponents();

        this.tags = tags;
        if (tags.length > 0){
            setTitle(getTitle() + "[" + Stream.of(tags).reduce((a, b) -> a + " " + b).get() + "]");
        }
        setLocationRelativeTo(null);
        postsPanel.setLayout(new FlowLayout(){

            @Override
            public void layoutContainer(Container target){

                int columnCount = target.getSize().width / (SAMPLE_WIDTH + getHgap());
                int rowCount = posts.size() / columnCount + 1;
                target.setPreferredSize(new Dimension(1,
                    rowCount * (SAMPLE_HEIGHT + getVgap()) + getVgap()
                ));
                super.layoutContainer(target);
            }

        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        loadMoreLabel = new JLabel("加载更多");
        loadMoreLabel.setForeground(Color.WHITE);
        loadMoreLabel.setHorizontalAlignment(JLabel.CENTER);
        loadMoreLabel.setPreferredSize(new Dimension(SAMPLE_WIDTH, SAMPLE_HEIGHT));
        loadMoreLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadMoreLabel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e){
                if (loadMoreLabel.isEnabled()){
                    loadImages();
                }
            }

        });

        postsPanel.add(loadMoreLabel);

        loadImages();
    }

    @Override
    protected void processWindowEvent(WindowEvent e){
        if (e.getID() == WindowEvent.WINDOW_CLOSING){
            postFrame.dispose();
//            postFrameMap.values().forEach(ShowPostFrame::dispose);
        }
        super.processWindowEvent(e);
    }

    private void loadImages(){
        loadMoreLabel.setText("加载中……");
        loadMoreLabel.setEnabled(false);
        MoebooruViewer.execute(() -> {
            List<Post> postList = MoebooruViewer.getNetIO().retry(() -> MoebooruViewer.getMAPI().listPosts(pageCount, tags));
            SwingUtilities.invokeLater(() -> {
                pageCount++;
                postsPanel.remove(loadMoreLabel);
                for (Post post : postList){
                    if (posts.contains(post)){
                        continue;
                    }
                    posts.add(post);
                    JLabel label = new JLabel("加载中……");
                    label.setForeground(Color.WHITE);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setPreferredSize(new Dimension(SAMPLE_WIDTH, SAMPLE_HEIGHT));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    class LoadImageTask implements Runnable{

                        boolean force = false;

                        @Override
                        public void run(){
                            Image image = MoebooruViewer.getNetIO().loadPreview(post, force);
                            SwingUtilities.invokeLater(() -> {
                                if (image != null){
                                    label.setText("");
                                    Dimension size = label.getPreferredSize();
                                    label.setIcon(new ImageIcon(MoebooruViewer.resizeImage(image, size.getWidth(), size.getHeight())));
                                }else{
                                    label.setText("加载失败！");
                                }
                            });
                        }

                    }

                    label.addMouseListener(new MouseAdapter(){

                        @Override
                        public void mouseClicked(MouseEvent e){
                            if (SwingUtilities.isLeftMouseButton(e)){
//                                 ShowPostFrame showPostFrame = postFrameMap.get(post);
//                                if (showPostFrame == null){
//                                    showPostFrame = new ShowPostFrame();
//                                    showPostFrame.showPost(post);
//                                }
//                                showPostFrame.setVisible(true);
                                postFrame.showPost(post);
                                postFrame.setVisible(true);
                            }else if (SwingUtilities.isRightMouseButton(e)){
                                label.setIcon(null);
                                label.setText("加载中……");
                                LoadImageTask task = new LoadImageTask();
                                task.force = true;
                                MoebooruViewer.execute(task);
                            }
                        }

                    });
                    postsPanel.add(label);
                    LoadImageTask task = new LoadImageTask();
                    MoebooruViewer.execute(task);
                }
                postsPanel.add(loadMoreLabel);
                loadMoreLabel.setText("加载更多");
                loadMoreLabel.setEnabled(true);
            });
        });
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        postsPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Moebooru Viewer");
        setMinimumSize(new java.awt.Dimension(850, 650));
        setPreferredSize(new java.awt.Dimension(850, 650));

        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        postsPanel.setBackground(new java.awt.Color(0, 0, 0));
        postsPanel.setPreferredSize(new java.awt.Dimension(800, 10000));
        postsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        scrollPane.setViewportView(postsPanel);

        jMenu1.setText("功能");

        jMenuItem1.setText("搜索tag");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        new ListPostFrame(JOptionPane.showInputDialog("输入要搜索的tag，用空格分隔").split(" ")).setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel postsPanel;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
