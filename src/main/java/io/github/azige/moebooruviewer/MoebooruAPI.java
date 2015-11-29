/*
 * Created 2015-11-28 23:39:10
 */
package io.github.azige.moebooruviewer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Azige
 */
public class MoebooruAPI{

    private static final int LIMIT = 20;
    private static final String POSTS_PATH = "/post.json";
    private static final String TAG_PATH = "/tag.json";
    private static final Logger logger = LoggerFactory.getLogger(MoebooruAPI.class);

    private String rootUrl;
    private ObjectMapper mapper = new ObjectMapper();

    public MoebooruAPI(String site){
        this.rootUrl = site;
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
            Stream.of(tags).collect(StringBuilder::new, (sb, s) -> sb.append('+').append(s), (sb1, sb2) -> sb1.append('+').append(sb2)).toString()
        );
        URL url = new URL(rootUrl + POSTS_PATH + "?" + parameters);
        try (InputStream input = MoebooruViewer.getNetIO().openStream(url)){
            JsonNode posts = mapper.readTree(input);
            List<Post> postList = new ArrayList<>();
            posts.forEach(post -> postList.add(mapper.convertValue(post, Post.class)));
            return postList;
        }
    }

    public Tag findTag(String name) throws IOException{
        String parameters = String.format("name=%s", name);
        URL url = new URL(rootUrl + TAG_PATH + "?" + parameters);
        try (InputStream input = MoebooruViewer.getNetIO().openStream(url)){
            JsonNode tags = mapper.readTree(input);
            for (JsonNode tagNode : tags){
                Tag tag = mapper.convertValue(tagNode, Tag.class);
                if (tag.getName().equals(name)){
                    return tag;
                }
            }
            return null;
        }
    }
}
