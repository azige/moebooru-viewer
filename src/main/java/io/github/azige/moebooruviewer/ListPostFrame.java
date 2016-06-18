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
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ListPostFrame extends javax.swing.JFrame{

    private static final Logger logger = LoggerFactory.getLogger(ListPostFrame.class);
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 100;
    private static final int PREVIEW_BORDER_THICKNESS = 3;
    private static final Color CLICKED_POST_BORDER_COLOR = Color.YELLOW;
    private static final Color CACHED_POST_BORDER_COLOR = Color.GREEN;

    @Autowired
    private ApplicationContext context;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private MoebooruViewer moebooruViewer;
    @Autowired
    private NetIO netIO;
    @Autowired
    private ExecutorService executor;
    @Autowired
    private MoebooruAPI mapi;
    @Autowired
    private UserSetting userSetting;

    private int pageCount = 1;
    private Set<Post> posts = new HashSet<>();
    private final JLabel loadMoreLabel;
    private String[] tags = {};
    private int pageSize;

    /**
     * Creates new form MainFrame
     */
    public ListPostFrame(){
        initComponents();

        setLocationRelativeTo(null);
        postsPanel.setLayout(new FlowLayout(){

            @Override
            public void layoutContainer(Container target){

                int columnCount = target.getSize().width / (PREVIEW_WIDTH + getHgap());
                int rowCount = posts.size() / columnCount + 1;
                target.setPreferredSize(new Dimension(1,
                    rowCount * (PREVIEW_HEIGHT + getVgap()) + getVgap()
                ));
                super.layoutContainer(target);
            }

        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        loadMoreLabel = new JLabel("加载更多");
        loadMoreLabel.setForeground(Color.WHITE);
        loadMoreLabel.setHorizontalAlignment(JLabel.CENTER);
        loadMoreLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
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
    }

    @PostConstruct
    private void init(){
        setTitle(siteConfig.getName() + " Viewer");
        pageSize = userSetting.getPageSize();
    }

    public String[] getTags(){
        return tags;
    }

    public void setTags(String[] tags){
        this.tags = Objects.requireNonNull(tags);
        refreshTitle();
    }

    @Override
    protected void processWindowEvent(WindowEvent e){
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING || e.getID() == WindowEvent.WINDOW_CLOSED){
        }
    }

    private void refreshTitle(){
        StringBuilder sb = new StringBuilder();
        sb.append(siteConfig.getName()).append(" Viewer");
        sb.append(" [page: ").append(pageCount).append("]");
        if (tags.length > 0){
            sb.append(" [").append(Stream.of(tags).reduce((a, b) -> a + " " + b).get()).append("]");
        }

        setTitle(sb.toString());
    }

    public void loadImages(){
        loadMoreLabel.setText("加载中……");
        loadMoreLabel.setEnabled(false);
        refreshTitle();
        executor.execute(() -> {
            List<Post> postList = netIO.retry(() -> mapi.listPosts(pageCount, pageSize, tags));
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
                    label.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    if (netIO.getSampleFile(post).exists()){
                        label.setBorder(new LineBorder(CACHED_POST_BORDER_COLOR, PREVIEW_BORDER_THICKNESS));
                    }

                    class LoadImageTask implements Runnable{

                        boolean force = false;

                        @Override
                        public void run(){
                            Image image = netIO.loadPreview(post, force);
                            SwingUtilities.invokeLater(() -> {
                                if (image != null){
                                    label.setText("");
                                    Dimension size = label.getPreferredSize();
                                    label.setIcon(new ImageIcon(Utils.resizeImage(image, size.getWidth(), size.getHeight())));
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
                                moebooruViewer.showPost(post);
                                label.setBorder(new LineBorder(CLICKED_POST_BORDER_COLOR, PREVIEW_BORDER_THICKNESS));
                            }else if (SwingUtilities.isRightMouseButton(e)){
                                label.setIcon(null);
                                label.setText("加载中……");
                                LoadImageTask task = new LoadImageTask();
                                task.force = true;
                                executor.execute(task);
                            }
                        }

                    });
                    postsPanel.add(label);
                    LoadImageTask task = new LoadImageTask();
                    executor.execute(task);
                }
                postsPanel.add(loadMoreLabel);
                if (!postList.isEmpty()){
                    loadMoreLabel.setText("加载更多");
                    loadMoreLabel.setEnabled(true);
                }else{
                    loadMoreLabel.setText("没有更多了");
                }
            });
        });
    }

    public void clear(){
        posts.clear();
        postsPanel.removeAll();
        postsPanel.add(loadMoreLabel);
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        postsPanel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        openPostMenuItem = new javax.swing.JMenuItem();
        jumpPageMenuItem = new javax.swing.JMenuItem();
        searchTagMenuItem = new javax.swing.JMenuItem();
        searchHistoryMenu = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        switchKonachanMenuItem = new javax.swing.JMenuItem();
        switchYandereMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        configMenuItem = new javax.swing.JMenuItem();
        cleanCacheMenuItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        showVersionMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Moebooru Viewer");
        setMinimumSize(new java.awt.Dimension(850, 650));

        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        postsPanel.setBackground(new java.awt.Color(34, 34, 34));
        postsPanel.setPreferredSize(new java.awt.Dimension(800, 10000));
        postsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        scrollPane.setViewportView(postsPanel);

        jMenu1.setText("功能");
        jMenu1.setMinimumSize(new java.awt.Dimension(200, 0));

        openPostMenuItem.setText("按id检索Post");
        openPostMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPostMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(openPostMenuItem);

        jumpPageMenuItem.setText("跳至页数");
        jumpPageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jumpPageMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(jumpPageMenuItem);

        searchTagMenuItem.setText("搜索tag");
        searchTagMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTagMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(searchTagMenuItem);

        searchHistoryMenu.setText("搜索历史");
        searchHistoryMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchHistoryMenuMouseEntered(evt);
            }
        });
        jMenu1.add(searchHistoryMenu);

        jMenu2.setText("切换站点");

        switchKonachanMenuItem.setText("Konachan.com");
        switchKonachanMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchKonachanMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(switchKonachanMenuItem);

        switchYandereMenuItem.setText("yande.re");
        switchYandereMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchYandereMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(switchYandereMenuItem);

        jMenu1.add(jMenu2);
        jMenu1.add(jSeparator1);

        exitMenuItem.setText("退出");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu4.setText("设置");

        configMenuItem.setText("设置");
        configMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(configMenuItem);

        cleanCacheMenuItem.setText("清空缓存");
        cleanCacheMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanCacheMenuItemActionPerformed(evt);
            }
        });
        jMenu4.add(cleanCacheMenuItem);

        jMenuBar1.add(jMenu4);

        jMenu3.setText("帮助");

        showVersionMenuItem.setText("版本");
        showVersionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showVersionMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(showVersionMenuItem);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 850, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 629, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchTagMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTagMenuItemActionPerformed
        String keywords = JOptionPane.showInputDialog(this, "输入要搜索的tag，用空格分隔");
        if (keywords != null){
            moebooruViewer.listPosts(keywords.split(" "));
            userSetting.addSearchHistory(keywords);
        }
    }//GEN-LAST:event_searchTagMenuItemActionPerformed

    private void switchKonachanMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchKonachanMenuItemActionPerformed
        moebooruViewer.switchSite(SiteConfig.KONACHAN);
    }//GEN-LAST:event_switchKonachanMenuItemActionPerformed

    private void switchYandereMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchYandereMenuItemActionPerformed
        moebooruViewer.switchSite(SiteConfig.YANDERE);
    }//GEN-LAST:event_switchYandereMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        moebooruViewer.exit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void jumpPageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jumpPageMenuItemActionPerformed
        String pageString = JOptionPane.showInputDialog(this, "输入要跳转到的页数");
        if (pageString != null){
            pageCount = Integer.parseInt(pageString);
            clear();
            loadImages();
        }
    }//GEN-LAST:event_jumpPageMenuItemActionPerformed

    private void searchHistoryMenuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchHistoryMenuMouseEntered
        searchHistoryMenu.removeAll();
        userSetting.getSearchHistories().forEach(keywords -> {
            JMenuItem menuItem = new JMenuItem(keywords);
            menuItem.addActionListener(event -> moebooruViewer.listPosts(keywords.split(" ")));
            searchHistoryMenu.add(menuItem);
        });
    }//GEN-LAST:event_searchHistoryMenuMouseEntered

    private void showVersionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showVersionMenuItemActionPerformed
        String version;
        try{
            version = IOUtils.toString(getClass().getResourceAsStream("/io/github/azige/moebooruviewer/version"));
            JOptionPane.showMessageDialog(this, version);
        }catch (IOException ex){
            logger.error("无法读取版本标记", ex);
        }
    }//GEN-LAST:event_showVersionMenuItemActionPerformed

    private void openPostMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPostMenuItemActionPerformed
        String id = JOptionPane.showInputDialog(this, "输入要检索的id");
        if (id != null){
            try{
                moebooruViewer.showPostById(Integer.parseInt(id));
            }catch (NumberFormatException ex){
                JOptionPane.showMessageDialog(null, "输入的id格式有误");
            }
        }
    }//GEN-LAST:event_openPostMenuItemActionPerformed

    private void configMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configMenuItemActionPerformed
        ConfigDialog configDialog = context.getBean(ConfigDialog.class);
        configDialog.setLocationRelativeTo(this);
        configDialog.setVisible(true);
    }//GEN-LAST:event_configMenuItemActionPerformed

    private void cleanCacheMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanCacheMenuItemActionPerformed
        if (netIO.cleanCache()){
            JOptionPane.showMessageDialog(this, "删除成功！");
        }else{
            JOptionPane.showMessageDialog(this, "删除失败！");
        }
    }//GEN-LAST:event_cleanCacheMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem cleanCacheMenuItem;
    private javax.swing.JMenuItem configMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JMenuItem jumpPageMenuItem;
    private javax.swing.JMenuItem openPostMenuItem;
    private javax.swing.JPanel postsPanel;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JMenu searchHistoryMenu;
    private javax.swing.JMenuItem searchTagMenuItem;
    private javax.swing.JMenuItem showVersionMenuItem;
    private javax.swing.JMenuItem switchKonachanMenuItem;
    private javax.swing.JMenuItem switchYandereMenuItem;
    // End of variables declaration//GEN-END:variables
}
