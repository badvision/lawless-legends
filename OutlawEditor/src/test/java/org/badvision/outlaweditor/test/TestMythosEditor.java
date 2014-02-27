package org.badvision.outlaweditor.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringWriter;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.badvision.outlaweditor.data.xml.Block;
import org.badvision.outlaweditor.data.xml.GameData;
import org.badvision.outlaweditor.data.xml.Map;
import org.badvision.outlaweditor.data.xml.Map.Scripts;
import org.badvision.outlaweditor.data.xml.Script;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Various sanity checks of the Mythos script editing feature
 *
 * @author blurry
 */
public class TestMythosEditor {

    static public final String[] testData = {"testData/blocklytest1.xml", "testData/blocklytest2.xml"};
    static public final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    
    @Test
    public void deserializeTest() throws Exception {
        for (String path : testData) {
            System.out.println("testing " + path);
            Block theBlock = getBlock(path);
            assertNotNull(theBlock);
        }
    }

    @Test
    public void roundtripTest() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.badvision.outlaweditor.data.xml");
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        for (String path : testData) {
            System.out.println("testing " + path);
            Block theBlock = getBlock(path);
            StringWriter testWriter = new StringWriter();
            
            GameData d = new GameData();
            Map map = new Map();
            d.getMap().add(map);
            Script script = new Script();
            script.setName("name");
            script.setDescription("description");
            script.setBlock(theBlock);
            map.setScripts(new Scripts());
            map.getScripts().getEnterOrExitOrStepOn().add(new JAXBElement<Script>(new QName("outlaw", "enter"), Script.class, script));            
            m.marshal(d, testWriter);
            String testOutput = testWriter.getBuffer().toString();
            assertNotNull(testOutput);
//            assertSimilar(XML_HEADER + testOutput, getFileContents(path));
        }
    }

    public String getFileContents(String path) throws IOException {
        BufferedInputStream data = (BufferedInputStream) getClass().getClassLoader().getResource(path).getContent();
        byte[] buf = new byte[1024];
        StringBuilder contents = new StringBuilder();
        while (data.available() > 0) {
            int len = data.read(buf);
            if (len > 0) {
                String append = new String(buf, 0, len);
                contents.append(append);
            }
        }
        return contents.toString();
    }

    public void assertSimilar(String s1, String s2) {
        s1 = s1.replaceAll("\\s", "");
        s1 = s1.replaceAll("\\n", "");
        s2 = s2.replaceAll("\\s", "");
        s2 = s2.replaceAll("\\n", "");
        assertEquals(s1, s2);
    }

    public Block getBlock(String resourcePath) throws IOException {
        BufferedInputStream data = (BufferedInputStream) getClass().getClassLoader().getResource(resourcePath).getContent();
        assertNotNull(data);
        GameData gd = JAXB.unmarshal(data, GameData.class);
        assertNotNull(gd);
        assertNotNull(gd.getMap());
        assertEquals(1, gd.getMap().size());
        Scripts s = gd.getMap().get(0).getScripts();
        assertNotNull(s);
        assertNotNull(s.getEnterOrExitOrStepOn());
        assertEquals(1, s.getEnterOrExitOrStepOn().size());
        Script scr = (Script) s.getEnterOrExitOrStepOn().get(0).getValue();
        return scr.getBlock();
    }
}
