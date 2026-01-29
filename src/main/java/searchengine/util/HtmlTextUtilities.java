package searchengine.util;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;

public class HtmlTextUtilities {

    public static Logger logger = LoggerFactory.getLogger(HtmlTextUtilities.class);
    public static String extractTitle(String html) {
        Document doc = Jsoup.parse(html);
        return doc.title();
    }

    public static String extractSnippetContainingWord(String html, String query) {
//        // This matches full HTML elements containing the query
//        Pattern pattern = Pattern.compile(
//                "<[^>]+>[^<]*?" + Pattern.quote(query) + "[^<]*?</[^>]+>",
//                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
//        );
//        Matcher matcher = pattern.matcher(html);
//        return matcher.find() ? matcher.group() : null;
        // Remove all HTML tags


//        String text = html.replaceAll("<[^>]*>", " ");
        Document doc = Jsoup.parse(html);
        doc.select("script, style, nav, header, footer, aside, noscript, iframe, a, button, label[for=captcha_accept]").remove();// removes irrelevant parts of site
        String text = doc.text();

        Instant start = Instant.now();
        logger.info("Start time {}", start.toString());

        Pattern pattern = Pattern.compile("([^.?!]{0,170}\\b" + Pattern.quote(query) + "\\b[^.?!]*)([.?!]|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        String snippet = "";
        if (matcher.find()) {
            int desiredLength = 245;
            snippet = matchedSentenceSizeEditor(desiredLength, matcher, text);

            snippet = snippet.replaceAll("(?i)\\b" + Pattern.quote(query) + "\\b", "<b>" + query + "</b>");

        } else {
            try {
                LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
                LemmaFinder lemmaFinder = new LemmaFinder(luceneMorphology);

                String[] arrayOfWordsFromText = text.split("[^\\p{L}\\p{N}']+");
                logger.debug("Length of array: {}", arrayOfWordsFromText.length);

                Set<String> uniqueWordsFromText = new LinkedHashSet<>(Arrays.asList(arrayOfWordsFromText));
                logger.debug("Length of uniqueWordsFromText {}", uniqueWordsFromText.size());

                LinkedHashMap<String, List<String>> wordsAndLemmasFromText = new LinkedHashMap<>();
//                List<String> listOfWordsFromLemma = new ArrayList<>();
                int count2 = 1;
                for (String wordFromText : arrayOfWordsFromText) {
                    String lemmaOfWordFromText = "";
                    try {
                        lemmaOfWordFromText = luceneMorphology.getNormalForms(wordFromText.toLowerCase()).get(0);
                    } catch (Exception e){
                    }
                    if (wordsAndLemmasFromText.containsKey(lemmaOfWordFromText)) {
                        List<String> listOfWordsFromLemma = wordsAndLemmasFromText.get(lemmaOfWordFromText);
                        listOfWordsFromLemma.add(wordFromText);
                        wordsAndLemmasFromText.replace(lemmaOfWordFromText, listOfWordsFromLemma);
                    } else {
                        List<String> listOfWordsFromLemma = new ArrayList<>();
                        listOfWordsFromLemma.add(wordFromText);
                        wordsAndLemmasFromText.put(lemmaOfWordFromText, listOfWordsFromLemma);
                    }
                }

                String foundWord = "";
                List<String> queryLemmasList = new ArrayList<>();
                for (String queryWord : query.split("\\s")) {
//                    logger.info("Query word inside loop: {}", queryWord);
                    String lemmaOfQueryWord = luceneMorphology.getNormalForms(queryWord.toLowerCase()).get(0);
//                    logger.info("Lemma of query word inside loop: {}", lemmaOfQueryWord);
                    queryLemmasList.add(lemmaOfQueryWord);
                }
                for (String lemmaOfQueryWord : queryLemmasList) {
                    boolean lemmaOfQueryWordIsInText = wordsAndLemmasFromText.containsKey(lemmaOfQueryWord);
                    if (lemmaOfQueryWordIsInText) {
                        foundWord = wordsAndLemmasFromText.get(lemmaOfQueryWord).get(0);
//                        logger.info("found word insige loop queryLemmasList: {}", foundWord);
                        if (!foundWord.equals("не")) {
                            break;
                        }
                    }
                }

                if (foundWord.isEmpty()) {
                    return snippet;
                }
//                logger.info("Found word: {}", foundWord);
                pattern = Pattern.compile("([^.?!]{0,170}\\b" + Pattern.quote(foundWord) + "\\b[^.?!]*)([.?!]|$)",
                        Pattern.CASE_INSENSITIVE);
                Matcher matcher2 = pattern.matcher(text);
                matcher2.find();
//                if (!matcher2.find()) {
//                    return snippet;
//                }
                String matchedSentence = matcher2.group(1).trim();
                int desiredLength = 245;

                snippet = matchedSentenceSizeEditor(desiredLength, matcher2, text);

                HashSet<String> wordsFromFoundLemma = new HashSet<>();

                for (String lemmaOfQueryWord : queryLemmasList) {
//                    logger.info("lemma of query word: {}", lemmaOfQueryWord);
                    try {
                        wordsFromFoundLemma.addAll(wordsAndLemmasFromText.get(lemmaOfQueryWord));
                    } catch (NullPointerException e) {
                    }
                }
                for (String word : wordsFromFoundLemma) {
//                    logger.info("inside loop wordsFromFoundLemma, word: {}", word);
                    snippet = snippet.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "<b>" + word + "</b>");
                }
            } catch (IOException e) {
                logger.error("error in LuceneMorphology: ", e);
            }
        }

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        logger.info("Execution time {}", timeElapsed.toMillis());
        return snippet;

//                for (String queryWord : query.split("\\s")) {
//                    for (String wordFromText : arrayOfWordsFromText) {
//                        String lemmaOfWordFromText = "";
//                        String lemmaOfWordQuery = "";
//                        try {
//                            lemmaOfWordFromText = luceneMorphology.getNormalForms(wordFromText.toLowerCase()).get(0);
//                            lemmaOfWordQuery = luceneMorphology.getNormalForms(queryWord.toLowerCase()).get(0);
//                        } catch (Exception e) {
//                        }
//                        if (!lemmaOfWordFromText.isEmpty() && lemmaOfWordQuery.equals(lemmaOfWordFromText)) {
//                            logger.info("lemmas are equal of query: " + wordFromText + " - Lemma of query from text: " + lemmaOfWordFromText);
//                            pattern = Pattern.compile("([^.?!]*\\b" + Pattern.quote(wordFromText) + "\\b[^.?!]*[.?!])",
//                                    Pattern.CASE_INSENSITIVE);
//                            Matcher matcher2 = pattern.matcher(text);
//                            if (matcher2.find()) {
//                                String matchedSentence = matcher2.group(1).trim();
//                                int desiredLength = 245;
//
//                                snippet = matchedSentenceSizeEditor(desiredLength, matcher2, text);
//
//                                snippet = snippet.replaceAll("(?i)\\b" + Pattern.quote(wordFromText) + "\\b", "<b>" + wordFromText + "</b>");
//                                break;
//                            }
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                logger.error("error in LuceneMorphology: ", e);
//            }
//        }
//
//        // Return snippet around the query (if found)
//        return snippet;
    }

    public static String matchedSentenceSizeEditor(int desiredLength, Matcher matcher, String text) {
        String matchedSentence = matcher.group(1).trim();
        String snippet = "";
        if (matchedSentence.length() >= desiredLength) {
            int cutOff = matchedSentence.lastIndexOf(' ', desiredLength);
            if (cutOff == -1) cutOff = desiredLength;
            snippet = matchedSentence.substring(0, cutOff).trim() + "...";
        } else {
            // If sentence is too short, extend it from the end of sentence
            int sentenceEnd = matcher.end();
            int remainingChars = desiredLength - matchedSentence.length();
            int endIndex = Math.min(text.length(), sentenceEnd + remainingChars);

            String extra = text.substring(sentenceEnd, endIndex);
            int lastSpace = extra.lastIndexOf(' ');
            if (lastSpace != -1) {
                extra = extra.substring(0, lastSpace);
            }
            snippet = (matchedSentence + " " + extra).trim();

            if (snippet.length() < text.length()) {
                snippet += "...";
            }
        }
        return snippet;
    }

    public static String cleanHtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("script, style, nav, header, footer, aside, noscript, iframe, a, button, label[for=captcha_accept]").remove();
        return doc.text();
    }
}
