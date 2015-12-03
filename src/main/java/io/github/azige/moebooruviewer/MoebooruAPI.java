/*
 * Created 2015-11-28 23:39:10
 */
package io.github.azige.moebooruviewer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Azige
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
        String parameters = String.format("page=%d&limit=%d&tags=%s", page, limit,
            Stream.of(tags).reduce((s1, s2) -> s1 + "+" + s2).orElse("")
        );
        URL url = new URL(siteConfig.getRootUrl() + POSTS_PATH + "?" + parameters);
        try (InputStream input = netIO.openStream(url)){
            JsonNode posts = mapper.readTree(input);
            List<Post> postList = new ArrayList<>();
            posts.forEach(post -> postList.add(mapper.convertValue(post, Post.class)));
            return postList;
        }
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
        URL url = new URL(siteConfig.getRootUrl() + TAG_PATH + "?" + parameters);
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
