package cz.cvut.kbss.nlp.gate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import gate.annotation.AnnotationFactory;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.DefaultAnnotationFactory;
import gate.annotation.NodeImpl;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static cz.cvut.kbss.nlp.gate.ExtractAnnotations.*;

public class LocateByText {
    private static final Logger LOG = LoggerFactory.getLogger(LocateByText.class);

//    protected long fromIndex = 0L;

    /**
     *
     * @param a
     * @param from
     * @param to
     */
    public Map<Integer, Integer> locateFromIn(Annotation a, Document from, Document to){
        String txt = getAnnotatedText(from, a);
        String txtContext = getContent(from, a.getStartNode().getOffset() - 5, a.getEndNode().getOffset() + 5);
        Map<Integer, Integer> locations = findTextIn(txt, txtContext, to,
                suggestFromIndex(a, from, to),
                suggestToIndex(a, from, to));

        if(locations.size() == 1){

        }else if(locations.size() > 1){
            LOG.info("move annotation failed - multiple locations [{}], text={}, id={} ",
                    locations.entrySet().stream().map(i -> i + "").collect(Collectors.joining(", ")),
                    txt, a.getId());
        }else {
            LOG.info("move annotation failed - text not found, text={}, id={} ", txt, a.getId());
        }
        return locations;
    }

    public long suggestFromIndex(Annotation a, Document from, Document to){
//        return 0L; // default implementation
        return targetPages.findPageIndexOrPrev(sourcePages.prevPage(a));
//        return sourcePages.pre
    }

