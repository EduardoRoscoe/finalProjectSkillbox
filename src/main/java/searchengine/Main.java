package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(Main.class);

        final List<String> list = List.of("dog", "cat", "hamster");

        list.stream()
                .filter(s -> {
                    System.out.println("filter: " + s);
                    return s.length() <= 3;
                })
                .map(s1 -> {
                    System.out.println("map: " + s1);
                    return s1.toUpperCase();
                })
                .sorted()
                .forEach(x -> {
                    System.out.println("forEach: " + x);
                });

        String hello = "hello everybody I'm John.";
        String regex = "[^a-zA-Z0-9]";
        System.out.println("regex: " + hello.split(regex).length);

        URL url = new URL("https://skillbox.ru:8080");
        logger.info("Url path: {}", url.getPath().isEmpty());
        logger.info("Url host: {}", url.getHost());
        logger.info("Url protocol: {}", url.getProtocol());
        logger.info("Url port: {}", url.getPort());
        logger.info("Url authority: {}", url.getAuthority());
        logger.info("Url query: {}", url.getQuery());
        logger.info("Url ref: {}", url.getRef());
        logger.info("Url userInfo: {}", url.getUserInfo());
        logger.info("Url toString: {}", url.toString());

        URL url1 = null;
        Optional<URL> optionalUrl = Optional.ofNullable(url1);
        logger.info("Optional url is empty: {}", optionalUrl.isEmpty());

        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            LemmaFinder lemmaFinder = new LemmaFinder(luceneMorphology);
            Set<String> lemmaSet = lemmaFinder.getLemmaSet("какаю");
            lemmaSet.forEach(System.out::println);
            List<String> normalForms = luceneMorphology.getNormalForms("не");
            normalForms.forEach(System.out::println);
            luceneMorphology.getMorphInfo("какаю").forEach(System.out::println);
            lemmaFinder.getLemmaSet("hello").forEach(System.out::println);

        } catch (IOException e) {
            throw e;
        }
        String word = "мне";
        String text = "Обо мне Java разработчик веду телеграм канал провожу встречи один из организаторов организатор эксперт Skillbox консультирую по Java Core, Spring Boot, провожу вебинары и подкасты увлекаюсь фотографией ";
        Pattern pattern = Pattern.compile("([^.?!]*\\b" + Pattern.quote(word) + "\\b[^.?!]*)([.?!]|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            logger.info("Pattern of exact word: {}", matcher.group(1).trim());
        }
        logger.info("Pattern of after if sentencte exact word: {}", matcher.group(1).trim());
    }
}
