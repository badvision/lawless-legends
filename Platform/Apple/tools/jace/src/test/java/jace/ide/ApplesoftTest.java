/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jace.ide;

import com.google.common.collect.Lists;
import jace.applesoft.ApplesoftProgram;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author blurry
 */
public class ApplesoftTest {
    
    public ApplesoftTest() {
    }
    
    static Byte[] lemonadeStandBinary;
    
    @BeforeClass
    public static void setUpClass() throws URISyntaxException, IOException {
        byte[] lemonadeStand = readBinary("/jace/lemonade_stand.bin");
        lemonadeStandBinary = ApplesoftProgram.toObjects(lemonadeStand);
    }
    
    public static byte[] readBinary(String path) throws IOException, URISyntaxException {
        Path resource = Paths.get(ApplesoftTest.class.getResource(path).toURI());
        return Files.readAllBytes(resource);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void deserializeBinaryTest() {
        ApplesoftProgram program = ApplesoftProgram.fromBinary(Lists.newArrayList(lemonadeStandBinary), 0x0801);
        assertNotNull(program);
        assertNotSame("", program.toString());
    }
    
    @Test
    public void roundTripStringComparisonTest() {
        ApplesoftProgram program = ApplesoftProgram.fromBinary(Lists.newArrayList(lemonadeStandBinary), 0x0801);
        String serialized = program.toString();
        ApplesoftProgram deserialized = ApplesoftProgram.fromString(serialized);
        String[] serializedLines = serialized.split("\\n");
        String[] researializedLines = deserialized.toString().split("\\n");
        for (int i=0; i < serializedLines.length; i++) {
            assertEquals("Line "+(i+1)+" should match", serializedLines[i], researializedLines[i]);
        }
    }
}
