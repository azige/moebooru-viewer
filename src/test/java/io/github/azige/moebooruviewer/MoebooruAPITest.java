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
        // pool_post.json comes from https://yande.re/post.json?api_version=2&include_pools=1&page=1&limit=5&tags=pool:4094
        when(netIO.openStream(new URL("http://yande.re/post.json?api_version=2&include_pools=1&page=1&limit=5&tags=pool:4094"))).thenReturn(getClass().getResourceAsStream("/pool_post.json"));

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
}
