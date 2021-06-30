package cz.cvut.kbss.common;

import com.drew.lang.StreamReader;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.WeakHashMap;

public class InMemoryURL {
    private final Map<URL, byte[]> contents = new WeakHashMap<>();
    private final URLStreamHandler handler = new InMemoryStreamHandler();

    private static InMemoryURL instance = null;

    public static synchronized InMemoryURL getInstance() {
        if(instance == null)
            instance = new InMemoryURL();
        return instance;
    }

    private InMemoryURL() {

    }

    public URL build(String path, String data) {
        try {
            return build(path, data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public URL build(String path, byte[] data) {
        URL url = build(path, handler);
        contents.put(url, data);
        return url;
    }

    protected URL build(String path){
        return build(path, (URLStreamHandler)null);
    }
    protected URL build(String path, URLStreamHandler streamHandler){
        try {
            if(streamHandler == null)
                return new URL("memory", "", -1, path);
            else
                return new URL("memory", "", -1, path, streamHandler);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void free(String path){
        free(build(path));
    }

    public void free(URL url){
        contents.remove(url);
    }

    private class InMemoryStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            if(!u.getProtocol().equals("memory")) {
                throw new IOException("Cannot handle protocol: " + u.getProtocol());
            }
            return new URLConnection(u) {

                private byte[] data = null;

                @Override
                public void connect() throws IOException {
                    initDataIfNeeded();
                    checkDataAvailability();
                    // Protected field from superclass
                    connected = true;
                }

                @Override
                public long getContentLengthLong() {
                    initDataIfNeeded();
                    if(data == null)
                        return 0;
                    return data.length;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    initDataIfNeeded();
                    checkDataAvailability();
                    return new ByteArrayInputStream(data);
                }

                private void initDataIfNeeded() {
                    if(data == null)
                        data = contents.get(u);
                }

                private void checkDataAvailability() throws IOException {
                    if(data == null)
                        throw new IOException("In-memory data cannot be found for: " + u.getPath());
                }

            };
        }
    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        String path = "my.path.haha";
        URL url = InMemoryURL.getInstance().build(path, "This is some in memory data!!!\nsecond line.");
        try(BufferedReader r = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))){
            r.lines().forEach(l -> System.out.println("line read from in memory resource : " + l));
        }

        try(BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()))){
            r.lines().forEach(l -> System.out.println("line read from in memory resource : " + l));
        }

        url = url.toURI().toURL();

        URI u = new URI("as");


        try(BufferedReader r = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))){
            r.lines().forEach(l -> System.out.println("line read from in memory resource : " + l));
        }

        try(BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()))){
            r.lines().forEach(l -> System.out.println("line read from in memory resource : " + l));
        }
    }
}
