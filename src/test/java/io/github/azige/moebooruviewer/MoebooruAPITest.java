/*
 * Created 2015-11-29 0:16:59
 */
package io.github.azige.moebooruviewer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

/**
 *
 * @author Azige
 */
public class MoebooruAPITest{

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

    public void testSomeMethod() throws IOException{
        MoebooruAPI mapi = new MoebooruAPI(getClass().getClassLoader().getResource("").toString());
        System.out.println(mapi.listPosts());
    }

}
