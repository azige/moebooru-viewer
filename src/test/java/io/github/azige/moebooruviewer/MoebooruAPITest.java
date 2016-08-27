/*
 * Copyright (C) 2016 Azige
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.azige.moebooruviewer;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Azige
 */
public class MoebooruAPITest{

    private SiteConfig siteConfig = SiteConfig.YANDERE;
    private NetIO netIO = mock(NetIO.class);
    private MoebooruAPI mapi = new MoebooruAPI(siteConfig, netIO);

    public MoebooruAPITest(){
    }

    @BeforeClass
    public static void setUpClass(){
    }

    @AfterClass
    public static void tearDownClass(){
    }

    @Before
    public void setUp(){
    }

    @After
    public void tearDown(){
    }

    @Test
    public void testListPostsWithPool() throws IOException{
        // postV2_pool4094.json comes from https://yande.re/post.json?api_version=2&include_pools=1&page=1&limit=5&tags=pool:4094
        String resourceName = "/postV2_pool4094.json";
        when(netIO.openStream(new URL("http://yande.re/post.json?api_version=2&include_pools=1&page=1&limit=5&tags=pool:4094"))).thenReturn(getClass().getResourceAsStream(resourceName));

        List<Post> posts = mapi.listPosts(1, 5, "pool:4094");
        assertThat(posts.size(), is(5));
        assertThat(posts, hasItem(allOf(
            hasProperty("id", is(360242)),
            hasProperty("tags", is("cameltoe erect_nipples ryohka school_swimsuit see_through swimsuits undressing"))
        )));
        Pool pool = posts.get(0).getPool();
        assertThat(pool, allOf(
            hasProperty("id", is(4094)),
            hasProperty("name", is("Otona_no_Moeoh_2016-Summer_-_Chotto_Daitanna_Mizugi_Hon"))
        ));
    }

    @Test
    public void testListPostsOrder() throws IOException{
        String resourceName = "/postV2_limit100.json";
        when(netIO.openStream(new URL("http://yande.re/post.json?api_version=2&include_pools=1&page=1&limit=100&tags="))).thenReturn(getClass().getResourceAsStream(resourceName));

        List<Post> posts = mapi.listPosts(1, 100);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode postNode = mapper.readTree(getClass().getResourceAsStream(resourceName)).get("posts");

        List<Integer> originalIdList = StreamSupport.stream(postNode.spliterator(), false)
            .map(node -> node.get("id").asInt())
            .collect(Collectors.toList());
        List<Integer> convertedIdList = posts.stream()
            .map(post -> post.getId())
            .collect(Collectors.toList());
        assertThat(convertedIdList, is(equalTo(originalIdList)));
    }
}
