/*
 * Created 2015-11-30 23:35:10
 */
package io.github.azige.moebooruviewer.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import io.github.azige.moebooruviewer.Localization;
import io.github.azige.moebooruviewer.MoebooruAPI;
import io.github.azige.moebooruviewer.MoebooruViewer;
import io.github.azige.moebooruviewer.Utils;
import io.github.azige.moebooruviewer.config.UserSetting;
import io.github.azige.moebooruviewer.config.UserSetting.SaveLocation;
import io.github.azige.moebooruviewer.io.MoebooruRepository;
import io.github.azige.moebooruviewer.model.Post;
import io.github.azige.moebooruviewer.model.Tag;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ShowPostPanel extends javax.swing.JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ShowPostPanel.class);

    @Autowired
    MoebooruViewer moebooruViewer;
    @Autowired
    private MoebooruAPI mapi;
    @Autowired
    private UserSetting userSetting;
    @Autowired
    private MoebooruRepository moebooruRepository;

    private static final Map<Integer, Color> TAG_COLOR_MAP;
    private static final Color COLOR_SUCCESS = Color.decode("0x339900");
    private static final Color COLOR_FAIL = Color.decode("0xCC0000");
    private static final Color COLOR_UNKNOWN_TAG_TYPE = Color.decode("0xFF2020");

    private Post presentingPost;
    private Image image;
    private List<LoadingListener> loadingListeners = new ArrayList<>();
    private JFileChooser fileChooser = new JFileChooser();
    private boolean needResizeImage = false;
    private Tag showingPopupMenuTag;

    static {
        TAG_COLOR_MAP = new HashMap<>();
        TAG_COLOR_MAP.put(Tag.TYPE_GENERAL, Color.decode("0xEE8887"));
        TAG_COLOR_MAP.put(Tag.TYPE_ARTIST, Color.decode("0xCCCC00"));
        TAG_COLOR_MAP.put(Tag.TYPE_COPYRIGHT, Color.decode("0xDD00DD"));
        TAG_COLOR_MAP.put(Tag.TYPE_CHARACTER, Color.decode("0x00AA00"));
    }

    public class LoadingEvent extends EventObject {

        public LoadingEvent() {
            super(ShowPostPanel.this);
        }

        @Override
        public ShowPostPanel getSource() {
            return (ShowPostPanel) super.getSource();
        }
    }

    public interface LoadingListener extends EventListener {

        void loading(LoadingEvent event);

        void done(LoadingEvent event);
    }

    /**
     * Creates new form ShowPostFramePanel
     */
    public ShowPostPanel() {
        initComponents();
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                needResizeImage = true;
            }

        });
    }

    private void initTagPanel() {
        final int lineLimit = 25;
        final List<Integer> tagTypeOrders = Arrays.asList(Tag.TYPE_ARTIST, Tag.TYPE_COPYRIGHT, Tag.TYPE_CHARACTER, Tag.TYPE_GENERAL);

//        List<String> resolveFailedTagNames = new ArrayList<>();
        Flowable.fromArray(presentingPost.getTags().split(" "))
            .subscribeOn(Schedulers.io())
            .flatMap(tagName -> mapi.findTag(tagName).onErrorComplete().toFlowable())
            .sorted((a, b) -> {
                int va = tagTypeOrders.indexOf(a.getType());
                if (va == -1) {
                    va = Integer.MAX_VALUE;
                }
                int vb = tagTypeOrders.indexOf(b.getType());
                if (vb == -1) {
                    vb = Integer.MAX_VALUE;
                }
                return va - vb;
            })
            .toList()
            .observeOn(Schedulers.from(SwingUtilities::invokeLater))
            .subscribe(sortedTags -> {
                tagPanel.removeAll();
                sortedTags.forEach(tag -> {
                    String tagName = tag.getName();
                    String viewTagName = tagName.replaceAll("_", " ");
                    if (viewTagName.length() > lineLimit) {
                        StringBuilder targetBuilder = new StringBuilder("<html>");
                        int count = 0;
                        while (true) {
                            targetBuilder.append(viewTagName, count, Math.min(count + lineLimit, viewTagName.length()));
                            if (count + lineLimit < viewTagName.length()) {
                                targetBuilder.append("<br/>");
                                count += lineLimit;
                            } else {
                                break;
                            }
                        }
                        targetBuilder.append("</html>");
                        viewTagName = targetBuilder.toString();
                    }

                    JLabel label = new JLabel(viewTagName);
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    Color color = TAG_COLOR_MAP.get(tag.getType());
                    if (color != null) {
                        label.setForeground(color);
                    } else {
                        label.setForeground(COLOR_UNKNOWN_TAG_TYPE);
                    }
                    label.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mousePressed(MouseEvent e) {
                            popupMenu(e);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            popupMenu(e);
                        }

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                moebooruViewer.searchByTags(tagName);
                            }
                        }

                        private void popupMenu(MouseEvent e) {
                            if (e.isPopupTrigger()) {
                                showingPopupMenuTag = tag;
                                tagPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    });
                    tagPanel.add(label);
                });
            });
    }

    private void initToolPanel() {
        userSetting.getSaveLocations().forEach(sl -> {
            samplePanel.add(createDownloadLabel(sl, moebooruRepository.getSampleFile(presentingPost), presentingPost.getSampleUrl()));
            jpegPanel.add(createDownloadLabel(sl, null, presentingPost.getJpegUrl()));
            originPanel.add(createDownloadLabel(sl, moebooruRepository.getOriginFile(presentingPost), presentingPost.getOriginUrl()));
        });
        samplePanel.add(createDownloadToLabel(moebooruRepository.getSampleFile(presentingPost), presentingPost.getSampleUrl()));
        samplePanel.add(createCopyToClipboardLabel(presentingPost.getSampleUrl()));
        jpegPanel.add(createDownloadToLabel(null, presentingPost.getJpegUrl()));
        jpegPanel.add(createCopyToClipboardLabel(presentingPost.getJpegUrl()));
        originPanel.add(createDownloadToLabel(moebooruRepository.getOriginFile(presentingPost), presentingPost.getOriginUrl()));
        originPanel.add(createCopyToClipboardLabel(presentingPost.getOriginUrl()));

        String source = presentingPost.getSource();
        if (source != null && source.startsWith("http")) {
            try {

                source = convertSource(source);

                URL url = new URL(source);
                sourceLinkLabel.setText(url.getHost());
                sourceLinkLabel.setToolTipText(url.toString());
                sourceLinkLabel.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            try {
                                Desktop.getDesktop().browse(url.toURI());
                            } catch (IOException | URISyntaxException ex) {
                                logger.warn("无法浏览URL：" + url, ex);
                            }
                        }
                    }
                });
            } catch (MalformedURLException ex) {
                logger.info("URL格式错误", ex);
                sourcePanel.setVisible(false);
            }
        } else {
            sourcePanel.setVisible(false);
        }
    }

    /**
     * 将特定的来源 URL 转换成可用的形式。
     * 例如某些来源于 pixiv 的 URL 是其资源服务器上的文件，
     * 而这些文件是无法从 pixiv 投稿页面以外的地方引用的。
     */
    private String convertSource(String source) {

        // 转换 pixiv 来源的 URL
        Matcher matcher;

        // 诸如 https://i2.pixiv.net/img-original/img/2016/10/08/00/51/39/59361141_p0.jpg 的来源
        matcher = Pattern.compile(".+\\.pixiv\\.net.*/(\\d+)").matcher(source);
        if (matcher.find()) {
            return "http://www.pixiv.net/artworks/" + matcher.group(1);
        }

        // 诸如 https://i.pximg.net/img-original/img/2017/08/07/01/07/06/64267464_p0.jpg 的来源
        matcher = Pattern.compile(".+\\.pximg\\.net.*/(\\d+)").matcher(source);
        if (matcher.find()) {
            return "http://www.pixiv.net/artworks/" + matcher.group(1);
        }

        return source;
    }

    private void initAssociatePanels() {
        if (presentingPost.getParentId() != null) {
            int parentId = presentingPost.getParentId();
            parentLinkLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    moebooruViewer.showPostById(parentId, ShowPostPanel.this);
                }
            });
        } else {
            parentPanel.setVisible(false);
        }

        if (presentingPost.isHasChildren()) {
            childrenLinkLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    moebooruViewer.listPosts("parent:" + presentingPost.getId());
                }
            });
        } else {
            childrenPanel.setVisible(false);
        }

        if (presentingPost.getPool() != null) {
            poolLinkLabel.setToolTipText(presentingPost.getPool().getName());
            poolLinkLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    moebooruViewer.listPosts("pool:" + presentingPost.getPool().getId());
                }
            });
            poolDownloadLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    downloadFileTo(poolDownloadLabel, null, mapi.getUrlOfPoolArchive(presentingPost.getPool()));
                }
            });
            poolViewOnSiteLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(mapi.getUrlOfPool(presentingPost.getPool())));
                    } catch (URISyntaxException | IOException ex) {
                        logger.info("无法启动浏览器", ex);
                    }
                }
            });
        } else {
            poolPanel.setVisible(false);
        }
    }

    private JLabel createDownloadLabel(SaveLocation sl, File localFile, String url) {
        JLabel label = new JLabel(Localization.format("download_to_format", sl.getName()));
        label.setForeground(Color.WHITE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    downloadFile(label, localFile, url, new File(sl.getLocation(), Utils.getFileNameFromUrl(url)));
                }
            }

        });
        return label;
    }

    private JLabel createDownloadToLabel(File localFile, String url) {
        JLabel label = new JLabel(Localization.getString("download_to"));
        label.setForeground(Color.WHITE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    downloadFileTo(label, localFile, url);
                }
            }

        });
        return label;
    }

    private JLabel createCopyToClipboardLabel(String url) {
        JLabel label = new JLabel(Localization.getString("copy_link_to_clipboard"));
        label.setForeground(Color.WHITE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // quick fix for that URLs from konachan.net API does not contain protocol
        final String fixedUrl;
        if (url.startsWith("//")) {
            fixedUrl = "http:" + url;
        } else {
            fixedUrl = url;
        }
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    getToolkit().getSystemClipboard().setContents(new StringSelection(fixedUrl), null);
                    JOptionPane.showMessageDialog(ShowPostPanel.this, Localization.getString("success"));
                }
            }

        });
        return label;
    }

    private void makeLabelDone(JLabel label, File file) {
        label.setEnabled(true);
        label.setText(Localization.getString("download_completed"));
        label.setForeground(COLOR_SUCCESS);
        Stream.of(label.getMouseListeners())
            .forEach(label::removeMouseListener);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(file.getParentFile().toURI());
                } catch (IOException ex) {
                    logger.warn("打开浏览器出错", ex);
                }
            }
        });
    }

    /**
     * Submit a task of downloading.
     *
     * @param label     Which label clicked
     * @param localFile Possibly existed local file, could be null. If it's not
     *                  null and exists, simplely copy this file to saveFile
     * @param url       The url to download
     * @param saveFile  Where to save downloaded file
     */
    private void downloadFile(JLabel label, File localFile, String url, File saveFile) {
        label.setEnabled(false);
        if (localFile != null && localFile.exists()) {
            try {
                FileUtils.copyFile(localFile, saveFile);
                makeLabelDone(label, saveFile);
            } catch (IOException ex) {
                logger.warn("复制文件异常", ex);
                label.setText(Localization.getString("failed_to_copy_files"));
                label.setForeground(COLOR_FAIL);
                label.setEnabled(true);
            }
        } else {
            label.setText(Localization.getString("downloading"));
            label.setForeground(Color.WHITE);
            moebooruViewer.downloadFile(saveFile, url, null);
//            executor.execute(() -> {
//                boolean flag = netIO.cacheFile(saveFile, url, true);
//                SwingUtilities.invokeLater(() -> {
//                    if (flag){
//                        makeLabelDone(label, saveFile);
//                    }else{
//                        label.setText(Localization.getString("failed_to_download_file"));
//                        label.setForeground(COLOR_FAIL);
//                        label.setEnabled(true);
//                    }
//                });
//            });
        }
    }

    private void downloadFileTo(JLabel label, File localFile, String url) {
        if (label.isEnabled()) {
            if (userSetting.getLastSaveDir() != null) {
                fileChooser.setCurrentDirectory(userSetting.getLastSaveDir());
            }
            fileChooser.setSelectedFile(new File(Utils.getFileNameFromUrl(url)));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                userSetting.setLastSaveDir(fileChooser.getCurrentDirectory());
                downloadFile(label, localFile, url, fileChooser.getSelectedFile());
            }
        }
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

        tagPopupMenu = new javax.swing.JPopupMenu();
        copyTagNameMenuItem = new javax.swing.JMenuItem();
        addToFavoriteMenuItem = new javax.swing.JMenuItem();
        postPopupMenu = new javax.swing.JPopupMenu();
        copyPostImageMenuItem = new javax.swing.JMenuItem();
        reloadMenuItem = new javax.swing.JMenuItem();
        viewOnSiteMenuItem = new javax.swing.JMenuItem();
        poolDownloadLabel = new javax.swing.JLabel();
        toolPanel = new javax.swing.JPanel();
        samplePanel = new javax.swing.JPanel();
        jpegPanel = new javax.swing.JPanel();
        originPanel = new javax.swing.JPanel();
        sourcePanel = new javax.swing.JPanel();
        sourceLinkLabel = new javax.swing.JLabel();
        centerPanel = new javax.swing.JPanel();
        postLabel = new javax.swing.JLabel();
        infoPanel = new javax.swing.JPanel();
        poolPanel = new javax.swing.JPanel();
        poolLinkLabel = new javax.swing.JLabel();
        poolViewOnSiteLabel = new javax.swing.JLabel();
        parentPanel = new javax.swing.JPanel();
        parentLinkLabel = new javax.swing.JLabel();
        childrenPanel = new javax.swing.JPanel();
        childrenLinkLabel = new javax.swing.JLabel();
        tagPanel = new javax.swing.JPanel();
        tagLoadingLabel = new javax.swing.JLabel();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("io/github/azige/moebooruviewer/Messages"); // NOI18N
        copyTagNameMenuItem.setText(bundle.getString("copy")); // NOI18N
        copyTagNameMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTagNameMenuItemActionPerformed(evt);
            }
        });
        tagPopupMenu.add(copyTagNameMenuItem);

        addToFavoriteMenuItem.setText(Localization.getString("add_to_favorite")); // NOI18N
        addToFavoriteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addToFavoriteMenuItemActionPerformed(evt);
            }
        });
        tagPopupMenu.add(addToFavoriteMenuItem);

        copyPostImageMenuItem.setText(bundle.getString("copy_to_clipboard")); // NOI18N
        copyPostImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyPostImageMenuItemActionPerformed(evt);
            }
        });
        postPopupMenu.add(copyPostImageMenuItem);

        reloadMenuItem.setText(bundle.getString("reload")); // NOI18N
        reloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadMenuItemActionPerformed(evt);
            }
        });
        postPopupMenu.add(reloadMenuItem);

        viewOnSiteMenuItem.setText(Localization.getString("view_on_site")); // NOI18N
        viewOnSiteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewOnSiteMenuItemActionPerformed(evt);
            }
        });
        postPopupMenu.add(viewOnSiteMenuItem);

        poolDownloadLabel.setForeground(new java.awt.Color(255, 255, 255));
        poolDownloadLabel.setText(bundle.getString("click_to_download_archive")); // NOI18N
        poolDownloadLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setBackground(new java.awt.Color(34, 34, 34));
        setLayout(new java.awt.BorderLayout());

        toolPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolPanel.setOpaque(false);
        toolPanel.setLayout(new javax.swing.BoxLayout(toolPanel, javax.swing.BoxLayout.Y_AXIS));

        samplePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("sample"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        samplePanel.setAlignmentX(0.0F);
        samplePanel.setOpaque(false);
        samplePanel.setLayout(new javax.swing.BoxLayout(samplePanel, javax.swing.BoxLayout.Y_AXIS));
        toolPanel.add(samplePanel);

        jpegPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "JPEG", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255)));
        jpegPanel.setAlignmentX(0.0F);
        jpegPanel.setOpaque(false);
        jpegPanel.setLayout(new javax.swing.BoxLayout(jpegPanel, javax.swing.BoxLayout.Y_AXIS));
        toolPanel.add(jpegPanel);

        originPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("original_file"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        originPanel.setAlignmentX(0.0F);
        originPanel.setOpaque(false);
        originPanel.setLayout(new javax.swing.BoxLayout(originPanel, javax.swing.BoxLayout.Y_AXIS));
        toolPanel.add(originPanel);

        sourcePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("source"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        sourcePanel.setOpaque(false);
        sourcePanel.setLayout(new javax.swing.BoxLayout(sourcePanel, javax.swing.BoxLayout.Y_AXIS));

        sourceLinkLabel.setForeground(new java.awt.Color(255, 255, 255));
        sourceLinkLabel.setText("Source link"); // NOI18N
        sourceLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sourcePanel.add(sourceLinkLabel);

        toolPanel.add(sourcePanel);

        add(toolPanel, java.awt.BorderLayout.LINE_END);

        centerPanel.setOpaque(false);
        centerPanel.setLayout(new java.awt.BorderLayout());

        postLabel.setForeground(new java.awt.Color(255, 255, 255));
        postLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        postLabel.setText(bundle.getString("loading")); // NOI18N
        postLabel.setPreferredSize(new java.awt.Dimension(800, 600));
        centerPanel.add(postLabel, java.awt.BorderLayout.CENTER);

        add(centerPanel, java.awt.BorderLayout.CENTER);

        infoPanel.setOpaque(false);
        infoPanel.setLayout(new javax.swing.BoxLayout(infoPanel, javax.swing.BoxLayout.Y_AXIS));

        poolPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("pool"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        poolPanel.setOpaque(false);
        poolPanel.setLayout(new javax.swing.BoxLayout(poolPanel, javax.swing.BoxLayout.Y_AXIS));

        poolLinkLabel.setForeground(new java.awt.Color(255, 255, 255));
        poolLinkLabel.setText(bundle.getString("click_to_search")); // NOI18N
        poolLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        poolPanel.add(poolLinkLabel);

        poolViewOnSiteLabel.setForeground(new java.awt.Color(255, 255, 255));
        poolViewOnSiteLabel.setText(bundle.getString("view_pool_on_site")); // NOI18N
        poolViewOnSiteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        poolPanel.add(poolViewOnSiteLabel);

        infoPanel.add(poolPanel);

        parentPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("parent_post"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        parentPanel.setOpaque(false);
        parentPanel.setLayout(new javax.swing.BoxLayout(parentPanel, javax.swing.BoxLayout.Y_AXIS));

        parentLinkLabel.setForeground(new java.awt.Color(255, 255, 255));
        parentLinkLabel.setText(bundle.getString("click_to_view")); // NOI18N
        parentLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        parentPanel.add(parentLinkLabel);

        infoPanel.add(parentPanel);

        childrenPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("child_post"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        childrenPanel.setOpaque(false);
        childrenPanel.setLayout(new javax.swing.BoxLayout(childrenPanel, javax.swing.BoxLayout.Y_AXIS));

        childrenLinkLabel.setForeground(new java.awt.Color(255, 255, 255));
        childrenLinkLabel.setText(bundle.getString("click_to_search")); // NOI18N
        childrenLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        childrenPanel.add(childrenLinkLabel);

        infoPanel.add(childrenPanel);

        tagPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, Localization.getString("tags"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(255, 255, 255))); // NOI18N
        tagPanel.setOpaque(false);
        tagPanel.setLayout(new javax.swing.BoxLayout(tagPanel, javax.swing.BoxLayout.Y_AXIS));

        tagLoadingLabel.setForeground(new java.awt.Color(255, 255, 255));
        tagLoadingLabel.setText(bundle.getString("loading")); // NOI18N
        tagLoadingLabel.setEnabled(false);
        tagPanel.add(tagLoadingLabel);

        infoPanel.add(tagPanel);

        add(infoPanel, java.awt.BorderLayout.LINE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void copyTagNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyTagNameMenuItemActionPerformed
        if (showingPopupMenuTag != null) {
            getToolkit().getSystemClipboard().setContents(new StringSelection(showingPopupMenuTag.getName()), null);
        }
    }//GEN-LAST:event_copyTagNameMenuItemActionPerformed

    private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadMenuItemActionPerformed
        loadPost(presentingPost, true);
    }//GEN-LAST:event_reloadMenuItemActionPerformed

    private void copyPostImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyPostImageMenuItemActionPerformed
        getToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
    }//GEN-LAST:event_copyPostImageMenuItemActionPerformed

    private void viewOnSiteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewOnSiteMenuItemActionPerformed
        String url = userSetting.getSiteConfig().getRootUrl() + "/post/show/" + presentingPost.getId();
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException ex) {
            logger.info("无法启动浏览器", ex);
        }
    }//GEN-LAST:event_viewOnSiteMenuItemActionPerformed

    private void addToFavoriteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addToFavoriteMenuItemActionPerformed
        userSetting.getFavoriteTags().add(showingPopupMenuTag.getName());
    }//GEN-LAST:event_addToFavoriteMenuItemActionPerformed

    public void addLoadingListener(LoadingListener listener) {
        loadingListeners.add(listener);
    }

    public void removeLoadingListener(LoadingListener listener) {
        loadingListeners.remove(listener);
    }

    public Image getImage() {
        return image;
    }

    public boolean isNeedResizeImage() {
        return needResizeImage;
    }

    public void showPost(Post post) {
        presentingPost = post;
        loadPost(post, false);

        postLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                popupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                popupMenu(e);
            }

            private void popupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    postPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        initTagPanel();

        initToolPanel();

        initAssociatePanels();
    }

    private void loadPost(Post post, boolean force) {
        postLabel.setIcon(null);
        String loadingText = Localization.getString("loading");
        postLabel.setText(loadingText);
        loadingListeners.forEach(l -> l.loading(new LoadingEvent()));
        image = null;

        moebooruRepository.loadSampleAsync(post, !force, progress -> {
            String progressText = String.format("%s %.2f%%", loadingText, progress * 100);
            SwingUtilities.invokeLater(() -> {
                postLabel.setText(progressText);
            });
        })
            .observeOn(Schedulers.from(SwingUtilities::invokeLater))
            .doOnSuccess(img -> {
                this.image = img;
                showImage();
            })
            .doOnError(ex -> postLabel.setText(Localization.getString("unable_to_load")))
            .doAfterTerminate(() -> loadingListeners.forEach(l -> l.done(new LoadingEvent())))
            .subscribe();
    }

    private void showImage() {
        postLabel.setText("");
        resizeImage();
    }

    public void updateImage() {
        if (image != null) {
            resizeImage();
        }
    }

    private void resizeImage() {
        postLabel.setIcon(new ImageIcon(Utils.resizeImage(image, postLabel.getWidth(), postLabel.getHeight())));
        needResizeImage = false;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem addToFavoriteMenuItem;
    private javax.swing.JPanel centerPanel;
    private javax.swing.JLabel childrenLinkLabel;
    private javax.swing.JPanel childrenPanel;
    private javax.swing.JMenuItem copyPostImageMenuItem;
    private javax.swing.JMenuItem copyTagNameMenuItem;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JPanel jpegPanel;
    private javax.swing.JPanel originPanel;
    private javax.swing.JLabel parentLinkLabel;
    private javax.swing.JPanel parentPanel;
    private javax.swing.JLabel poolDownloadLabel;
    private javax.swing.JLabel poolLinkLabel;
    private javax.swing.JPanel poolPanel;
    private javax.swing.JLabel poolViewOnSiteLabel;
    private javax.swing.JLabel postLabel;
    private javax.swing.JPopupMenu postPopupMenu;
    private javax.swing.JMenuItem reloadMenuItem;
    private javax.swing.JPanel samplePanel;
    private javax.swing.JLabel sourceLinkLabel;
    private javax.swing.JPanel sourcePanel;
    private javax.swing.JLabel tagLoadingLabel;
    private javax.swing.JPanel tagPanel;
    private javax.swing.JPopupMenu tagPopupMenu;
    private javax.swing.JPanel toolPanel;
    private javax.swing.JMenuItem viewOnSiteMenuItem;
    // End of variables declaration//GEN-END:variables
}