    public long suggestToIndex(Annotation a, Document from, Document to){
//        return to.getContent().size(); // default implementation
        return targetPages.findPageIndexOrNext(sourcePages.nextPage(a));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Custom code /////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private Pages sourcePages;
    private Pages targetPages;

    private class Pages {

        public List<Integer> pageOffsets;
        public Map<Integer, String> pageSourceMap;
        public Map<String, Integer> inverse;


        public Pages(Map<Integer, String> pageSourceMap) {
            this.pageSourceMap = pageSourceMap;
            this.pageOffsets =  pageSourceMap.keySet().stream().sorted().collect(Collectors.toList());
            inverse = new HashMap<>();
            this.pageSourceMap.entrySet().forEach(e -> inverse.put(e.getValue(), e.getKey()));
        }

        public Pair<Integer, String> prevPage(Annotation a){
//            int startOfPageInText = pageOffsets.stream().mapToInt(i -> i).filter(i -> i < a.getStartNode().getOffset()).max().orElse(-1);
            int startOfPageInText = pageOffsets.get(-Collections.binarySearch(pageOffsets,a.getStartNode().getOffset().intValue())-3);
            String pageText = pageSourceMap.get(startOfPageInText);
            if(startOfPageInText >= 0 && pageText != null){
                return Pair.of(startOfPageInText, pageText);
            }
            return null;
        }

        public Pair<Integer, String> nextPage(Annotation a){
//            int index = pageOffsets.stream().mapToInt(i -> i).filter(i -> i > a.getEndNode().getOffset()).min().orElse(-1);
            int index = pageOffsets.get(-Collections.binarySearch(pageOffsets,a.getEndNode().getOffset().intValue())-2);
            String pageText = pageSourceMap.get(index);
            if(index >= 0 && pageText != null){
                return Pair.of(index, pageText);
            }
            return null;
        }

        public int findPageIndexOrPrev(Pair<Integer, String> p){
            Integer index = inverse.get(p.getValue());
            if(index == null){
//                for(int i = ps.pageOffsets.indexOf(p.getKey()); i > 0; i--){
//                    get
//                }
                return -1;
            }
            return index;
        }

        public int findPageIndexOrNext(Pair<Integer, String> p){
            Integer index = inverse.get(p.getValue());
            return index == null ? -1 : index;
        }
    }

    public Map<Integer, String> findSourcePages(Document d) {
        Map<Integer, String> map = new HashMap<>();

//        String even = "(Page\\s+.+\\s+-\\s+.+\\s+Rev\\. 8\\s+15-Dec-2017[EASaproved ]*Doc. No. 7.01.05-E)";
//        String odd = "(Doc. No. 7.01.05-E\\s+Rev\\. 8\\s+15-Dec-2017[EASaproved ]*Page\\s+.+\\s+-\\s+.+)";
//        String even = "(Page\\s+.+\\s+-\\s+.+\\s+Rev\\. 8\\s+15-Dec-2017[EASaproved\\s]*Doc. No. 7.01.05-E)";
//        String odd = "(Doc. No. 7.01.05-E\\s+Rev\\. 8\\s+15-Dec-2017[EASaproved\\s]*Page\\s+.+\\s+-\\s+.+)";
        String reg = "(Pag\\s*e\\s+[^\\s]++\\s+-\\s+[^\\s]+)";

        String str = getContent(d, 0L, d.getContent().size());
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(str);
        while (m != null) {
            if (m.find()) {
                map.put(m.start(), m.group(1).replaceAll("\n", ""));
            } else {
                m = null;
            }
        }

//        LOG.info("found {} pages \n{}", map.size(),
//                map.entrySet().stream()
//                        .sorted(Comparator.comparing(en -> en.getKey()))
//                        .map(en -> String.format("%08d - %s", en.getKey(), en.getValue()))
//                        .collect(Collectors.joining("\n"))
//        );
        LOG.info("found {} pages", map.size());

        sourcePages = new Pages(map);

        return map;
    }

    public void findTargetPages(Document d){
        Map<Integer, String> map = new HashMap<>();
        String str = getContent(d, 0L, d.getContent().size());
        sourcePages.pageSourceMap.entrySet().stream()
                .forEach(e -> {
                    int i = str.indexOf(e.getValue());
                    if(i > -1){
                        map.put(i, e.getValue());
                    }else{
                        LOG.info("cant find page {} in target document", e.getValue());
                    }
                });

//        LOG.info("found {} pages \n{}", map.size(),
//                map.entrySet().stream()
//                        .sorted(Comparator.comparing(en -> en.getKey()))
//                        .map(en -> String.format("%08d - %s", en.getKey(), en.getValue().replaceAll("\n", "")))
//                        .collect(Collectors.joining("\n"))
//        );
        LOG.info("found {} pages", map.size());

        targetPages = new Pages(map);
    }

    public Document process(Document from, Document to){
        findSourcePages(from);
        findTargetPages(to);
        AnnotationSet as = from.getAnnotations(ORIGINAL_MARKUP);
        int good = 0;
        int all = 0;

        AnnotationSet newas = new AnnotationSetImpl(to, as.getName());
        to.getNamedAnnotationSets().put(newas.getName(), newas);
        AnnotationFactory af = new DefaultAnnotationFactory();
        for(Annotation a : as){
            String at = a.getType();
            if(!MENTION.equals(at))
                continue;
            all ++;
            Map<Integer, Integer> locs = locateFromIn(a, from, to);
            if(locs.size() == 1){
                Map.Entry<Integer, Integer> e = locs.entrySet().iterator().next();
                NodeImpl n1 = new NodeImpl(a.getStartNode().getId(), (long)e.getKey());
                NodeImpl n2 = new NodeImpl(a.getEndNode().getId(), (long)(e.getKey() + e.getValue()));
                FeatureMap map = new SimpleFeatureMapImpl();
                map.putAll(a.getFeatures());
                af.createAnnotationInSet(newas, a.getId(), n1, n2, a.getType(), map);
                good ++;
//                fromIndex = locs.get(0);
            }

        }

        LOG.info("good = {}, bad = {}, total = {}", good, all - good, all);
        return to;
    }

    public static void main(String[] args) throws Exception {
        String root = "c:/Users/user/Documents/skola/projects/2019-msmt-inter-excelence/code/semantic-reliability/reliability-model/dev-b-input-analysis/001-data_blue-sky_manua-text-annotation/";
        Document from = readDocument(root+ "update_DA42-POH.xml");
        Document to = readDocument(root+ "DA42-POH.txt");
        Document fixed = new LocateByText().process(from, to);
        writeDocument(fixed, root + "DA42-POH-fixed.xml");
    }
}
