package dev.tr7zw.mango2j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tr7zw.mango2j.util.ImageNameSorterUtil;

public class SortingTest {

    @Test
    public void testSimple() {
        List<String> list = Arrays.asList("001.png", "002.png", "003.png");
        List<String> copy = new ArrayList<String>(list);
        copy.sort(ImageNameSorterUtil.COMPARATOR);
        assertEquals(list, copy);
    }

    @Test
    public void testPrefixed() {
        List<String> list = Arrays.asList("A_001.png", "A_002.png", "A_003.png", "B_001.png", "B_002.png", "B_003.png");
        List<String> copy = new ArrayList<String>(list);
        copy.sort(ImageNameSorterUtil.COMPARATOR);
        assertEquals(list, copy);
    }

    @Test
    public void testMixed() {
        List<String> list = Arrays.asList("001_test_1.jpg", "002_test_1_Page1_Image1.jpg",
                "146_Test_2.jpg", "178_Test_2_Page32_Image1.jpg", "267_Test_3.jpg",
                "317_Test_3_Page50_Image1.jpg");
        List<String> copy = new ArrayList<String>(list);
        copy.sort(ImageNameSorterUtil.COMPARATOR);
        assertEquals(list, copy);
    }
    
    @Test
    public void testWithDot() {
        List<String> list = Arrays.asList("A_00.06", "A_001", "A_001.123", "A_002", "B_001", "B_003.123");
        List<String> copy = new ArrayList<String>(list);
        copy.sort(ImageNameSorterUtil.COMPARATOR);
        assertEquals(list, copy);
    }
    
    @Test
    public void testWithSubdir() {
        List<String> list = Arrays.asList("Sub Directory/7.png", "Sub Directory/22.png", "Sub Directory/105.png");
        List<String> copy = new ArrayList<String>(list);
        copy.sort(ImageNameSorterUtil.COMPARATOR);
        assertEquals(list, copy);
    }


}
