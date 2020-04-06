package cz.cvut.kbss.nlp.gate;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class URIUtils {

    protected static MessageDigest md5;
    static {
        try {
            URIUtils.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String normalizedAnnotationType(String annotationType){
        return annotationType.trim().toLowerCase().replaceAll("\\s", "-");
    }

    public static String constructUri(String name, String prefix, String ns) throws Exception {
        return String.format("%s%s_%s", ns, prefix, name);
    }

    public static String constructUriUsingMD5FromNormalizedLabel(String lable, String prefix) throws Exception {
        // normalize string
        String norm = lable
                .trim()
                .replaceAll("\\s", "-");
        byte[] bytes = md5.digest(norm.getBytes("UTF-8"));

        norm = DatatypeConverter.printHexBinary(bytes).toUpperCase();
//        try {
//            norm = URLEncoder.encode(norm, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        // TODO: fix this simplified creation of uri's local name
        return String.format("%s-%s",  prefix, norm);
    }

    public static String toMD5(String label){
        byte[] bytes = md5.digest(label.getBytes(Charset.defaultCharset()));
        return DatatypeConverter.printHexBinary(bytes).toUpperCase();
    }
}
