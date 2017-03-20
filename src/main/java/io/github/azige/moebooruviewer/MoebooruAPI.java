/*
 * Created 2015-11-28 23:39:10
 */
package io.github.azige.moebooruviewer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
 * @see https://yande.re/help/api
 * @see https://yande.re/wiki/show?title=api_v2
 */
@Component
public class MoebooruAPI{

    private static final int LIMIT = 20;
    private static final String POSTS_PATH = "/post.json";
    private static final String TAG_PATH = "/tag.json";
    private static final Logger logger = LoggerFactory.getLogger(MoebooruAPI.class);

    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private NetIO netIO;

    private Map<String, Tag> tagMap = new ConcurrentHashMap<>();
    private ObjectMapper mapper = new ObjectMapper();

    public MoebooruAPI(){
    }

    public MoebooruAPI(SiteConfig siteConfig, NetIO netIO){
        this.siteConfig = siteConfig;
        this.netIO = netIO;
    }

    public List<Post> listPosts() throws IOException{
        return listPosts(1, LIMIT);
    }

    public List<Post> listPosts(int page) throws IOException{
        return listPosts(page, LIMIT);
    }

    public List<Post> listPosts(int page, String... tags) throws IOException{
        return listPosts(page, LIMIT, tags);
    }

    public List<Post> listPosts(int page, int limit, String... tags) throws IOException{
        String parameters = String.format("api_version=2&include_pools=1&page=%d&limit=%d&tags=%s", page, limit,
            Stream.of(tags).reduce((s1, s2) -> s1 + "+" + s2).orElse("")
        );
        String url = siteConfig.getRootUrl() + POSTS_PATH + "?" + parameters;
        try (InputStream input = netIO.openStream(url)){
            JsonNode root = mapper.readTree(input);
            JsonNode posts = root.get("posts");
            Map<Integer, Post> postMap = StreamSupport.stream(posts.spliterator(), false)
                .map(post -> mapper.convertValue(post, Post.class))
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            JsonNode pools = root.get("pools");
            Map<Integer, Pool> poolMap = StreamSupport.stream(pools.spliterator(), false)
                .map(pool -> mapper.convertValue(pool, Pool.class))
                .collect(Collectors.toMap(Pool::getId, Function.identity(), (a, b) -> a));
            JsonNode poolPosts = root.get("pool_posts");
            StreamSupport.stream(poolPosts.spliterator(), false)
                .map(poolPost -> mapper.convertValue(poolPost, PoolPost.class))
                .forEach(poolPost -> {
                    Post post = postMap.get(poolPost.getPostId());
                    if (post != null){
                        post.setPool(poolMap.get(poolPost.getPoolId()));
                    }
                });
            return new ArrayList<>(postMap.values());
        }
    }

    // Need login to access
    public String getUrlOfPoolArchive(Pool pool){
        String url = siteConfig.getRootUrl()
            + String.format("/pool/zip/%d/%s.zip?jpeg=1", pool.getId(), pool.getName());
        return url;
    }

    public String getUrlOfPool(Pool pool){
        String url = siteConfig.getRootUrl()
            + String.format("/pool/show/%d", pool.getId());
        return url;
    }

    public Map<String, Tag> getTagMap(){
        return tagMap;
    }

    public void setTagMap(Map<String, Tag> tagMap){
        this.tagMap = tagMap;
    }

    public Tag findTag(String name) throws IOException{
        Tag tag = tagMap.get(name);
        if (tag != null){
            return tag;
        }
        String parameters = String.format("name=%s", name);
        String url = siteConfig.getRootUrl() + TAG_PATH + "?" + parameters;
        try (InputStream input = netIO.openStream(url)){
            JsonNode tags = mapper.readTree(input);
            for (JsonNode tagNode : tags){
                tag = mapper.convertValue(tagNode, Tag.class);
                if (tag.getName().equals(name)){
                    tagMap.put(name, tag);
                    return tag;
                }
            }
            return null;
        }
    }
}
