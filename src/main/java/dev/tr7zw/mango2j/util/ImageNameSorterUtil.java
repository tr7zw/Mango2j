package dev.tr7zw.mango2j.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageNameSorterUtil {

    public static final Comparator<String> COMPARATOR = new Comparator<String>() {
        public int compare(final String o1, final String o2) {
            if (o1.contains("/") && o2.contains("/")) {
                // We are in a zip with sub directories, first compare the parent dirs, 
                // then compare the name when it is the same
                int parentDir = COMPARATOR.compare(o1.substring(0, o1.lastIndexOf('/')),
                        o2.substring(0, o2.lastIndexOf('/')));
                if (parentDir != 0) {
                    return parentDir;
                }
                return compare(o1.substring(o1.lastIndexOf('/') + 1), o2.substring(o2.lastIndexOf('/') + 1));
            }
            boolean hasAlphaPrefix1 = hasAlphaPrefix(o1);
            boolean hasAlphaPrefix2 = hasAlphaPrefix(o2);

            if (hasAlphaPrefix1 && hasAlphaPrefix2) {
                return compareAlphaNumeric(o1, o2);
            } else if (!hasAlphaPrefix1 && !hasAlphaPrefix2) {
                return compareNumeric(o1, o2);
            } else {
                // One of the strings has an alphabetical prefix, the other does not.
                // Strings with alphabetical prefixes come first.
                return hasAlphaPrefix1 ? -1 : 1;
            }
        }
    };

    private static boolean hasAlphaPrefix(String s) {
        return s.matches("^[a-zA-Z]+.*");
    }

    private static int compareAlphaNumeric(String o1, String o2) {
        String[] parts1 = o1.split("_");
        String[] parts2 = o2.split("_");

        // Compare the alphabetical part first
        int alphaCompare = parts1[0].compareTo(parts2[0]);
        if (alphaCompare != 0) {
            return alphaCompare;
        }

        // If the alphabetical part is the same, compare the numeric parts
        List<Double> nums1 = extractDoubles(parts1);
        List<Double> nums2 = extractDoubles(parts2);

        // Compare each pair of numeric parts
        int numCompare = compareNumericLists(nums1, nums2);
        if (numCompare != 0) {
            return numCompare;
        }

        // If both strings have the same numeric parts, compare the rest of the string
        return o1.compareTo(o2);
    }

    private static int compareNumeric(String o1, String o2) {
        List<Double> nums1 = extractDoublesFromWholeString(o1);
        List<Double> nums2 = extractDoublesFromWholeString(o2);

        // Compare each pair of numeric parts
        int numCompare = compareNumericLists(nums1, nums2);
        if (numCompare != 0) {
            return numCompare;
        }

        // If both strings have the same numeric parts, compare the rest of the string
        return o1.compareTo(o2);
    }

    private static List<Double> extractDoubles(String[] parts) {
        List<Double> nums = new ArrayList<>();
        for (String part : parts) {
            nums.addAll(extractDoublesFromWholeString(part));
        }
        return nums;
    }

    private static List<Double> extractDoublesFromWholeString(String s) {
        List<Double> nums = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+\\.\\d+|\\d+").matcher(s);
        while (matcher.find()) {
            try {
                nums.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ex) {
                // Ignore invalid number format
            }
        }
        return nums;
    }

    private static int compareNumericLists(List<Double> nums1, List<Double> nums2) {
        int size1 = nums1.size();
        int size2 = nums2.size();
        int minSize = Math.min(size1, size2);

        for (int i = 0; i < minSize; i++) {
            int comparison = Double.compare(nums1.get(i), nums2.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }

        // If one list is longer, the shorter one is considered "less" since it has fewer numbers
        return Integer.compare(size1, size2);
    }

}
