package codesquad.springcafe.database.article;

import codesquad.springcafe.model.Article;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ArticleMemoryDatabase implements ArticleDatabase {
    private final Map<Long, Article> store = new ConcurrentHashMap<>();
    private Long id = 0L;

    @Override
    public Article add(Article article) {
        article.setId(++id);
        store.put(article.getId(), article);
        return article;
    }

    @Override
    public Optional<Article> findBy(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Article> findAll(String nickname) {
        return findAll().stream()
                .filter(article -> article.hasSameWriter(nickname))
                .collect(Collectors.toList());
    }

    @Override
    public List<Article> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(Article article) {
        store.put(article.getId(), article);
    }

//    @Override
//    public void delete(Long id) {
//        store.remove(id);
//    }

    @Override
    public void clear() {
        store.clear();
    }
}
