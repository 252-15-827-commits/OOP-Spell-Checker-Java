import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

// 1. Trie Data Structure Node
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
}

// 2. Trie Dictionary Implementation
class TrieDictionary {
    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }
        current.isEndOfWord = true;
    }

    public boolean search(String word) {
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            TrieNode node = current.children.get(ch);
            if (node == null) return false;
            current = node;
        }
        return current.isEndOfWord;
    }

    // Levenshtein Distance Algorithm for Suggestions
    public List<String> getSuggestions(String misspelledWord, int maxDistance) {
        List<String> suggestions = new ArrayList<>();
        List<String> allWords = new ArrayList<>();
        collectAllWords(root, "", allWords);

        for (String word : allWords) {
            if (computeLevenshteinDistance(misspelledWord.toLowerCase(), word) <= maxDistance) {
                suggestions.add(word);
            }
        }
        return suggestions;
    }

    private void collectAllWords(TrieNode node, String currentWord, List<String> result) {
        if (node.isEndOfWord) {
            result.add(currentWord);
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            collectAllWords(entry.getValue(), currentWord + entry.getKey(), result);
        }
    }

    private int computeLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= str2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[str1.length()][str2.length()];
    }
}

// 3. Main GUI Application
public class SpellCheckerApp extends JFrame {
    private JTextPane textPane;
    private DefaultListModel<String> suggestionListModel;
    private JList<String> suggestionList;
    private TrieDictionary dictionary;
    private String currentSelectedWord = "";
    private int currentWordStart = -1;
    private int currentWordEnd = -1;

    public SpellCheckerApp() {
        setTitle("Microsoft Word Style Real-time Spell Checker");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize Dictionary
        dictionary = new TrieDictionary();
        loadSampleDictionary();

        // UI Setup
        textPane = new JTextPane();
        textPane.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        JScrollPane textScrollPane = new JScrollPane(textPane);

        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel);
        suggestionList.setFont(new Font("Arial", Font.BOLD, 14));
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
        suggestionScrollPane.setPreferredSize(new Dimension(200, 0));
        suggestionScrollPane.setBorder(BorderFactory.createTitledBorder("Suggestions"));

        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, suggestionScrollPane);
        splitPane.setDividerLocation(520);
        add(splitPane, BorderLayout.CENTER);

        // Event Listeners
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                checkSpellingAndHighlight();
            }
        });

        textPane.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                checkSpellingAndHighlight();
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    replaceSelectedWord();
                }
            }
        });
    }

    private void loadSampleDictionary() {
        boolean loaded = false;

        // src/dictionary.txt ফাইল পড়ার জন্য ClassLoader
        try (InputStream is = getClass().getResourceAsStream("/dictionary.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String word;
                    while ((word = reader.readLine()) != null) {
                        word = word.trim().toLowerCase();
                        if (!word.isEmpty()) {
                            dictionary.insert(word);
                        }
                    }
                    loaded = true;
                    System.out.println("Dictionary Loaded Successfully from src!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!loaded) {
            try (BufferedReader reader = new BufferedReader(new FileReader("dictionary.txt"))) {
                String word;
                while ((word = reader.readLine()) != null) {
                    word = word.trim().toLowerCase();
                    if (!word.isEmpty()) {
                        dictionary.insert(word);
                    }
                }
                loaded = true;
                System.out.println("Dictionary Loaded via Root Directory Path!");
            } catch (Exception ignored) {}
        }

        
        if (!loaded) {
            System.err.println("dictionary.txt .");
            String[] fallbackWords = {
                    "fun", "mother", "father", "matter", "brother", "sister",
                    "apple", "application", "banana", "cat", "catch", "dog",
                    "hello", "world", "java", "program", "programming", "algorithm",
                    "structure", "object", "oriented", "microsoft", "word", "spell",
                    "checker", "system", "quick", "brown", "fox", "jumps", "over",
                    "lazy", "daffodil", "university", "mahim"
            };
            for (String w : fallbackWords) {
                dictionary.insert(w);
            }
        }
    }

    private void checkSpellingAndHighlight() {
        StyledDocument doc = textPane.getStyledDocument();
        Style defaultStyle = textPane.addStyle("Default", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);
        StyleConstants.setUnderline(defaultStyle, false);

        Style wrongStyle = textPane.addStyle("Wrong", null);
        StyleConstants.setForeground(wrongStyle, Color.RED);
        StyleConstants.setUnderline(wrongStyle, true);

        String text = textPane.getText();
        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

        int caretPosition = textPane.getCaretPosition();
        String[] tokens = text.split("\\s+");

        int index = 0;
        suggestionListModel.clear();

        for (String token : tokens) {
            String cleanToken = token.replaceAll("[^a-zA-Z]", "");
            String searchToken = cleanToken.toLowerCase();

            int wordStart = text.indexOf(token, index);
            int wordEnd = wordStart + token.length();
            index = wordEnd;

            if (!cleanToken.isEmpty() && !dictionary.search(searchToken)) {
                doc.setCharacterAttributes(wordStart, token.length(), wrongStyle, false);

                if (caretPosition >= wordStart && caretPosition <= wordEnd) {
                    currentSelectedWord = cleanToken;
                    currentWordStart = wordStart;
                    currentWordEnd = wordEnd;
                    updateSuggestions(searchToken);
                }
            }
        }
    }

    private void updateSuggestions(String word) {
        suggestionListModel.clear();
        List<String> suggestions = dictionary.getSuggestions(word, 2);
        if (suggestions.isEmpty()) {
            suggestionListModel.addElement("No suggestions found");
        } else {
            for (String s : suggestions) {
                suggestionListModel.addElement(s);
            }
        }
    }

    private void replaceSelectedWord() {
        String selectedSuggestion = suggestionList.getSelectedValue();
        if (selectedSuggestion != null && !selectedSuggestion.equals("No suggestions found") && currentWordStart != -1) {
            try {
                StyledDocument doc = textPane.getStyledDocument();
                doc.remove(currentWordStart, currentWordEnd - currentWordStart);
                doc.insertString(currentWordStart, selectedSuggestion, null);
                suggestionListModel.clear();
                checkSpellingAndHighlight();
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SpellCheckerApp().setVisible(true);
        });
    }
}
